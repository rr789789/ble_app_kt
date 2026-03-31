package com.example.mithermoble.domain.repository

import com.example.mithermoble.domain.model.BleDevice
import com.example.mithermoble.domain.model.ConnectionState
import com.example.mithermoble.domain.model.SensorData
import kotlinx.coroutines.flow.Flow

/**
 * 传感器数据仓库接口
 */
interface SensorRepository {

    // BLE操作
    fun startScan(): Flow<List<BleDevice>>
    fun stopScan()
    fun getConnectionState(): Flow<ConnectionState>
    fun getSensorData(): Flow<SensorData?>
    suspend fun connect(address: String): Result<Unit>
    suspend fun disconnect()
    suspend fun readHistoryData(): Result<List<SensorData>>

    // 本地数据操作
    fun getLocalSensorData(deviceAddress: String): Flow<List<SensorData>>
    suspend fun saveSensorData(data: SensorData)
    suspend fun saveSensorDataList(dataList: List<SensorData>)
    suspend fun deleteSensorDataOlderThan(deviceAddress: String, timestamp: Long)
    suspend fun exportToCsv(deviceAddress: String): Result<String>
}
