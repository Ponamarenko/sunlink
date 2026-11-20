package com.sunlink.svitgrid.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

enum class RequestType { UPDATE, REST, WEBSOCKET }

/**
 * Событие диагностики без секретов: тип запроса, endpoint, этап, код/тип ошибки, время.
 */
data class DiagnosticEvent(
    val type: RequestType,
    val endpoint: String,
    val stage: String,
    val httpCode: Int? = null,
    val errorType: String? = null,
    val atUtc: String = Instant.now().toString()
)

object NetworkDiagnostics {
    private const val MAX_EVENTS = 30
    private val _events = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val events: StateFlow<List<DiagnosticEvent>> = _events

    private val _lastSuccessUtc = MutableStateFlow<String?>(null)
    val lastSuccessUtc: StateFlow<String?> = _lastSuccessUtc

    /** Удаляет userinfo, query и fragment — в диагностику не попадают токены. */
    fun sanitize(raw: String): String {
        var value = raw.substringBefore('?').substringBefore('#')
        val schemeEnd = value.indexOf("://")
        if (schemeEnd >= 0) {
            val rest = value.substring(schemeEnd + 3)
            val at = rest.indexOf('@')
            if (at >= 0) value = value.substring(0, schemeEnd + 3) + rest.substring(at + 1)
        }
        return value
    }

    fun record(
        type: RequestType,
        endpoint: String,
        stage: String,
        httpCode: Int? = null,
        errorType: String? = null,
        success: Boolean = false
    ) {
        val event = DiagnosticEvent(
            type = type,
            endpoint = sanitize(endpoint),
            stage = stage,
            httpCode = httpCode,
            errorType = errorType?.take(120)
        )
        _events.value = (listOf(event) + _events.value).take(MAX_EVENTS)
        if (success) _lastSuccessUtc.value = event.atUtc
    }
}
