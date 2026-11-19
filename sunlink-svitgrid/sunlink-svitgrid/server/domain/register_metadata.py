"""
SunPortal Data Layer — register metadata and confidence levels.

Metadata is based on the SG02LP1 register map (docs/modbus-register-map.md,
level B) cross-checked against Sunsynk single-phase definitions and the
physical readings from stages 3–4 and the Home Assistant log (stage 6).

Only the registers needed for the first monitoring set are included, per
stage 7 requirements. Registers 166, 167, 169, 172, 175–178, 182, 192, 194
and 76–84 are deliberately NOT included without a separate decision.
"""

from __future__ import annotations

from dataclasses import dataclass
from .models import ConfidenceLevel


@dataclass(frozen=True)
class RegisterMetadata:
    """Metadata for one holding register."""

    address: int
    name: str
    data_type: str  # "U_WORD" | "S_WORD"
    scale: float
    unit: str
    confidence: ConfidenceLevel
    formula: str
    offset: float = 0.0
    is_signed: bool = False
    description: str = ""
    provisional_reason: str = ""


# ---------------------------------------------------------------------------
# Active register map — first monitoring set (stage 7)
# ---------------------------------------------------------------------------

REGISTER_MAP: dict[int, RegisterMetadata] = {
    183: RegisterMetadata(
        address=183,
        name="battery_voltage",
        data_type="U_WORD",
        scale=0.01,
        unit="V",
        confidence=ConfidenceLevel.CORROBORATED,
        formula="raw × 0.01",
        is_signed=False,
        description="Battery voltage",
    ),
    184: RegisterMetadata(
        address=184,
        name="battery_soc",
        data_type="U_WORD",
        scale=1.0,
        unit="%",
        confidence=ConfidenceLevel.CORROBORATED,
        formula="raw × 1",
        is_signed=False,
        description="Battery state of charge",
    ),
    186: RegisterMetadata(
        address=186,
        name="pv1_power",
        data_type="S_WORD",
        scale=1.0,
        unit="W",
        confidence=ConfidenceLevel.CORROBORATED,
        formula="signed_raw × 1",
        is_signed=True,
        description="PV1 power (first MPPT)",
    ),
    187: RegisterMetadata(
        address=187,
        name="pv2_power",
        data_type="S_WORD",
        scale=1.0,
        unit="W",
        confidence=ConfidenceLevel.CORROBORATED,
        formula="signed_raw × 1",
        is_signed=True,
        description="PV2 power (second MPPT)",
    ),
    188: RegisterMetadata(
        address=188,
        name="pv3_power",
        data_type="S_WORD",
        scale=1.0,
        unit="W",
        confidence=ConfidenceLevel.CORROBORATED,
        formula="signed_raw × 1",
        is_signed=True,
        description="PV3 power (third MPPT, 10/12 kW models)",
    ),
    191: RegisterMetadata(
        address=191,
        name="battery_current",
        data_type="S_WORD",
        scale=0.01,
        unit="A",
        confidence=ConfidenceLevel.CORROBORATED,
        formula="signed_raw × 0.01",
        is_signed=True,
        description="Battery current",
    ),
    190: RegisterMetadata(
        address=190,
        name="battery_power",
        data_type="S_WORD",
        scale=1.0,
        unit="W",
        confidence=ConfidenceLevel.PROVISIONAL,
        formula="signed_raw × 1",
        is_signed=True,
        description="Battery power — charge/discharge sign not yet confirmed",
        provisional_reason=(
            "Sign semantics for reg 190 on SG02LP1-AM3 not confirmed: "
            "local adapter interprets positive as charge, Sunsynk "
            "micro_hybrid_ess interprets positive as discharge. HA power "
            "balance is consistent with positive=charge but requires "
            "synchronous display verification."
        ),
    ),
    90: RegisterMetadata(
        address=90,
        name="dc_temperature",
        data_type="S_WORD",
        scale=0.1,
        unit="°C",
        confidence=ConfidenceLevel.PROVISIONAL,
        formula="signed_raw × 0.1 − 100",
        offset=100.0,
        is_signed=True,
        description="DC (inverter) temperature",
        provisional_reason=(
            "Offset −100 confirmed by Sunsynk TempSensor and kbialek "
            "deye-inverter-mqtt, but not yet verified by physical AM3 "
            "display reading. Raw value preserved independently."
        ),
    ),
    91: RegisterMetadata(
        address=91,
        name="ac_temperature",
        data_type="S_WORD",
        scale=0.1,
        unit="°C",
        confidence=ConfidenceLevel.PROVISIONAL,
        formula="signed_raw × 0.1 − 100",
        offset=100.0,
        is_signed=True,
        description="AC (inverter) temperature",
        provisional_reason=(
            "Offset −100 confirmed by Sunsynk TempSensor and kbialek "
            "deye-inverter-mqtt, but not yet verified by physical AM3 "
            "display reading. Raw value preserved independently."
        ),
    ),
}

# Registers deliberately excluded from the active map (stage 7 requirements):
#   166 (aux/gen power), 167 (CT L1), 169 (grid power — scale ×1 vs ×10
#     unresolved), 172 (grid CT), 175–178 (inverter/load power), 182
#     (battery temperature — non-physical 222 °C in HA log), 192
#     (load frequency), 194 (grid connected), 76–84 (energy counters),
#     248–279 (TOU). None of these are added without a separate decision.

# Этап 20: активный map физического polling содержит ТОЛЬКО регистр,
# подтверждённый реальным FC03-ответом Stage 19 (raw 0x0063 -> 99 %).
# Остальные регистры остаются в REGISTER_MAP со своими уровнями достоверности,
# но НЕ опрашиваются физически без отдельного разрешения.
ACTIVE_POLL_REGISTERS: dict[int, RegisterMetadata] = {184: REGISTER_MAP[184]}


__all__ = [
    "RegisterMetadata",
    "REGISTER_MAP",
    "ACTIVE_POLL_REGISTERS",
]
