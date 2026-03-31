package com.example.thermometer.domain.usecase

import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.repository.DataRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSensorHistoryUseCase @Inject constructor(
    private val dataRepository: DataRepository
) {
    operator fun invoke(mac: String): Flow<List<SensorData>> =
        dataRepository.getSensorData(mac)

    operator fun invoke(mac: String, from: Long, to: Long): Flow<List<SensorData>> =
        dataRepository.getSensorDataRange(mac, from, to)
}
