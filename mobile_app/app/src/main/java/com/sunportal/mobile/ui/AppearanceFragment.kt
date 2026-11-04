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

/**
 * AppearanceFragment - фрагмент для настроек внешнего вида приложения
 * Позволяет пользователю выбирать тему и размер шрифта
 *
 * @author Ponamarenko
 * @version 1.1.3
 */
class AppearanceFragment : Fragment() {

    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var fontRadioGroup: RadioGroup
    private lateinit var applyButton: Button

    /**
     * Создает и возвращает view для фрагмента
     * @param inflater LayoutInflater для создания view
     * @param container родительский контейнер
     * @param savedInstanceState сохраненное состояние
     * @return созданное view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_appearance, container, false)

        // Инициализация компонентов UI
        themeRadioGroup = view.findViewById(R.id.theme_radio_group)
        fontRadioGroup = view.findViewById(R.id.font_radio_group)
        applyButton = view.findViewById(R.id.apply_button)

        // Загрузка сохраненных настроек
        loadSettings()

        // Установка обработчика нажатия на кнопку применения
        applyButton.setOnClickListener {
            applySettings()
        }

        return view
    }

    /**
     * Загружает сохраненные настройки темы и размера шрифта
     */
    private fun loadSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("sunportal_prefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        val currentFontSize = sharedPreferences.getString("font_size", "medium")

        // Установка выбранной темы
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

        // Установка выбранного размера шрифта
        when (currentFontSize) {
            "small" -> fontRadioGroup.check(R.id.radio_small)
            "medium" -> fontRadioGroup.check(R.id.radio_medium)
            "large" -> fontRadioGroup.check(R.id.radio_large)
        }
    }

    /**
     * Применяет выбранные настройки и сохраняет их
     */
    private fun applySettings() {
        val selectedThemeId = themeRadioGroup.checkedRadioButtonId
        val selectedFontId = fontRadioGroup.checkedRadioButtonId

        // Определение режима темы
        val themeMode = when (selectedThemeId) {
            R.id.radio_night -> AppCompatDelegate.MODE_NIGHT_YES
            R.id.radio_system -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }

        // Определение размера шрифта
        val fontSize = when (selectedFontId) {
            R.id.radio_small -> "small"
            R.id.radio_large -> "large"
            else -> "medium"
        }

        // Сохранение настроек
        val sharedPreferences = requireContext().getSharedPreferences("sunportal_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("theme_mode", themeMode).apply()
        sharedPreferences.edit().putString("font_size", fontSize).apply()

        // Применение темы
        AppCompatDelegate.setDefaultNightMode(themeMode)

        // Показываем сообщение пользователю
        Toast.makeText(requireContext(), "Налаштування застосовано", Toast.LENGTH_SHORT).show()

        // Перезапускаем активность для применения всех изменений
        requireActivity().recreate()
    }
}