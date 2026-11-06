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
                    currentPower = kotlin.random.Random.nextInt(2000, 4000).toDouble(),
                    voltage = kotlin.random.Random.nextInt(220, 240).toDouble(),
                    current = kotlin.random.Random.nextInt(8, 15).toDouble(),
                    temperature = kotlin.random.Random.nextInt(30, 45).toDouble(),
                    status = listOf("Normal", "Warning", "Error").random(),
                    efficiency = kotlin.random.Random.nextInt(85, 98).toDouble(),
                    frequency = 49.5 + kotlin.random.Random.nextDouble() * 1.0,
                    powerFactor = 0.85 + kotlin.random.Random.nextDouble() * 0.13,
                    totalEnergy = kotlin.random.Random.nextInt(10000, 20000).toDouble(),
                    dailyEnergy = kotlin.random.Random.nextInt(15, 25).toDouble(),
                    reactivePower = kotlin.random.Random.nextInt(200, 800).toDouble(),
                    apparentPower = kotlin.random.Random.nextInt(2500, 3500).toDouble()
                )

                val batteryData = BatteryData(
                    status = listOf("Normal", "Warning", "Error").random(),
                    level = kotlin.random.Random.nextInt(60, 95),
                    temperature = kotlin.random.Random.nextInt(20, 35).toDouble(),
                    voltage = kotlin.random.Random.nextInt(45, 55).toDouble(),
                    current = kotlin.random.Random.nextInt(-10, 20).toDouble(),
                    health = kotlin.random.Random.nextInt(80, 100),
                    cycles = kotlin.random.Random.nextInt(100, 500),
                    voltageMax = 54.0,
                    voltageMin = 42.0,
                    currentMax = 25.0,
                    currentMin = -10.0,
                    power = kotlin.random.Random.nextInt(-500, 1000).toDouble(),
                    energy = kotlin.random.Random.nextInt(5, 20).toDouble()
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