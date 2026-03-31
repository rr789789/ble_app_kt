package com.example.thermometer.domain.repository

import com.example.thermometer.domain.model.HistoryData
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.model.SensorDevice
import kotlinx.coroutines.flow.Flow

interface DataRepository {

    // Device CRUD
    suspend fun saveDevice(device: SensorDevice)
    suspend fun getDevices(): List<SensorDevice>
    suspend fun getDeviceByMac(mac: String): SensorDevice?
    suspend fun deleteDevice(mac: String)
    fun observeDevices(): Flow<List<SensorDevice>>

    // Sensor data
    suspend fun saveSensorData(data: SensorData)
    suspend fun saveSensorDataBatch(dataList: List<SensorData>)
    fun getSensorData(mac: String): Flow<List<SensorData>>
    fun getSensorDataRange(mac: String, from: Long, to: Long): Flow<List<SensorData>>
    suspend fun deleteSensorDataOlderThan(mac: String, timestamp: Long)
}
