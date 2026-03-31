package com.example.thermometer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thermometer.domain.model.SensorData
import com.example.thermometer.ui.viewmodel.HistoryViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    deviceMac: String,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sensorHistory by viewModel.sensorHistory.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(sensorHistory) {
        if (sensorHistory.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = sensorHistory.map { it.timestamp },
                        y = sensorHistory.map { it.temperature.toDouble() }
                    )
                    series(
                        x = sensorHistory.map { it.timestamp },
                        y = sensorHistory.map { it.humidity.toDouble() }
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史数据") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (sensorHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无历史数据",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Stats summary
                val temps = sensorHistory.map { it.temperature }
                val hums = sensorHistory.map { it.humidity }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("最高温度", String.format("%.1f°C", temps.maxOrNull() ?: 0f))
                    StatCard("最低温度", String.format("%.1f°C", temps.minOrNull() ?: 0f))
                    StatCard("平均湿度", String.format("%.1f%%", hums.average().toFloat()))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart
                val timeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                val bottomValueFormatter = CartesianValueFormatter { x, _, _ ->
                    timeFormat.format(Date(x.toLong()))
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lines = listOf(
                                rememberLine { },
                                rememberLine { }
                            )
                        ),
                        startAxis = rememberStart(),
                        bottomAxis = rememberBottom(valueFormatter = bottomValueFormatter),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    scrollState = rememberVicoScrollState(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Data count
                Text(
                    text = "共 ${sensorHistory.size} 条记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
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
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
