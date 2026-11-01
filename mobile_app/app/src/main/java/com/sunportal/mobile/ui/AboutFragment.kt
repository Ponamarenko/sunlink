package com.sunportal.mobile.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sunportal.mobile.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        
        val textView = view.findViewById<TextView>(R.id.about_text)
        textView.text = "SunPortal v1.0.4\n\n" +
                       "SunPortal - система моніторингу сонячної енергетики\n\n" +
                       "🌞 Моніторинг сонячної енергії\n" +
                       "🔋 Управління акумулятором\n" +
                       "⚡ Параметри мережі\n" +
                       "📊 Енергетична статистика\n" +
                       "📱 Мобільний контроль\n\n" +
                       "© 2026 SunPortal"
        
        return view
    }
}