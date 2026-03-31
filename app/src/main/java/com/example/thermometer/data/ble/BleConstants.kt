package com.example.thermometer.data.ble

import java.util.UUID

object BleConstants {

    // Device filter
    const val DEVICE_NAME_PREFIX = "LYWSD03MMC"
    const val DEVICE_NAME_ATC = "ATC_"

    // Environmental Sensing Service
    val SERVICE_ENVIRONMENTAL_SENSING: UUID =
        UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")

    // Temperature Characteristic
    val CHARACTERISTIC_TEMPERATURE: UUID =
        UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")

    // Humidity Characteristic
    val CHARACTERISTIC_HUMIDITY: UUID =
        UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")

    // Client Characteristic Configuration Descriptor (for notifications)
    val DESCRIPTOR_CCC: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Xiaomi specific service for device info and binding
    val SERVICE_XIAOMI_DEVICE: UUID =
        UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")

    // Xiaomi specific: firmware version
    val CHARACTERISTIC_FIRMWARE_VERSION: UUID =
        UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

    // Xiaomi specific: device binding
    val CHARACTERISTIC_XIAOMI_BIND: UUID =
        UUID.fromString("00000010-0000-1000-8000-00805f9b34fb")

    // Xiaomi specific: token
    val CHARACTERISTIC_XIAOMI_TOKEN: UUID =
        UUID.fromString("00000001-0000-1000-8000-00805f9b34fb")

    // History data service
    val SERVICE_HISTORY: UUID =
        UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")

    val CHARACTERISTIC_HISTORY_TIMESTAMP: UUID =
        UUID.fromString("00002a1f-0000-1000-8000-00805f9b34fb")

    val CHARACTERISTIC_HISTORY_RECORD: UUID =
        UUID.fromString("00002a20-0000-1000-8000-00805f9b34fb")

    // Service Data UUID in advertisement
    val SERVICE_DATA_UUID: UUID =
        UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")

    // Xiaomi product ID for LYWSD03MMC
    const val PRODUCT_ID_LYWSD03MMC = 0x055B

    // Frame control flags
    const val FLAG_BINDING = 0x0001
    const val FLAG_CAPABILITIES = 0x0010

    // Scan timeout in milliseconds
    const val SCAN_TIMEOUT_MS = 15_000L
}
