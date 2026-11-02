package app.sunportal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sunportal.api.RetrofitClient
import app.sunportal.api.OpenMeteoRetrofitClient
import app.sunportal.data.InverterData
import app.sunportal.data.WeatherData
import app.sunportal.data.model.OpenMeteoData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * ViewModel для главного экрана приложения
 */
class MainViewModel : ViewModel() {
    
    // Состояния приложения
    private val _inverterData = MutableStateFlow<InverterData?>(null)
    val inverterData: StateFlow<InverterData?> = _inverterData.asStateFlow()
    
    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow<String>("connecting")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Загрузка данных инвертора
    fun loadInverterData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val data = RetrofitClient.apiService.getInverterData()
                _inverterData.value = data
                _connectionStatus.value = "online"
            } catch (e: HttpException) {
                _connectionStatus.value = "error"
                _error.value = "HTTP Error: ${e.code()}"
            } catch (e: IOException) {
                _connectionStatus.value = "offline"
                _error.value = "Network Error: ${e.message}"
            } catch (e: Exception) {
                _connectionStatus.value = "error"
                _error.value = "Unknown Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузка погодных данных
    fun loadWeatherData(latitude: Double = 50.0, longitude: Double = 30.0) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val data = OpenMeteoRetrofitClient.openMeteoApi.getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude
                )
                _weatherData.value = data.toWeatherData()
            } catch (e: Exception) {
                _error.value = "Weather Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузка исторических данных
    fun loadHistoryData(period: String = "day") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val data = RetrofitClient.apiService.getHistoryData(period = period)
                // Обработка исторических данных
                _inverterData.value = _inverterData.value?.copy(
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _error.value = "History Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузка данных блекаута
    fun loadBlackoutData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val data = RetrofitClient.apiService.getBlackoutData()
                // Обработка данных блекаута
            } catch (e: Exception) {
                _error.value = "Blackout Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузка прогноза
    fun loadForecastData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val data = RetrofitClient.apiService.getForecastData()
                // Обработка прогноза
            } catch (e: Exception) {
                _error.value = "Forecast Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузка тарифов TOU
    fun loadTouData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val data = RetrofitClient.apiService.getTouData()
                // Обработка тарифов
            } catch (e: Exception) {
                _error.value = "TOU Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Обновление данных инвертора
    fun updateInverterSettings(settings: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val response = RetrofitClient.apiService.updateInverterSettings(settings)
                // Обновление настроек
                loadInverterData() // Перезагрузка данных после обновления
            } catch (e: Exception) {
                _error.value = "Update Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Обновление настроек батареи
    fun updateBatterySettings(settings: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val response = RetrofitClient.apiService.updateBatterySettings(settings)
                // Обновление настроек батареи
                loadInverterData() // Перезагрузка данных после обновления
            } catch (e: Exception) {
                _error.value = "Battery Update Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Обновление настроек тарифов
    fun updateTariffSettings(settings: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val response = RetrofitClient.apiService.updateTariffSettings(settings)
                // Обновление настроек тарифов
            } catch (e: Exception) {
                _error.value = "Tariff Update Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Перезагрузка всех данных
    fun refreshAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                loadInverterData()
                loadWeatherData()
                loadHistoryData()
                loadBlackoutData()
                loadForecastData()
                loadTouData()
            } catch (e: Exception) {
                _error.value = "Refresh Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Проверка соединения
    fun checkConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val status = RetrofitClient.apiService.getSystemStatus()
                _connectionStatus.value = "online"
            } catch (e: Exception) {
                _connectionStatus.value = "offline"
                _error.value = "Connection Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}