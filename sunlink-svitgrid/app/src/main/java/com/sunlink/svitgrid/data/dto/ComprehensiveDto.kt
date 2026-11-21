package com.sunlink.svitgrid.data.dto

import com.google.gson.annotations.SerializedName

/** Одно измерение в категориальной модели телеметрии. */
data class MeasurementValueDto(
    val raw_value: Int? = null,
    val normalized_value: Double? = null,
    val unit: String? = null,
    val confidence: String? = null,
    val quality: String? = null,
    val address: Int? = null,
    val timestamp: String? = null
)

/** Полный снимок `/api/telemetry/full`. Отсутствующие значения остаются null. */
data class TelemetryFullDto(
    val source: String? = null,
    val timestamp: String? = null,
    val connection_status: Boolean? = null,
    val stale: Boolean? = null,
    val inverter_id: String? = null,
    val categories: Map<String, Map<String, MeasurementValueDto?>>? = null
)

/** Агрегация `/api/stats/summary`. */
data class StatsSummaryDto(
    val parameter: String? = null,
    val inverter_id: String? = null,
    val start: String? = null,
    val end: String? = null,
    val count: Int = 0,
    val min: Double? = null,
    val max: Double? = null,
    val avg: Double? = null,
    val sum: Double? = null
)

data class HealthDto(
    val status: String? = null,
    val read_only: Boolean? = null,
    val source: String? = null,
    val connection_status: Boolean? = null,
    val timestamp: String? = null
)

data class VersionDto(
    val api: String? = null,
    val gateway: String? = null,
    val protocol: String? = null
)

/** Для обратной совместимости со старыми именами JSON-полей. */
data class LegacyTelemetryValueDto(
    @SerializedName("normalized_value") val normalizedValue: Double? = null
)
