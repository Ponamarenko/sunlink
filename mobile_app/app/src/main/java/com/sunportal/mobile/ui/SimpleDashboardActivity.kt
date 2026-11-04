package com.sunportal.mobile.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.sunportal.mobile.R

class SimpleDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_dashboard)
        
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)
        val btnEmergency = findViewById<Button>(R.id.btn_emergency_stop)
        
        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Данные обновлены", Toast.LENGTH_SHORT).show()
        }
        
        btnEmergency.setOnClickListener {
            Toast.makeText(this, "Экстренная остановка активирована", Toast.LENGTH_SHORT).show()
        }
        
        // Автоматическое обновление
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                Toast.makeText(this@SimpleDashboardActivity, "Данные обновлены", Toast.LENGTH_SHORT).show()
                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }
}