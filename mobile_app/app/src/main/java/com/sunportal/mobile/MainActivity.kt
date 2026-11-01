package com.sunportal.mobile

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "SunPortal v1.0.2\n\nSunPortal - система моніторингу сонячної енергетики\n\n🌞 Моніторинг сонячної енергії\n🔋 Управління акумулятором\n⚡ Параметри мережі\n📊 Енергетична статистика\n📱 Мобільний контроль"
        textView.textSize = 18f
        
        setContentView(textView)
    }
}