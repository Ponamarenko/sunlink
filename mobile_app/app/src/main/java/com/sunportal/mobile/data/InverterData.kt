package com.sunportal.mobile.data

import java.util.Date

/**
 * Данные инвертора в реальном времени
 * @param timestamp Время получения данных
 * @param batterySOC Заряд батареи (%)
 * @param batteryVoltage Напряжение батареи (V)
 * @param batteryTemp Температура батареи (°C)
 * @param batteryPower Мощность батареи (W)
 * @param batteryCurrent Ток батареи (A)
 * @param pv1Power Мощность PV1 (W)
 * @param pv2Power Мощность PV2 (W)
 * @param pv3Power Мощность PV3 (W)
 * @param gridVoltage Напряжение сети (V)
 * @param gridFrequency Частота сети (Hz)
 * @param gridInverterPower Мощность инвертора (W)
 * @param totalLoadPower Общая нагрузка (W)
 * @param totalPVProduction Общая генерация (kWh)
 * @param dailyPVProduction Дневная генерация (kWh)
 * @param dailyBuyEnergy Дневная покупка (kWh)
 * @param dailySellEnergy Дневная продажа (kWh)
 */
data class InverterData(
    val timestamp: Date = Date(),
    val batterySOC: Double = 0.0,
    val batteryVoltage: Double = 0.0,
    val batteryTemp: Double = 0.0,
    val batteryPower: Double = 0.0,
    val batteryCurrent: Double = 0.0,
    val pv1Power: Double = 0.0,
    val pv2Power: Double = 0.0,
    val pv3Power: Double = 0.0,
    val gridVoltage: Double = 0.0,
    val gridFrequency: Double = 0.0,
    val gridInverterPower: Double = 0.0,
    val totalLoadPower: Double = 0.0,
    val totalPVProduction: Double = 0.0,
    val dailyPVProduction: Double = 0.0,
    val dailyBuyEnergy: Double = 0.0,
    val dailySellEnergy: Double = 0.0
) {
    fun getTotalPVPower(): Double = pv1Power + pv2Power + pv3Power
    
    fun getGridPower(): Double = gridInverterPower
    
    fun getLoadPower(): Double = totalLoadPower
    
    fun getBatteryStatus(): BatteryStatus {
        return when {
            batterySOC > 80 -> BatteryStatus.HIGH
            batterySOC > 20 -> BatteryStatus.NORMAL
            else -> BatteryStatus.LOW
        }
    }
    
    fun getGridStatus(): GridStatus {
        return when {
            gridInverterPower > 100 -> GridStatus.EXPORTING
            gridInverterPower < -100 -> GridStatus.IMPORTING
            else -> GridStatus.STANDBY
        }
    }
}

enum class BatteryStatus {
    HIGH, NORMAL, LOW, CRITICAL
}

enum class GridStatus {
    EXPORTING, IMPORTING, STANDBY
}

/**
 * Исторические данные для графиков
 */
data class HistoricalData(
    val timestamp: Date,
    val value: Double,
    val type: DataType
)

enum class DataType {
    BATTERY_SOC,
    BATTERY_POWER,
    PV_POWER,
    GRID_POWER,
    LOAD_POWER,
    GRID_VOLTAGE,
    BATTERY_TEMP
}

/**
 * События и уведомления
 */
data class SystemEvent(
    val id: String,
    val timestamp: Date,
    val type: EventType,
    val message: String,
    val severity: EventSeverity,
    val resolved: Boolean = false
)

enum class EventType {
    BATTERY_LOW,
    BATTERY_HIGH,
    GRID_OVERVOLTAGE,
    GRID_UNDERVOLTAGE,
    INVERTER_ERROR,
    PV_PANEL_ERROR,
    COMMUNICATION_ERROR,
    SYSTEM_WARNING
}

enum class EventSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Настройки управления инвертором
 */
data class InverterSettings(
    val maxBatteryChargeCurrent: Int = 50, // А
    val maxBatteryDischargeCurrent: Int = 50, // А
    val maxSolarPowerLimit: Int = 10000, // Вт
    val touEnabled: Boolean = false,
    val emergencyStop: Boolean = false,
    val solarPowerLimit: Int = 12000, // Вт
    val batteryChargeCurrent: Int = 50, // А
    val batteryDischargeCurrent: Int = 50 // А
)

/**
 * Энергетическая статистика за день
 */
data class EnergyStats(
    val date: Date,
    val pvGeneration: Double, // kWh
    val gridImport: Double, // kWh
    val gridExport: Double, // kWh
    val batteryCharge: Double, // kWh
    val batteryDischarge: Double, // kWh
    val consumption: Double // kWh
)

/**
 * Прогноз солнечной генерации
 */
data class SolarForecast(
    val date: Date,
    val hourlyForecast: List<HourlyForecast>,
    val totalForecast: Double // kWh
)

data class HourlyForecast(
    val hour: Int,
    val forecast: Double // kWh
)

/**
 * Расписание управления
 */
data class Schedule(
    val id: String,
    val name: String,
    val events: List<ScheduleEvent>
)

data class ScheduleEvent(
    val id: String,
    val time: String, // HH:mm
    val type: ScheduleEventType,
    val action: String,
    val enabled: Boolean = true
)

enum class ScheduleEventType {
    CHARGE_BATTERY,
    DISCHARGE_BATTERY,
    SOLAR_LIMIT,
    EMERGENCY_STOP,
    GRID_SWITCH
}