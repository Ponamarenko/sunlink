package com.sunlink.svitgrid.data.dto

data class CategoryQualityDto(
    val total: Int = 0,
    val available: Int = 0
)

data class ExtendedPollingStatusDto(
    val enabled: Boolean? = null,
    val jobs: Int? = null,
    val dry_run: Boolean? = null,
    val executed: List<Int> = emptyList()
)

data class TelemetryQualityDto(
    val source: String? = null,
    val timestamp: String? = null,
    val stale: Boolean? = null,
    val known_parameters: Int = 0,
    val available_parameters: Int = 0,
    val unavailable_parameters: Int = 0,
    val by_confidence: Map<String, Int> = emptyMap(),
    val categories: Map<String, CategoryQualityDto> = emptyMap(),
    val active_registers: List<Int> = emptyList(),
    val extended_polling: ExtendedPollingStatusDto? = null
)
