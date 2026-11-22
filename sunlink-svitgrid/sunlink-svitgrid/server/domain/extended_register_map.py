"""
SunLink — расширенная карта регистров Deye SUN-12K-SG02LP1-EU-AM3.

Stage 40: описание MAXIMUM доступных параметров. Это НЕ разрешение на чтение.
`production_allowed=True` только у регистра 184 — единственного физически
подтверждённого. Все остальные записи имеют status DOCUMENTED/MODEL_COMPATIBLE
и требуют отдельного согласования перед включением в production polling.

Никаких физических запросов этот модуль не выполняет.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List, Optional

SOURCE_KBIALEK_PV = "kbialek/deye-inverter-mqtt docs/metric_group_deye_sg02lp1.md"
SOURCE_KBIALEK_BATTERY = "kbialek/deye-inverter-mqtt docs/metric_group_deye_sg02lp1_battery.md"
SOURCE_MARKLABS = "marklabs.pl Deye Modbus Complete Register List (SG02LP1)"
SOURCE_LOCAL = "docs/modbus-register-map.md (project)"


@dataclass(frozen=True)
class ExtendedRegister:
    address: int
    name: str
    group: str
    data_type: str
    register_count: int
    function_code: int
    is_signed: bool
    scale: float
    unit: str
    offset: float = 0.0
    candidate_scale: Optional[float] = None
    word_order: str = "LW_HW"
    confidence: str = "MODEL_COMPATIBLE"
    physically_verified: bool = False
    production_allowed: bool = False
    model: str = "SUN-12K-SG02LP1-EU-AM3"
    source: str = SOURCE_KBIALEK_PV
    description: str = ""


def _r(**kwargs) -> ExtendedRegister:
    return ExtendedRegister(**kwargs)


EXTENDED_REGISTERS: Dict[int, ExtendedRegister] = {
    # ---- Battery --------------------------------------------------------
    183: _r(address=183, production_allowed=True, name="battery_voltage", group="battery", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.01, unit="V", confidence="PHYSICALLY_VERIFIED", physically_verified=True, source=SOURCE_KBIALEK_BATTERY, description="Battery voltage"),
    184: _r(address=184, name="battery_soc", group="battery", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="%", confidence="PHYSICALLY_VERIFIED", physically_verified=True, production_allowed=True, source=SOURCE_KBIALEK_BATTERY, description="Battery SOC — active production register"),
    190: _r(address=190, name="battery_power", group="battery", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", candidate_scale=0.1, confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Battery power; ×0.1 is an unconfirmed local hypothesis"),
    191: _r(address=191, production_allowed=True, name="battery_current", group="battery", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=0.01, unit="A", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Battery current, sign semantics not display-verified"),
    182: _r(address=182, name="battery_temperature", group="battery", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="°C", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Battery temperature; historical HA value non-physical, needs verification"),
    189: _r(address=189, name="battery_status", group="battery", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="code", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Battery status code; value map not documented"),
    70: _r(address=70, production_allowed=True, name="daily_battery_charge", group="battery", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Daily battery charge energy"),
    71: _r(address=71, production_allowed=True, name="daily_battery_discharge", group="battery", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Daily battery discharge energy"),
    72: _r(address=72, production_allowed=True, name="total_battery_charge_low", group="battery", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Total charge energy, LW word at 72, HW at 73"),
    73: _r(address=73, name="total_battery_charge_high", group="battery", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Total charge energy high word"),
    74: _r(address=74, production_allowed=True, name="total_battery_discharge_low", group="battery", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Total discharge energy, LW at 74, HW at 75"),
    75: _r(address=75, name="total_battery_discharge_high", group="battery", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_BATTERY, description="Total discharge energy high word"),
    # ---- PV / MPPT ------------------------------------------------------
    109: _r(address=109, production_allowed=True, name="pv1_voltage", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="V", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    110: _r(address=110, production_allowed=True, name="pv1_current", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="A", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    186: _r(address=186, production_allowed=True, name="pv1_power", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="W", confidence="PHYSICALLY_VERIFIED", physically_verified=True, source=SOURCE_KBIALEK_PV),
    111: _r(address=111, production_allowed=True, name="pv2_voltage", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="V", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    112: _r(address=112, production_allowed=True, name="pv2_current", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="A", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    187: _r(address=187, production_allowed=True, name="pv2_power", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="W", confidence="PHYSICALLY_VERIFIED", physically_verified=True, source=SOURCE_KBIALEK_PV),
    113: _r(address=113, production_allowed=True, name="pv3_voltage", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="V", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV, description="12 kW AM3 has 3 MPPT according to official datasheet"),
    114: _r(address=114, production_allowed=True, name="pv3_current", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="A", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    188: _r(address=188, production_allowed=True, name="pv3_power", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="W", confidence="PHYSICALLY_VERIFIED", physically_verified=True, source=SOURCE_KBIALEK_PV),
    108: _r(address=108, production_allowed=True, name="daily_pv_energy", group="pv", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    96: _r(address=96, production_allowed=True, name="total_pv_energy_low", group="pv", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV, description="LW at 96, HW at 97"),
    97: _r(address=97, name="total_pv_energy_high", group="pv", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    # ---- Grid -----------------------------------------------------------
    150: _r(address=150, production_allowed=True, name="grid_voltage_l1", group="grid", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="V", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    151: _r(address=151, name="grid_voltage_l2", group="grid", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="V", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV, description="NOT frequency (legacy SunPortal mistake)"),
    164: _r(address=164, name="grid_current_l1", group="grid", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=0.01, unit="A", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    165: _r(address=165, name="grid_current_l2", group="grid", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=0.01, unit="A", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    169: _r(address=169, name="grid_power", group="grid", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=10.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_MARKLABS, description="SG02LP1 scale ×10 per group docs; ×1 used for SG01LP1 — needs HMI check"),
    167: _r(address=167, name="internal_ct_l1_power", group="grid", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    168: _r(address=168, name="internal_ct_l2_power", group="grid", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    170: _r(address=170, name="external_ct_l1_power", group="grid", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    171: _r(address=171, name="external_ct_l2_power", group="grid", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    192: _r(address=192, production_allowed=True, name="grid_frequency", group="grid", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.01, unit="Hz", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    76: _r(address=76, production_allowed=True, name="daily_grid_import", group="grid", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    77: _r(address=77, production_allowed=True, name="daily_grid_export", group="grid", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    78: _r(address=78, name="total_grid_import_low", group="grid", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV, description="LW at 78, HW at 80 (non-adjacent pair)"),
    80: _r(address=80, name="total_grid_import_high", group="grid", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    81: _r(address=81, name="total_grid_export_low", group="grid", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV, description="LW at 81, HW at 82"),
    82: _r(address=82, name="total_grid_export_high", group="grid", data_type="U_DWORD", register_count=2, function_code=3, is_signed=False, scale=0.1, unit="kWh", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    194: _r(address=194, name="grid_connected", group="grid", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="bool", confidence="MODEL_COMPATIBLE", source=SOURCE_LOCAL, description="Grid connection flag; value semantics not verified"),
    # ---- Load -----------------------------------------------------------
    175: _r(address=175, production_allowed=True, name="load_power", group="load", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV, description="Total load power (backward-compatible alias p_load)"),
    173: _r(address=173, name="inverter_l1_power", group="load", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    174: _r(address=174, name="inverter_l2_power", group="load", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    # ---- Inverter / temperatures / diagnostics --------------------------
    90: _r(address=90, name="dc_temperature", group="inverter", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=0.1, unit="°C", candidate_scale=0.1, confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV, description="Current maps: ×0.1 only; local historical hypothesis ×0.1−100 unconfirmed"),
    91: _r(address=91, name="ac_temperature", group="inverter", data_type="S_WORD", register_count=1, function_code=3, is_signed=True, scale=0.1, unit="°C", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    166: _r(address=166, name="micro_inverter_power", group="inverter", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="W", confidence="MODEL_COMPATIBLE", source=SOURCE_KBIALEK_PV),
    103: _r(address=103, name="fault_code_1", group="inverter", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="code", confidence="UNKNOWN", source=SOURCE_LOCAL, description="Format unknown for AM3 — do not decode as a diagnosis"),
    104: _r(address=104, name="fault_code_2", group="inverter", data_type="U_WORD", register_count=1, function_code=3, is_signed=False, scale=1.0, unit="code", confidence="UNKNOWN", source=SOURCE_LOCAL),
}


def production_allowed_registers() -> Dict[int, ExtendedRegister]:
    """Только явно разрешённые регистры. По умолчанию — один 184."""
    return {a: r for a, r in EXTENDED_REGISTERS.items() if r.production_allowed}


GROUPS = ("battery", "pv", "grid", "load", "inverter")


def build_read_plan(
    *,
    enabled_addresses: Optional[List[int]] = None,
    max_block: int = 8
) -> List[Dict[str, object]]:
    """План чтения по приоритетам, только разрешённые адреса.

    Без `enabled_addresses` план равен одному регистру 184. Явный список
    адресов не обходит `production_allowed` — он лишь выбирает подмножество.
    Непрерывные диапазоны объединяются только внутри одной группы и только
    если ВСЕ адреса диапазона разрешены.
    """
    allowed = production_allowed_registers()
    requested = sorted(set(enabled_addresses or allowed.keys()))
    selected = [a for a in requested if a in allowed]
    plan: List[Dict[str, object]] = []
    group_order = ["battery", "pv", "grid", "load", "inverter"]
    for group in group_order:
        addresses = [a for a in selected if EXTENDED_REGISTERS[a].group == group]
        i = 0
        while i < len(addresses):
            start = addresses[i]
            block = [start]
            j = i + 1
            while j < len(addresses) and len(block) < max_block and addresses[j] == block[-1] + 1:
                block.append(addresses[j])
                j += 1
            plan.append({
                "group": group,
                "start": start,
                "count": len(block),
                "addresses": block,
                "priority": "critical" if start == 184 else "normal"
            })
            i = j
    return plan


__all__ = [
    "ExtendedRegister", "EXTENDED_REGISTERS", "GROUPS",
    "production_allowed_registers", "build_read_plan",
]
