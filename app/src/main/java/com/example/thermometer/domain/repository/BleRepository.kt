package com.example.thermometer.domain.repository

import com.example.thermometer.domain.model.HistoryData
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.model.SensorDevice
import kotlinx.coroutines.flow.Flow

interface BleRepository {

    fun scanDevices(): Flow<List<SensorDevice>>

    fun stopScan()

    suspend fun connect(device: SensorDevice): Result<Unit>

    suspend fun disconnect()

    suspend fun bindDevice(device: SensorDevice): Result<String>

    fun readRealtimeData(): Flow<SensorData>

    suspend fun readHistoryData(deviceMac: String): Result<List<HistoryData>>

    val isConnected: Flow<Boolean>
}
