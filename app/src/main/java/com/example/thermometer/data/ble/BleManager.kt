package com.example.thermometer.data.ble

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.example.thermometer.domain.model.HistoryData
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.model.SensorDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    private val handler = Handler(Looper.getMainLooper())

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var isScanning = false

    private val _scannedDevices = MutableStateFlow<List<SensorDevice>>(emptyList())
    private val _realtimeData = MutableStateFlow<SensorData?>(null)
    private val _isConnected = MutableStateFlow(false)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    private var currentDevice: SensorDevice? = null
    private var notificationCharacteristic: BluetoothGattCharacteristic? = null

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
     * Start BLE scan for Xiaomi thermometer devices.
     */
    fun scanDevices(): Flow<List<SensorDevice>> {
        _scannedDevices.value = emptyList()
        startScan()
        return _scannedDevices.asStateFlow()
    }

    private fun startScan() {
        if (isScanning) stopScan()

        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_DATA_UUID))
                .build()
        )

        isScanning = true
        scanner?.startScan(filters, settings, scanCallback)

        // Auto-stop after timeout
        handler.postDelayed({
            stopScan()
        }, BleConstants.SCAN_TIMEOUT_MS)
    }

    fun stopScan() {
        if (isScanning) {
            scanner?.stopScan(scanCallback)
            isScanning = false
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val scanRecord = result.scanRecord ?: return

            val name = device.name ?: "LYWSD03MMC"
            val macAddress = device.address

            // Parse Xiaomi service data
            val serviceData = scanRecord.getServiceData(ParcelUuid(BleConstants.SERVICE_DATA_UUID))
            val parsed = if (serviceData != null) {
                XiaomiBleParser.parseServiceData(serviceData, macAddress, name)
            } else {
                XiaomiBleParser.ParsedAdvertisement(macAddress = macAddress, name = name)
            }

            val sensorDevice = SensorDevice(
                macAddress = macAddress,
                name = parsed.name,
                lastSeen = System.currentTimeMillis()
            )

            val current = _scannedDevices.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.macAddress == macAddress }
            if (existingIndex >= 0) {
                current[existingIndex] = sensorDevice
            } else {
                current.add(sensorDevice)
            }
            _scannedDevices.value = current
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
        }
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

            bluetoothGatt = bleDevice.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothGatt.TRANSPORT_LE
            )

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
        // Binding key generation from token
        // In a real implementation, we would read the token characteristic
        // For now, return a placeholder indicating binding is needed
        return try {
            val gatt = bluetoothGatt ?: return Result.failure(Exception("Not connected"))

            // Read token from device
            val tokenChar = findCharacteristic(
                gatt,
                BleConstants.SERVICE_XIAOMI_DEVICE,
                BleConstants.CHARACTERISTIC_XIAOMI_TOKEN
            )

            if (tokenChar != null) {
                gatt.readCharacteristic(tokenChar)
                // In production, we'd wait for onCharacteristicRead callback
                // and then generate the binding key
            }

            // Generate a placeholder binding key
            Result.success("")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Subscribe to real-time sensor data notifications.
     */
    fun readRealtimeData(): Flow<SensorData> {
        return _realtimeData.asStateFlow().let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { data ->
                    if (data != null) emit(data)
                }
            }
        }
    }

    /**
     * Read historical data from device.
     */
    suspend fun readHistoryData(deviceMac: String): Result<List<HistoryData>> {
        return try {
            val gatt = bluetoothGatt ?: return Result.failure(Exception("Not connected"))

            // Read history timestamp
            val historyTsChar = findCharacteristic(
                gatt,
                BleConstants.SERVICE_HISTORY,
                BleConstants.CHARACTERISTIC_HISTORY_TIMESTAMP
            )

            val historyRecChar = findCharacteristic(
                gatt,
                BleConstants.SERVICE_HISTORY,
                BleConstants.CHARACTERISTIC_HISTORY_RECORD
            )

            // In production, implement history reading protocol
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    _isConnected.value = true
                    _connectionState.value = ConnectionState.Connected
                    // Start service discovery
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
        val envService = gatt.getService(BleConstants.SERVICE_ENVIRONMENTAL_SENSING)
        if (envService == null) {
            // Try to find service by iterating
            for (service in gatt.services) {
                if (service.uuid == BleConstants.SERVICE_ENVIRONMENTAL_SENSING) {
                    subscribeCharacteristics(gatt, service)
                    return
                }
            }
            return
        }
        subscribeCharacteristics(gatt, envService)
    }

    private fun subscribeCharacteristics(gatt: BluetoothGatt, service: BluetoothGattService) {
        val tempChar = service.getCharacteristic(BleConstants.CHARACTERISTIC_TEMPERATURE)
        val humChar = service.getCharacteristic(BleConstants.CHARACTERISTIC_HUMIDITY)

        tempChar?.let { enableNotification(gatt, it) }
        humChar?.let { enableNotification(gatt, it) }

        // Also read initial values
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

    private fun findCharacteristic(
        gatt: BluetoothGatt,
        serviceUuid: UUID,
        charUuid: UUID
    ): BluetoothGattCharacteristic? {
        val service = gatt.getService(serviceUuid)
        return service?.getCharacteristic(charUuid)
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
