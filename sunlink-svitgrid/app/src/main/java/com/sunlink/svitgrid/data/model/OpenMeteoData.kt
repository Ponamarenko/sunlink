package app.sunportal.data.model

import com.google.gson.annotations.SerializedName

/**
 * Модель данных Open-Meteo API
 */
data class OpenMeteoData(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("timezone_abbreviation") val timezoneAbbreviation: String,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int,
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    @SerializedName("hourly") val hourly: Hourly?,
    @SerializedName("daily") val daily: Daily?
) {
    // Текущая погода
    data class CurrentWeather(
        @SerializedName("temperature") val temperature: Double,
        @SerializedName("windspeed") val windSpeed: Double,
        @SerializedName("winddirection") val windDirection: Double,
        @SerializedName("weathercode") val weatherCode: Int,
        @SerializedName("time") val time: String
    )
    
    // Почасовые данные
    data class Hourly(
        @SerializedName("time") val time: List<String>,
        @SerializedName("temperature_2m") val temperature2m: List<Double>,
        @SerializedName("relativehumidity_2m") val relativeHumidity2m: List<Double>,
        @SerializedName("windspeed_10m") val windSpeed10m: List<Double>,
        @SerializedName("weathercode") val weatherCode: List<Int>
    )
    
    // Ежедневные данные
    data class Daily(
        @SerializedName("time") val time: List<String>,
        @SerializedName("weathercode") val weatherCode: List<Int>,
        @SerializedName("temperature_2m_max") val temperature2mMax: List<Double>,
        @SerializedName("temperature_2m_min") val temperature2mMin: List<Double>
    )
    
    // Конструктор без параметров для Gson
    constructor() : this(
        latitude = 0.0,
        longitude = 0.0,
        timezone = "auto",
        timezoneAbbreviation = "UTC",
        utcOffsetSeconds = 0,
        currentWeather = null,
        hourly = null,
        daily = null
    )
    
    // Преобразование в WeatherData
    fun toWeatherData(): WeatherData {
        return WeatherData(
            timestamp = System.currentTimeMillis(),
            temperature = currentWeather?.temperature ?: 0.0,
            humidity = hourly?.relativeHumidity2m?.firstOrNull()?.toDouble() ?: 0.0,
            pressure = 1013.0, // Open-Meteo не предоставляет давление
            windSpeed = currentWeather?.windSpeed ?: 0.0,
            windDirection = currentWeather?.windDirection ?: 0.0,
            cloudiness = 0.0, // Open-Meteo не предоставляет облачность напрямую
            visibility = 10.0, // Значение по умолчанию
            uvIndex = 0.0, // Open-Meteo не предоставляет UV индекс
            location = "Unknown",
            source = "Open-Meteo"
        )
    }
}