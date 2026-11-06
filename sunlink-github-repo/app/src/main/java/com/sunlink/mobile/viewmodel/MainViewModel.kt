package com.sunlink.mobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunlink.mobile.data.model.BatteryData
import com.sunlink.mobile.data.model.InverterData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {

    private val _inverterData = MutableLiveData<InverterData>()
    val inverterData: LiveData<InverterData> = _inverterData

    private val _batteryData = MutableLiveData<BatteryData>()
    val batteryData: LiveData<BatteryData> = _batteryData

    private val _connectionStatus = MutableLiveData<String>("connecting")
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _lastUpdate = MutableLiveData<String>("")
    val lastUpdate: LiveData<String> = _lastUpdate

    // StateFlow for Compose UI
    private val _uiState = MutableStateFlow<MainUIState>(MainUIState.Loading)
    val uiState: StateFlow<MainUIState> = _uiState

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
                val inverterData = InverterData.generateMock()
                val batteryData = BatteryData.generateMock()

                _inverterData.value = inverterData
                _batteryData.value = batteryData
                _connectionStatus.value = "online"
                
                // Форматирование времени последнего обновления
                val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                _lastUpdate.value = dateFormat.format(Date())

                // Обновление UI состояния
                _uiState.value = MainUIState.Success(
                    inverterData = inverterData,
                    batteryData = batteryData,
                    connectionStatus = "online",
                    lastUpdate = _lastUpdate.value ?: ""
                )

            } catch (e: Exception) {
                _connectionStatus.value = "error"
                _uiState.value = MainUIState.Error(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadMockData()
    }

    fun getPowerStatus(): String {
        return when {
            _inverterData.value?.currentPower ?: 0.0 > 3000 -> "Высокая мощность"
            _inverterData.value?.currentPower ?: 0.0 > 2000 -> "Нормальная мощность"
            else -> "Низкая мощность"
        }
    }

    fun getBatteryStatus(): String {
        return when {
            _batteryData.value?.level ?: 0 > 80 -> "Высокий заряд"
            _batteryData.value?.level ?: 0 > 50 -> "Средний заряд"
            else -> "Низкий заряд"
        }
    }

    fun getEfficiencyColor(): Int {
        return when {
            (_inverterData.value?.efficiency ?: 0.0) > 90 -> android.graphics.Color.GREEN
            (_inverterData.value?.efficiency ?: 0.0) > 80 -> android.graphics.Color.YELLOW
            else -> android.graphics.Color.RED
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Очистка ресурсов при уничтожении ViewModel
    }
}

sealed class MainUIState {
    object Loading : MainUIState()
    data class Success(
        val inverterData: InverterData,
        val batteryData: BatteryData,
        val connectionStatus: String,
        val lastUpdate: String
    ) : MainUIState()
    data class Error(val message: String) : MainUIState()
}