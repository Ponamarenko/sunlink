package com.sunlink.svitgrid.data.map

/**
 * Соответствие имён параметров backend (`/ws` data keys) полям категориальной
 * модели `/api/telemetry/full`. Неизвестные имена намеренно игнорируются —
 * неизвестное значение не должно появляться в UI как измеренное.
 */
object WsToFullFieldMap {
    private val map = mapOf(
        "battery_soc" to "battery.soc",
        "battery_voltage" to "battery.voltage",
        "battery_current" to "battery.current",
        "battery_power" to "battery.power",
        "battery_temperature" to "battery.temperature",
        "daily_battery_charge" to "battery.daily_charge_energy",
        "daily_battery_discharge" to "battery.daily_discharge_energy",
        "pv1_voltage" to "pv.pv1_voltage",
        "pv1_current" to "pv.pv1_current",
        "pv1_power" to "pv.pv1_power",
        "pv2_voltage" to "pv.pv2_voltage",
        "pv2_current" to "pv.pv2_current",
        "pv2_power" to "pv.pv2_power",
        "pv3_voltage" to "pv.pv3_voltage",
        "pv3_current" to "pv.pv3_current",
        "pv3_power" to "pv.pv3_power",
        "daily_pv_energy" to "pv.daily_energy",
        "total_pv_energy" to "pv.total_energy",
        "grid_power" to "grid.power",
        "grid_voltage_l1" to "grid.voltage_l1",
        "grid_voltage_l2" to "grid.voltage_l2",
        "grid_frequency" to "grid.frequency",
        "grid_import_energy" to "grid.import_energy",
        "grid_export_energy" to "grid.export_energy",
        "grid_connected" to "grid.connected",
        "load_power" to "load.power",
        "load_voltage" to "load.voltage",
        "load_current" to "load.current",
        "load_energy" to "load.energy",
        "dc_temperature" to "inverter.dc_temperature",
        "ac_temperature" to "inverter.ac_temperature"
    )

    operator fun get(parameter: String): String? = map[parameter]
}
