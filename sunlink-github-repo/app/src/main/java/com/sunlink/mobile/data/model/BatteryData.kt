package com.sunlink.mobile.data.model

import androidx.annotation.Keep

@Keep
data class BatteryData(
    val status: String = "Normal",
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
    val energy: Double = 0.0,
    val charging: Boolean = false,
    val chargingRate: Double = 0.0,
    val dischargingRate: Double = 0.0,
    val estimatedRange: Double = 0.0,
    val timeToFull: Double = 0.0,
    val timeToEmpty: Double = 0.0,
    val lastChargeTime: Long = 0L,
    val lastDischargeTime: Long = 0L,
    var timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "status" to status,
            "level" to level,
            "temperature" to temperature,
            "voltage" to voltage,
            "current" to current,
            "health" to health,
            "cycles" to cycles,
            "voltageMax" to voltageMax,
            "voltageMin" to voltageMin,
            "currentMax" to currentMax,
            "currentMin" to currentMin,
            "power" to power,
            "energy" to energy,
            "charging" to charging,
            "chargingRate" to chargingRate,
            "dischargingRate" to dischargingRate,
            "estimatedRange" to estimatedRange,
            "timeToFull" to timeToFull,
            "timeToEmpty" to timeToEmpty,
            "lastChargeTime" to lastChargeTime,
            "lastDischargeTime" to lastDischargeTime,
            "timestamp" to timestamp
        )
    }
    
    companion object {
        fun generateMock(): BatteryData {
            return BatteryData(
                status = listOf("Normal", "Warning", "Error").random(),
                level = kotlin.random.Random.nextInt(60, 95),
                temperature = kotlin.random.Random.nextDouble(20.0, 35.0),
                voltage = kotlin.random.Random.nextDouble(45.0, 55.0),
                current = kotlin.random.Random.nextDouble(-10.0, 20.0),
                health = kotlin.random.Random.nextInt(80, 100),
                cycles = kotlin.random.Random.nextInt(100, 500),
                voltageMax = 54.0,
                voltageMin = 42.0,
                currentMax = 25.0,
                currentMin = -10.0,
                power = kotlin.random.Random.nextDouble(-500.0, 1000.0),
                energy = kotlin.random.Random.nextDouble(5.0, 20.0),
                charging = kotlin.random.Random.nextBoolean(),
                chargingRate = kotlin.random.Random.nextDouble(0.0, 50.0),
                dischargingRate = kotlin.random.Random.nextDouble(0.0, 30.0),
                estimatedRange = kotlin.random.Random.nextDouble(50.0, 200.0),
                timeToFull = kotlin.random.Random.nextDouble(1.0, 8.0),
                timeToEmpty = kotlin.random.Random.nextDouble(2.0, 12.0),
                lastChargeTime = System.currentTimeMillis() - kotlin.random.Random.nextLong(3600000, 86400000),
                lastDischargeTime = System.currentTimeMillis() - kotlin.random.Random.nextLong(3600000, 86400000)
            )
        }
    }
}