package com.example.mithermoble.data.ble

/**
 * 小米温湿度计2 (LYWSD03MMC) BLE常量定义
 */
object MiThermoConstants {

    // 设备名称过滤
    const val DEVICE_NAME_PREFIX = "LYWSD03MMC"
    const val DEVICE_NAME_ATC = "ATC_"

    // 服务UUID
    const val SERVICE_ENVIRONMENTAL_SENSING = "0000181a-0000-1000-8000-00805f9b34fb"
    const val SERVICE_DEVICE_INFORMATION = "0000180f-0000-1000-8000-00805f9b34fb"
    const val SERVICE_MI = "0000fe95-0000-1000-8000-00805f9b34fb"

    // 特征UUID - 标准BLE环境感知
    const val CHAR_TEMPERATURE = "00002a6e-0000-1000-8000-00805f9b34fb"
    const val CHAR_HUMIDITY = "00002a6f-0000-1000-8000-00805f9b34fb"
    const val CHAR_BATTERY = "00002a19-0000-1000-8000-00805f9b34fb"

    // 特征UUID - 小米自定义
    const val CHAR_MI_DATA = "0000aa20-0000-1000-8000-00805f9b34fb"
    const val CHAR_MI_HISTORY = "0000aa21-0000-1000-8000-00805f9b34fb"
    const val CHAR_MI_TIME = "0000aa22-0000-1000-8000-00805f9b34fb"

    // CCC描述符
    const val DESC_CCC = "00002902-0000-1000-8000-00805f9b34fb"

    // 扫描超时
    const val SCAN_TIMEOUT_MS = 10000L
}
