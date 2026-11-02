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

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    private fun applySavedSettings() {
        val sharedPreferences = getSharedPreferences("sunportal_prefs", Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadAboutFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, AboutFragment())
            .commit()
    }

    private fun loadConnectionFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, ConnectionFragment())
            .commit()
    }

    private fun loadAppearanceFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, AppearanceFragment())
            .commit()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}