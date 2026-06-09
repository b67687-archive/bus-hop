package com.bushop.sg.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.bushop.sg.domain.model.ColorSchemeOption

val LightColorScheme =
    lightColorScheme(
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
        onError = Color.White,
    )

val DarkColorScheme =
    darkColorScheme(
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
        onError = Color.White,
    )

/** Contrast Blue — punchier version of the classic scheme. */
val ContrastLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF0050CC),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E4FF),
        onPrimaryContainer = Color(0xFF002266),
        secondary = Color(0xFF2C9E4E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDAF0E0),
        onSecondaryContainer = Color(0xFF0C351D),
        background = Color(0xFFEBEFF5),
        onBackground = Color(0xFF111318),
        surface = Color.White,
        onSurface = Color(0xFF111318),
        surfaceVariant = Color(0xFFE2E6ED),
        onSurfaceVariant = Color(0xFF50545C),
        outline = Color(0xFFC4C8D0),
        error = Color(0xFFD32F2F),
        onError = Color.White,
    )

val ContrastDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF6EB0FF),
        onPrimary = Color(0xFF002F6C),
        primaryContainer = Color(0xFF0045A0),
        onPrimaryContainer = Color(0xFFD6E4FF),
        secondary = Color(0xFF4BC572),
        onSecondary = Color(0xFF003919),
        secondaryContainer = Color(0xFF145330),
        onSecondaryContainer = Color(0xFFC6F0D2),
        background = Color(0xFF0E0E11),
        onBackground = Color(0xFFE8E8EC),
        surface = Color(0xFF1A1A1F),
        onSurface = Color(0xFFE8E8EC),
        surfaceVariant = Color(0xFF26282E),
        onSurfaceVariant = Color(0xFFA9ADB5),
        outline = Color(0xFF4A4E56),
        error = Color(0xFFFF5252),
        onError = Color.White,
    )

@Composable
fun BusHopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemeOption: ColorSchemeOption = ColorSchemeOption.BLUE,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when (colorSchemeOption) {
            ColorSchemeOption.BLUE -> if (darkTheme) DarkColorScheme else LightColorScheme
            ColorSchemeOption.CONTRAST_BLUE -> if (darkTheme) ContrastDarkColorScheme else ContrastLightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
