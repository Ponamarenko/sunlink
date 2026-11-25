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

/**
 * Єдиний користувацький підпис достовірності. Технічні ідентифікатори
 * (model_compatible, physically_verified тощо) лишаються в Diagnostics.
 */
fun telemetryLabel(item: MeasurementValueDto?): String = when (item?.confidence) {
    "physically_verified" -> "Підтверджено вимірюванням"
    "verified" -> "Перевірено"
    "corroborated" -> "Підтверджено"
    "calculated" -> "Розраховано"
    "provisional" -> "Попередньо"
    "model_compatible" -> "Сумісне з моделлю"
    "unverified" -> "Не перевірено"
    null -> "Немає даних"
    else -> "Стан невідомий"
}

/** Технічний provenance тільки для Diagnostics/деталей параметра. */
fun telemetryProvenance(item: MeasurementValueDto?): String =
    item?.confidence ?: "unavailable"

fun freshnessLabel(freshness: Freshness): String = when (freshness) {
    Freshness.FRESH -> "Дані актуальні"
    Freshness.STALE -> "Дані застаріли"
    Freshness.PROVISIONAL -> "Попередні дані"
    Freshness.UNAVAILABLE -> "Немає даних"
    Freshness.OFFLINE -> "Немає з'єднання"
}

/**
 * Транзientна помилка REST не є помилкою всієї сторінки, якщо свіжі дані
 * уже отримані через WebSocket. Повідомлення лишається видимим, але
 * позначається як тимчасове джерело, а не як втрата телеметрії.
 */
fun telemetryErrorNote(error: String?, hasFreshTelemetry: Boolean): String? = when {
    error.isNullOrBlank() -> null
    hasFreshTelemetry -> "REST тимчасово недоступний · дані з WebSocket"
    else -> error
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


/** Повний баланс можна показувати тільки коли всі чотири вузли мають реальні значення. */
fun isBalanceComplete(values: List<Float?>): Boolean =
    values.size == 4 && values.all { it != null && !it.isNaN() && !it.isInfinite() }

/** SOC must be a physically valid percentage. Production history may contain bad samples. */
fun isValidSoc(value: Float?): Boolean =
    value != null && !value.isNaN() && !value.isInfinite() && value >= 0f && value <= 100f

/** Bad SOC samples from history become gaps; they are never clamped to 0..100. */
fun sanitizedSocSeries(series: com.sunlink.svitgrid.data.dto.HistorySeriesDto?): List<Float?> =
    series?.points?.map { point ->
        val value = point.value?.toFloat()
        if (point.gap || !isValidSoc(value)) null else value
    } ?: emptyList()
