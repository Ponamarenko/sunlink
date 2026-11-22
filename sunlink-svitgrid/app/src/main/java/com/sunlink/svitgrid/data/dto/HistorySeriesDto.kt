package com.sunlink.svitgrid.data.dto

/** Точка реального временного ряда; `value=null` означает пропуск измерения. */
data class HistorySeriesPointDto(
    val timestamp: String,
    val value: Double? = null,
    val gap: Boolean = false,
    val samples: Int? = null
)

data class HistorySeriesDto(
    val inverter_id: String? = null,
    val parameter: String? = null,
    val period: String? = null,
    val start: String? = null,
    val end: String? = null,
    val interval_seconds: Int = 0,
    val unit: String? = null,
    val count: Int = 0,
    val available_points: Int = 0,
    val missing_points: Int = 0,
    val points: List<HistorySeriesPointDto> = emptyList()
)
