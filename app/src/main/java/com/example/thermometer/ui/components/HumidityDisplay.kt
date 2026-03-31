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
import com.example.thermometer.ui.theme.HumidityHigh
import com.example.thermometer.ui.theme.HumidityLow
import com.example.thermometer.ui.theme.HumidityNormal

@Composable
fun HumidityDisplay(
    humidity: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 12.dp
) {
    val color = when {
        humidity < 30f -> HumidityLow
        humidity in 30f..70f -> HumidityNormal
        else -> HumidityHigh
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arcPadding = strokeWidth.toPx() / 2
            val arcSize = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx())

            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(arcPadding, arcPadding),
                size = arcSize,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            val progress = (humidity / 100f).coerceIn(0f, 1f)
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", humidity),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = "%RH",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = color.copy(alpha = 0.7f)
                )
            )
        }
    }
}
