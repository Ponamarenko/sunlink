package com.sunlink.svitgrid.data.dto

/**
 * Плоский ответ /api/live/raw от deye_gateway.py (этап 10).
 * Поля опциональны — сервер может вернуть только подмножество.
 */
data class LiveRawDto(
    val p_sun: Double? = null,
    val p_load: Double? = null,
    val p_batt: Double? = null,
    val p_grid: Double? = null,
    val batt_soc: Double? = null,
    val battery_voltage: Double? = null,
    val battery_current: Double? = null,
    val dc_temperature: Double? = null,
    val ac_temperature: Double? = null,
    val pv1_power: Double? = null,
    val pv2_power: Double? = null,
    val pv3_power: Double? = null,
    val p_string1: Double? = null,
    val p_string2: Double? = null,
    val p_string3: Double? = null,
    val pv1_voltage: Double? = null,
    val pv1_current: Double? = null,
    val pv2_voltage: Double? = null,
    val pv2_current: Double? = null,
    val pv3_voltage: Double? = null,
    val pv3_current: Double? = null,
    val grid_voltage_l1: Double? = null,
    val grid_voltage: Double? = null,
    val timestamp: String? = null,
    val connection_status: Boolean? = null,
    val error: String? = null,
    // Этап 12: реальные поля ответа deye_gateway.py
    val inverter_id: String? = null,
    val available_registers: Int? = null,
    val source: String? = null
)

/**
 * Ответ /api/measurements/latest.
 */
data class MeasurementsLatestDto(
    val inverter_id: String? = null,
    val count: Int = 0,
    val measurements: List<MeasurementDto> = emptyList()
)

/**
 * Одно измерение из /api/measurements/latest или /api/measurements/history.
 */
data class MeasurementDto(
    val inverter_id: String? = null,
    val register_address: Int? = null,
    val parameter_name: String? = null,
    val raw_value: Int? = null,
    val normalized_value: Double? = null,
    val unit: String? = null,
    val timestamp: String? = null,
    val quality: String? = null,
    val confidence: String? = null,
    val source: String? = null,
    val description: String? = null
)

/**
 * Ответ /api/measurements/history.
 */
data class MeasurementsHistoryDto(
    val inverter_id: String? = null,
    val parameter: String? = null,
    val start: String? = null,
    val end: String? = null,
    val count: Int = 0,
    val measurements: List<MeasurementDto> = emptyList()
)

/**
 * Ответ /api/inverters (этап 12).
 * Gateway одноканальный — список содержит один реальный инвертор.
 */
data class InvertersDto(
    val inverters: List<InverterDto> = emptyList(),
    val count: Int = 0,
    val source: String? = null
)

data class InverterDto(
    val inverter_id: String? = null,
    val online: Boolean? = null,
    val available_registers: Int? = null,
    val source: String? = null
)

/**
 * Ответ /status.
 */
data class StatusDto(
    val connection_status: Boolean? = null,
    val last_update: String? = null,
    val inverter_id: String? = null,
    val available_registers: Int = 0,
    val registers: List<Int> = emptyList(),
    val source: String? = null
)

/**
 * Описание одного регистра из /registers.
 */
data class RegisterDto(
    val address: Int,
    val name: String,
    val data_type: String,
    val scale: Double,
    val unit: String,
    val confidence: String,
    val provisional_reason: String,
    val description: String
)

/**
 * Ответ /registers.
 */
data class RegistersDto(
    val registers: List<RegisterDto> = emptyList(),
    val count: Int = 0,
    val source: String? = null
)

/**
 * Ответ /live (включает вложенный data + плоские поля).
 */
data class LiveDto(
    val data: Map<String, Any>? = null,
    val status: Map<String, Any>? = null,
    val p_sun: Double? = null,
    val p_load: Double? = null,
    val p_batt: Double? = null,
    val p_grid: Double? = null,
    val timestamp: String? = null
)
