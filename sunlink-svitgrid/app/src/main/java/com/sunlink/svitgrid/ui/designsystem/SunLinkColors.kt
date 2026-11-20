package com.sunlink.svitgrid.ui.designsystem

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * SUNLINK — PREMIUM ENERGY MONITORING.
 *
 * Централизованная семантическая палитра. Один и тот же смысл цвета в Dark и Light.
 * Никаких «случайных» цветов в Composable — только эти токены.
 */
data class SunLinkColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val solar: Color,
    val battery: Color,
    val batteryCharge: Color,
    val batteryDischarge: Color,
    val grid: Color,
    val gridImport: Color,
    val gridExport: Color,
    val load: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val offline: Color,
    val disabled: Color,
    val flowIdle: Color
)

fun sunLinkDarkColors() = SunLinkColors(
    background = Color(0xFF0B1014),
    surface = Color(0xFF121A20),
    surfaceElevated = Color(0xFF1A242C),
    border = Color(0xFF2A3A44),
    textPrimary = Color(0xFFF2F7F9),
    textSecondary = Color(0xFFB4C6CF),
    textTertiary = Color(0xFF7F97A3),
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF04211D),
    primarySoft = Color(0x1F2DD4BF),
    solar = Color(0xFFF5B942),
    battery = Color(0xFF34D399),
    batteryCharge = Color(0xFF34D399),
    batteryDischarge = Color(0xFFFF9A5C),
    grid = Color(0xFFA78BFA),
    gridImport = Color(0xFFA78BFA),
    gridExport = Color(0xFF22D3EE),
    load = Color(0xFFF59E5B),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
    offline = Color(0xFF94A3B8),
    disabled = Color(0xFF54636C),
    flowIdle = Color(0xFF3A4A54)
)

fun sunLinkLightColors() = SunLinkColors(
    background = Color(0xFFF3F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF7FAFB),
    border = Color(0xFFDCE5EA),
    textPrimary = Color(0xFF10232B),
    textSecondary = Color(0xFF405A66),
    textTertiary = Color(0xFF6B828D),
    primary = Color(0xFF0F9D8C),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0x1A0F9D8C),
    solar = Color(0xFFC77800),
    battery = Color(0xFF12855C),
    batteryCharge = Color(0xFF12855C),
    batteryDischarge = Color(0xFFC2570B),
    grid = Color(0xFF7145C9),
    gridImport = Color(0xFF7145C9),
    gridExport = Color(0xFF0E7C93),
    load = Color(0xFFB45309),
    success = Color(0xFF12855C),
    warning = Color(0xFF9A6700),
    error = Color(0xFFC62828),
    offline = Color(0xFF5C6B73),
    disabled = Color(0xFFA5B1B7),
    flowIdle = Color(0xFFCBD6DC)
)

val LocalSunLinkColors = compositionLocalOf { sunLinkDarkColors() }
