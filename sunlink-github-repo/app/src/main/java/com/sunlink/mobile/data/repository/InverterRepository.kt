package com.sunlink.mobile.data.repository

import com.sunlink.mobile.data.model.BatteryData
import com.sunlink.mobile.data.model.InverterData
import com.sunlink.mobile.network.ApiService
import com.sunlink.mobile.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InverterRepository @Inject constructor(
    private val apiService: ApiService = RetrofitClient.apiService
) {
    
    private val _inverterData = mutableListOf<InverterData>()
    private val _batteryData = mutableListOf<BatteryData>()
    
    // In-memory cache
    private val inverterDataCache = mutableListOf<InverterData>()
    private val batteryDataCache = mutableListOf<BatteryData>()
    
    suspend fun getInverterData(): Result<InverterData> {
        return try {
            // Сначала проверяем кэш
            if (inverterDataCache.isNotEmpty()) {
                Result.success(inverterDataCache.last())
            } else {
                // Запрос с сервера
                val response = apiService.getInverterData()
                if (response.isSuccessful) {
                    val data = response.body()!!
                    inverterDataCache.add(data)
                    Result.success(data)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            // Возвращаем последние известные данные или генерируем mock
            if (inverterDataCache.isNotEmpty()) {
                Result.success(inverterDataCache.last())
            } else {
                val mockData = InverterData.generateMock()
                inverterDataCache.add(mockData)
                Result.success(mockData)
            }
        }
    }
    
    suspend fun getBatteryData(): Result<BatteryData> {
        return try {
            // Сначала проверяем кэш
            if (batteryDataCache.isNotEmpty()) {
                Result.success(batteryDataCache.last())
            } else {
                // Запрос с сервера
                val response = apiService.getBatteryData()
                if (response.isSuccessful) {
                    val data = response.body()!!
                    batteryDataCache.add(data)
                    Result.success(data)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            // Возвращаем последние известные данные или генерируем mock
            if (batteryDataCache.isNotEmpty()) {
                Result.success(batteryDataCache.last())
            } else {
                val mockData = BatteryData.generateMock()
                batteryDataCache.add(mockData)
                Result.success(mockData)
            }
        }
    }
    
    suspend fun controlInverter(action: String, parameters: Map<String, Any> = emptyMap()): Result<Unit> {
        return try {
            val response = apiService.controlInverter(
                com.sunlink.mobile.network.ControlRequest(action, parameters)
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Control failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun controlBattery(action: String, targetLevel: Int? = null, maxPower: Double? = null): Result<Unit> {
        return try {
            val response = apiService.controlBattery(
                com.sunlink.mobile.network.BatteryControlRequest(
                    action = action,
                    targetLevel = targetLevel,
                    maxPower = maxPower
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Battery control failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getInverterDataFlow(): Flow<List<InverterData>> = flow {
        emit(inverterDataCache)
    }
    
    fun getBatteryDataFlow(): Flow<List<BatteryData>> = flow {
        emit(batteryDataCache)
    }
    
    fun clearCache() {
        inverterDataCache.clear()
        batteryDataCache.clear()
    }
    
    fun getHistoricalData(hours: Int = 24): List<InverterData> {
        return inverterDataCache.takeLast(hours)
    }
    
    fun getBatteryHistory(hours: Int = 24): List<BatteryData> {
        return batteryDataCache.takeLast(hours)
    }
}