package com.sunlink.svitgrid.data

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

/**
 * Этап 12 — проверка адреса backend и сохранения настройки.
 *
 * Использует in-memory SharedPreferences: никакого Android-эмулятора.
 * Два разных репозитория поверх одного хранилища имитируют перезапуск
 * приложения (SharedPreferences переживают перезапуск процесса).
 */
class SettingsRepositoryTest {

    /** Минимальная реализация SharedPreferences для JVM-тестов. */
    private class FakePrefs : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = map

        @Suppress("UNCHECKED_CAST")
        override fun getString(key: String?, defValue: String?): String? =
            map[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            override fun putString(key: String?, value: String?) = apply { pending[key as String] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { pending[key as String] = values }
            override fun putInt(key: String?, value: Int) = apply { pending[key as String] = value }
            override fun putLong(key: String?, value: Long) = apply { pending[key as String] = value }
            override fun putFloat(key: String?, value: Float) = apply { pending[key as String] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { pending[key as String] = value }
            override fun remove(key: String?) = apply { pending.remove(key) }
            override fun clear() = apply { pending.clear() }
            override fun commit(): Boolean { map.putAll(pending); pending.clear(); return true }
            override fun apply() { commit() }
        }

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}
    }

    @Test
    fun `default backend is deye gateway address`() {
        val repo = SettingsRepository(FakePrefs())
        assertEquals("ha4a.pp.ua", repo.serverAddress)
        assertEquals("8800", repo.serverPort)
        assertEquals("http://ha4a.pp.ua:8800/", repo.getBaseUrl())
    }

    @Test
    fun `backend url survives application restart`() {
        val prefs = FakePrefs()
        val first = SettingsRepository(prefs)
        first.serverAddress = "http://192.168.1.50:8801/path"
        first.serverPort = "8801"
        assertEquals("http://192.168.1.50:8801/", first.getBaseUrl())

        // Новый экземпляр = новый запуск приложения, то же хранилище
        val afterRestart = SettingsRepository(prefs)
        assertEquals("192.168.1.50", afterRestart.serverAddress)
        assertEquals("8801", afterRestart.serverPort)
        assertEquals("http://192.168.1.50:8801/", afterRestart.getBaseUrl())
    }

    @Test
    fun `backend url normalizes scheme and path`() {
        val repo = SettingsRepository(FakePrefs())
        repo.serverAddress = "https://example.com/some/path"
        repo.serverPort = "9999"
        assertEquals("example.com", repo.serverAddress)
        assertEquals("http://example.com:9999/", repo.getBaseUrl())
    }
}
