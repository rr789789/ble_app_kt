package com.example.mithermoble.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mithermoble.domain.model.BleDevice
import com.example.mithermoble.domain.model.ConnectionState
import com.example.mithermoble.domain.model.SensorData
import com.example.mithermoble.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            viewModel.startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小米温湿度") },
                actions = {
                    if (uiState.connectionState == ConnectionState.READY) {
                        IconButton(onClick = { viewModel.readHistory() }) {
                            Icon(Icons.Default.History, contentDescription = "读取历史")
                        }
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.BluetoothDisabled, contentDescription = "断开")
                        }
                    } else if (!uiState.isScanning) {
                        IconButton(onClick = {
                            val permissions = buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    add(Manifest.permission.BLUETOOTH_SCAN)
                                    add(Manifest.permission.BLUETOOTH_CONNECT)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        }) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = "扫描")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 错误提示
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            if (uiState.connectionState == ConnectionState.READY && uiState.currentData != null) {
                // 已连接 - 显示数据
                SensorDataView(
                    data = uiState.currentData!!,
                    deviceName = uiState.selectedDevice?.name ?: "未知设备",
                    onRefreshHistory = { viewModel.readHistory() }
                )

                // 历史数据
                if (uiState.historyData.isNotEmpty()) {
                    HistoryDataView(data = uiState.historyData)
                }
            } else if (uiState.isScanning || uiState.devices.isNotEmpty()) {
                // 扫描中 - 显示设备列表
                ScanningView(
                    isScanning = uiState.isScanning,
                    devices = uiState.devices,
                    onDeviceClick = { viewModel.connectToDevice(it) },
                    onStopScan = { viewModel.stopScan() }
                )
            } else if (uiState.connectionState == ConnectionState.CONNECTING) {
                // 连接中
                ConnectingView()
            } else {
                // 未连接 - 空状态
                EmptyStateView(onScan = {
                    val permissions = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            add(Manifest.permission.BLUETOOTH_SCAN)
                            add(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                })
            }
        }
    }
}

/**
 * 传感器数据展示
 */
@Composable
fun SensorDataView(
    data: SensorData,
    deviceName: String,
    onRefreshHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 设备名称
        Text(
            text = deviceName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 温度卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "温度",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${String.format("%.1f", data.temperature)}°C",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "温度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 湿度卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = "湿度",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${String.format("%.1f", data.humidity)}%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "湿度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 电量和时间
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (data.batteryLevel >= 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryStd,
                        contentDescription = "电量",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${data.batteryLevel}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = "时间",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateFormat.format(Date(data.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 读取历史按钮
        OutlinedButton(onClick = onRefreshHistory) {
            Icon(Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("读取设备历史数据")
        }
    }
}

/**
 * 历史数据展示
 */
@Composable
fun HistoryDataView(data: List<SensorData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "历史数据 (${data.size}条)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            items(data.take(50)) { item ->
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                ListItem(
                    headlineContent = {
                        Text(
                            "${String.format("%.1f", item.temperature)}°C / ${
                                String.format(
                                    "%.1f",
                                    item.humidity
                                )
                            }%"
                        )
                    },
                    supportingContent = {
                        Text(dateFormat.format(Date(item.timestamp)))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}

/**
 * 扫描中视图
 */
@Composable
fun ScanningView(
    isScanning: Boolean,
    devices: List<BleDevice>,
    onDeviceClick: (BleDevice) -> Unit,
    onStopScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "正在扫描设备...",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onStopScan) {
                    Text("停止")
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { onDeviceClick(device) }
                ) {
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text(device.address) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Text(
                                text = "${device.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 连接中视图
 */
@Composable
fun ConnectingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在连接设备...",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * 空状态视图
 */
@Composable
fun EmptyStateView(onScan: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "未连接设备",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击下方按钮扫描并连接小米温湿度计2",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onScan) {
                Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("扫描设备")
            }
        }
    }
}
