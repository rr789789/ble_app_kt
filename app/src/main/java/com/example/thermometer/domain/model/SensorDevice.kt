package com.example.thermometer.domain.model

data class SensorDevice(
    val macAddress: String,
    val name: String,
    val bindingKey: String = "",
    val isBound: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
