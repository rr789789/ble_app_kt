package com.example.mithermoble.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 传感器数据Room实体
 */
@Entity(
    tableName = "sensor_data",
    indices = [
        Index(value = ["deviceAddress", "timestamp"]),
        Index(value = ["deviceAddress"])
    ]
)
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceAddress: String,
    val temperature: Float,
    val humidity: Float,
    val batteryLevel: Int,
    val timestamp: Long
)
