package com.sunportal.mobile

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object PreferencesManager {
    
    private const val PREFS_NAME = "sunportal_prefs"
    private const val THEME_MODE_KEY = "theme_mode"
    private const val FONT_SIZE_KEY = "font_size"
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun setThemeMode(context: Context, mode: Int) {
        getPreferences(context).edit().putInt(THEME_MODE_KEY, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }
    
    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_NO)
    }
    
    fun setFontSize(context: Context, size: String) {
        getPreferences(context).edit().putString(FONT_SIZE_KEY, size).apply()
    }
    
    fun getFontSize(context: Context): String {
        return getPreferences(context).getString(FONT_SIZE_KEY, "medium") ?: "medium"
    }
    
    fun applyTheme(context: Context) {
        val mode = getThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
    
    fun applyFontSize(context: Context, size: String) {
        // Применяем размер шрифта через MainActivity
        val activity = context as? MainActivity
        activity?.applyFontSize(size)
    }
}