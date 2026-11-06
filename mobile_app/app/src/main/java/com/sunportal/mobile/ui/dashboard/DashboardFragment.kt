package com.sunportal.mobile.ui.dashboard

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.sunportal.mobile.databinding.FragmentDashboardBinding
import com.sunportal.mobile.viewmodel.MainViewModel
import com.sunportal.mobile.R

/**
 * DashboardFragment - главный экран приложения
 * Упрощенная реализация для Android 15+
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
        refreshData()
    }

    private fun setupObservers() {
        viewModel.inverterData.observe(viewLifecycleOwner, Observer { data ->
            data?.let {
                updateInverterData(it)
            }
        })

        viewModel.batteryData.observe(viewLifecycleOwner, Observer { data ->
            data?.let {
                updateBatteryData(it)
            }
        })

        viewModel.connectionStatus.observe(viewLifecycleOwner, Observer { status ->
            updateConnectionStatus(status)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        })
    }

    private fun updateInverterData(data: com.sunportal.mobile.data.InverterData) {
        // Находим элементы в layout
        val powerValue = binding.root.findViewById<TextView>(R.id.card_power_value)
        val voltageValue = binding.root.findViewById<TextView>(R.id.card_voltage_value)
        val efficiencyValue = binding.root.findViewById<TextView>(R.id.card_efficiency_value)
        val statusValue = binding.root.findViewById<TextView>(R.id.card_status_value)
        val lastUpdateText = binding.root.findViewById<TextView>(R.id.last_update_text)
        
        powerValue?.text = "${data.currentPower.toInt()} Вт"
        voltageValue?.text = "${data.voltage.toInt()} В"
        efficiencyValue?.text = "${data.efficiency.toInt()}%"
        
        // Обновление статуса
        when (data.status) {
            "Normal" -> {
                statusValue?.text = "Нормально"
                statusValue?.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }
            "Warning" -> {
                statusValue?.text = "Предупреждение"
                statusValue?.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            }
            "Error" -> {
                statusValue?.text = "Ошибка"
                statusValue?.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            }
            else -> {
                statusValue?.text = "Неизвестно"
                statusValue?.setTextColor(resources.getColor(android.R.color.darker_gray))
            }
        }
        
        lastUpdateText?.text = "Обновлено: ${System.currentTimeMillis() / 1000}s назад"
    }

    private fun updateBatteryData(data: com.sunportal.mobile.data.BatteryData) {
        // Находим элементы в layout
        val batteryLevelValue = binding.root.findViewById<TextView>(R.id.card_battery_level_value)
        val batteryLevelBar = binding.root.findViewById<ProgressBar>(R.id.card_battery_level_bar)
        
        batteryLevelValue?.text = "${data.level}%"
        batteryLevelBar?.progress = data.level
        
        // Обновление цвета батареи
        when (data.status) {
            "Normal" -> batteryLevelBar?.progressTintList = resources.getColorStateList(android.R.color.holo_green_dark)
            "Warning" -> batteryLevelBar?.progressTintList = resources.getColorStateList(android.R.color.holo_orange_dark)
            "Error" -> batteryLevelBar?.progressTintList = resources.getColorStateList(android.R.color.holo_red_dark)
            else -> batteryLevelBar?.progressTintList = resources.getColorStateList(android.R.color.darker_gray)
        }
    }

    private fun updateConnectionStatus(status: String) {
        when (status) {
            "online" -> {
                binding.statusIndicator.setImageResource(android.R.drawable.ic_dialog_info)
                binding.statusText.text = "Онлайн"
            }
            "offline" -> {
                binding.statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.statusText.text = "Офлайн"
            }
            "connecting" -> {
                binding.statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.statusText.text = "Подключение..."
            }
            "error" -> {
                binding.statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.statusText.text = "Ошибка"
            }
        }
    }

    private fun setupClickListeners() {
        // Находим элементы в layout
        val refreshButton = binding.root.findViewById<View>(R.id.refresh_button)
        val cardPower = binding.root.findViewById<View>(R.id.card_power)
        val cardVoltage = binding.root.findViewById<View>(R.id.card_voltage)
        val cardBattery = binding.root.findViewById<View>(R.id.card_battery)
        val cardStatus = binding.root.findViewById<View>(R.id.card_status)
        val cardEfficiency = binding.root.findViewById<View>(R.id.card_efficiency)

        refreshButton?.setOnClickListener {
            refreshData()
        }

        // Карточки для навигации
        cardPower?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Мощность", android.widget.Toast.LENGTH_SHORT).show()
        }

        cardVoltage?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Напряжение", android.widget.Toast.LENGTH_SHORT).show()
        }

        cardBattery?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Батарея", android.widget.Toast.LENGTH_SHORT).show()
        }

        cardStatus?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Статус", android.widget.Toast.LENGTH_SHORT).show()
        }

        cardEfficiency?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Эффективность", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshData() {
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}