package com.example.mithermoble.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mithermoble.domain.model.BleDevice
import com.example.mithermoble.domain.model.ConnectionState
import com.example.mithermoble.domain.model.SensorData
import com.example.mithermoble.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主界面的UI状态
 */
data class MainUiState(
    val devices: List<BleDevice> = emptyList(),
    val selectedDevice: BleDevice? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val currentData: SensorData? = null,
    val historyData: List<SensorData> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null
)

/**
 * 主ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: SensorRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // 收集连接状态
        viewModelScope.launch {
            repository.getConnectionState().collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        // 收集传感器数据
        viewModelScope.launch {
            repository.getSensorData().collect { data ->
                data?.let {
                    _uiState.update { currentState ->
                        currentState.copy(currentData = it)
                    }
                    // 自动保存到本地
                    repository.saveSensorData(it)
                }
            }
        }
    }

    /**
     * 开始扫描设备
     */
    fun startScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            try {
                repository.startScan().collect { devices ->
                    _uiState.update { it.copy(devices = devices) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "扫描失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        repository.stopScan()
        _uiState.update { it.copy(isScanning = false) }
    }

    /**
     * 连接设备
     */
    fun connectToDevice(device: BleDevice) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedDevice = device, error = null) }
            stopScan()
            val result = repository.connect(device.address)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "连接失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
            _uiState.update { it.copy(selectedDevice = null, currentData = null) }
        }
    }

    /**
     * 读取历史数据
     */
    fun readHistory() {
        viewModelScope.launch {
            val result = repository.readHistoryData()
            if (result.isSuccess) {
                val historyData = result.getOrDefault(emptyList())
                repository.saveSensorDataList(historyData)
                _uiState.update { it.copy(historyData = historyData) }
            } else {
                _uiState.update { it.copy(error = "读取历史数据失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    /**
     * 加载本地历史数据
     */
    fun loadLocalHistory(deviceAddress: String) {
        viewModelScope.launch {
            repository.getLocalSensorData(deviceAddress).collect { data ->
                _uiState.update { it.copy(historyData = data) }
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
