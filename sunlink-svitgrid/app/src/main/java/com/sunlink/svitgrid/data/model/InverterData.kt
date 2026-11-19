package com.sunlink.svitgrid.data.model

/**
 * Доменная модель данных инвертора для UI.
 * Преобразуется из DTO (Answer от deye_gateway.py) в модель приложения.
 */
data class InverterData(
    val inverterId: String = "deye_sg02lp1_am3",
    val pSun: Float? = null,           // Общая мощность PV (Вт)
    val pLoad: Float? = null,          // Мощность нагрузки (Вт)
    val pBatt: Float? = null,          // Мощность батареи (Вт, + = заряд, - = разряд)
    val pGrid: Float? = null,          // Мощность сети (Вт, + = экспорт, - = импорт)
    val batterySoc: Float? = null,     // SOC батареи (%)
    val batteryVoltage: Float? = null, // Напряжение батареи (В)
    val batteryCurrent: Float? = null, // Ток батареи (А)
    val pv1Power: Float? = null,       // Мощность PV1 (Вт)
    val pv2Power: Float? = null,       // Мощность PV2 (Вт)
    val pv3Power: Float? = null,       // Мощность PV3 (Вт)
    val pv1Voltage: Float? = null,     // Напряжение PV1 (В)
    val pv1Current: Float? = null,     // Ток PV1 (А)
    val pv2Voltage: Float? = null,     // Напряжение PV2 (В)
    val pv2Current: Float? = null,     // Ток PV2 (А)
    val pv3Voltage: Float? = null,     // Напряжение PV3 (В)
    val pv3Current: Float? = null,     // Ток PV3 (А)
    val gridVoltageL1: Float? = null,  // Напряжение сети L1 (В)
    val dcTemperature: Float? = null,  // Температура DC (°C)
    val acTemperature: Float? = null,  // Температура AC (°C)
    val timestamp: String? = null,     // Время последнего измерения
    val connectionStatus: Boolean = false,
    val source: String = "unknown",    // "modbus_register" | "api_fallback" | "demo"
    val quality: String = "unverified", // "valid" | "unavailable" | "stale" | "error" | "unverified"
    val confidence: String = "unverified", // "corroborated" | "provisional" | "verified" | "unverified"
    // Этап 24: имена параметров с PROVISIONAL достоверностью
    val provisionalParameters: Set<String> = emptySet()
) {
    /** Есть ли хотя бы одно не-null значение измерения. */
    val hasMeasurements: Boolean
        get() = listOf(
            pSun, pLoad, pBatt, pGrid, batterySoc, batteryVoltage, batteryCurrent,
            pv1Power, pv2Power, pv3Power, pv1Voltage, pv1Current, pv2Voltage,
            pv2Current, pv3Voltage, pv3Current, gridVoltageL1, dcTemperature, acTemperature
        ).any { it != null }

    /** Направление батареи: заряд, разряд или неизвестно. */
    val batteryDirection: String
        get() = when {
            pBatt == null -> "—"
            pBatt > 5f -> "Заряд"
            pBatt < -5f -> "Розряд"
            else -> "Очікування"
        }

    /** Направление сети: экспорт, импорт или баланс. */
    val gridDirection: String
        get() = when {
            pGrid == null -> "—"
            pGrid > 0.5f -> "Експорт"
            pGrid < -0.5f -> "Імпорт"
            else -> "Баланс"
        }

    /** Преобразование в Map для совместимости с существующими UI-компонентами. */
    fun toMap(): Map<String, Any?> = mapOf(
        "p_sun" to pSun,
        "p_load" to pLoad,
        "p_batt" to pBatt,
        "p_grid" to pGrid,
        "batt_soc" to batterySoc,
        "battery_voltage" to batteryVoltage,
        "battery_current" to batteryCurrent,
        "pv1_power" to pv1Power,
        "pv2_power" to pv2Power,
        "pv3_power" to pv3Power,
        "pv1_voltage" to pv1Voltage,
        "pv1_current" to pv1Current,
        "pv2_voltage" to pv2Voltage,
        "pv2_current" to pv2Current,
        "pv3_voltage" to pv3Voltage,
        "pv3_current" to pv3Current,
        "grid_voltage_l1" to gridVoltageL1,
        "grid_voltage" to gridVoltageL1,
        "dc_temperature" to dcTemperature,
        "ac_temperature" to acTemperature,
        "timestamp" to timestamp,
        "connection_status" to connectionStatus,
        "source" to source,
        "quality" to quality,
        "confidence" to confidence,
        "provisional_parameters" to provisionalParameters.toList()
    )
}

/**
 * Состояние подключения к инвертору.
 */
data class InverterStatus(
    val online: Boolean = false,
    val lastUpdate: String? = null,
    val inverterId: String = "deye_sg02lp1_am3",
    val availableRegisters: Int = 0,
    val error: String? = null
)

/**
 * Историческое измерение для графика.
 */
data class HistoryPoint(
    val timestamp: String,
    val value: Float?,
    val unit: String
)

/**
 * История измерений по параметру.
 */
data class HistoryData(
    val parameter: String,
    val unit: String,
    val points: List<HistoryPoint> = emptyList()
)
