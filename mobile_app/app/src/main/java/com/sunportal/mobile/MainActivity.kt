package com.sunportal.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.widget.Toast
import com.sunportal.mobile.ui.AboutFragment
import com.sunportal.mobile.ui.ConnectionFragment
import com.sunportal.mobile.ui.AppearanceFragment
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * MainActivity - главный экран приложения SunPortal
 * Реализует навигацию через боковое меню и управление фрагментами
 * 
 * @author Ponamarenko
 * @version 1.1.1
 */
class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar

    /**
     * Инициализация MainActivity
     * Загружает layout, настраивает навигацию и применяет сохраненные настройки
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Применяем тему до setContentView для правильного отображения
        applyThemeOnStart()

        // Инициализация компонентов UI
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        // Настройка кнопки меню
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        // Применяем сохраненные настройки при запуске
        applySavedSettings()

        // Загружаем начальный фрагмент
        loadAboutFragment()
    }

    /**
     * Применяет тему при запуске приложения
     */
    private fun applyThemeOnStart() {
        val sharedPreferences = getSharedPreferences("sunportal_prefs", Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    /**
     * Применяет сохраненные настройки размера шрифта
     */
    private fun applySavedSettings() {
        val sharedPreferences = getSharedPreferences("sunportal_prefs", Context.MODE_PRIVATE)
        val savedFontSize = sharedPreferences.getString("font_size", "medium") ?: "medium"
        
        // Применяем размер шрифта ко всем элементам
        applyFontSizeToAll(savedFontSize)
    }

    /**
     * Применяет размер шрифта ко всем элементам интерфейса
     */
    private fun applyFontSizeToAll(size: String) {
        val fontSize = when (size) {
            "small" -> 14f
            "large" -> 22f
            else -> 18f
        }
        
        // Применяем размер шрифта ко всем TextView в текущей активности
        val rootView = findViewById<android.view.View>(android.R.id.content)
        applyFontSizeRecursive(rootView, fontSize)
    }

    /**
     * Рекурсивно применяет размер шрифта ко всем элементам
     */
    private fun applyFontSizeRecursive(view: android.view.View, size: Float) {
        if (view is android.widget.TextView) {
            view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size)
        }
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFontSizeRecursive(view.getChildAt(i), size)
            }
        }
    }

    /**
     * Обработка выбора пунктов меню навигации
     * @param item выбранный пункт меню
     * @return true если обработано успешно
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                loadAboutFragment()
                toolbar.title = "SunPortal"
            }
            R.id.nav_about -> {
                loadAboutFragment()
                toolbar.title = "Про додаток"
            }
            R.id.nav_connection -> {
                loadConnectionFragment()
                toolbar.title = "Підключення"
            }
            R.id.nav_appearance -> {
                loadAppearanceFragment()
                toolbar.title = "Вигляд"
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Налаштування", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                Toast.makeText(this, "Вихід з додатку", Toast.LENGTH_SHORT).show()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Обработка нажатия на кнопку меню
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Загружает фрагмент "Про додаток"
     */
    private fun loadAboutFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, AboutFragment())
            .commit()
    }

    /**
     * Загружает фрагмент "Підключення"
     */
    private fun loadConnectionFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, ConnectionFragment())
            .commit()
    }

    /**
     * Загружает фрагмент "Вигляд"
     */
    private fun loadAppearanceFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, AppearanceFragment())
            .commit()
    }

    /**
     * Обработка нажатия кнопки "назад"
     */
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}