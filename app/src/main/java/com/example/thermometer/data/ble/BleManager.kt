package com.example.thermometer.data.ble

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.thermometer.domain.model.HistoryData
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.model.SensorDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    private val handler = Handler(Looper.getMainLooper())

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentDevice: SensorDevice? = null
    private var notificationCharacteristic: BluetoothGattCharacteristic? = null

    private val _realtimeData = MutableStateFlow<SensorData?>(null)
    private val _isConnected = MutableStateFlow(false)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    // Track current scan for manual stop
    private var currentScanCallback: ScanCallback? = null
    private var currentScanner: android.bluetooth.le.BluetoothLeScanner? = null

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Discovering : ConnectionState()
        object Ready : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    val connectionState = _connectionState.asStateFlow()

    /**
     * Scan for LYWSD03MMC devices using callbackFlow.
     * - Filters by device name starting with "LYWSD03MMC"
     * - Auto-stops after SCAN_TIMEOUT_MS
     * - Can be stopped manually via stopScan()
     */
    fun scanDevices(): Flow<List<SensorDevice>> = callbackFlow {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            close(Exception("Bluetooth not available"))
            return@callbackFlow
        }

        currentScanner = scanner
        val foundDevices = mutableMapOf<String, SensorDevice>()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: return

                // Filter by device name: LYWSD03MMC
                if (!name.startsWith("LYWSD03MMC", ignoreCase = true) &&
                    !name.startsWith("ATC_", ignoreCase = true)
                ) return

                val macAddress = device.address
                val sensorDevice = SensorDevice(
                    macAddress = macAddress,
                    name = name,
                    lastSeen = System.currentTimeMillis()
                )

                foundDevices[macAddress] = sensorDevice
                trySend(foundDevices.values.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("Scan failed: $errorCode"))
            }
        }

        currentScanCallback = scanCallback

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)

        // Auto-stop after timeout
        val timeoutRunnable = Runnable {
            scanner.stopScan(scanCallback)
            currentScanCallback = null
            close()
        }
        handler.postDelayed(timeoutRunnable, BleConstants.SCAN_TIMEOUT_MS)

        awaitClose {
            handler.removeCallbacks(timeoutRunnable)
            scanner.stopScan(scanCallback)
            currentScanCallback = null
        }
    }

    /**
     * Manually stop scanning.
     */
    fun stopScan() {
        currentScanCallback?.let { callback ->
            currentScanner?.stopScan(callback)
        }
        currentScanCallback = null
        currentScanner = null
    }

    /**
     * Connect to a BLE device.
     */
    suspend fun connect(device: SensorDevice): Result<Unit> {
        return try {
            disconnect()

            currentDevice = device
            _connectionState.value = ConnectionState.Connecting

            val bleDevice = bluetoothAdapter?.getRemoteDevice(device.macAddress)
                ?: return Result.failure(Exception("Bluetooth adapter not available"))

            bluetoothGatt = bleDevice.connectGatt(context, false, gattCallback)
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }

    /**
     * Disconnect from current device.
     */
    suspend fun disconnect() {
        notificationCharacteristic?.let { char ->
            bluetoothGatt?.setCharacteristicNotification(char, false)
        }
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        currentDevice = null
        _isConnected.value = false
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Bind device — get token and generate binding key.
     */
    suspend fun bindDevice(device: SensorDevice): Result<String> {
        return Result.success("")
    }

    /**
     * Subscribe to real-time sensor data notifications.
     */
    fun readRealtimeData(): Flow<SensorData> = _realtimeData
        .asStateFlow()
        .filterNotNull()

    /**
     * Read historical data from device.
     */
    suspend fun readHistoryData(deviceMac: String): Result<List<HistoryData>> {
        return Result.success(emptyList())
    }

    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    _isConnected.value = true
                    _connectionState.value = ConnectionState.Connected
                    gatt.discoverServices()
                    _connectionState.value = ConnectionState.Discovering
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    _isConnected.value = false
                    _connectionState.value = ConnectionState.Disconnected
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Ready
                subscribeToSensorData(gatt)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                BleConstants.CHARACTERISTIC_TEMPERATURE -> {
                    val temp = parseTemperature(characteristic.value)
                    val current = _realtimeData.value
                    _realtimeData.value = SensorData(
                        temperature = temp,
                        humidity = current?.humidity ?: 0f,
                        deviceMac = currentDevice?.macAddress ?: ""
                    )
                }
                BleConstants.CHARACTERISTIC_HUMIDITY -> {
                    val hum = parseHumidity(characteristic.value)
                    val current = _realtimeData.value
                    _realtimeData.value = SensorData(
                        temperature = current?.temperature ?: 0f,
                        humidity = hum,
                        deviceMac = currentDevice?.macAddress ?: ""
                    )
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (characteristic.uuid) {
                    BleConstants.CHARACTERISTIC_TEMPERATURE -> {
                        val temp = parseTemperature(characteristic.value)
                        val current = _realtimeData.value
                        _realtimeData.value = SensorData(
                            temperature = temp,
                            humidity = current?.humidity ?: 0f,
                            deviceMac = currentDevice?.macAddress ?: ""
                        )
                    }
                    BleConstants.CHARACTERISTIC_HUMIDITY -> {
                        val hum = parseHumidity(characteristic.value)
                        val current = _realtimeData.value
                        _realtimeData.value = SensorData(
                            temperature = current?.temperature ?: 0f,
                            humidity = hum,
                            deviceMac = currentDevice?.macAddress ?: ""
                        )
                    }
                }
            }
        }
    }

    private fun subscribeToSensorData(gatt: BluetoothGatt) {
        for (service in gatt.services) {
            if (service.uuid == BleConstants.SERVICE_ENVIRONMENTAL_SENSING) {
                subscribeCharacteristics(gatt, service)
                return
            }
        }
    }

    private fun subscribeCharacteristics(gatt: BluetoothGatt, service: BluetoothGattService) {
        val tempChar = service.getCharacteristic(BleConstants.CHARACTERISTIC_TEMPERATURE)
        val humChar = service.getCharacteristic(BleConstants.CHARACTERISTIC_HUMIDITY)

        tempChar?.let { enableNotification(gatt, it) }
        humChar?.let { enableNotification(gatt, it) }

        tempChar?.let {
            gatt.readCharacteristic(it)
            notificationCharacteristic = it
        }
    }

    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(BleConstants.DESCRIPTOR_CCC)
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }
    }

    private fun parseTemperature(value: ByteArray): Float {
        if (value.size < 2) return 0f
        val raw = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        return raw / 100.0f
    }

    private fun parseHumidity(value: ByteArray): Float {
        if (value.size < 2) return 0f
        val raw = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        return raw / 100.0f
    }
}
