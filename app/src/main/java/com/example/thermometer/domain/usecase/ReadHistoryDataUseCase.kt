package com.example.thermometer.domain.usecase

import com.example.thermometer.domain.model.HistoryData
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.repository.BleRepository
import com.example.thermometer.domain.repository.DataRepository
import javax.inject.Inject

class ReadHistoryDataUseCase @Inject constructor(
    private val bleRepository: BleRepository,
    private val dataRepository: DataRepository
) {
    suspend operator fun invoke(deviceMac: String): Result<List<HistoryData>> {
        val result = bleRepository.readHistoryData(deviceMac)
        if (result.isSuccess) {
            val historyList = result.getOrThrow()
            val sensorDataList = historyList.map {
                SensorData(
                    temperature = it.temperature,
                    humidity = it.humidity,
                    timestamp = it.timestamp,
                    deviceMac = it.deviceMac
                )
            }
            dataRepository.saveSensorDataBatch(sensorDataList)
        }
        return result
    }
}
