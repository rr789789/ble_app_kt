package com.example.thermometer.domain.usecase

import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.repository.DataRepository
import javax.inject.Inject

class SaveDeviceUseCase @Inject constructor(
    private val dataRepository: DataRepository
) {
    suspend operator fun invoke(device: SensorDevice) {
        dataRepository.saveDevice(device)
    }
}
