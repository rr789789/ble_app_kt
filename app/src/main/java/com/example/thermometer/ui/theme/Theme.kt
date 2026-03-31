package com.example.thermometer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue80,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.Black,
    secondary = Teal40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Teal80,
    tertiary = TemperatureNormal,
    surface = LightSurface,
    onSurface = androidx.compose.ui.graphics.Color.Black,
    surfaceVariant = LightCard,
    onSurfaceVariant = androidx.compose.ui.graphics.Color.DarkGray,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue80,
    secondary = Teal80,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    secondaryContainer = Teal40,
    tertiary = TemperatureNormal,
    surface = DarkSurface,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = androidx.compose.ui.graphics.Color.LightGray,
)

@Composable
fun ThermometerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
