package com.sunportal.mobile.data

/**
 * Данные батареи для SunLink
 */
data class BatteryData(
    val status: String = "Unknown",
    val level: Int = 0,
    val temperature: Double = 0.0,
    val voltage: Double = 0.0,
    val current: Double = 0.0,
    val health: Int = 0,
    val cycles: Int = 0,
    val voltageMax: Double = 0.0,
    val voltageMin: Double = 0.0,
    val currentMax: Double = 0.0,
    val currentMin: Double = 0.0,
    val power: Double = 0.0,
    val energy: Double = 0.0
)