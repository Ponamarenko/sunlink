package com.sunlink.mobile.data.model

import androidx.annotation.Keep

@Keep
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
    val apparentPower: Double = 0.0,
    val dcVoltage: Double = 0.0,
    val dcCurrent: Double = 0.0,
    var timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "currentPower" to currentPower,
            "voltage" to voltage,
            "current" to current,
            "temperature" to temperature,
            "status" to status,
            "efficiency" to efficiency,
            "frequency" to frequency,
            "powerFactor" to powerFactor,
            "totalEnergy" to totalEnergy,
            "dailyEnergy" to dailyEnergy,
            "reactivePower" to reactivePower,
            "apparentPower" to apparentPower,
            "dcVoltage" to dcVoltage,
            "dcCurrent" to dcCurrent,
            "timestamp" to timestamp
        )
    }
    
    companion object {
        fun generateMock(): InverterData {
            return InverterData(
                currentPower = kotlin.random.Random.nextDouble(2000.0, 4000.0),
                voltage = kotlin.random.Random.nextDouble(220.0, 240.0),
                current = kotlin.random.Random.nextDouble(8.0, 15.0),
                temperature = kotlin.random.Random.nextDouble(30.0, 45.0),
                status = listOf("Normal", "Warning", "Error").random(),
                efficiency = kotlin.random.Random.nextDouble(85.0, 98.0),
                frequency = 49.5 + kotlin.random.Random.nextDouble(-0.5, 0.5),
                powerFactor = 0.85 + kotlin.random.Random.nextDouble(-0.1, 0.1),
                totalEnergy = kotlin.random.Random.nextDouble(10000.0, 20000.0),
                dailyEnergy = kotlin.random.Random.nextDouble(15.0, 25.0),
                reactivePower = kotlin.random.Random.nextDouble(200.0, 800.0),
                apparentPower = kotlin.random.Random.nextDouble(2500.0, 3500.0),
                dcVoltage = kotlin.random.Random.nextDouble(400.0, 600.0),
                dcCurrent = kotlin.random.Random.nextDouble(5.0, 10.0)
            )
        }
    }
}