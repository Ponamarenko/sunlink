package com.sunportal.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val titleTextView = TextView(this)
        titleTextView.text = "SunPortal\nВерсія: 1.0.3\n\nSunPortal - система моніторингу сонячної енергетики\n\n🌞 Моніторинг сонячної енергії\n🔋 Управління акумулятором\n⚡ Параметри мережі\n📊 Енергетична статистика\n📱 Мобільний контроль"
        titleTextView.textSize = 20f
        
        val updateButton = Button(this)
        updateButton.text = "Перевірка оновлення"
        
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.addView(titleTextView)
        container.addView(updateButton)
        
        setContentView(container)
        
        updateButton.setOnClickListener {
            Toast.makeText(this, "Перевірка оновлень...", Toast.LENGTH_SHORT).show()
        }
    }
}