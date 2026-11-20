package com.sunlink.svitgrid.data

/**
 * Единый источник значений по умолчанию для адреса backend.
 *
 * Stage 25B.1: локальный HTTP — http://192.168.25.20:8800/ ,
 * публичный HTTPS — https://sunlink.pp.ua:9443/ .
 * Порт 8443 больше не используется (устаревшая конфигурация Stage 25B).
 */
object BackendDefaults {
    const val HOST = "sunlink.pp.ua"
    const val PORT = 9443
    const val USE_HTTPS = true

    val BASE_URL: String = "https://$HOST:$PORT/"

    /** Локальный HTTP backend в LAN (не публичный endpoint). */
    const val LOCAL_HOST = "192.168.25.20"
    const val LOCAL_PORT = 8800

    /** Устаревший порт публичного HTTPS (Stage 25B) — мигрируется. */
    const val LEGACY_HTTPS_PORT = 8443

    /** Известные устаревшие ПУБЛИЧНЫЕ хосты — мигрируются автоматически. */
    val LEGACY_PUBLIC_HOSTS = setOf(
        "ha4a.pp.ua",
        "k4a.pp.ua",
        "sunline.pp.ua"
    )

    /** Локальные/adb-хосты, для которых допустим HTTP (только dev). */
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

    /** Адрес изолированного DEMO backend (этап 12) для подсказки в UI. */
    const val DEMO_HOST = "127.0.0.1"
    const val DEMO_PORT = 8801
}
