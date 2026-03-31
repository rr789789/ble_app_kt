package com.example.thermometer.data.repository

import com.example.thermometer.data.db.dao.SensorDataDao
import com.example.thermometer.data.db.dao.SensorDeviceDao
import com.example.thermometer.data.db.entity.SensorDataEntity
import com.example.thermometer.data.db.entity.SensorDeviceEntity
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.repository.DataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepositoryImpl @Inject constructor(
    private val sensorDeviceDao: SensorDeviceDao,
    private val sensorDataDao: SensorDataDao
) : DataRepository {

    // --- Device operations ---

    override suspend fun saveDevice(device: SensorDevice) {
        sensorDeviceDao.insertDevice(device.toEntity())
    }

    override suspend fun getDevices(): List<SensorDevice> {
        return sensorDeviceDao.getAllDevices().first().map { it.toDomain() }
    }

    override suspend fun getDeviceByMac(mac: String): SensorDevice? {
        return sensorDeviceDao.getDeviceByMac(mac)?.toDomain()
    }

    override suspend fun deleteDevice(mac: String) {
        sensorDeviceDao.deleteByMac(mac)
        sensorDataDao.deleteByDevice(mac)
    }

    override fun observeDevices(): Flow<List<SensorDevice>> {
        return sensorDeviceDao.getAllDevices().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // --- Sensor data operations ---

    override suspend fun saveSensorData(data: SensorData) {
        sensorDataDao.insert(data.toEntity())
    }

    override suspend fun saveSensorDataBatch(dataList: List<SensorData>) {
        sensorDataDao.insertAll(dataList.map { it.toEntity() })
    }

    override fun getSensorData(mac: String): Flow<List<SensorData>> {
        return sensorDataDao.getDataByDevice(mac).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSensorDataRange(mac: String, from: Long, to: Long): Flow<List<SensorData>> {
        return sensorDataDao.getDataByDeviceAndRange(mac, from, to).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteSensorDataOlderThan(mac: String, timestamp: Long) {
        sensorDataDao.deleteOlderThan(mac, timestamp)
    }

    // --- Mappers ---

    private fun SensorDevice.toEntity() = SensorDeviceEntity(
        macAddress = macAddress,
        name = name,
        bindingKey = bindingKey,
        isBound = isBound,
        lastSeen = lastSeen
    )

    private fun SensorDeviceEntity.toDomain() = SensorDevice(
        macAddress = macAddress,
        name = name,
        bindingKey = bindingKey,
        isBound = isBound,
        lastSeen = lastSeen
    )

    private fun SensorData.toEntity() = SensorDataEntity(
        temperature = temperature,
        humidity = humidity,
        timestamp = timestamp,
        deviceMac = deviceMac
    )

    private fun SensorDataEntity.toDomain() = SensorData(
        temperature = temperature,
        humidity = humidity,
        timestamp = timestamp,
        deviceMac = deviceMac
    )
}
