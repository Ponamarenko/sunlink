package com.sunlink.svitgrid.ui.redesign

import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

/** Генерація PV завжди йде від панелі до інвертора — напрямок підтверджений знаком потужності. */
fun pvFlowDirection(value: Float?, confirmed: Boolean): FlowDirection =
    if (confirmed && value != null && value > 1f) FlowDirection.TO_HUB else FlowDirection.UNKNOWN

/** Позитивна потужність мережі = імпорт (мережа → інвертор), негативна = експорт. */
fun gridFlowDirection(value: Float?, confirmed: Boolean): FlowDirection = when {
    !confirmed || value == null -> FlowDirection.UNKNOWN
    value > 1f -> FlowDirection.TO_HUB
    value < -1f -> FlowDirection.FROM_HUB
    else -> FlowDirection.UNKNOWN
}

/** Споживання навантаження йде від інвертора до дому. */
fun loadFlowDirection(value: Float?, confirmed: Boolean): FlowDirection =
    if (confirmed && value != null && value > 1f) FlowDirection.FROM_HUB else FlowDirection.UNKNOWN

/** Напрямок батареї сервер не підтверджує: конфлікт масштабу reg. 190. */
fun batteryFlowDirection(): FlowDirection = FlowDirection.UNKNOWN


/** Людський формат часу для UI; точний ISO-timestamp залишається в моделі. */
fun formatTelemetryTimestamp(
    iso: String?,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val formatter = DateTimeFormatter.ofPattern("d MMM, HH:mm:ss", locale).withZone(zoneId)
        formatter.format(Instant.parse(iso))
    } catch (e: Exception) {
        // Не ламаємо USDA-сумісність: повертаємо стабільний префікс без 'T'/'Z'.
        iso.take(19).replace('T', ' ')
    }
}


/** WS OPEN тільки після підтвердженого OPEN; CONNECTING ніколи не позначається як OPEN. */
fun transportLabel(state: TelemetryWebSocketClient.WsTransportState): String =
    if (state == TelemetryWebSocketClient.WsTransportState.OPEN) "WS OPEN" else "WS ${state.name}"

/**
 * Регістр 175 (p_load) — S_WORD, is_signed=true, scale=1.0. Від'ємне значення є
 * справжнім підписаним вимірюванням, а не помилкою UI. Ми НЕ застосовуємо abs()
 * і не змінюємо знак без підтвердженої семантики джерела.
 */
fun displayLoadPower(value: Float?): Float? = value
