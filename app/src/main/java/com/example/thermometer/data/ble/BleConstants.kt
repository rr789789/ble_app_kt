package com.example.thermometer.data.ble

import java.util.UUID

object BleConstants {

    const val DEVICE_NAME_PREFIX = "LYWSD03MMC"
    const val DEVICE_NAME_ATC = "ATC_"
    const val SCAN_TIMEOUT_MS = 15_000L

    // Environmental Sensing Service
    val SERVICE_ENVIRONMENTAL_SENSING: UUID =
        UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")

    val CHARACTERISTIC_TEMPERATURE: UUID =
        UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")

    val CHARACTERISTIC_HUMIDITY: UUID =
        UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")

    val DESCRIPTOR_CCC: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val SERVICE_DATA_UUID: UUID =
        UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")

    const val PRODUCT_ID_LYWSD03MMC = 0x055B
    const val FLAG_BINDING = 0x0001
    const val FLAG_CAPABILITIES = 0x0010
}
