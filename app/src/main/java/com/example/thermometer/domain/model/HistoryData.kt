package com.example.thermometer.domain.model

data class HistoryData(
    val temperature: Float,
    val humidity: Float,
    val timestamp: Long,
    val deviceMac: String
)
