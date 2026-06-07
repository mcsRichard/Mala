package com.meritminder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Terracotta40,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer80,
    onPrimaryContainer = Color(0xFF3B0B01),
    secondary = WarmBrown40,
    onSecondary = Color.White,
    secondaryContainer = WarmBrownContainer80,
    onSecondaryContainer = Color(0xFF2D1509),
    background = WarmWhite,
    surface = Color.White,
    onBackground = Color(0xFF201A18),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF4EDE8),
    onSurfaceVariant = Color(0xFF52443F),
    error = ErrorLight
)

@Composable
fun MalaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
