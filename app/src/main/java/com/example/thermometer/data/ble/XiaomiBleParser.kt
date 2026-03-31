package com.example.thermometer.data.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser for Xiaomi BLE advertisement data.
 * Decodes service data from LYWSD03MMC broadcast packets.
 */
object XiaomiBleParser {

    data class ParsedAdvertisement(
        val macAddress: String,
        val name: String,
        val temperature: Float? = null,
        val humidity: Float? = null,
        val batteryLevel: Int? = null,
        val isBound: Boolean = false,
        val token: ByteArray? = null
    )

    /**
     * Parse service data from BLE scan record.
     * Xiaomi data format:
     *   [0-1] Frame Control
     *   [2-3] Product ID
     *   [4] Frame Counter
     *   [5-10] MAC Address (reversed)
     *   [11+] Capability / Event data
     */
    fun parseServiceData(
        serviceData: ByteArray,
        macAddress: String,
        deviceName: String
    ): ParsedAdvertisement {
        if (serviceData.size < 11) {
            return ParsedAdvertisement(macAddress = macAddress, name = deviceName)
        }

        val buffer = ByteBuffer.wrap(serviceData).order(ByteOrder.LITTLE_ENDIAN)
        val frameControl = buffer.short.toInt() and 0xFFFF
        val productId = buffer.short.toInt() and 0xFFFF
        val frameCounter = buffer.get().toInt() and 0xFF

        // Read MAC address (reversed byte order)
        val macBytes = ByteArray(6)
        buffer.get(macBytes)
        val parsedMac = macBytes.reversed().joinToString(":") {
            String.format("%02X", it)
        }

        val isBound = (frameControl and BleConstants.FLAG_BINDING) != 0

        var temperature: Float? = null
        var humidity: Float? = null
        var batteryLevel: Int? = null
        var token: ByteArray? = null

        // Parse event data
        if (serviceData.size > 11) {
            val eventType = buffer.short.toInt() and 0xFFFF
            val eventLength = buffer.short.toInt() and 0xFFFF

            if (buffer.remaining() >= eventLength) {
                when (eventType) {
                    0x1004 -> { // Temperature and humidity
                        if (buffer.remaining() >= 4) {
                            val tempRaw = buffer.short.toInt()
                            temperature = tempRaw / 10.0f
                            val humRaw = buffer.short.toInt() and 0xFFFF
                            humidity = humRaw / 10.0f
                        }
                    }
                    0x1006 -> { // Temperature only
                        if (buffer.remaining() >= 2) {
                            val tempRaw = buffer.short.toInt()
                            temperature = tempRaw / 10.0f
                        }
                    }
                    0x1007 -> { // Humidity only
                        if (buffer.remaining() >= 2) {
                            val humRaw = buffer.short.toInt() and 0xFFFF
                            humidity = humRaw / 10.0f
                        }
                    }
                    0x1008 -> { // Battery
                        if (buffer.remaining() >= 1) {
                            batteryLevel = buffer.get().toInt() and 0xFF
                        }
                    }
                }
            }
        }

        // Token is in capabilities frame for unbound devices
        if (!isBound && serviceData.size > 14) {
            val capOffset = 11
            if (serviceData.size >= capOffset + 5) {
                val capByte = serviceData[capOffset].toInt() and 0xFF
                if ((capByte and 0x20) != 0) {
                    token = serviceData.copyOfRange(capOffset + 1, capOffset + 5)
                }
            }
        }

        return ParsedAdvertisement(
            macAddress = parsedMac,
            name = deviceName,
            temperature = temperature,
            humidity = humidity,
            batteryLevel = batteryLevel,
            isBound = isBound,
            token = token
        )
    }

    /**
     * Convert a byte array to hex string.
     */
    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { String.format("%02X", it) }
}
