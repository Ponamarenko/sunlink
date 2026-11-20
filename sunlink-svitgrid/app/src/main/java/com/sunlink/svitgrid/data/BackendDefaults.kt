package com.sunlink.svitgrid.data

import com.sunlink.svitgrid.BuildConfig

/**
 * Единый источник адресов backend.
 *
 * Release/public: https://sunlink.pp.ua:9443/ — согласованный endpoint.
 * Debug LAN: http://192.168.25.20:18800/ — только для Debug и только в LAN.
 * Порт 8800 — устаревший публичный endpoint: оставлен только как маркер
 * миграции, НЕ используется как активный адрес.
 */
object BackendDefaults {
    const val HOST = "sunlink.pp.ua"
    const val PORT = 9443
    const val USE_HTTPS = true

    val BASE_URL: String = "https://$HOST:$PORT/"

    /** Устаревшие порты — только для распознавания при миграции. */
    const val LEGACY_HTTP_PORT = 8800
    const val LEGACY_HTTPS_PORT = 8443

    /** Debug-only LAN endpoint (никогда не используется в Release). */
    const val DEBUG_LAN_HOST = "192.168.25.20"
    const val DEBUG_LAN_PORT = 18800
    val DEBUG_LAN_URL: String = "http://$DEBUG_LAN_HOST:$DEBUG_LAN_PORT/"

    val LEGACY_PUBLIC_HOSTS = setOf(
        "ha4a.pp.ua",
        "k4a.pp.ua",
        "sunline.pp.ua"
    )

    val LOCAL_HOSTS = setOf("127.0.0.1", "localhost", "10.0.2.2")

    fun isLocalHost(host: String): Boolean {
        val normalized = host.trim().lowercase()
        if (normalized in LOCAL_HOSTS) return true
        if (normalized.startsWith("192.168.")) return true
        if (normalized.matches(Regex("^10\\.\\d{1,3}(\\.\\d{1,3}){2}$"))) return true
        val m = Regex("^172\\.(\\d{1,3})\\.").find(normalized) ?: return false
        val second = m.groupValues[1].toIntOrNull() ?: return false
        return second in 16..31
    }

    /** Публичный хост (текущий или устаревший) — для него обязательны HTTPS и :9443. */
    fun isPublicHost(host: String): Boolean {
        val normalized = host.trim().lowercase()
        return normalized == HOST.lowercase() || normalized in LEGACY_PUBLIC_HOSTS
    }

    /** Адрес по умолчанию всегда публичный HTTPS:9443 — и в Debug, и в Release. */
    val defaultBaseUrl: String = BASE_URL

    /** Явный debug-only переход на LAN endpoint. В Release возвращает null. */
    fun debugLanUrlOrNull(): String? = if (BuildConfig.DEBUG) DEBUG_LAN_URL else null

    /** Адрес изолированного DEMO backend (этап 12) для подсказки в UI. */
    const val DEMO_HOST = "127.0.0.1"
    const val DEMO_PORT = 8801
}
