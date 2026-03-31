package com.example.thermometer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thermometer.ui.theme.TemperatureHigh
import com.example.thermometer.ui.theme.TemperatureLow
import com.example.thermometer.ui.theme.TemperatureNormal

@Composable
fun TemperatureDisplay(
    temperature: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 12.dp
) {
    val color = when {
        temperature < 10f -> TemperatureLow
        temperature in 10f..30f -> TemperatureNormal
        else -> TemperatureHigh
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Arc background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arcPadding = strokeWidth.toPx() / 2
            val arcSize = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx())

            // Background arc
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(arcPadding, arcPadding),
                size = arcSize,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Value arc (map -10..50 to 0..240 degrees)
            val progress = ((temperature + 10f) / 60f).coerceIn(0f, 1f)
            drawArc(
                color = color,
                startAngle = 150f,
                sweepAngle = 240f * progress,
                useCenter = false,
                topLeft = Offset(arcPadding, arcPadding),
                size = arcSize,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        // Text in center
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", temperature),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = "°C",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = color.copy(alpha = 0.7f)
                )
            )
        }
    }
}
