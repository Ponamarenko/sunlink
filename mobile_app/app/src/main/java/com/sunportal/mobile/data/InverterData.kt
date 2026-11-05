package com.sunportal.mobile.data

/**
 * InverterData - данные инвертора
 */
data class InverterData(
    val currentPower: Double = 0.0,
    val voltage: Double = 0.0,
    val current: Double = 0.0,
    val temperature: Double = 0.0,
    val status: String = "Normal",
    val efficiency: Double = 0.0,
    val frequency: Double = 0.0,
    val powerFactor: Double = 0.0,
    val totalEnergy: Double = 0.0,
    val dailyEnergy: Double = 0.0,
    val reactivePower: Double = 0.0,
    val apparentPower: Double = 0.0
)