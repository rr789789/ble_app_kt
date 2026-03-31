package com.example.thermometer.domain.usecase

import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.repository.BleRepository
import com.example.thermometer.domain.repository.DataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ReadSensorDataUseCase @Inject constructor(
    private val bleRepository: BleRepository,
    private val dataRepository: DataRepository
) {
    operator fun invoke(): Flow<SensorData> {
        return bleRepository.readRealtimeData().onEach { data ->
            dataRepository.saveSensorData(data)
        }
    }
}
