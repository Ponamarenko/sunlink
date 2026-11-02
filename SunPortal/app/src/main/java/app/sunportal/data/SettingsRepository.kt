package app.sunportal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с настройками приложения
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    // Ключи для настроек
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val SERVER_ADDRESS_KEY = stringPreferencesKey("server_address")
        private val SERVER_TIMEOUT_KEY = intPreferencesKey("server_timeout")
        private val AUTO_CONNECT_KEY = booleanPreferencesKey("auto_connect")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val AUTO_START_KEY = booleanPreferencesKey("auto_start")
        private val BACKGROUND_MODE_KEY = booleanPreferencesKey("background_mode")
        private val CACHING_ENABLED_KEY = booleanPreferencesKey("caching_enabled")
    }
    
    // Тема
    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system"
    }
    
    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
    
    // Размер шрифта
    val fontSize: Flow<String> = dataStore.data.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: "medium"
    }
    
    suspend fun setFontSize(fontSize: String) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = fontSize
        }
    }
    
    // Адрес сервера
    val serverAddress: Flow<String> = dataStore.data.map { preferences ->
        preferences[SERVER_ADDRESS_KEY] ?: "192.168.1.100:8800"
    }
    
    suspend fun setServerAddress(address: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = address
        }
    }
    
    // Таймаут сервера
    val serverTimeout: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SERVER_TIMEOUT_KEY] ?: 30
    }
    
    suspend fun setServerTimeout(timeout: Int) {
        dataStore.edit { preferences ->
            preferences[SERVER_TIMEOUT_KEY] = timeout
        }
    }
    
    // Автоматическое подключение
    val autoConnect: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_CONNECT_KEY] ?: true
    }
    
    suspend fun setAutoConnect(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_CONNECT_KEY] = enabled
        }
    }
    
    // Уведомления
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
    
    // Автозапуск
    val autoStart: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_START_KEY] ?: true
    }
    
    suspend fun setAutoStart(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_START_KEY] = enabled
        }
    }
    
    // Фоновый режим
    val backgroundMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BACKGROUND_MODE_KEY] ?: true
    }
    
    suspend fun setBackgroundMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_MODE_KEY] = enabled
        }
    }
    
    // Кэширование
    val cachingEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CACHING_ENABLED_KEY] ?: true
    }
    
    suspend fun setCachingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CACHING_ENABLED_KEY] = enabled
        }
    }
    
    // Получить все настройки
    suspend fun getAllSettings(): Map<String, Any> {
        val preferences = dataStore.data.first()
        return mapOf(
            "theme" to preferences[THEME_KEY] ?: "system",
            "font_size" to preferences[FONT_SIZE_KEY] ?: "medium",
            "server_address" to preferences[SERVER_ADDRESS_KEY] ?: "192.168.1.100:8800",
            "server_timeout" to preferences[SERVER_TIMEOUT_KEY] ?: 30,
            "auto_connect" to preferences[AUTO_CONNECT_KEY] ?: true,
            "notifications_enabled" to preferences[NOTIFICATIONS_ENABLED_KEY] ?: true,
            "auto_start" to preferences[AUTO_START_KEY] ?: true,
            "background_mode" to preferences[BACKGROUND_MODE_KEY] ?: true,
            "caching_enabled" to preferences[CACHING_ENABLED_KEY] ?: true
        )
    }
}