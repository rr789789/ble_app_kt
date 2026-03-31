package com.example.thermometer.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_data",
    indices = [
        Index(value = ["deviceMac", "timestamp"]),
        Index(value = ["deviceMac"])
    ]
)
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val temperature: Float,
    val humidity: Float,
    val timestamp: Long,
    val deviceMac: String
)
