package app.sunportal.api

import retrofit2.http.GET
import retrofit2.http.Query
import app.sunportal.data.model.InverterData
import app.sunportal.data.model.WeatherData
import app.sunportal.data.model.OpenMeteoData
import retrofit2.http.POST
import retrofit2.http.Body

/**
 * API сервис для SunPortal приложения
 */
interface ApiService {
    
    // Получение данных инвертора
    @GET("api/live/raw")
    suspend fun getInverterData(): InverterData
    
    @GET("api/live")
    suspend fun getInverterLiveData(): InverterData
    
    // Исторические данные
    @GET("api/?action=history&period=day")
    suspend fun getHistoryData(@Query("period") period: String = "day"): Map<String, Any>
    
    @GET("api/?action=history&period=week")
    suspend fun getWeeklyHistory(): Map<String, Any>
    
    @GET("api/?action=history&period=month")
    suspend fun getMonthlyHistory(): Map<String, Any>
    
    // Данные блекаута
    @GET("api/?action=blackout")
    suspend fun getBlackoutData(): Map<String, Any>
    
    // Погодные данные
    @GET("weather_read.php")
    suspend fun getWeatherData(): WeatherData
    
    // Прогноз производства
    @GET("forecast_read.php")
    suspend fun getForecastData(): Map<String, Any>
    
    // Тарифы TOU
    @GET("tou_read.php")
    suspend fun getTouData(): Map<String, Any>
    
    // Статус системы
    @GET("api/status")
    suspend fun getSystemStatus(): Map<String, Any>
    
    // WebSocket для real-time обновлений
    @POST("api/ws/connect")
    suspend fun connectWebSocket(@Body connectionParams: Map<String, String>): Map<String, Any>
    
    // Настройки инвертора
    @POST("api/inverter/settings")
    suspend fun updateInverterSettings(@Body settings: Map<String, Any>): Map<String, Any>
    
    // Управление батареей
    @POST("api/battery/settings")
    suspend fun updateBatterySettings(@Body settings: Map<String, Any>): Map<String, Any>
    
    // Управление тарифами
    @POST("api/tariffs/settings")
    suspend fun updateTariffSettings(@Body settings: Map<String, Any>): Map<String, Any>
}