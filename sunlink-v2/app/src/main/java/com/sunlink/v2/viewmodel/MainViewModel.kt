package com.sunlink.v2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunlink.v2.data.InverterData
import com.sunlink.v2.data.BatteryData
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

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
                    currentPower = Random.nextInt(2000, 4000).toDouble(),
                    voltage = Random.nextInt(220, 240).toDouble(),
                    current = Random.nextInt(8, 15).toDouble(),
                    temperature = Random.nextInt(30, 45).toDouble(),
                    status = listOf("Normal", "Warning", "Error").random(),
                    efficiency = Random.nextInt(85, 98).toDouble(),
                    frequency = 49.5 + Random.nextDouble() * 1.0,
                    powerFactor = 0.85 + Random.nextDouble() * 0.13,
                    totalEnergy = Random.nextInt(10000, 20000).toDouble(),
                    dailyEnergy = Random.nextInt(15, 25).toDouble(),
                    reactivePower = Random.nextInt(200, 800).toDouble(),
                    apparentPower = Random.nextInt(2500, 3500).toDouble()
                )

                val batteryData = BatteryData(
                    status = listOf("Normal", "Warning", "Error").random(),
                    level = Random.nextInt(60, 95),
                    temperature = Random.nextInt(20, 35).toDouble(),
                    voltage = Random.nextInt(45, 55).toDouble(),
                    current = Random.nextInt(-10, 20).toDouble(),
                    health = Random.nextInt(80, 100),
                    cycles = Random.nextInt(100, 500),
                    voltageMax = 54.0,
                    voltageMin = 42.0,
                    currentMax = 25.0,
                    currentMin = -10.0,
                    power = Random.nextInt(-500, 1000).toDouble(),
                    energy = Random.nextInt(5, 20).toDouble()
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