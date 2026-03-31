package com.example.thermometer.domain.usecase

import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanDevicesUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    operator fun invoke(): Flow<List<SensorDevice>> = bleRepository.scanDevices()

    fun stopScan() = bleRepository.stopScan()
}
