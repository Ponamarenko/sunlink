package com.sunportal.mobile.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import com.sunportal.mobile.data.InverterData
import com.sunportal.mobile.data.BatteryData
import com.sunportal.mobile.data.ForecastData
import retrofit2.Call

/**
 * API сервис для SunLink приложения
 * Основан на SunPortal API с адаптацией под Android
 */
interface ApiService {
    
    // === ИНВЕРТОР ===
    
    /**
     * Получение текущих данных инвертора
     */
    @GET("api/live/raw")
    suspend fun getInverterData(): InverterData
    
    /**
     * Получение live данных инвертора
     */
    @GET("api/live")
    suspend fun getInverterLiveData(): InverterData
    
    /**
     * Получение исторических данных
     */
    @GET("api/?action=history")
    suspend fun getHistoryData(@Query("period") period: String = "day"): Map<String, Any>
    
    @GET("api/?action=history&period=week")
    suspend fun getWeeklyHistory(): Map<String, Any>
    
    @GET("api/?action=history&period=month")
    suspend fun getMonthlyHistory(): Map<String, Any>
    
    @GET("api/?action=history&period=year")
    suspend fun getYearlyHistory(): Map<String, Any>
    
    // === БАТАРЕЯ ===
    
    /**
     * Получение данных батареи
     */
    @GET("api/battery")
    suspend fun getBatteryData(): BatteryData
    
    /**
     * Получение данных зарядки/разрядки
     */
    @GET("api/battery/charging")
    suspend fun getBatteryChargingData(): Map<String, Any>
    
    // === ПРОГНОЗ ===
    
    /**
     * Получение прогноза производства
     */
    @GET("forecast_read.php")
    suspend fun getForecastData(): Map<String, Any>
    
    /**
     * Получение погодных данных
     */
    @GET("weather_read.php")
    suspend fun getWeatherData(): Map<String, Any>
    
    // === СИСТЕМА ===
    
    /**
     * Получение статуса системы
     */
    @GET("api/status")
    suspend fun getSystemStatus(): Map<String, Any>
    
    /**
     * Получение данных блекаута
     */
    @GET("api/?action=blackout")
    suspend fun getBlackoutData(): Map<String, Any>
    
    // === ТАРИФЫ ===
    
    /**
     * Получение тарифов TOU
     */
    @GET("tou_read.php")
    suspend fun getTouData(): Map<String, Any>
    
    // === УПРАВЛЕНИЕ ===
    
    /**
     * Отправка команды управления
     */
    @POST("api/control")
    suspend fun sendControlCommand(@Body command: ControlCommand): Map<String, Any>
    
    /**
     * Перезапуск инвертора
     */
    @POST("api/restart")
    suspend fun restartInverter(): Map<String, Any>
    
    /**
     * Обновление прошивки
     */
    @POST("api/update")
    suspend fun updateFirmware(@Body updateRequest: UpdateRequest): Map<String, Any>
}

/**
 * Команда управления
 */
data class ControlCommand(
    val action: String,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * Запрос на обновление
 */
data class UpdateRequest(
    val version: String,
    val force: Boolean = false
)

/**
 * Retrofit клиент для API
 */
object RetrofitClient {
    private const val BASE_URL = "http://sunlink.pp.ua:8000/"
    
    val apiService: ApiService by lazy {
        // TODO: Реализовать создание Retrofit клиента
        // Для начала используем заглушки
        throw NotImplementedError("Retrofit клиент не реализован")
    }
}

/**
 * Mock API сервис для тестирования
 * TODO: Заменить на реальный Retrofit клиент
 */
object MockApiService {
    
    /**
     * Генерация mock данных инвертора
     */
    suspend fun getMockInverterData(): InverterData {
        return InverterData(
            currentPower = (2000..4000).random().toDouble(),
            voltage = (220..240).random().toDouble(),
            current = (8..15).random().toDouble(),
            temperature = (30..45).random().toDouble(),
            status = when ((1..10).random()) {
                1 -> "Error"
                2 -> "Warning"
                else -> "Normal"
            },
            efficiency = (85..98).random().toDouble(),
            frequency = 49.5 + Math.random() * 1.0,
            powerFactor = 0.85 + Math.random() * 0.13,
            totalEnergy = (10000..20000).random().toDouble(),
            dailyEnergy = (15..25).random().toDouble(),
            reactivePower = (200..800).random().toDouble(),
            apparentPower = (2500..3500).random().toDouble()
        )
    }
    
    /**
     * Генерация mock данных батареи
     */
    suspend fun getMockBatteryData(): BatteryData {
        return BatteryData(
            status = when ((1..10).random()) {
                1 -> "Error"
                2 -> "Warning"
                else -> "Normal"
            },
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
    }
    
    /**
     * Генерация mock прогноза
     */
    suspend fun getMockForecastData(): ForecastData {
        return ForecastData(
            todayMaxPower = 2.5 + Math.random() * 1.5,
            todayEnergy = 18 + Math.random() * 10,
            tomorrowMaxPower = 2.0 + Math.random() * 1.5,
            tomorrowEnergy = 15 + Math.random() * 10,
            weeklyTotal = 120 + Math.random() * 60,
            monthlyTotal = 500 + Math.random() * 300,
            cloudiness = (10 + Math.random() * 70).toInt(),
            windSpeed = Math.random() * 10,
            temperature = 15 + Math.random() * 15,
            humidity = (40 + Math.random() * 40).toInt(),
            uvIndex = (1 + Math.random() * 9).toInt(),
            sunrise = "06:15",
            sunset = "18:30",
            weeklyEnergy = mapOf(
                "Пн" to (18 + Math.random() * 7),
                "Вт" to (16 + Math.random() * 8),
                "Ср" to (15 + Math.random() * 8),
                "Чт" to (17 + Math.random() * 8),
                "Пт" to (19 + Math.random() * 7),
                "Сб" to (20 + Math.random() * 8),
                "Вс" to (16 + Math.random() * 8)
            )
        )
    }
}