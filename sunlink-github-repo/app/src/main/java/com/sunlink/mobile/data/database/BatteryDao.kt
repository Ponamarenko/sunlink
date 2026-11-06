package com.sunlink.mobile.data.database

import androidx.room.*
import com.sunlink.mobile.data.model.BatteryData
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batteryData: BatteryData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batteryDataList: List<BatteryData>)
    
    @Query("SELECT * FROM battery_data ORDER BY timestamp DESC")
    fun getAllBatteryData(): Flow<List<BatteryData>>
    
    @Query("SELECT * FROM battery_data WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getBatteryDataByTimeRange(startTime: Long): Flow<List<BatteryData>>
    
    @Query("SELECT * FROM battery_data ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBatteryData(): BatteryData?
    
    @Query("DELETE FROM battery_data")
    suspend fun deleteAll()
    
    @Query("DELETE FROM battery_data WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}