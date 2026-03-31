package com.example.thermometer.data.db.dao

import androidx.room.*
import com.example.thermometer.data.db.entity.SensorDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDeviceDao {

    @Query("SELECT * FROM sensor_devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<SensorDeviceEntity>>

    @Query("SELECT * FROM sensor_devices WHERE macAddress = :mac")
    suspend fun getDeviceByMac(mac: String): SensorDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: SensorDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: SensorDeviceEntity)

    @Query("DELETE FROM sensor_devices WHERE macAddress = :mac")
    suspend fun deleteByMac(mac: String)
}
