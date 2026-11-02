package app.sunportal.data.model

import com.google.gson.annotations.SerializedName

/**
 * Модель данных инвертора Deye
 */
data class InverterData(
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("pv_power") val pvPower: Double,
    @SerializedName("grid_power") val gridPower: Double,
    @SerializedName("load_power") val loadPower: Double,
    @SerializedName("battery_power") val batteryPower: Double,
    @SerializedName("battery_soc") val batterySoc: Double,
    @SerializedName("battery_voltage") val batteryVoltage: Double,
    @SerializedName("battery_current") val batteryCurrent: Double,
    @SerializedName("battery_temperature") val batteryTemperature: Double,
    @SerializedName("grid_voltage") val gridVoltage: Double,
    @SerializedName("grid_current") val gridCurrent: Double,
    @SerializedName("load_voltage") val loadVoltage: Double,
    @SerializedName("load_current") val loadCurrent: Double,
    @SerializedName("pv_voltage") val pvVoltage: Double,
    @SerializedName("pv_current") val pvCurrent: Double,
    @SerializedName("total_energy") val totalEnergy: Double,
    @SerializedName("today_energy") val todayEnergy: Double,
    @SerializedName("battery_capacity") val batteryCapacity: Double,
    @SerializedName("max_charge_power") val maxChargePower: Double,
    @SerializedName("max_discharge_power") val maxDischargePower: Double,
    @SerializedName("status") val status: String,
    @SerializedName("error_code") val errorCode: Int,
    @SerializedName("error_message") val errorMessage: String?
) {
    // Конструктор без параметров для Gson
    constructor() : this(
        timestamp = System.currentTimeMillis(),
        pvPower = 0.0,
        gridPower = 0.0,
        loadPower = 0.0,
        batteryPower = 0.0,
        batterySoc = 0.0,
        batteryVoltage = 0.0,
        batteryCurrent = 0.0,
        batteryTemperature = 0.0,
        gridVoltage = 0.0,
        gridCurrent = 0.0,
        loadVoltage = 0.0,
        loadCurrent = 0.0,
        pvVoltage = 0.0,
        pvCurrent = 0.0,
        totalEnergy = 0.0,
        todayEnergy = 0.0,
        batteryCapacity = 0.0,
        maxChargePower = 0.0,
        maxDischargePower = 0.0,
        status = "unknown",
        errorCode = 0,
        errorMessage = null
    )
    
    // Вспомогательные свойства
    val isCharging: Boolean get() = batteryPower > 0
    val isDischarging: Boolean get() = batteryPower < 0
    val isGridExporting: Boolean get() = gridPower > 0
    val isGridImporting: Boolean get() = gridPower < 0
    
    // Форматированные значения
    val formattedPvPower: String get() = "${pvPower.toInt()} Вт"
    val formattedGridPower: String get() = "${gridPower.toInt()} Вт"
    val formattedLoadPower: String get() = "${loadPower.toInt()} Вт"
    val formattedBatteryPower: String get() = "${batteryPower.toInt()} Вт"
    val formattedBatterySoc: String get() = "${batterySoc.toInt()}%"
    val formattedBatteryVoltage: String get() = "${batteryVoltage.toInt()} В"
    val formattedBatteryCurrent: String get() = "${batteryCurrent.toInt()} А"
    val formattedBatteryTemperature: String get() = "${batteryTemperature.toInt()}°C"
    val formattedTotalEnergy: String get() = "${totalEnergy.toInt()} кВт·ч"
    val formattedTodayEnergy: String get() = "${todayEnergy.toInt()} кВт·ч"
    val formattedBatteryCapacity: String get() = "${batteryCapacity.toInt()} кВт·ч"
    
    // Статус системы
    val systemStatus: String get() = when {
        status == "error" -> "Ошибка"
        status == "warning" -> "Предупреждение"
        status == "normal" -> "Нормально"
        else -> "Неизвестно"
    }
    
    // Цвет статуса
    val statusColor: String get() = when {
        status == "error" -> "#F44336"
        status == "warning" -> "#FFC107"
        status == "normal" -> "#4CAF50"
        else -> "#9E9E9E"
    }
}