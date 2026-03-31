package com.example.thermometer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thermometer.ui.components.ConnectionStatus
import com.example.thermometer.ui.components.HumidityDisplay
import com.example.thermometer.ui.components.TemperatureDisplay
import com.example.thermometer.ui.viewmodel.DeviceDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceMac: String,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val device by viewModel.device.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()
    val error by viewModel.error.collectAsState()
    val isReadingHistory by viewModel.isReadingHistory.collectAsState()

    LaunchedEffect(deviceMac) {
        if (!isConnected) {
            viewModel.connect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.name ?: "温度计") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToHistory(deviceMac) },
                        enabled = isConnected
                    ) {
                        Icon(Icons.Default.History, contentDescription = "历史数据")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connection status
            ConnectionStatus(
                isConnected = isConnected,
                isConnecting = isConnecting,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error display
            error?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.connect() }) {
                    Text("重试连接")
                }
            }

            // Temperature display
            TemperatureDisplay(
                temperature = sensorData?.temperature ?: 0f,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Humidity display
            HumidityDisplay(
                humidity = sensorData?.humidity ?: 0f,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Additional info
            if (sensorData != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoCard(label = "设备", value = device?.name ?: "-")
                    InfoCard(
                        label = "更新时间",
                        value = formatTimestamp(sensorData!!.timestamp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isConnected) {
                    OutlinedButton(onClick = { viewModel.disconnect() }) {
                        Text("断开连接")
                    }
                    Button(
                        onClick = { viewModel.readHistory() },
                        enabled = !isReadingHistory
                    ) {
                        if (isReadingHistory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("读取历史")
                    }
                } else if (!isConnecting) {
                    Button(onClick = { viewModel.connect() }) {
                        Text("连接设备")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
