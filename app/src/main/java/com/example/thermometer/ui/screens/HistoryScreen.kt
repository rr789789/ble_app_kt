package com.example.thermometer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thermometer.ui.theme.TemperatureHigh
import com.example.thermometer.ui.theme.TemperatureNormal
import com.example.thermometer.ui.viewmodel.HistoryViewModel
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

                Spacer(modifier = Modifier.height(24.dp))

                // Temperature chart
                Text(
                    "温度趋势",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SimpleLineChart(
                    data = temps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    lineColor = TemperatureNormal,
                    label = "°C"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Humidity chart
                Text(
                    "湿度趋势",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SimpleLineChart(
                    data = hums,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    lineColor = Color(0xFF00BCD4),
                    label = "%"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Data count
                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                Text(
                    text = "共 ${sensorHistory.size} 条记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sensorHistory.size >= 2) {
                    Text(
                        text = "${timeFormat.format(Date(sensorHistory.last().timestamp))} ~ ${timeFormat.format(Date(sensorHistory.first().timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = TemperatureNormal,
    label: String = ""
) {
    if (data.isEmpty()) return

    val minVal = data.minOrNull() ?: 0f
    val maxVal = data.maxOrNull() ?: 0f
    val range = if (maxVal - minVal < 0.1f) 1f else maxVal - minVal
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 28f
        color = android.graphics.Color.GRAY
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            val w = size.width
            val h = size.height
            val padding = 40f

            // Draw Y-axis labels
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f%s", maxVal, label),
                0f,
                paint.textSize,
                paint
            )
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f%s", minVal, label),
                0f,
                h + paint.textSize,
                paint
            )

            // Draw grid lines
            for (i in 0..3) {
                val y = padding + (h - padding - padding) * i / 3
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(padding, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            if (data.size < 2) return@Canvas

            // Draw line
            val path = Path()
            val step = (w - padding) / (data.size - 1)

            data.forEachIndexed { index, value ->
                val x = padding + index * step
                val y = h - padding - ((value - minVal) / range) * (h - padding * 2)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                // Draw data point
                drawCircle(
                    color = lineColor,
                    radius = 3f,
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
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
