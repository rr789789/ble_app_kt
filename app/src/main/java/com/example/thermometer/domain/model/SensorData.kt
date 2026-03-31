package com.example.thermometer.domain.model

data class SensorData(
    val temperature: Float,
    val humidity: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceMac: String = ""
)
