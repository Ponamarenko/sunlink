package com.sunlink.svitgrid.data

/**
 * Единый источник значений по умолчанию для адреса backend.
 *
 * Stage 25B: публичный backend — https://sunlink.pp.ua:8443/
 * (Cloudflare DNS-only, TLS завершается непосредственно на SunLink backend).
 */
object BackendDefaults {
    const val HOST = "sunlink.pp.ua"
    const val PORT = 8443
    const val USE_HTTPS = true

    val BASE_URL: String = "https://$HOST:$PORT/"

    /** Известные устаревшие ПУБЛИЧНЫЕ хосты — мигрируются автоматически. */
    val LEGACY_PUBLIC_HOSTS = setOf(
        "ha4a.pp.ua",
        "k4a.pp.ua",
        "sunline.pp.ua"
    )

    /** Локальные/adb-хосты, для которых допустим HTTP (только dev). */
    val LOCAL_HOSTS = setOf(
        "127.0.0.1",
        "localhost",
        "10.0.2.2"
    )

    fun isLocalHost(host: String): Boolean {
        val normalized = host.trim().lowercase()
        return normalized in LOCAL_HOSTS || normalized.startsWith("192.168.")
    }

    /** Адрес изолированного DEMO backend (этап 12) для подсказки в UI. */
    const val DEMO_HOST = "127.0.0.1"
    const val DEMO_PORT = 8801
}
