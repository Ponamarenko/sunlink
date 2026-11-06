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
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.sunportal.mobile.ui.dashboard.DashboardFragment
import com.sunportal.mobile.ui.AboutFragment
import com.sunportal.mobile.viewmodel.MainViewModel

/**
 * MainActivity - главный экран приложения SunLink
 * Простая реализация для Android 15+
 */
class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupNavigation()
        setupViewModel()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

    private fun setupNavigation() {
        // Загружаем главный экран по умолчанию
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, DashboardFragment())
            .commit()
    }

    private fun setupViewModel() {
        // Настраиваем наблюдение за данными
        viewModel.inverterData.observe(this) { data ->
            // Обновление UI данными инвертора
        }
        
        viewModel.connectionStatus.observe(this) { status ->
            // Обновление статуса соединения
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                loadFragment(DashboardFragment())
                toolbar.title = "SunLink"
            }
            R.id.nav_live -> {
                Toast.makeText(this, "Real-time в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_charts -> {
                Toast.makeText(this, "Графики в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_battery -> {
                Toast.makeText(this, "Батарея в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_forecast -> {
                Toast.makeText(this, "Прогноз в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_about -> {
                loadFragment(AboutFragment())
                toolbar.title = "О приложении"
            }
            R.id.nav_connection -> {
                Toast.makeText(this, "Підключення в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_appearance -> {
                Toast.makeText(this, "Вигляд в разработке", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Функция в разработке", Toast.LENGTH_SHORT).show()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}