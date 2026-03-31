package com.example.mithermoble.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 传感器数据DAO
 */
@Dao
interface SensorDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: SensorDataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataList: List<SensorDataEntity>)

    @Delete
    suspend fun delete(data: SensorDataEntity)

    @Query("DELETE FROM sensor_data WHERE deviceAddress = :deviceAddress AND timestamp < :timestamp")
    suspend fun deleteOlderThan(deviceAddress: String, timestamp: Long)

    @Query("SELECT * FROM sensor_data WHERE deviceAddress = :deviceAddress ORDER BY timestamp DESC")
    fun getByDevice(deviceAddress: String): Flow<List<SensorDataEntity>>

    @Query("SELECT * FROM sensor_data WHERE deviceAddress = :deviceAddress AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getByDeviceAndTimeRange(deviceAddress: String, startTime: Long, endTime: Long): Flow<List<SensorDataEntity>>

    @Query("SELECT * FROM sensor_data WHERE deviceAddress = :deviceAddress ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentByDevice(deviceAddress: String, limit: Int): Flow<List<SensorDataEntity>>

    @Query("SELECT COUNT(*) FROM sensor_data WHERE deviceAddress = :deviceAddress")
    suspend fun getCount(deviceAddress: String): Int
}
