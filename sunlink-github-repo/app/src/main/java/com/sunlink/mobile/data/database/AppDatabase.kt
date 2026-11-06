package com.sunlink.mobile.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sunlink.mobile.data.model.InverterData
import com.sunlink.mobile.data.model.BatteryData

@Database(
    entities = [InverterData::class, BatteryData::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun inverterDao(): InverterDao
    abstract fun batteryDao(): BatteryDao
}