package com.sunlink.svitgrid.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Настройки подключения к backend (Stage 25B).
 *
 * Дефолт: https://sunlink.pp.ua:8443/ . Пользовательский адрес сохраняется,
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
        const val CURRENT_MIGRATION_VERSION = 2

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
        if (savedHost != null && savedHost.lowercase() in BackendDefaults.LEGACY_PUBLIC_HOSTS) {
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
}
