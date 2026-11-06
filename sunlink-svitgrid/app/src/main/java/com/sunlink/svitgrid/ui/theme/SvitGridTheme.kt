package com.sunlink.svitgrid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF131A24),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A0D12),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF388E3C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF131A24),
    onTertiaryContainer = Color.White,
    background = Color(0xFF0A0D12),
    onBackground = Color.White,
    surface = Color(0xFF131A24),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1976D2),
    onSurfaceVariant = Color.White,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF4527A0),
    tertiary = Color(0xFF388E3C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF424242),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun SvitGridTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
