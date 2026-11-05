package com.sunportal.mobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunportal.mobile.data.InverterData
import com.sunportal.mobile.data.BatteryData
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * MainViewModel для Android 15+
 * Упрощенная реализация без сложных зависимостей
 */
class MainViewModel : ViewModel() {

    private val _inverterData = MutableLiveData<InverterData>()
    val inverterData: LiveData<InverterData> = _inverterData

    private val _batteryData = MutableLiveData<BatteryData>()
    val batteryData: LiveData<BatteryData> = _batteryData

    private val _connectionStatus = MutableLiveData<String>("connecting")
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        startDataUpdates()
    }

    private fun startDataUpdates() {
        viewModelScope.launch {
            while (true) {
                loadMockData()
                delay(5000) // Обновление каждые 5 секунд
            }
        }
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Генерация mock данных
                val inverterData = InverterData(
                    currentPower = (2000..4000).random().toDouble(),
                    voltage = (220..240).random().toDouble(),
                    current = (8..15).random().toDouble(),
                    temperature = (30..45).random().toDouble(),
                    status = listOf("Normal", "Warning", "Error").random(),
                    efficiency = (85..98).random().toDouble(),
                    frequency = 49.5 + kotlin.math.random() * 1.0,
                    powerFactor = 0.85 + kotlin.math.random() * 0.13,
                    totalEnergy = (10000..20000).random().toDouble(),
                    dailyEnergy = (15..25).random().toDouble(),
                    reactivePower = (200..800).random().toDouble(),
                    apparentPower = (2500..3500).random().toDouble()
                )

                val batteryData = BatteryData(
                    status = listOf("Normal", "Warning", "Error").random(),
                    level = (60..95).random(),
                    temperature = (20..35).random().toDouble(),
                    voltage = (45..55).random().toDouble(),
                    current = (-10..20).random().toDouble(),
                    health = (80..100).random(),
                    cycles = (100..500).random(),
                    voltageMax = 54.0,
                    voltageMin = 42.0,
                    currentMax = 25.0,
                    currentMin = -10.0,
                    power = (-500..1000).random().toDouble(),
                    energy = (5..20).random().toDouble()
                )

                _inverterData.value = inverterData
                _batteryData.value = batteryData
                _connectionStatus.value = "online"

            } catch (e: Exception) {
                _connectionStatus.value = "error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadMockData()
    }

    override fun onCleared() {
        super.onCleared()
        // Очистка ресурсов при уничтожении ViewModel
    }
}