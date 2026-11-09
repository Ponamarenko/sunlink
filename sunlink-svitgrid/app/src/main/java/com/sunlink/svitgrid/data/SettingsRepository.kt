package com.sunlink.svitgrid.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sunlink_settings", Context.MODE_PRIVATE)

    var serverAddress: String
        get() = prefs.getString("server_address", "ha4a.pp.ua") ?: "ha4a.pp.ua"
        set(value) {
            var v = value.trim()
            v = v.removePrefix("http://").removePrefix("https://")
            v = v.split("/")[0]
            v = v.split(":")[0]
            prefs.edit().putString("server_address", v).apply()
        }

    var serverPort: String
        get() = prefs.getString("server_port", "8000") ?: "8000"
        set(value) = prefs.edit().putString("server_port", value).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(value) = prefs.edit().putString("password", value).apply()

    fun getBaseUrl(): String = "http://${serverAddress}:${serverPort}/"
}
