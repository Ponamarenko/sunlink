package com.sunportal.mobile.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import com.sunportal.mobile.R

class AppearanceFragment : Fragment() {

    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var fontRadioGroup: RadioGroup
    private lateinit var applyButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_appearance, container, false)
        
        themeRadioGroup = view.findViewById(R.id.theme_radio_group)
        fontRadioGroup = view.findViewById(R.id.font_radio_group)
        applyButton = view.findViewById(R.id.apply_button)
        
        loadSettings()
        
        applyButton.setOnClickListener {
            applySettings()
        }
        
        return view
    }
    
    private fun loadSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("sunportal_prefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        val currentFontSize = sharedPreferences.getString("font_size", "medium")
        
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> {
                themeRadioGroup.check(R.id.radio_night)
            }
            AppCompatDelegate.MODE_NIGHT_NO -> {
                themeRadioGroup.check(R.id.radio_day)
            }
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                themeRadioGroup.check(R.id.radio_system)
            }
        }
        
        when (currentFontSize) {
            "small" -> fontRadioGroup.check(R.id.radio_small)
            "medium" -> fontRadioGroup.check(R.id.radio_medium)
            "large" -> fontRadioGroup.check(R.id.radio_large)
        }
    }
    
    private fun applySettings() {
        val selectedThemeId = themeRadioGroup.checkedRadioButtonId
        val selectedFontId = fontRadioGroup.checkedRadioButtonId
        
        val themeMode = when (selectedThemeId) {
            R.id.radio_night -> AppCompatDelegate.MODE_NIGHT_YES
            R.id.radio_system -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
        
        val fontSize = when (selectedFontId) {
            R.id.radio_small -> "small"
            R.id.radio_large -> "large"
            else -> "medium"
        }
        
        // Сохраняем настройки
        val sharedPreferences = requireContext().getSharedPreferences("sunportal_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("theme_mode", themeMode).apply()
        sharedPreferences.edit().putString("font_size", fontSize).apply()
        
        // Применяем тему
        AppCompatDelegate.setDefaultNightMode(themeMode)
        
        // Показываем сообщение
        Toast.makeText(requireContext(), "Налаштування застосовано", Toast.LENGTH_SHORT).show()
        
        // Перезапускаем активность для применения изменений
        requireActivity().recreate()
    }
}