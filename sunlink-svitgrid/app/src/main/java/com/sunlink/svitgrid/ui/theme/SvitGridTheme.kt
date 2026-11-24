package com.sunlink.svitgrid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeController(
    val mode: ThemeMode,
    val setMode: (ThemeMode) -> Unit
)

val LocalThemeController = compositionLocalOf { ThemeController(ThemeMode.DARK) {} }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF04211D),
    primaryContainer = Color(0xFF16223A),
    onPrimaryContainer = Color(0xFFF2F7F9),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF04211D),
    secondaryContainer = Color(0xFF16223A),
    onSecondaryContainer = Color(0xFFF2F7F9),
    tertiary = Color(0xFFF5B942),
    onTertiary = Color(0xFF2A1B00),
    tertiaryContainer = Color(0xFF16223A),
    onTertiaryContainer = Color(0xFFF2F7F9),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFF2F7F9),
    surface = Color(0xFF0F1726),
    onSurface = Color(0xFFF2F7F9),
    surfaceVariant = Color(0xFF16223A),
    onSurfaceVariant = Color(0xFFB4C6CF),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0808)
)

/** Hybryd Premium: корінь приложения NEVER light, независимо от сохранённой настройки. */
fun resolveRootDarkTheme(themeMode: ThemeMode): Boolean = true

/**
 * Единая точка входа в тему. SYSTEM/LIGHT сохранены для совместимости настроек,
 * но не могут включить светлую палитру на основных экранах.
 */
@Composable
fun SvitGridTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    // Stage 53.1: єдина тема застосунку — Dark Hybrid Premium.
    // Локальних світлих тем більше не існує; themeMode зберігається для сумісності.
    resolveRootDarkTheme(themeMode)
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
