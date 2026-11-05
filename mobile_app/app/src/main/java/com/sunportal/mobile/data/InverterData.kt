package com.sunportal.mobile.data

/**
 * Данные инвертора для SunLink
 */
data class InverterData(
    val currentPower: Double = 0.0,
    val voltage: Double = 0.0,
    val current: Double = 0.0,
    val temperature: Double = 0.0,
    val status: String = "Unknown",
    val efficiency: Double = 0.0,
    val frequency: Double = 0.0,
    val powerFactor: Double = 0.0,
    val totalEnergy: Double = 0.0,
    val dailyEnergy: Double = 0.0,
    val reactivePower: Double = 0.0,
    val apparentPower: Double = 0.0
)