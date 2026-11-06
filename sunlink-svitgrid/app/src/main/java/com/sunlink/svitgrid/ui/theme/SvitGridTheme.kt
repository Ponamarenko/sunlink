package com.sunlink.svitgrid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GraphitePrimary,
    onPrimary = Color.Black,
    primaryContainer = GraphiteSurface,
    onPrimaryContainer = Color.White,
    secondary = GraphiteSecondary,
    onSecondary = Color.Black,
    secondaryContainer = GraphiteSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = GraphiteTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = GraphiteSurfaceVariant,
    onTertiaryContainer = Color.White,
    background = GraphiteBackground,
    onBackground = Color.White,
    surface = GraphiteSurface,
    onSurface = Color.White,
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = GraphiteError,
    onError = Color.White
)

@Composable
fun SvitGridTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
