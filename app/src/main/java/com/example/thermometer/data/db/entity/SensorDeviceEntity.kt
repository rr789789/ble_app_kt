package com.example.thermometer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_devices")
data class SensorDeviceEntity(
    @PrimaryKey
    val macAddress: String,
    val name: String,
    val bindingKey: String = "",
    val isBound: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
