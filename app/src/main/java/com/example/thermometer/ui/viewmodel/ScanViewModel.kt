package com.example.thermometer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thermometer.domain.model.SensorDevice
import com.example.thermometer.domain.usecase.ScanDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanDevicesUseCase: ScanDevicesUseCase
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<SensorDevice>>(emptyList())
    val scannedDevices: StateFlow<List<SensorDevice>> = _scannedDevices.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startScan() {
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            _scannedDevices.value = emptyList()

            scanDevicesUseCase().collect { devices ->
                _scannedDevices.value = devices
            }

            _isScanning.value = false
        }
    }

    fun stopScan() {
        scanDevicesUseCase.stopScan()
        _isScanning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
