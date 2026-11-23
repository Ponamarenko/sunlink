package com.sunlink.svitgrid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.ui.designsystem.sunLinkLightColors
import com.sunlink.svitgrid.ui.designsystem.sunLinkAnalyticsColors

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

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F9D8C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6F2ED),
    onPrimaryContainer = Color(0xFF063B35),
    secondary = Color(0xFF1D6FA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDEEF8),
    onSecondaryContainer = Color(0xFF0B2C43),
    tertiary = Color(0xFFC77800),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBEBCF),
    onTertiaryContainer = Color(0xFF402400),
    background = Color(0xFFF3F6F8),
    onBackground = Color(0xFF10232B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10232B),
    surfaceVariant = Color(0xFFEDF3F6),
    onSurfaceVariant = Color(0xFF405A66),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF)
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
    // Stage 50.2: коренева тема завжди темна. Світла палітра існує тільки
    // всередині Analytics/Diagnostics через PremiumAnalyticsTheme.
    val darkTheme = resolveRootDarkTheme(themeMode)
    val sunLinkColors = if (darkTheme) sunLinkDarkColors() else sunLinkLightColors()
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSunLinkColors provides sunLinkColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}


/** Локальная светлая аналитическая тема: только экран Analytics, остальное приложение тёмное. */
@Composable
fun PremiumAnalyticsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkAnalyticsColors()) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Color(0xFF2563EB),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFDBEAFE),
                onPrimaryContainer = Color(0xFF172554),
                background = Color(0xFFEDF2F9),
                onBackground = Color(0xFF16243A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF16243A),
                surfaceVariant = Color(0xFFF1F5F9),
                onSurfaceVariant = Color(0xFF4B5F79)
            ),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}


/** Технічна діагностика використовує ту саму світлу палітру, що й аналітика. */
@Composable
fun PremiumDiagnosticsTheme(content: @Composable () -> Unit) = PremiumAnalyticsTheme(content)
