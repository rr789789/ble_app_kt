package com.example.thermometer.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.domain.usecase.GetSensorHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSensorHistoryUseCase: GetSensorHistoryUseCase
) : ViewModel() {

    private val deviceMac: String = savedStateHandle["deviceMac"] ?: ""

    private val _sensorHistory = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorHistory: StateFlow<List<SensorData>> = _sensorHistory.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getSensorHistoryUseCase(deviceMac).collect { data ->
                _sensorHistory.value = data
            }
        }
    }

    fun loadHistoryRange(from: Long, to: Long) {
        viewModelScope.launch {
            getSensorHistoryUseCase(deviceMac, from, to).collect { data ->
                _sensorHistory.value = data
            }
        }
    }
}
