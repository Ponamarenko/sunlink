package com.sunlink.svitgrid.data

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

/**
 * Stage 25B — nouvelle конфигурация backend, миграция старых URL и валидация.
 */
class SettingsRepositoryTest {

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
    fun `default backend is direct HTTPS endpoint`() {
        val repo = SettingsRepository(FakePrefs())
        assertEquals("sunlink.pp.ua", repo.serverAddress)
        assertEquals("9443", repo.serverPort)
        assertTrue(repo.useHttps)
        assertEquals("https://sunlink.pp.ua:9443/", repo.getBaseUrl())
    }

    @Test
    fun `custom local URL survives restart`() {
        val prefs = FakePrefs()
        val first = SettingsRepository(prefs)
        first.serverAddress = "http://192.168.1.50:8801/path"
        first.serverPort = "8801"
        first.useHttps = false
        assertEquals("http://192.168.1.50:8801/", first.getBaseUrl())

        val afterRestart = SettingsRepository(prefs)
        assertEquals("192.168.1.50", afterRestart.serverAddress)
        assertEquals("8801", afterRestart.serverPort)
        assertEquals("http://192.168.1.50:8801/", afterRestart.getBaseUrl())
    }

    @Test
    fun `full https URL is parsed and stored`() {
        val repo = SettingsRepository(FakePrefs())
        val endpoint = SettingsRepository.parseEndpoint("https://example.com/some/path")
        assertNotNull(endpoint)
        assertEquals("example.com", endpoint!!.host)
        assertEquals(true, endpoint.https)
        repo.serverAddress = "https://example.com/some/path"
        repo.serverPort = "9999"
        assertEquals("example.com", repo.serverAddress)
    }

    @Test
    fun `parseEndpoint extracts host and port and rejects invalid input`() {
        val endpoint = SettingsRepository.parseEndpoint("http://127.0.0.1:8801/path")
        assertNotNull(endpoint)
        assertEquals("127.0.0.1", endpoint!!.host)
        assertEquals(8801, endpoint.port)
        assertEquals(false, endpoint.https)
        assertEquals("192.168.25.20", SettingsRepository.parseEndpoint("192.168.25.20")?.host)
        assertNull(SettingsRepository.parseEndpoint(""))
        assertNull(SettingsRepository.parseEndpoint("bad host name"))
        assertNull(SettingsRepository.parseEndpoint("host:notaport"))
        assertNull(SettingsRepository.parseEndpoint("host:70000"))
        assertNull(SettingsRepository.parseEndpoint("999.1.1.1"))
        assertTrue(SettingsRepository.isHttpsInput("https://example.com"))
        assertFalse(SettingsRepository.isHttpsInput("http://example.com"))
    }

    @Test
    fun `validation requires HTTPS for public hosts`() {
        assertNull(SettingsRepository.validate("sunlink.pp.ua", "9443", true))
        assertNull(SettingsRepository.validate("127.0.0.1", "8801", false))
        assertNotNull(SettingsRepository.validate("sunlink.pp.ua", "9443", false))
        assertNotNull(SettingsRepository.validate("91.226.253.235", "9443", true))
        assertNotNull(SettingsRepository.validate("", "9443", true))
        assertNotNull(SettingsRepository.validate("bad host", "9443", true))
        assertNotNull(SettingsRepository.validate("sunlink.pp.ua", "0", true))
        assertNotNull(SettingsRepository.validate("sunlink.pp.ua", "70000", true))
    }

    @Test
    fun `migration replaces all known legacy public hosts`() {
        for (old in listOf("ha4a.pp.ua", "k4a.pp.ua", "sunline.pp.ua")) {
            val prefs = FakePrefs()
            prefs.edit()
                .putString("server_address", old)
                .putString("server_port", "8800")
                .putBoolean("server_https", false)
                .commit()

            val repo = SettingsRepository(prefs)
            assertEquals("sunlink.pp.ua", repo.serverAddress)
            assertEquals("9443", repo.serverPort)
            assertTrue(repo.useHttps)
            assertEquals("https://sunlink.pp.ua:9443/", repo.getBaseUrl())
            assertEquals(
                SettingsRepository.CURRENT_MIGRATION_VERSION,
                prefs.getInt("settings_migration_version", 0)
            )
        }
    }

    @Test
    fun `migration preserves custom host and is idempotent`() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("server_address", "custom.example.net")
            .putString("server_port", "9443")
            .putBoolean("server_https", true)
            .commit()

        val first = SettingsRepository(prefs)
        assertEquals("custom.example.net", first.serverAddress)
        assertEquals("https://custom.example.net:9443/", first.getBaseUrl())

        // Повторная миграция не меняет корректные настройки
        val second = SettingsRepository(prefs)
        assertEquals("custom.example.net", second.serverAddress)
        assertEquals("9443", second.serverPort)
        assertEquals("https://custom.example.net:9443/", second.getBaseUrl())
    }

    @Test
    fun `migration replaces legacy default port 8443 with 9443`() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("server_address", "sunlink.pp.ua")
            .putString("server_port", com.sunlink.svitgrid.data.BackendDefaults.LEGACY_HTTPS_PORT.toString())
            .putBoolean("server_https", true)
            .putInt("settings_migration_version", 2)
            .commit()

        val repo = SettingsRepository(prefs)
        assertEquals("sunlink.pp.ua", repo.serverAddress)
        assertEquals("9443", repo.serverPort)
        assertTrue(repo.useHttps)
        assertEquals("https://sunlink.pp.ua:9443/", repo.getBaseUrl())
        // Повторная миграция ничего не меняет
        assertEquals("https://sunlink.pp.ua:9443/", SettingsRepository(prefs).getBaseUrl())
    }

    @Test
    fun `LAN private ranges allow HTTP while public hosts require HTTPS`() {
        for (host in listOf("192.168.25.20", "10.0.0.5", "172.16.0.1", "172.31.255.1", "127.0.0.1")) {
            val result = SettingsRepository.validate(host, "8800", false)
            assertTrue("validate($host) = $result", result == null)
        }
        assertNotNull(SettingsRepository.validate("172.32.0.1", "8800", false))
        assertNotNull(SettingsRepository.validate("sunlink.pp.ua", "8800", false))
        // HTTPS на IP без сертификата с SAN запрещён — используется имя хоста.
        assertNotNull(SettingsRepository.validate("192.168.25.20", "8800", true))
    }

    @Test
    fun `invalid values do not overwrite valid saved config`() {
        val prefs = FakePrefs()
        val repo = SettingsRepository(prefs)
        repo.serverAddress = "good.host"
        repo.serverPort = "9443"

        repo.serverAddress = "bad host"
        repo.serverPort = "not-a-port"

        assertEquals("good.host", repo.serverAddress)
        assertEquals("9443", repo.serverPort)
    }

    @Test
    fun `HOTFIX saved public host with port 8800 migrates to 9443 https`() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("server_address", "sunlink.pp.ua")
            .putString("server_port", "8800")
            .putBoolean("server_https", false)
            .putInt("settings_migration_version", 3)
            .commit()

        val repo = SettingsRepository(prefs)
        assertEquals("sunlink.pp.ua", repo.serverAddress)
        assertEquals("9443", repo.serverPort)
        assertTrue(repo.useHttps)
        assertEquals("https://sunlink.pp.ua:9443/", repo.getBaseUrl())
        assertEquals(4, prefs.getInt("settings_migration_version", 0))
    }

    @Test
    fun `HOTFIX partial public settings with port 8800 also migrate`() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("server_port", "8800")
            .putBoolean("server_https", false)
            .commit()
        val repo = SettingsRepository(prefs)
        assertEquals("https://sunlink.pp.ua:9443/", repo.getBaseUrl())
    }

    @Test
    fun `HOTFIX custom LAN debug endpoint is preserved`() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("server_address", "192.168.25.20")
            .putString("server_port", "18800")
            .putBoolean("server_https", false)
            .commit()
        val repo = SettingsRepository(prefs)
        assertEquals("http://192.168.25.20:18800/", repo.getBaseUrl())
    }

    @Test
    fun `theme mode is persisted and defaults to system`() {
        val repo = SettingsRepository(FakePrefs())
        assertEquals("SYSTEM", repo.themeModeName)
        repo.themeModeName = "DARK"
        assertEquals("DARK", repo.themeModeName)
        repo.themeModeName = "LIGHT"
        assertEquals("LIGHT", repo.themeModeName)
    }
}
