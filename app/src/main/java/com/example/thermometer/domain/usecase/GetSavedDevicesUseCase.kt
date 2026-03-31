package com.example.thermometer.domain.usecase

import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.repository.DataRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavedDevicesUseCase @Inject constructor(
    private val dataRepository: DataRepository
) {
    operator fun invoke(): Flow<List<SensorDevice>> = dataRepository.observeDevices()

    suspend fun getByMac(mac: String): SensorDevice? = dataRepository.getDeviceByMac(mac)

    suspend fun delete(mac: String) = dataRepository.deleteDevice(mac)
}
