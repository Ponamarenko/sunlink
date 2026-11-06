package com.sunlink.mobile.data.database

import androidx.room.*
import com.sunlink.mobile.data.model.InverterData
import kotlinx.coroutines.flow.Flow

@Dao
interface InverterDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inverterData: InverterData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inverterDataList: List<InverterData>)
    
    @Query("SELECT * FROM inverter_data ORDER BY timestamp DESC")
    fun getAllInverterData(): Flow<List<InverterData>>
    
    @Query("SELECT * FROM inverter_data WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getInverterDataByTimeRange(startTime: Long): Flow<List<InverterData>>
    
    @Query("SELECT * FROM inverter_data ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestInverterData(): InverterData?
    
    @Query("DELETE FROM inverter_data")
    suspend fun deleteAll()
    
    @Query("DELETE FROM inverter_data WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}