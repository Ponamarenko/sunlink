package com.sunlink.svitgrid.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Настройки подключения к backend.
 *
 * Хранит только адрес, порт и схему. Пользователь может ввести адрес вручную
 * в Settings — пересборка APK не требуется. Все значения проверяются до
 * сохранения, чтобы некорректный ввод не ломал Retrofit.
 */
class SettingsRepository(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences("sunlink_settings", Context.MODE_PRIVATE)
    )

    data class Endpoint(val host: String, val port: Int?)

    companion object {
        const val KEY_HOST = "server_address"
        const val KEY_PORT = "server_port"
        const val KEY_HTTPS = "server_https"

        private val HOST_REGEX = Regex("^[A-Za-z0-9._-]+$")

        /**
         * Разбирает ввод пользователя. Принимает как хост, так и полный URL:
         * "192.168.25.20", "http://127.0.0.1:8801/path", "https://example.com".
         * Возвращает null для пустого/некорректного ввода.
         */
        fun parseEndpoint(input: String): Endpoint? {
            var value = input.trim()
            if (value.isEmpty() || value.any { it.isWhitespace() }) return null

            if (value.contains("://")) value = value.substringAfter("://")
            value = value.substringBefore("/").substringBefore("?").substringBefore("#")

            var port: Int? = null
            // IPv6 в кавычках не поддерживаем — явный hostname/IPv4.
            if (value.count { it == ':' } == 1) {
                val rawPort = value.substringAfter(":")
                port = normalizePort(rawPort) ?: return null
                value = value.substringBefore(":")
            } else if (value.count { it == ':' } > 1) {
                return null
            }

            if (value.isEmpty() || !HOST_REGEX.matches(value)) return null
            // Схема (http/https) читается отдельно через isHttpsInput().
            return Endpoint(host = value, port = port)
        }

        fun isHttpsInput(input: String): Boolean =
            input.trim().startsWith("https://", ignoreCase = true)

        fun normalizePort(input: String): Int? {
            val value = input.trim().toIntOrNull() ?: return null
            return if (value in 1..65535) value else null
        }

        /** Сообщение об ошибке или null, если значения корректны. */
        fun validate(host: String, port: String): String? {
            if (host.isBlank()) return "Вкажіть адресу сервера"
            if (parseEndpoint(host) == null) return "Некоректна адреса сервера"
            if (normalizePort(port) == null) return "Порт має бути числом від 1 до 65535"
            return null
        }

        fun buildBaseUrl(host: String, port: Int, useHttps: Boolean): String =
            "${if (useHttps) "https" else "http"}://$host:$port/"
    }

    /** Имя хоста без схемы и порта. */
    var serverAddress: String
        get() = prefs.getString(KEY_HOST, BackendDefaults.HOST) ?: BackendDefaults.HOST
        set(value) {
            parseEndpoint(value)?.host?.let {
                prefs.edit().putString(KEY_HOST, it).apply()
            }
        }

    /** Порт как строка (удобно для TextField). */
    var serverPort: String
        get() = prefs.getString(KEY_PORT, BackendDefaults.PORT.toString())
            ?: BackendDefaults.PORT.toString()
        set(value) {
            normalizePort(value)?.let {
                prefs.edit().putString(KEY_PORT, it.toString()).apply()
            }
        }

    /** HTTPS для production, HTTP для изолированного тестового backend. */
    var useHttps: Boolean
        get() = prefs.getBoolean(KEY_HTTPS, false)
        set(value) = prefs.edit().putBoolean(KEY_HTTPS, value).apply()

    fun getBaseUrl(): String = buildBaseUrl(
        serverAddress,
        serverPort.toIntOrNull() ?: BackendDefaults.PORT,
        useHttps
    )
}
