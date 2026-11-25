package com.sunlink.svitgrid.ui.preview

import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.HistorySeriesPointDto
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.StatsSummaryDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.TelemetryQualityDto
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus
import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.model.success
import com.sunlink.svitgrid.data.repository.DataRepository

/** Isolated preview fixtures. Never used by production telemetry. */
object PreviewFixtures {
    private const val TS = "2026-11-25T09:24:00+00:00"

    private fun m(value: Double, unit: String, confidence: String, address: Int? = null) =
        MeasurementValueDto(
            raw_value = value.toInt(),
            normalized_value = value,
            unit = unit,
            confidence = confidence,
            quality = "valid",
            address = address,
            timestamp = TS
        )

    fun full(): TelemetryFullDto = TelemetryFullDto(
        source = "deye_modbus",
        timestamp = TS,
        connection_status = true,
        stale = false,
        inverter_id = "deye_sg02lp1_am3",
        categories = mapOf(
            "pv" to mapOf(
                "pv1_power" to m(1012.0, "W", "physically_verified", 186),
                "pv1_voltage" to m(312.0, "V", "physically_verified", 109),
                "pv1_current" to m(3.4, "A", "physically_verified", 110),
                "pv2_power" to m(196.0, "W", "physically_verified", 187),
                "pv2_voltage" to m(319.9, "V", "physically_verified", 111),
                "pv2_current" to m(0.6, "A", "physically_verified", 112),
                "pv3_power" to m(206.0, "W", "physically_verified", 188),
                "pv3_voltage" to m(286.9, "V", "physically_verified", 113),
                "pv3_current" to m(0.7, "A", "physically_verified", 114),
                "total_power" to m(1414.0, "W", "calculated"),
                "daily_energy" to m(2.2, "kWh", "model_compatible", 108),
                "total_energy" to m(733.1, "kWh", "model_compatible", 96)
            ),
            "battery" to mapOf(
                "soc" to m(93.0, "%", "corroborated", 184),
                "voltage" to m(53.1, "V", "physically_verified", 183),
                "current" to m(6.91, "A", "physically_verified", 191),
                "daily_charge_energy" to m(12.1, "kWh", "model_compatible", 70),
                "daily_discharge_energy" to m(4.2, "kWh", "model_compatible", 71),
                "total_charge_energy" to m(607.4, "kWh", "model_compatible", 72),
                "total_discharge_energy" to m(573.6, "kWh", "model_compatible", 74)
            ),
            "grid" to mapOf(
                "voltage_l1" to m(235.2, "V", "model_compatible", 150),
                "frequency" to m(50.01, "Hz", "model_compatible", 192),
                "import_energy" to m(15.4, "kWh", "model_compatible", 76),
                "export_energy" to m(0.0, "kWh", "model_compatible", 77)
            ),
            "load" to mapOf(
                "power" to m(525.0, "W", "verified", 175),
                "daily_energy" to m(6.8, "kWh", "model_compatible"),
                "energy" to m(158.4, "kWh", "model_compatible")
            )
        )
    )

    fun series(parameter: String): HistorySeriesDto {
        val points = (0..12).map { i ->
            val value = when (parameter) {
                "battery_soc" -> 80.0 + i
                "pv_total_power" -> (i * 120.0).coerceAtMost(1414.0)
                "load_power" -> 420.0 + (i % 4) * 45.0
                else -> 100.0 + i * 10
            }
            HistorySeriesPointDto(
                timestamp = "2026-11-25T${"%02d".format(6 + i / 2)}:${"%02d".format((i % 2) * 30)}:00+00:00",
                value = if (i == 7) null else value,
                gap = i == 7
            )
        }
        return HistorySeriesDto(
            parameter = parameter,
            period = "24h",
            unit = if (parameter == "battery_soc") "%" else "W",
            count = points.size,
            available_points = points.count { it.value != null },
            missing_points = points.count { it.value == null },
            points = points
        )
    }

    fun quality(): TelemetryQualityDto = TelemetryQualityDto(
        source = "deye_modbus", timestamp = TS, stale = false,
        known_parameters = 80, available_parameters = 49, unavailable_parameters = 31,
        active_registers = listOf(90, 91, 96, 108, 109, 110, 150, 175, 183, 184, 186, 187, 188, 192),
        polling_running = true, cycles = 132, failed_cycles = 2, missed_cycles = 0, last_cycle_ms = 312.0
    )

    fun inverterData(): InverterData = InverterData(
        pSun = 1414f, pLoad = 525f, pBatt = -120f, pGrid = -769f,
        batterySoc = 93f, batteryVoltage = 53.1f, batteryCurrent = 6.91f,
        pv1Power = 1012f, pv2Power = 196f, pv3Power = 206f,
        gridVoltageL1 = 235.2f, gridFrequency = 50.01f,
        dailyPvEnergy = 2.2f, dailyBatteryCharge = 12.1f, dailyBatteryDischarge = 4.2f,
        timestamp = TS, connectionStatus = true, source = "modbus_register",
        quality = "valid", confidence = "corroborated"
    )

    fun status(): InverterStatus = InverterStatus(
        online = true, lastUpdate = TS, inverterId = "deye_sg02lp1_am3", availableRegisters = 49
    )

    class Repository : DataRepository(null) {
        override suspend fun loadFullTelemetry(): Result<TelemetryFullDto> = success(full())
        override suspend fun loadHistorySeries(parameter: String, period: String): Result<HistorySeriesDto> = success(series(parameter))
        override suspend fun loadTelemetryQuality(): Result<TelemetryQualityDto> = success(quality())
        override suspend fun loadInverterData(): Result<Pair<InverterData, InverterStatus>> = success(inverterData() to status())
        override suspend fun loadStatus(): Result<InverterStatus> = success(status())
        override suspend fun loadStats(parameter: String, start: String, end: String): Result<StatsSummaryDto> = success(
            StatsSummaryDto(parameter = parameter, count = 13, min = 300.0, max = 1414.0, avg = 720.0, sum = 9360.0)
        )
    }
}

/** Minimal in-memory preferences for Paparazzi snapshots only. */
private class PreviewPrefs : android.content.SharedPreferences {
    private val map = mutableMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = map[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): android.content.SharedPreferences.Editor = object : android.content.SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        override fun putString(key: String?, value: String?) = apply { pending[key as String] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { pending[key as String] = values }
        override fun putInt(key: String?, value: Int) = apply { pending[key as String] = value }
        override fun putLong(key: String?, value: Long) = apply { pending[key as String] = value }
        override fun putFloat(key: String?, value: Float) = apply { pending[key as String] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { pending[key as String] = value }
        override fun remove(key: String?) = apply { pending.remove(key) }
        override fun clear() = apply { pending.clear() }
        override fun commit(): Boolean { map.putAll(pending); pending.clear(); return true }
        override fun apply() { commit() }
    }
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

    fun settings(): com.sunlink.svitgrid.data.SettingsRepository =
        com.sunlink.svitgrid.data.SettingsRepository(this).apply {
            language = "uk"; pollIntervalSeconds = 5; alertsEnabled = true
            lowSocWarningEnabled = true; lowSocThreshold = 20
        }
}

fun previewSettingsRepository(): com.sunlink.svitgrid.data.SettingsRepository = PreviewPrefs().settings()
