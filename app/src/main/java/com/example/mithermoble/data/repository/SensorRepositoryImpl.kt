package com.example.mithermoble.data.repository

import com.example.mithermoble.data.ble.BleManager
import com.example.mithermoble.data.db.SensorDataDao
import com.example.mithermoble.data.db.toDomainModel
import com.example.mithermoble.data.db.toEntity
import com.example.mithermoble.domain.model.BleDevice
import com.example.mithermoble.domain.model.ConnectionState
import com.example.mithermoble.domain.model.SensorData
import com.example.mithermoble.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 传感器数据仓库实现
 */
class SensorRepositoryImpl @Inject constructor(
    private val bleManager: BleManager,
    private val sensorDataDao: SensorDataDao
) : SensorRepository {

    override fun startScan(): Flow<List<BleDevice>> {
        return bleManager.startScan()
    }

    override fun stopScan() {
        bleManager.stopScan()
    }

    override fun getConnectionState(): Flow<ConnectionState> {
        return bleManager.connectionState
    }

    override fun getSensorData(): Flow<SensorData?> {
        return bleManager.sensorData
    }

    override suspend fun connect(address: String): Result<Unit> {
        return bleManager.connect(address)
    }

    override suspend fun disconnect() {
        bleManager.disconnect()
    }

    override suspend fun readHistoryData(): Result<List<SensorData>> {
        return bleManager.readHistoryData()
    }

    override fun getLocalSensorData(deviceAddress: String): Flow<List<SensorData>> {
        return sensorDataDao.getByDevice(deviceAddress).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun saveSensorData(data: SensorData) {
        sensorDataDao.insert(data.toEntity())
    }

    override suspend fun saveSensorDataList(dataList: List<SensorData>) {
        sensorDataDao.insertAll(dataList.map { it.toEntity() })
    }

    override suspend fun deleteSensorDataOlderThan(deviceAddress: String, timestamp: Long) {
        sensorDataDao.deleteOlderThan(deviceAddress, timestamp)
    }

    override suspend fun exportToCsv(deviceAddress: String): Result<String> {
        return try {
            val sb = StringBuilder()
            sb.appendLine("timestamp,temperature,humidity,battery")
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
