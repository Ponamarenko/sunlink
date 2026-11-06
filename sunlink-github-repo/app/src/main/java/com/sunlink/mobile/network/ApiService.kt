package com.sunlink.mobile.network

import com.sunlink.mobile.data.model.BatteryData
import com.sunlink.mobile.data.model.InverterData
import retrofit2.http.*
import retrofit2.Response

interface ApiService {
    
    @GET("/api/inverter/data")
    suspend fun getInverterData(): Response<InverterData>
    
    @GET("/api/battery/data")
    suspend fun getBatteryData(): Response<BatteryData>
    
    @POST("/api/inverter/control")
    suspend fun controlInverter(
        @Body controlRequest: ControlRequest
    ): Response<ControlResponse>
    
    @POST("/api/battery/control")
    suspend fun controlBattery(
        @Body batteryControlRequest: BatteryControlRequest
    ): Response<BatteryControlResponse>
    
    @GET("/api/forecast/data")
    suspend fun getForecastData(): Response<ForecastData>
    
    @GET("/api/weather/data")
    suspend fun getWeatherData(): Response<WeatherData>
    
    @POST("/api/settings")
    suspend fun updateSettings(
        @Body settings: Settings
    ): Response<SettingsResponse>
    
    @GET("/api/system/status")
    suspend fun getSystemStatus(): Response<SystemStatus>
}

data class ControlRequest(
    val action: String,
    val parameters: Map<String, Any> = emptyMap()
)

data class ControlResponse(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any>? = null
)

data class BatteryControlRequest(
    val action: String, // charge, discharge, optimize
    val targetLevel: Int? = null,
    val maxPower: Double? = null
)

data class BatteryControlResponse(
    val success: Boolean,
    val message: String,
    val estimatedTime: Double? = null
)

data class ForecastData(
    val energyProduction: List<Double>,
    val weatherForecast: List<WeatherForecast>,
    val recommendations: List<String>
)

data class WeatherForecast(
    val date: String,
    val temperature: Double,
    val humidity: Double,
    val windSpeed: Double,
    val condition: String
)

data class WeatherData(
    val temperature: Double,
    val humidity: Double,
    val windSpeed: Double,
    val condition: String,
    val icon: String
)

data class Settings(
    val theme: String = "light",
    val language: String = "en",
    val updateInterval: Int = 5,
    val notifications: Boolean = true,
    val autoOptimization: Boolean = false
)

data class SettingsResponse(
    val success: Boolean,
    val message: String,
    val settings: Settings
)

data class SystemStatus(
    val online: Boolean,
    val uptime: Long,
    val lastUpdate: Long,
    val errors: List<String>
)