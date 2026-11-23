package com.sunlink.svitgrid.ui.redesign

import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto

fun telemetryValue(dto: TelemetryFullDto?, category: String, field: String): MeasurementValueDto? =
    dto?.categories?.get(category)?.get(field)

fun telemetryNumber(dto: TelemetryFullDto?, category: String, field: String): Float? =
    telemetryValue(dto, category, field)?.normalized_value?.toFloat()

fun telemetryLabel(item: MeasurementValueDto?): String = when {
    item == null -> "Немає даних"
    item.confidence == "calculated" -> "Розраховано"
    item.confidence == "corroborated" -> "Підтверджено"
    item.confidence == "provisional" -> "Попередньо"
    else -> item.confidence ?: "Немає даних"
}

/** Лише підтверджені сервером значення можуть керувати анімацією потоків. */
fun isConfirmed(item: MeasurementValueDto?, stale: Boolean): Boolean {
    if (item == null || stale) return false
    if (item.quality == "stale" || item.quality == "unavailable") return false
    return item.confidence in setOf("verified", "corroborated", "calculated")
}

/** Розриває ряд на сегменти з індексами: null/NaN не з'єднуються лінією. */
fun seriesSegments(values: List<Float?>): List<List<Pair<Int, Float>>> {
    val segments = mutableListOf<List<Pair<Int, Float>>>()
    var current = mutableListOf<Pair<Int, Float>>()
    values.forEachIndexed { index, value ->
        if (value == null || value.isNaN() || value.isInfinite()) {
            if (current.isNotEmpty()) segments.add(current.toList())
            current = mutableListOf()
        } else {
            current.add(index to value)
        }
    }
    if (current.isNotEmpty()) segments.add(current.toList())
    return segments
}
