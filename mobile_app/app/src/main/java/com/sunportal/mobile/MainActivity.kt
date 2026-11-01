package com.sunportal.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import android.widget.Button
import android.widget.TextView
import com.sunportal.mobile.ui.AboutFragment
import com.sunportal.mobile.ui.ConnectionFragment

class MainActivity : AppCompatActivity() {

    private lateinit var contentFrame: FrameLayout
    private lateinit var btnAbout: Button
    private lateinit var btnConnection: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contentFrame = findViewById(R.id.content_frame)
        btnAbout = findViewById(R.id.btn_about)
        btnConnection = findViewById(R.id.btn_connection)

        btnAbout.setOnClickListener {
            loadAboutFragment()
        }

        btnConnection.setOnClickListener {
            loadConnectionFragment()
        }

        // Загружаем начальный фрагмент
        loadAboutFragment()
    }

    private fun loadAboutFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, AboutFragment())
            .commit()
        
        btnAbout.setBackgroundColor(getColor(R.color.svitgrid_primary))
        btnConnection.setBackgroundColor(getColor(R.color.svitgrid_secondary))
    }

    private fun loadConnectionFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, ConnectionFragment())
            .commit()
        
        btnConnection.setBackgroundColor(getColor(R.color.svitgrid_primary))
        btnAbout.setBackgroundColor(getColor(R.color.svitgrid_secondary))
    }
}