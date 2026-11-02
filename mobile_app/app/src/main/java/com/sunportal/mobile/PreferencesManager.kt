package com.sunportal.mobile

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * PreferencesManager - менеджер для управления настройками приложения
 * Предоставляет методы для сохранения и загрузки настроек темы и размера шрифта
 * 
 * @author Ponamarenko
 * @version 1.1.0
 */
object PreferencesManager {
    
    private const val PREFS_NAME = "sunportal_prefs"
    private const val THEME_MODE_KEY = "theme_mode"
    private const val FONT_SIZE_KEY = "font_size"
    
    /**
     * Получает SharedPreferences экземпляр
     * @param context контекст приложения
     * @return SharedPreferences экземпляр
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Устанавливает режим темы
     * @param context контекст приложения
     * @param mode режим темы (DAY/NIGHT/FOLLOW_SYSTEM)
     */
    fun setThemeMode(context: Context, mode: Int) {
        getPreferences(context).edit().putInt(THEME_MODE_KEY, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }
    
    /**
     * Получает текущий режим темы
     * @param context контекст приложения
     * @return режим темы
     */
    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_NO)
    }
    
    /**
     * Устанавливает размер шрифта
     * @param context контекст приложения
     * @param размер шрифта (small/medium/large)
     */
    fun setFontSize(context: Context, size: String) {
        getPreferences(context).edit().putString(FONT_SIZE_KEY, size).apply()
    }
    
    /**
     * Получает текущий размер шрифта
     * @param context контекст приложения
     * @return размер шрифта
     */
    fun getFontSize(context: Context): String {
        return getPreferences(context).getString(FONT_SIZE_KEY, "medium") ?: "medium"
    }
    
    /**
     * Применяет сохраненную тему
     * @param context контекст приложения
     */
    fun applyTheme(context: Context) {
        val mode = getThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}