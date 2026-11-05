package com.sunportal.mobile.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.sunportal.mobile.R
import com.sunportal.mobile.viewmodel.MainViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Фрагмент главного дашборда SunLink
 * Реализует real-time мониторинг системы
 */
class DashboardFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var powerCard: CardView
    private lateinit var voltageCard: CardView
    private lateinit var batteryCard: CardView
    private lateinit var statusCard: CardView
    private lateinit var efficiencyCard: CardView
    
    private lateinit var powerValue: TextView
    private lateinit var voltageValue: TextView
    private lateinit var batteryLevelValue: TextView
    private lateinit var batteryLevelBar: ProgressBar
    private lateinit var statusValue: TextView
    private lateinit var efficiencyValue: TextView
    
    private lateinit var lastUpdateText: TextView
    private lateinit var statusIndicator: ImageView
    private lateinit var refreshButton: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViewModel()
        initViews(view)
        setupDataObservers()
        loadData()
        startRealTimeUpdates()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    private fun initViews(view: View) {
        // Заголовок
        val title = view.findViewById<TextView>(R.id.dashboard_title)
        title.text = "SunLink Dashboard"
        
        // Карточки
        powerCard = view.findViewById(R.id.card_power)
        voltageCard = view.findViewById(R.id.card_voltage)
        batteryCard = view.findViewById(R.id.card_battery)
        statusCard = view.findViewById(R.id.card_status)
        efficiencyCard = view.findViewById(R.id.card_efficiency)
        
        // Значения
        powerValue = view.findViewById(R.id.card_power_value)
        voltageValue = view.findViewById(R.id.card_voltage_value)
        batteryLevelValue = view.findViewById(R.id.card_battery_level_value)
        batteryLevelBar = view.findViewById(R.id.card_battery_level_bar)
        statusValue = view.findViewById(R.id.card_status_value)
        efficiencyValue = view.findViewById(R.id.card_efficiency_value)
        
        // Статус и обновление
        lastUpdateText = view.findViewById(R.id.last_update_text)
        statusIndicator = view.findViewById(R.id.status_indicator)
        refreshButton = view.findViewById(R.id.refresh_button)
        
        // Обработчики
        refreshButton.setOnClickListener {
            refreshData()
        }
        
        // Обработчики карточек
        powerCard.setOnClickListener {
            navigateTo("live")
        }
        
        voltageCard.setOnClickListener {
            navigateTo("charts")
        }
        
        batteryCard.setOnClickListener {
            navigateTo("battery")
        }
        
        statusCard.setOnClickListener {
            navigateTo("overview")
        }
        
        efficiencyCard.setOnClickListener {
            navigateTo("forecast")
        }
    }

    private fun setupDataObservers() {
        // Временно отключаем observers для избежания проблем с совместимостью
        // TODO: Реализовать правильную работу с LiveData в будущем
    }

    private fun updateInverterData(data: com.sunportal.mobile.data.InverterData?) {
        data?.let {
            powerValue.text = "${it.currentPower.toInt()} Вт"
            voltageValue.text = "${it.voltage.toInt()} В"
            
            // Обновление цвета статуса
            when (it.status) {
                "Normal" -> {
                    statusValue.text = "Нормально"
                    statusValue.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                }
                "Warning" -> {
                    statusValue.text = "Предупреждение"
                    statusValue.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                }
                "Error" -> {
                    statusValue.text = "Ошибка"
                    statusValue.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                else -> {
                    statusValue.text = "Неизвестно"
                    statusValue.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }
            }
            
            efficiencyValue.text = "${it.efficiency.toInt()}%"
        }
    }

    private fun updateBatteryData(data: com.sunportal.mobile.data.BatteryData?) {
        data?.let {
            batteryLevelValue.text = "${it.level}%"
            batteryLevelBar.progress = it.level
            
            // Обновление цвета батареи
            when (it.status) {
                "Normal" -> {
                    batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.holo_green_dark)
                }
                "Warning" -> {
                    batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.holo_orange_dark)
                }
                "Error" -> {
                    batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.holo_red_dark)
                }
                else -> {
                    batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.darker_gray)
                }
            }
        }
    }

    private fun updateConnectionStatus(status: String) {
        when (status) {
            "online" -> {
                statusIndicator.setImageResource(android.R.drawable.ic_dialog_info)
                lastUpdateText.text = viewModel.getLastUpdateTimeFormatted()
            }
            "offline" -> {
                statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                lastUpdateText.text = "Нет соединения"
            }
            "connecting" -> {
                statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                lastUpdateText.text = "Подключение..."
            }
            "loading" -> {
                statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                lastUpdateText.text = "Загрузка данных..."
            }
            "restarting" -> {
                statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                lastUpdateText.text = "Перезапуск..."
            }
            "error" -> {
                statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                lastUpdateText.text = "Ошибка соединения"
            }
        }
    }

    private fun showError(error: String?) {
        error?.let {
            // Показать ошибку через Toast или Snackbar
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun updateLastUpdateTime() {
        lastUpdateText.text = viewModel.getLastUpdateTimeFormatted()
    }

    private fun loadData() {
        viewModel.refreshData()
    }

    private fun refreshData() {
        viewModel.refreshData()
    }

    private fun startRealTimeUpdates() {
        lifecycleScope.launch {
            while (true) {
                delay(5000) // Обновление каждые 5 секунд
                viewModel.refreshData()
            }
        }
    }

    private fun navigateTo(screen: String) {
        activity?.let {
            (it as? com.sunportal.mobile.MainActivity)?.navigateTo(screen)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Остановка real-time обновлений
        // TODO: Реализовать остановку корутин
    }
}