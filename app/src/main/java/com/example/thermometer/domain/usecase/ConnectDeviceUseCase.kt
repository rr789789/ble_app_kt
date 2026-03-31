package com.example.thermometer.domain.usecase

import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.repository.BleRepository
import com.example.thermometer.domain.repository.DataRepository
import javax.inject.Inject

class ConnectDeviceUseCase @Inject constructor(
    private val bleRepository: BleRepository,
    private val dataRepository: DataRepository
) {
    suspend operator fun invoke(device: SensorDevice): Result<Unit> {
        return bleRepository.connect(device)
    }

    suspend fun bindAndConnect(device: SensorDevice): Result<Unit> {
        val bindResult = bleRepository.bindDevice(device)
        if (bindResult.isFailure) return Result.failure(bindResult.exceptionOrNull()!!)

        val bindingKey = bindResult.getOrThrow()
        val boundDevice = device.copy(bindingKey = bindingKey, isBound = true)
        dataRepository.saveDevice(boundDevice)

        return bleRepository.connect(boundDevice)
    }

    suspend fun disconnect() = bleRepository.disconnect()
}
