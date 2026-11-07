package com.sunlink.v2.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import android.widget.Toast
import com.sunlink.v2.databinding.FragmentDashboardBinding
import com.sunlink.v2.viewmodel.MainViewModel
import com.sunlink.v2.R

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

    private fun updateInverterData(data: com.sunlink.v2.data.InverterData) {
        binding.powerValue.text = "${data.currentPower.toInt()} Вт"
        binding.voltageValue.text = "${data.voltage.toInt()} В"
        binding.efficiencyValue.text = "${data.efficiency.toInt()}%"
        
        // Обновление статуса
        when (data.status) {
            "Normal" -> {
                binding.statusValue.text = "Нормально"
                binding.statusValue.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }
            "Warning" -> {
                binding.statusValue.text = "Предупреждение"
                binding.statusValue.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            }
            "Error" -> {
                binding.statusValue.text = "Ошибка"
                binding.statusValue.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            }
            else -> {
                binding.statusValue.text = "Неизвестно"
                binding.statusValue.setTextColor(resources.getColor(android.R.color.darker_gray))
            }
        }
        
        binding.lastUpdateText.text = "Обновлено: ${System.currentTimeMillis() / 1000}s назад"
    }

    private fun updateBatteryData(data: com.sunlink.v2.data.BatteryData) {
        binding.batteryLevelValue.text = "${data.level}%"
        binding.batteryLevelBar.progress = data.level
        
        // Обновление цвета батареи
        when (data.status) {
            "Normal" -> binding.batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.holo_green_dark)
            "Warning" -> binding.batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.holo_orange_dark)
            "Error" -> binding.batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.holo_red_dark)
            else -> binding.batteryLevelBar.progressTintList = resources.getColorStateList(android.R.color.darker_gray)
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
        binding.refreshButton.setOnClickListener {
            refreshData()
        }

        // Карточки для навигации
        binding.powerCard.setOnClickListener {
            Toast.makeText(requireContext(), "Мощность", Toast.LENGTH_SHORT).show()
        }

        binding.voltageCard.setOnClickListener {
            Toast.makeText(requireContext(), "Напряжение", Toast.LENGTH_SHORT).show()
        }

        binding.batteryCard.setOnClickListener {
            Toast.makeText(requireContext(), "Батарея", Toast.LENGTH_SHORT).show()
        }

        binding.statusCard.setOnClickListener {
            Toast.makeText(requireContext(), "Статус", Toast.LENGTH_SHORT).show()
        }

        binding.efficiencyCard.setOnClickListener {
            Toast.makeText(requireContext(), "Эффективность", Toast.LENGTH_SHORT).show()
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