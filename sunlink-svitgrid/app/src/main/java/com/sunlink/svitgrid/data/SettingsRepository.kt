package com.sunlink.svitgrid.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Настройки подключения к backend (Stage 25B).
 *
 * Дефолт: https://sunlink.pp.ua:9443/ . Пользовательский адрес сохраняется,
 * но известные устаревшие ПУБЛИЧНЫЕ хосты мигрируются один раз и идемпотентно.
 * Публичный HTTP запрещён; HTTP разрешён только для локальных dev-хостов.
 */
class SettingsRepository(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences("sunlink_settings", Context.MODE_PRIVATE)
    )

    init {
        migrate()
    }

    data class Endpoint(val host: String, val port: Int?, val https: Boolean? = null)

    companion object {
        const val KEY_HOST = "server_address"
        const val KEY_PORT = "server_port"
        const val KEY_HTTPS = "server_https"
        const val KEY_MIGRATION_VERSION = "settings_migration_version"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE = "language"
        const val KEY_POLL_INTERVAL = "poll_interval_seconds"
        const val KEY_ALERTS_ENABLED = "alerts_enabled"
        const val KEY_LOW_SOC_ENABLED = "low_soc_enabled"
        const val KEY_LOW_SOC_THRESHOLD = "low_soc_threshold"
        const val CURRENT_MIGRATION_VERSION = 4

        private val HOST_REGEX = Regex("^[A-Za-z0-9._-]+$")
        private val IPV4_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

        /** Разбирает хост, полный URL или `host:port`. */
        fun parseEndpoint(input: String): Endpoint? {
            var value = input.trim()
            if (value.isEmpty() || value.any { it.isWhitespace() }) return null

            val https = when {
                value.startsWith("https://", ignoreCase = true) -> true
                value.startsWith("http://", ignoreCase = true) -> false
                else -> null
            }
            if (value.contains("://")) value = value.substringAfter("://")
            value = value.substringBefore("/").substringBefore("?").substringBefore("#")
            if (value.endsWith("/")) value = value.trimEnd('/')

            var port: Int? = null
            if (value.count { it == ':' } == 1) {
                port = normalizePort(value.substringAfter(":")) ?: return null
                value = value.substringBefore(":")
            } else if (value.count { it == ':' } > 1) {
                return null // IPv6 не поддерживается
            }

            if (value.isEmpty() || !HOST_REGEX.matches(value)) return null
            if (IPV4_REGEX.matches(value) && !isValidIpv4(value)) return null
            return Endpoint(host = value, port = port, https = https)
        }

        private fun isValidIpv4(value: String): Boolean =
            value.split('.').all { it.toIntOrNull() in 0..255 }

        fun isHttpsInput(input: String): Boolean =
            input.trim().startsWith("https://", ignoreCase = true)

        fun normalizePort(input: String): Int? {
            val value = input.trim().toIntOrNull() ?: return null
            return if (value in 1..65535) value else null
        }

        /**
         * Полная проверка с учётом публичной политики:
         * публичный хост — только HTTPS; HTTP — только localhost/LAN.
         */
        fun validate(host: String, port: String, useHttps: Boolean): String? {
            if (host.isBlank()) return "Вкажіть адресу сервера"
            val endpoint = parseEndpoint(host) ?: return "Некоректна адреса сервера"
            if (normalizePort(port) == null) return "Порт має бути числом від 1 до 65535"
            if (!useHttps && !BackendDefaults.isLocalHost(endpoint.host)) {
                return "Публічний backend допускається тільки через HTTPS"
            }
            if (useHttps && IPV4_REGEX.matches(endpoint.host)) {
                return "Для TLS вкажіть ім'я хоста, а не IP-адресу"
            }
            return null
        }

        /** backward-compatible overload (используется старым UI) */
        fun validate(host: String, port: String): String? =
            validate(host, port, isHttpsInput(host))

        fun buildBaseUrl(host: String, port: Int, useHttps: Boolean): String =
            "${if (useHttps) "https" else "http"}://$host:$port/"
    }

    /** Выполняется один раз; версия сохраняется, повтор не меняет настройки. */
    fun migrate() {
        val version = prefs.getInt(KEY_MIGRATION_VERSION, 0)
        if (version >= CURRENT_MIGRATION_VERSION) return

        val savedHost = prefs.getString(KEY_HOST, null)?.trim()
        val savedPort = prefs.getString(KEY_PORT, null)?.trim()
        val legacyHost = savedHost != null &&
            savedHost.lowercase() in BackendDefaults.LEGACY_PUBLIC_HOSTS
        val oldDefaultPort = savedHost?.equals(BackendDefaults.HOST, ignoreCase = true) == true &&
            savedPort == BackendDefaults.LEGACY_HTTPS_PORT.toString()
        // HOTFIX: сохранённый публичный хост с любым не-9443 портом или без HTTPS
        // (включая устаревший публичный HTTP-порт) приводится к согласованному endpoint.
        val effectiveHost = savedHost ?: BackendDefaults.HOST
        val effectivePort = savedPort ?: BackendDefaults.PORT.toString()
        val effectiveHttps = prefs.getBoolean(KEY_HTTPS, BackendDefaults.USE_HTTPS)
        val publicMisconfigured = BackendDefaults.isPublicHost(effectiveHost) &&
            (effectivePort != BackendDefaults.PORT.toString() || !effectiveHttps)

        if (legacyHost || oldDefaultPort || publicMisconfigured) {
            prefs.edit()
                .putString(KEY_HOST, BackendDefaults.HOST)
                .putString(KEY_PORT, BackendDefaults.PORT.toString())
                .putBoolean(KEY_HTTPS, true)
                .putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION)
                .apply()
        } else {
            // Пользовательский нестандартный адрес не перезаписываем.
            prefs.edit()
                .putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION)
                .apply()
        }
    }

    var serverAddress: String
        get() = prefs.getString(KEY_HOST, BackendDefaults.HOST) ?: BackendDefaults.HOST
        set(value) {
            parseEndpoint(value)?.host?.let { prefs.edit().putString(KEY_HOST, it).apply() }
        }

    var serverPort: String
        get() = prefs.getString(KEY_PORT, BackendDefaults.PORT.toString())
            ?: BackendDefaults.PORT.toString()
        set(value) {
            normalizePort(value)?.let { prefs.edit().putString(KEY_PORT, it.toString()).apply() }
        }

    var useHttps: Boolean
        get() = prefs.getBoolean(KEY_HTTPS, BackendDefaults.USE_HTTPS)
        set(value) = prefs.edit().putBoolean(KEY_HTTPS, value).apply()

    fun getBaseUrl(): String = buildBaseUrl(
        serverAddress,
        serverPort.toIntOrNull() ?: BackendDefaults.PORT,
        useHttps
    )

    /** HOTFIX:LAN endpoint доступен только в Debug-сборке и только по явному действию. */
    fun useDebugLanEndpoint(): Boolean {
        if (!com.sunlink.svitgrid.BuildConfig.DEBUG) return false
        val url = BackendDefaults.debugLanUrlOrNull() ?: return false
        val endpoint = parseEndpoint(url) ?: return false
        prefs.edit()
            .putString(KEY_HOST, endpoint.host)
            .putString(KEY_PORT, (endpoint.port ?: BackendDefaults.DEBUG_LAN_PORT).toString())
            .putBoolean(KEY_HTTPS, false)
            .apply()
        return true
    }

    /** Возврат к согласованному публичному endpoint. */
    fun usePublicEndpoint() {
        prefs.edit()
            .putString(KEY_HOST, BackendDefaults.HOST)
            .putString(KEY_PORT, BackendDefaults.PORT.toString())
            .putBoolean(KEY_HTTPS, true)
            .apply()
    }

    /** Stage 31: SYSTEM / LIGHT / DARK. Значение сохраняется в тех же SharedPreferences. */
    var themeModeName: String
        get() = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
        set(value) { prefs.edit().putString(KEY_THEME_MODE, value).apply() }

    /**
     * Stage 50.2: старые установки могли сохранить "SYSTEM", "LIGHT" или некорректное
     * значение. Приводим любое не-DARK к тёмной теме один раз при запуске — основные
     * экраны обязаны запускаться в Hybrid Premium независимо от системной темы Android.
     */
    fun migrateLegacyThemeMode(): String {
        val saved = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
        val valid = runCatching { com.sunlink.svitgrid.ui.theme.ThemeMode.valueOf(saved) }.getOrNull()
        return if (valid != com.sunlink.svitgrid.ui.theme.ThemeMode.DARK) {
            themeModeName = com.sunlink.svitgrid.ui.theme.ThemeMode.DARK.name
            com.sunlink.svitgrid.ui.theme.ThemeMode.DARK.name
        } else {
            saved
        }
    }

    /** Мова інтерфейсу: Stitch: Ukrainian; other values reserved. */
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "uk") ?: "uk"
        set(value) { prefs.edit().putString(KEY_LANGUAGE, value).apply() }

    /** Інтервал оновлення телеметрії, секунди (5 / 15 / 30). */
    var pollIntervalSeconds: Int
        get() = prefs.getInt(KEY_POLL_INTERVAL, 5).coerceIn(5, 30)
        set(value) { prefs.edit().putInt(KEY_POLL_INTERVAL, value.coerceIn(5, 30)).apply() }

    var alertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALERTS_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_ALERTS_ENABLED, value).apply() }

    var lowSocWarningEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOW_SOC_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_LOW_SOC_ENABLED, value).apply() }

    var lowSocThreshold: Int
        get() = prefs.getInt(KEY_LOW_SOC_THRESHOLD, 20).coerceIn(5, 80)
        set(value) { prefs.edit().putInt(KEY_LOW_SOC_THRESHOLD, value.coerceIn(5, 80)).apply() }
}
