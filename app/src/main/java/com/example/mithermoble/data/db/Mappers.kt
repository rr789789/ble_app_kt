package com.example.mithermoble.data.db

import com.example.mithermoble.domain.model.SensorData

/**
 * 数据库实体和领域模型之间的转换
 */

fun SensorDataEntity.toDomainModel(): SensorData {
    return SensorData(
        id = id,
        deviceAddress = deviceAddress,
        temperature = temperature,
        humidity = humidity,
        batteryLevel = batteryLevel,
        timestamp = timestamp
    )
}

fun SensorData.toEntity(): SensorDataEntity {
    return SensorDataEntity(
        id = if (id > 0) id else 0,
        deviceAddress = deviceAddress,
        temperature = temperature,
        humidity = humidity,
        batteryLevel = batteryLevel,
        timestamp = timestamp
    )
}
