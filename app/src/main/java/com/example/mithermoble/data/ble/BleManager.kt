package com.example.mithermoble.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.mithermoble.domain.model.BleDevice
import com.example.mithermoble.domain.model.ConnectionState
import com.example.mithermoble.domain.model.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * BLE通信管理器
 * 负责设备扫描、连接、数据读取
 */
class BleManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        private const val TAG = "BleManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

    private var connectedDeviceAddress: String? = null
    private var pendingHistoryData = mutableListOf<SensorData>()

    // GATT回调
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val device = gatt.device
            Log.d(TAG, "Connection state changed: $status -> $newState for ${device.address}")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.d(TAG, "Connected to GATT server, discovering services...")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Log.d(TAG, "Disconnected from GATT server")
                    bluetoothGatt = null
                    connectedDeviceAddress = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.READY
                Log.d(TAG, "Services discovered successfully")
                enableNotifications(gatt)
            } else {
                Log.e(TAG, "Service discovery failed: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChange(characteristic, value)
        }

        @Deprecated("Use the new overload for API 33+")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleCharacteristicChange(characteristic, characteristic.value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicRead(characteristic, value)
            }
        }

        @Deprecated("Use the new overload for API 33+")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicRead(characteristic, characteristic.value)
            }
        }
    }

    private fun handleCharacteristicChange(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val uuid = characteristic.uuid.toString()
        Log.d(TAG, "Characteristic changed: $uuid, data: ${value.toHexString()}")

        val address = connectedDeviceAddress ?: return
        val currentData = _sensorData.value

        when (uuid) {
            MiThermoConstants.CHAR_TEMPERATURE -> {
                val temp = parseTemperature(value)
                _sensorData.value = (currentData ?: SensorData(
                    deviceAddress = address,
                    temperature = 0f,
                    humidity = 0f,
                    timestamp = System.currentTimeMillis()
                )).copy(temperature = temp, timestamp = System.currentTimeMillis())
            }
            MiThermoConstants.CHAR_HUMIDITY -> {
                val humidity = parseHumidity(value)
                _sensorData.value = (currentData ?: SensorData(
                    deviceAddress = address,
                    temperature = 0f,
                    humidity = 0f,
                    timestamp = System.currentTimeMillis()
                )).copy(humidity = humidity, timestamp = System.currentTimeMillis())
            }
            MiThermoConstants.CHAR_MI_DATA -> {
                val parsed = parseMiData(value)
                _sensorData.value = SensorData(
                    deviceAddress = address,
                    temperature = parsed.first,
                    humidity = parsed.second,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    private fun handleCharacteristicRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val uuid = characteristic.uuid.toString()
        Log.d(TAG, "Characteristic read: $uuid, data: ${value.toHexString()}")

        when (uuid) {
            MiThermoConstants.CHAR_BATTERY -> {
                val currentData = _sensorData.value
                val battery = if (value.isNotEmpty()) value[0].toInt() and 0xFF else -1
                if (currentData != null) {
                    _sensorData.value = currentData.copy(batteryLevel = battery)
                }
            }
            MiThermoConstants.CHAR_MI_HISTORY -> {
                val historyEntry = parseHistoryData(value)
                if (historyEntry != null) {
                    pendingHistoryData.add(historyEntry)
                }
            }
        }
    }

    /**
     * 扫描BLE设备
     */
    @SuppressLint("MissingPermission")
    fun startScan(): Flow<List<BleDevice>> = callbackFlow {
        val devices = mutableMapOf<String, BleDevice>()

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: return
                val address = device.address

                if (name.startsWith(MiThermoConstants.DEVICE_NAME_PREFIX) ||
                    name.startsWith(MiThermoConstants.DEVICE_NAME_ATC)
                ) {
                    devices[address] = BleDevice(
                        name = name,
                        address = address,
                        rssi = result.rssi
                    )
                    trySend(devices.values.toList())
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed: $errorCode")
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)

        awaitClose {
            scanner.stopScan(scanCallback)
        }
    }

    /**
     * 停止扫描
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.flushPendingScanResults()
    }

    /**
     * 连接设备
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            return@withContext Result.failure(Exception("Bluetooth not available"))
        }

        disconnect()

        val device = bluetoothAdapter.getRemoteDevice(address)
        _connectionState.value = ConnectionState.CONNECTING

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        connectedDeviceAddress = address

        val ready = withTimeoutOrNull(15000L) {
            connectionState.first { it == ConnectionState.READY || it == ConnectionState.DISCONNECTED }
        }

        if (ready == ConnectionState.READY) {
            Result.success(Unit)
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
            Result.failure(Exception("Connection timeout or failed"))
        }
    }

    /**
     * 断开连接
     */
    @SuppressLint("MissingPermission")
    suspend fun disconnect() {
        bluetoothGatt?.let { gatt ->
            withContext(Dispatchers.Main) {
                try {
                    gatt.disconnect()
                    gatt.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error disconnecting", e)
                }
            }
        }
        bluetoothGatt = null
        connectedDeviceAddress = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _sensorData.value = null
    }

    /**
     * 启用通知
     */
    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt) {
        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val cccDescriptor = characteristic.getDescriptor(
                        UUID.fromString(MiThermoConstants.DESC_CCC)
                    )
                    cccDescriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(it)
                    }
                }
            }
        }
        readBatteryLevel(gatt)
    }

    /**
     * 读取电池电量
     */
    @SuppressLint("MissingPermission")
    private fun readBatteryLevel(gatt: BluetoothGatt) {
        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.uuid.toString() == MiThermoConstants.CHAR_BATTERY) {
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                        gatt.readCharacteristic(characteristic)
                    }
                }
            }
        }
    }

    /**
     * 读取历史数据
     */
    @SuppressLint("MissingPermission")
    suspend fun readHistoryData(): Result<List<SensorData>> = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt ?: return@withContext Result.failure(Exception("Not connected"))

        pendingHistoryData.clear()

        var historyChar: BluetoothGattCharacteristic? = null
        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.uuid.toString() == MiThermoConstants.CHAR_MI_HISTORY) {
                    historyChar = characteristic
                    break
                }
            }
            if (historyChar != null) break
        }

        if (historyChar == null) {
            return@withContext Result.failure(Exception("History characteristic not found"))
        }

        gatt.setCharacteristicNotification(historyChar, true)
        val cccDescriptor = historyChar.getDescriptor(
            UUID.fromString(MiThermoConstants.DESC_CCC)
        )
        cccDescriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }

        gatt.readCharacteristic(historyChar)

        delay(3000)

        Result.success(pendingHistoryData.toList())
    }

    private fun parseTemperature(value: ByteArray): Float {
        if (value.size < 2) return 0f
        val raw = (value[1].toInt() and 0xFF shl 8) or (value[0].toInt() and 0xFF)
        return raw / 100f
    }

    private fun parseHumidity(value: ByteArray): Float {
        if (value.isEmpty()) return 0f
        return (value[0].toInt() and 0xFF) / 100f
    }

    private fun parseMiData(value: ByteArray): Pair<Float, Float> {
        if (value.size < 3) return Pair(0f, 0f)

        val tempRaw = (value[1].toInt() and 0xFF shl 8) or (value[0].toInt() and 0xFF)
        val temp = if (tempRaw > 0x7FFF) {
            (tempRaw - 0x10000) / 100f
        } else {
            tempRaw / 100f
        }
        val humidity = (value[2].toInt() and 0xFF) / 100f

        return Pair(temp, humidity)
    }

    private fun parseHistoryData(value: ByteArray): SensorData? {
        if (value.size < 6) return null
        val address = connectedDeviceAddress ?: return null

        val tempRaw = (value[1].toInt() and 0xFF shl 8) or (value[0].toInt() and 0xFF)
        val temp = if (tempRaw > 0x7FFF) {
            (tempRaw - 0x10000) / 100f
        } else {
            tempRaw / 100f
        }
        val humidity = (value[2].toInt() and 0xFF) / 100f
        val timestamp = (
            (value[5].toLong() and 0xFF shl 24) or
            (value[4].toLong() and 0xFF shl 16) or
            (value[3].toLong() and 0xFF shl 8) or
            (value[3].toLong() and 0xFF)
        )

        return SensorData(
            deviceAddress = address,
            temperature = temp,
            humidity = humidity,
            timestamp = timestamp
        )
    }

    private fun ByteArray.toHexString(): String =
        joinToString(" ", transform = { "%02X".format(it) })
}
