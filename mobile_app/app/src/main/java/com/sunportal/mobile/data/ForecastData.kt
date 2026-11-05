package com.sunportal.mobile.data

import java.util.*

/**
 * Данные прогноза для SunLink
 */
data class ForecastData(
    val todayMaxPower: Double = 0.0,
    val todayEnergy: Double = 0.0,
    val tomorrowMaxPower: Double = 0.0,
    val tomorrowEnergy: Double = 0.0,
    val weeklyTotal: Double = 0.0,
    val monthlyTotal: Double = 0.0,
    val cloudiness: Int = 0,
    val windSpeed: Double = 0.0,
    val temperature: Double = 0.0,
    val humidity: Int = 0,
    val uvIndex: Int = 0,
    val sunrise: String = "06:00",
    val sunset: String = "18:00",
    val weeklyEnergy: Map<String, Double> = emptyMap(),
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList()
)

/**
 * Почасовой прогноз
 */
data class HourlyForecast(
    val time: String,
    val power: Double,
    val energy: Double,
    val temperature: Double,
    val cloudiness: Int,
    val windSpeed: Double
)

/**
 * Дневной прогноз
 */
data class DailyForecast(
    val date: Date,
    val maxPower: Double,
    val totalEnergy: Double,
    val avgTemperature: Double,
    val cloudiness: Int,
    val windSpeed: Double
)