"""
Deye SUN-12K-SG02LP1-EU-AM3 (SG02LP1 family) — read-only Modbus adapter.

SECURITY: READ-ONLY. This module implements ONLY Modbus function code 03
(Read Holding Registers). It contains NO write capability whatsoever:

  * Function Code 05  (Write Single Coil)     - NOT IMPLEMENTED
  * Function Code 06  (Write Single Register) - NOT IMPLEMENTED
  * Function Code 15  (Write Multiple Coils)  - NOT IMPLEMENTED
  * Function Code 16  (Write Multiple Regs)   - NOT IMPLEMENTED

There is no write_register / write_registers / write_coil / any TOU or
battery-limit / power-limit / emergency command in this module.

Data provenance labels (see AdapterSnapshot.summary / RegisterValue.source):
  * "measured"     - read directly from a confirmed inverter register
  * "calculated"   - derived from measured values (never reported as measured)
  * "unconfirmed"  - register address not verified for this inverter

Register map basis: SG02LP1 family table (SUN-7.6/8/10/12K-SG02LP1-EU-AM2/AM3).
Confidence level B for the family; official per-model confirmation for AM3 is
still pending, so unconfirmed registers are never emitted as trusted values.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple

from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException, ConnectionException

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Data provenance
# ---------------------------------------------------------------------------
SOURCE_MEASURED = "measured"
SOURCE_CALCULATED = "calculated"
SOURCE_UNCONFIRMED = "unconfirmed"


@dataclass
class RegisterDefinition:
    """Description of a single holding register (or one half of a DWORD pair)."""

    address: int
    name: str
    data_type: str          # "U_WORD" | "S_WORD" | "U_DWORD"
    scale: float
    unit: str
    source: str             # measured / calculated / unconfirmed
    confirmed: bool         # verified against SG02LP1 map
    description: str = ""


@dataclass
class RegisterValue:
    """Result of reading one register (or one DWORD value)."""

    address: int
    name: str
    data_type: str
    scale: float
    unit: str
    source: str
    confirmed: bool
    raw: Optional[int] = None
    value: Optional[float] = None
    timestamp: Optional[str] = None
    error: Optional[str] = None

    @property
    def ok(self) -> bool:
        return self.error is None and self.value is not None


@dataclass
class AdapterSnapshot:
    """One read-only poll of all configured registers."""

    timestamp: str
    connection_ok: bool
    error: Optional[str]
    registers: Dict[str, RegisterValue]
    summary: Dict[str, object]


class DeyeReadonlyAdapter:
    """Read-only Modbus TCP adapter for Deye SG02LP1-AM3 inverter.

    Only Read Holding Registers (FC 03) is ever issued. There is no write
    path - write methods simply do not exist on this class.
    """

    # ------------------------------------------------------------------
    # Verified SG02LP1 register map (confidence B for family).
    # Unconfirmed registers (e.g. 182 battery_temp) are deliberately excluded
    # from the trusted set.
    # ------------------------------------------------------------------
    REGISTER_MAP: Dict[int, RegisterDefinition] = {
        # ---- PV (3 MPPT on 10/12 kW models) ----
        186: RegisterDefinition(186, "pv1_power", "U_WORD", 1.0, "W",
                                SOURCE_MEASURED, True, "PV1 Power"),
        187: RegisterDefinition(187, "pv2_power", "U_WORD", 1.0, "W",
                                SOURCE_MEASURED, True, "PV2 Power"),
        188: RegisterDefinition(188, "pv3_power", "U_WORD", 1.0, "W",
                                SOURCE_MEASURED, True, "PV3 Power"),
        109: RegisterDefinition(109, "pv1_voltage", "U_WORD", 0.1, "V",
                                SOURCE_MEASURED, True, "PV1 Voltage"),
        110: RegisterDefinition(110, "pv1_current", "U_WORD", 0.1, "A",
                                SOURCE_MEASURED, True, "PV1 Current"),
        111: RegisterDefinition(111, "pv2_voltage", "U_WORD", 0.1, "V",
                                SOURCE_MEASURED, True, "PV2 Voltage"),
        112: RegisterDefinition(112, "pv2_current", "U_WORD", 0.1, "A",
                                SOURCE_MEASURED, True, "PV2 Current"),
        113: RegisterDefinition(113, "pv3_voltage", "U_WORD", 0.1, "V",
                                SOURCE_MEASURED, True, "PV3 Voltage"),
        114: RegisterDefinition(114, "pv3_current", "U_WORD", 0.1, "A",
                                SOURCE_MEASURED, True, "PV3 Current"),
        # ---- Battery ----
        183: RegisterDefinition(183, "battery_voltage", "U_WORD", 0.01, "V",
                                SOURCE_MEASURED, True, "Battery Voltage"),
        184: RegisterDefinition(184, "battery_soc", "U_WORD", 1.0, "%",
                                SOURCE_MEASURED, True, "Battery SOC"),
        190: RegisterDefinition(190, "battery_power", "S_WORD", 1.0, "W",
                                SOURCE_MEASURED, True, "Battery Power"),
        191: RegisterDefinition(191, "battery_current", "S_WORD", 0.01, "A",
                                SOURCE_MEASURED, True, "Battery Current"),
        # NOTE: 182 battery_temperature is UNCONFIRMED for SG02LP1-AM3 and is
        # intentionally NOT present in REGISTER_MAP.
        # ---- Grid ----
        167: RegisterDefinition(167, "grid_ct_l1_power", "S_WORD", 1.0, "W",
                                SOURCE_MEASURED, True, "Grid CT L1 Power"),
        169: RegisterDefinition(169, "grid_total_power", "S_WORD", 10.0, "W",
                                SOURCE_MEASURED, True, "Total Grid Power"),
        # ---- Temperatures ----
        90: RegisterDefinition(90, "dc_temperature", "S_WORD", 0.1, "°C",
                               SOURCE_MEASURED, True, "DC Temperature"),
        91: RegisterDefinition(91, "ac_temperature", "S_WORD", 0.1, "°C",
                               SOURCE_MEASURED, True, "AC Temperature"),
        # ---- Energy (DWORD = 2 holding registers, LW at lower address) ----
        96:  RegisterDefinition(96,  "total_pv_production", "U_DWORD", 0.1, "kWh",
                                SOURCE_MEASURED, True,
                                "Total PV Production (low word 96, high word 97)"),
        97:  RegisterDefinition(97,  "total_pv_production_hi", "U_DWORD", 0.1, "kWh",
                                SOURCE_MEASURED, True,
                                "Total PV Production high word"),
        108: RegisterDefinition(108, "daily_pv_production", "U_WORD", 0.1, "kWh",
                                SOURCE_MEASURED, True, "Daily PV Production"),
        72:  RegisterDefinition(72,  "total_battery_charge", "U_DWORD", 0.1, "kWh",
                                SOURCE_MEASURED, True,
                                "Total Battery Charge (low word 72, high word 73)"),
        73:  RegisterDefinition(73,  "total_battery_charge_hi", "U_DWORD", 0.1, "kWh",
                                SOURCE_MEASURED, True,
                                "Total Battery Charge high word"),
        74:  RegisterDefinition(74,  "total_battery_discharge", "U_DWORD", 0.1, "kWh",
                                SOURCE_MEASURED, True,
                                "Total Battery Discharge (low word 74, high word 75)"),
        75:  RegisterDefinition(75,  "total_battery_discharge_hi", "U_DWORD", 0.1, "kWh",
                                SOURCE_MEASURED, True,
                                "Total Battery Discharge high word"),
    }

    # High words of DWORD pairs - read only as part of a pair, never alone.
    DWORD_HIGH_ADDRESSES = frozenset({97, 73, 75})
    DWORD_PAIRS: Tuple[Tuple[int, int], ...] = ((96, 97), (72, 73), (74, 75))

    # ------------------------------------------------------------------
    def __init__(self, host: str = "192.168.25.15", port: int = 502,
                 slave_id: int = 1, timeout: float = 2.0) -> None:
        self.host = host
        self.port = port
        self.slave_id = slave_id
        self.timeout = timeout
        self._client: Optional[ModbusTcpClient] = None
        self._connected = False
        self._last_successful_read: Optional[str] = None
        self._last_error: Optional[str] = None
        self._last_snapshot: Optional[AdapterSnapshot] = None

    # ------------------------------------------------------------------
    # Connection
    # ------------------------------------------------------------------
    def connect(self) -> bool:
        """Open the Modbus TCP connection (read-only client)."""
        self.disconnect()
        try:
            self._client = ModbusTcpClient(
                host=self.host,
                port=self.port,
                timeout=self.timeout,
            )
            ok = self._client.connect()
            self._connected = bool(ok)
            if self._connected:
                logger.info(
                    "Read-only Modbus connected %s:%s slave=%s",
                    self.host, self.port, self.slave_id,
                )
            else:
                self._last_error = f"connect failed to {self.host}:{self.port}"
                logger.error(self._last_error)
            return self._connected
        except Exception as exc:  # defensive: never crash the caller
            self._connected = False
            self._client = None
            self._last_error = f"connect error: {exc}"
            logger.exception(self._last_error)
            return False

    def disconnect(self) -> None:
        """Close the Modbus TCP connection."""
        if self._client is not None:
            try:
                self._client.close()
            except Exception:  # defensive
                pass
            self._client = None
        self._connected = False

    @property
    def connected(self) -> bool:
        return self._connected

    @property
    def last_successful_read(self) -> Optional[str]:
        return self._last_successful_read

    @property
    def last_error(self) -> Optional[str]:
        return self._last_error

    # ------------------------------------------------------------------
    # Read primitives (FC 03 only)
    # ------------------------------------------------------------------
    @staticmethod
    def _decode_word(raw: int, data_type: str) -> int:
        """Convert raw 16-bit value to signed when required."""
        if data_type == "S_WORD" and raw > 32767:
            return raw - 65536
        return raw

    def read_register(self, address: int) -> RegisterValue:
        """Read one holding register (FC 03, count=1). Read-only."""
        reg = self.REGISTER_MAP.get(address)
        if reg is None:
            return RegisterValue(
                address=address, name="unknown", data_type="", scale=1.0, unit="",
                source=SOURCE_UNCONFIRMED, confirmed=False,
                error=f"unknown register address {address}",
            )

        if not self._connected or self._client is None:
            return RegisterValue(
                address=address, name=reg.name, data_type=reg.data_type,
                scale=reg.scale, unit=reg.unit, source=reg.source,
                confirmed=reg.confirmed, error="not connected",
            )

        try:
            result = self._client.read_holding_registers(
                address=address, count=1, device_id=self.slave_id,
            )
            if result.isError():
                self._connected = False
                self._last_error = (
                    f"modbus error reading reg {address}: {result}"
                )
                return RegisterValue(
                    address=address, name=reg.name, data_type=reg.data_type,
                    scale=reg.scale, unit=reg.unit, source=reg.source,
                    confirmed=reg.confirmed, error=self._last_error,
                )
            raw = result.registers[0]
            decoded = self._decode_word(raw, reg.data_type)
            value = decoded * reg.scale
            self._last_successful_read = datetime.now(timezone.utc).isoformat()
            self._last_error = None
            return RegisterValue(
                address=address, name=reg.name, data_type=reg.data_type,
                scale=reg.scale, unit=reg.unit, source=reg.source,
                confirmed=reg.confirmed, raw=raw, value=value,
                timestamp=self._last_successful_read,
            )
        except (ModbusIOException, ConnectionException) as exc:
            self._connected = False
            self._last_error = f"modbus io error reading reg {address}: {exc}"
            logger.warning(self._last_error)
            return RegisterValue(
                address=address, name=reg.name, data_type=reg.data_type,
                scale=reg.scale, unit=reg.unit, source=reg.source,
                confirmed=reg.confirmed, error=self._last_error,
            )
        except Exception as exc:  # defensive
            self._connected = False
            self._last_error = f"error reading reg {address}: {exc}"
            logger.exception(self._last_error)
            return RegisterValue(
                address=address, name=reg.name, data_type=reg.data_type,
                scale=reg.scale, unit=reg.unit, source=reg.source,
                confirmed=reg.confirmed, error=self._last_error,
            )

    def read_dword_pair(self, low_address: int, high_address: int) -> RegisterValue:
        """Read two holding registers as one 32-bit value (LW,HW order).

        value = low_raw + (high_raw << 16), then * scale.
        """
        low_def = self.REGISTER_MAP[low_address]
        high_def = self.REGISTER_MAP[high_address]
        low = self.read_register(low_address)
        high = self.read_register(high_address)

        if low.ok and high.ok and low.raw is not None and high.raw is not None:
            combined = low.raw + (high.raw << 16)
            low.raw = combined
            low.value = combined * low_def.scale
            low.timestamp = self._last_successful_read
        elif low.error is None and high.error is not None:
            low.error = high.error
        elif low.error is not None and high.error is None:
            pass  # keep low's error
        return low

    # ------------------------------------------------------------------
    # Full snapshot
    # ------------------------------------------------------------------
    def read_all(self) -> AdapterSnapshot:
        """Read every configured register once. Read-only snapshot."""
        started = datetime.now(timezone.utc).isoformat()
        values: Dict[str, RegisterValue] = {}

        # Single-word registers (exclude DWORD high words).
        single_addrs = [
            a for a in self.REGISTER_MAP
            if a not in self.DWORD_HIGH_ADDRESSES
        ]
        for addr in single_addrs:
            rv = self.read_register(addr)
            values[rv.name] = rv

        # DWORD pairs -> one combined RegisterValue each (under low name).
        for low_addr, high_addr in self.DWORD_PAIRS:
            rv = self.read_dword_pair(low_addr, high_addr)
            values[rv.name] = rv

        # ---- Derived / calculated fields (never reported as measured) ----
        pv_powers = [values.get(f"pv{i}_power") for i in (1, 2, 3)]
        p_sun = None
        if all(v is not None and v.ok for v in pv_powers):
            p_sun = sum(v.value for v in pv_powers)  # type: ignore[arg-type]

        p_batt = None
        bp = values.get("battery_power")
        if bp is not None and bp.ok:
            p_batt = bp.value

        # Measured grid power: prefer confirmed register 169, then CT 167,
        # each explicitly labelled. Never fall back to a silent estimate.
        p_grid_measured = None
        p_grid_source = None
        gt = values.get("grid_total_power")
        if gt is not None and gt.ok:
            p_grid_measured = gt.value
            p_grid_source = "measured_register_169"
        gc = values.get("grid_ct_l1_power")
        if gc is not None and gc.ok and p_grid_measured is None:
            p_grid_measured = gc.value
            p_grid_source = "measured_register_167_ct"

        p_grid_estimated = None
        pl = values.get("total_load_power")  # not in verified map -> None
        if p_sun is not None and p_batt is not None and pl is not None:
            p_grid_estimated = pl - p_sun - p_batt

        summary: Dict[str, object] = {
            "timestamp": started,
            "connection_ok": self._connected,
            "last_successful_read": self._last_successful_read,
            "error": self._last_error,
            # PV
            "p_sun": p_sun,
            "p_sun_source": SOURCE_CALCULATED if p_sun is not None else None,
            # Battery
            "p_batt": p_batt,
            "p_batt_source": SOURCE_MEASURED if p_batt is not None else None,
            "battery_soc": values.get("battery_soc").value if values.get("battery_soc") and values.get("battery_soc").ok else None,
            "battery_voltage": values.get("battery_voltage").value if values.get("battery_voltage") and values.get("battery_voltage").ok else None,
            # Grid (measured vs calculated clearly separated)
            "p_grid_measured": p_grid_measured,
            "p_grid_source": p_grid_source,
            "p_grid_estimated": p_grid_estimated,
            "p_grid_estimated_source": SOURCE_CALCULATED if p_grid_estimated is not None else None,
            # Temperatures
            "dc_temperature": values.get("dc_temperature").value if values.get("dc_temperature") and values.get("dc_temperature").ok else None,
            "ac_temperature": values.get("ac_temperature").value if values.get("ac_temperature") and values.get("ac_temperature").ok else None,
            # Energy
            "total_pv_production": values.get("total_pv_production").value if values.get("total_pv_production") and values.get("total_pv_production").ok else None,
            "daily_pv_production": values.get("daily_pv_production").value if values.get("daily_pv_production") and values.get("daily_pv_production").ok else None,
            "total_battery_charge": values.get("total_battery_charge").value if values.get("total_battery_charge") and values.get("total_battery_charge").ok else None,
            "total_battery_discharge": values.get("total_battery_discharge").value if values.get("total_battery_discharge") and values.get("total_battery_discharge").ok else None,
        }

        snapshot = AdapterSnapshot(
            timestamp=started,
            connection_ok=self._connected,
            error=self._last_error,
            registers=values,
            summary=summary,
        )
        self._last_snapshot = snapshot
        return snapshot

    # ------------------------------------------------------------------
    # NOTE: this class intentionally has NO write methods.
    # write_register / write_registers / write_coil / any TOU, battery,
    # power-limit or emergency command is absent by design.
    # ------------------------------------------------------------------


__all__ = [
    "DeyeReadonlyAdapter",
    "RegisterDefinition",
    "RegisterValue",
    "AdapterSnapshot",
    "SOURCE_MEASURED",
    "SOURCE_CALCULATED",
    "SOURCE_UNCONFIRMED",
]
