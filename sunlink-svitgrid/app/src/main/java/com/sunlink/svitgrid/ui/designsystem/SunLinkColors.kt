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
    background = Color(0xFF070B14),
    surface = Color(0xFF0F1726),
    surfaceElevated = Color(0xFF16223A),
    border = Color(0xFF24324A),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFA8B8C8),
    textTertiary = Color(0xFF6B7F93),
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF04211D),
    primarySoft = Color(0x1F2DD4BF),
    solar = Color(0xFFF5B942),
    battery = Color(0xFF10B981),
    batteryCharge = Color(0xFF10B981),
    batteryDischarge = Color(0xFFFB923C),
    grid = Color(0xFF3B82F6),
    gridImport = Color(0xFF3B82F6),
    gridExport = Color(0xFF38BDF8),
    load = Color(0xFFF59E0B),
    success = Color(0xFF10B981),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
    offline = Color(0xFF64748B),
    disabled = Color(0xFF475569),
    flowIdle = Color(0xFF334155)
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

/** Светлая аналитическая тема, предусмотренная макетом Hybrid Premium. */
fun sunLinkAnalyticsColors() = SunLinkColors(
    background = Color(0xFFEDF2F9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF8FAFD),
    border = Color(0xFFD9E2EE),
    textPrimary = Color(0xFF16243A),
    textSecondary = Color(0xFF4B5F79),
    textTertiary = Color(0xFF8496AC),
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0x1A2563EB),
    solar = Color(0xFFD97706),
    battery = Color(0xFF047857),
    batteryCharge = Color(0xFF047857),
    batteryDischarge = Color(0xFFC2410C),
    grid = Color(0xFF2563EB),
    gridImport = Color(0xFF2563EB),
    gridExport = Color(0xFF0891B2),
    load = Color(0xFFEA580C),
    success = Color(0xFF047857),
    warning = Color(0xFFB45309),
    error = Color(0xFFDC2626),
    offline = Color(0xFF64748B),
    disabled = Color(0xFFCBD5E1),
    flowIdle = Color(0xFFCBD5E1)
)

val LocalSunLinkColors = compositionLocalOf { sunLinkDarkColors() }
