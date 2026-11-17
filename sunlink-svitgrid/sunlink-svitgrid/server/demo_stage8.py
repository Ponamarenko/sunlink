
#!/usr/bin/env python3
"""
SunPortal Data Layer — stage 8 demonstration.

Modbus TCP -> DeyeReadonlyAdapter -> Polling Service -> Normalizer -> SQLite Repository.

Uses ONLY a mock Modbus device — no physical inverter, no Ebyte, no Solarman.
All data is labeled MOCK/DEMO — not live readings.
"""

from __future__ import annotations

import os
import sys
import tempfile
from datetime import datetime, timezone

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from domain.models import (  # noqa: E402
    ConfidenceLevel,
    MeasurementQuality,
    DataSource,
)
from domain.register_metadata import REGISTER_MAP
from domain.normalizer import normalize_register
from modbus.mock_client import MockModbusClient
from services.inverter_state import InverterState, ConnectionStatus
from services.modbus_polling_service import (  # noqa: E402
    ModbusPollingService,
    STAGE_7_REGISTERS,
)
from services.polling_config import PollingConfig
from storage.sqlite_repository import SQLiteMeasurementRepository

INVERTER_ID = "2507245705"  # from HA log — demo only

# Mock register values (stage-7 set) — representative demo values.
MOCK_REGISTERS = {
    90: 1365,    # dc_temperature -> 36.5 C (offset -100)
    91: 1424,    # ac_temperature -> 42.4 C (offset -100)
    183: 5322,   # battery_voltage -> 53.22 V
    184: 85,     # battery_soc -> 85 %
    186: 211,    # pv1_power -> 211 W
    187: 131,    # pv2_power -> 131 W
    188: 107,    # pv3_power -> 107 W
    190: 1915,   # battery_power -> 1915 W (provisional sign)
    191: 361,    # battery_current -> 3.61 A
}


def _wrap_adapter(mock_client):
    """
    Thin wrapper exposing DeyeReadonlyAdapter-like interface backed by MockModbusClient.
    read_register returns an object with .raw and .error fields.
    """
    class _Wrap:
        def __init__(self, client):
            self._client = client
            self.last_error = None
            self.connect_calls = 0
            self.disconnect_calls = 0
            self.read_calls = []

        @property
        def connected(self):
            return self._client.connected

        def connect(self):
            self.connect_calls += 1
            self.last_error = None
            ok = self._client.connect()
            if not ok:
                self.last_error = self._client.last_error
            return ok

        def disconnect(self):
            self.disconnect_calls += 1
            self._client.disconnect()

        def read_register(self, address):
            self.read_calls.append(address)
            raw = self._client.read_register(address)
            if raw is None:
                return type("RV", (), {"raw": None, "error": self._client.last_error or "read error"})()
            return type("RV", (), {"raw": raw, "error": None})()

    return _Wrap(mock_client)


def main() -> None:
    print("=" * 70)
    print("SunPortal Data Layer — stage 8 demonstration (MOCK/DEMO)")
    print("=" * 70)
    print()
    print("No physical inverter, Ebyte, or Solarman is contacted.")
    print("All data is mock/demonstration data.")
    print()

    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as f:
        db_path = f.name

    try:
        # 1. Create mock Modbus device.
        mock = MockModbusClient(registers=MOCK_REGISTERS, connect_ok=True)
        adapter = _wrap_adapter(mock)

        # 2. Configure and connect polling service.
        config = PollingConfig(
            inverter_id=INVERTER_ID,
            polling_interval=0.0,  # manual polling only
            connect_timeout=2.0,
            read_timeout=2.0,
            max_consecutive_failures=3,
            data_freshness_seconds=30.0,
        )
        repo = SQLiteMeasurementRepository(db_path)
        state = InverterState(
            inverter_id=INVERTER_ID,
            data_freshness_seconds=config.data_freshness_seconds,
        )
        service = ModbusPollingService(
            adapter=adapter,
            repository=repo,
            config=config,
            inverter_state=state,
        )

        print(f"Registers to poll: {STAGE_7_REGISTERS}")
        print(f"Register count: {len(STAGE_7_REGISTERS)}")
        print()

        # 3-5. Run several polling cycles, normalize, save to SQLite.
        for cycle in range(3):
            result = service.poll_once()
            print(f"Cycle {cycle + 1}: success={result.success}, "
                  f"measurements={len(result.measurements)}, "
                  f"errors={dict(list(result.errors.items())[:2])}, "
                  f"duration={result.duration_ms:.1f} ms")

        print()
        print("INVERTER STATE:")
        d = state.to_dict()
        for k, v in d.items():
            print(f"  {k}: {v}")
        print()

        # 6-7. Simulate a transient failure and recovery.
        print("SIMULATING TRANSIENT FAILURE (TCP refused)...")
        adapter.disconnect()
        mock.set_tcp_refused(True)
        result = service.poll_once()
        print(f"  Cycle during failure: success={result.success}, "
              f"state={state.connection_status}, error={state.last_error}")
        print("RECOVERING...")
        mock.set_tcp_refused(False)
        result = service.poll_once()
        print(f"  Cycle after recovery: success={result.success}, "
              f"state={state.connection_status}, error={state.last_error}")
        print()

        # 8-9. Print state and latest measurements.
        print("INVERTER STATE (final):")
        d = state.to_dict()
        for k, v in d.items():
            print(f"  {k}: {v}")
        print()

        print("LATEST MEASUREMENTS (from SQLite):")
        all_latest = repo.get_all_latest(INVERTER_ID)
        for m in all_latest:
            conf = m.confidence.value.upper()
            print(f"  {m.parameter_name:<20} {m.normalized_value:>8} {m.unit:<4} "
                  f"raw={m.raw_value:>6} [{conf}]")
        print()

        print("SAMPLE HISTORY (battery_voltage):")
        from datetime import timedelta
        start = datetime.now(timezone.utc) - timedelta(minutes=5)
        end = datetime.now(timezone.utc) + timedelta(minutes=5)
        hist = repo.get_history(INVERTER_ID, "battery_voltage", start, end)
        for m in hist:
            print(f"  {m.timestamp.isoformat()}  {m.normalized_value} {m.unit}  raw={m.raw_value}")
        print()

        repo.close()
    finally:
        os.unlink(db_path)

    print("NOTE: All values are MOCK/DEMO — not live readings from the inverter.")
    print("Temporary SQLite database has been deleted.")
    print("=" * 70)


if __name__ == "__main__":
    main()
