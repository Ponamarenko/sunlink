package app.sunportal.data.model

import com.google.gson.annotations.SerializedName

/**
 * Модель данных погоды
 */
data class WeatherData(
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("humidity") val humidity: Double,
    @SerializedName("pressure") val pressure: Double,
    @SerializedName("wind_speed") val windSpeed: Double,
    @SerializedName("wind_direction") val windDirection: Double,
    @SerializedName("cloudiness") val cloudiness: Double,
    @SerializedName("visibility") val visibility: Double,
    @SerializedName("uv_index") val uvIndex: Double,
    @SerializedName("location") val location: String,
    @SerializedName("source") val source: String
) {
    // Конструктор без параметров для Gson
    constructor() : this(
        timestamp = System.currentTimeMillis(),
        temperature = 0.0,
        humidity = 0.0,
        pressure = 0.0,
        windSpeed = 0.0,
        windDirection = 0.0,
        cloudiness = 0.0,
        visibility = 0.0,
        uvIndex = 0.0,
        location = "Unknown",
        source = "Unknown"
    )
    
    // Форматированные значения
    val formattedTemperature: String get() = "${temperature.toInt()}°C"
    val formattedHumidity: String get() = "${humidity.toInt()}%"
    val formattedPressure: String get() = "${pressure.toInt()} гПа"
    val formattedWindSpeed: String get() = "${windSpeed.toInt()} м/с"
    val formattedCloudiness: String get() = "${cloudiness.toInt()}%"
    val formattedVisibility: String get() = "${visibility.toInt()} км"
    val formattedUvIndex: String get() = uvIndex.toInt().toString()
    
    // Описание погоды
    val weatherDescription: String get() = when {
        uvIndex > 8 -> "Очень сильное UV излучение"
        uvIndex > 6 -> "Сильное UV излучение"
        uvIndex > 3 -> "Умеренное UV излучение"
        uvIndex > 1 -> "Слабое UV излучение"
        else -> "Минимальное UV излучение"
    }
    
    // Иконка погоды
    val weatherIcon: String get() = when {
        cloudiness > 80 -> "☁️"
        cloudiness > 60 -> "⛅"
        cloudiness > 40 -> "🌤️"
        cloudiness > 20 -> "☀️"
        else -> "🌞"
    }
}