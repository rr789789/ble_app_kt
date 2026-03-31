package com.example.mithermoble.domain.model

/**
 * 传感器数据模型
 */
data class SensorData(
    val id: Long = 0,
    val deviceAddress: String,
    val temperature: Float,       // 温度 (°C)
    val humidity: Float,          // 湿度 (%)
    val batteryLevel: Int = -1,   // 电量 (%)
    val timestamp: Long           // 时间戳
)

/**
 * 扫描到的BLE设备
 */
data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isBound: Boolean = false
)

/**
 * 设备连接状态
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCOVERING_SERVICES,
    READY
}

/**
 * 设备信息
 */
data class DeviceInfo(
    val address: String,
    val name: String,
    val firmwareVersion: String = "",
    val batteryLevel: Int = -1,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)
