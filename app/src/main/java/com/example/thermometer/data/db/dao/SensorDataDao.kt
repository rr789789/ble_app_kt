package com.example.thermometer.data.db.dao

import androidx.room.*
import com.example.thermometer.data.db.entity.SensorDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDataDao {

    @Query("SELECT * FROM sensor_data WHERE deviceMac = :mac ORDER BY timestamp DESC")
    fun getDataByDevice(mac: String): Flow<List<SensorDataEntity>>

    @Query("SELECT * FROM sensor_data WHERE deviceMac = :mac AND timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    fun getDataByDeviceAndRange(mac: String, from: Long, to: Long): Flow<List<SensorDataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<SensorDataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: SensorDataEntity)

    @Query("DELETE FROM sensor_data WHERE deviceMac = :mac AND timestamp < :timestamp")
    suspend fun deleteOlderThan(mac: String, timestamp: Long)

    @Query("DELETE FROM sensor_data WHERE deviceMac = :mac")
    suspend fun deleteByDevice(mac: String)
}
