package com.example.thermometer.data.repository

import com.example.thermometer.data.ble.BleManager
import com.example.thermometer.domain.model.HistoryData
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepositoryImpl @Inject constructor(
    private val bleManager: BleManager
) : BleRepository {

    override fun scanDevices(): Flow<List<SensorDevice>> {
        return bleManager.scanDevices()
    }

    override fun stopScan() {
        bleManager.stopScan()
    }

    override suspend fun connect(device: SensorDevice): Result<Unit> {
        return bleManager.connect(device)
    }

    override suspend fun disconnect() {
        bleManager.disconnect()
    }

    override suspend fun bindDevice(device: SensorDevice): Result<String> {
        return bleManager.bindDevice(device)
    }

    override fun readRealtimeData(): Flow<SensorData> {
        return bleManager.readRealtimeData()
    }

    override suspend fun readHistoryData(deviceMac: String): Result<List<HistoryData>> {
        return bleManager.readHistoryData(deviceMac)
    }

    override val isConnected: Flow<Boolean>
        get() = bleManager.isConnected
}
