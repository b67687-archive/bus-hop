package com.bushop.sg.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FE),
    onPrimaryContainer = Color(0xFF004A99),
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFEFEFF4),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFFE0E0E5),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1C3A5E),
    onPrimaryContainer = Color(0xFF8BB8FF),
    secondary = Color(0xFF30D158),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B3D23),
    onSecondaryContainer = Color(0xFF7AE08A),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF2C2C30),
    onSurfaceVariant = Color(0xFFA0A0A5),
    outline = Color(0xFF48484A),
    error = Color(0xFFFF453A),
    onError = Color.White
)

@Composable
fun BusHopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}