package com.example.thermometer.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.usecase.ConnectDeviceUseCase
import com.example.thermometer.domain.usecase.GetSavedDevicesUseCase
import com.example.thermometer.domain.usecase.ReadHistoryDataUseCase
import com.example.thermometer.domain.usecase.ReadSensorDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val readSensorDataUseCase: ReadSensorDataUseCase,
    private val readHistoryDataUseCase: ReadHistoryDataUseCase,
    private val getSavedDevicesUseCase: GetSavedDevicesUseCase
) : ViewModel() {

    private val deviceMac: String = savedStateHandle["deviceMac"] ?: ""

    private val _device = MutableStateFlow<SensorDevice?>(null)
    val device: StateFlow<SensorDevice?> = _device.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isReadingHistory = MutableStateFlow(false)
    val isReadingHistory: StateFlow<Boolean> = _isReadingHistory.asStateFlow()

    init {
        loadDevice()
    }

    private fun loadDevice() {
        viewModelScope.launch {
            _device.value = getSavedDevicesUseCase.getByMac(deviceMac)
        }
    }

    fun connect() {
        val device = _device.value ?: return
        viewModelScope.launch {
            _isConnecting.value = true
            _error.value = null

            val result = if (device.isBound) {
                connectDeviceUseCase(device)
            } else {
                connectDeviceUseCase.bindAndConnect(device)
            }

            result.onSuccess {
                _isConnected.value = true
                _isConnecting.value = false
                _device.value = if (!device.isBound) {
                    getSavedDevicesUseCase.getByMac(deviceMac)
                } else {
                    device
                }
                startReadingData()
            }.onFailure {
                _isConnecting.value = false
                _error.value = it.message
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectDeviceUseCase.disconnect()
            _isConnected.value = false
        }
    }

    private fun startReadingData() {
        viewModelScope.launch {
            readSensorDataUseCase().collect { data ->
                _sensorData.value = data
            }
        }
    }

    fun readHistory() {
        viewModelScope.launch {
            _isReadingHistory.value = true
            val result = readHistoryDataUseCase(deviceMac)
            _isReadingHistory.value = false
            result.onFailure {
                _error.value = "读取历史数据失败: ${it.message}"
            }
        }
    }
}
