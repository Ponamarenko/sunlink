package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

/**
 * Мережа — чесний стан зовнішньої лінії. Інвертор зараз не віддає grid-канали,
 * тому відсутні значення показуються як «—» / «Недоступно», а не як нулі.
 * Одна hero-поверхня стану, GROUPED-панелі параметрів і обміну, без стосу карток.
 */
@Composable
fun GridScreen(vm: MainViewModel = viewModel(), onNavigate: (String) -> Unit = {}) {
    val full by vm.fullTelemetry.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) { vm.loadFullTelemetry() }

    val powerItem = telemetryValue(full, "grid", "power")
    val hasGridData = listOf("power", "voltage_l1", "frequency", "current")
        .any { telemetryNumber(full, "grid", it) != null }
    val statusColor = if (hasGridData) colors.warning else colors.grid
    val freshness = freshnessOf(
        full?.stale, powerItem?.quality, powerItem?.confidence, full?.connection_status
    )

    SunLinkScreen(
        title = "Мережа",
        subtitle = "Параметри зовнішньої лінії",
        refreshing = refreshing,
        onRefresh = { vm.loadFullTelemetry() }
    ) {
        // Єдина hero-поверхня: стан підключення та свіжість даних.
        HeroPanel(accent = statusColor) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(colors.surfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.size(PremiumTokens.spaceMd))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (hasGridData) "Дані мережі частково доступні" else "Дані мережі недоступні",
                        style = SunLinkTypography.headline,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceXs))
                    Text(
                        if (hasGridData) {
                            "Частина параметрів мережі надходить від інвертора."
                        } else {
                            "Потужність, напруга та частота мережі зараз не надходять від інвертора."
                        },
                        style = SunLinkTypography.body,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceSm))
                    DataFreshnessIndicator(freshness, full?.timestamp, compact = true)
                }
            }
        }

        // Електричні параметри — чотири чесні рядки замість чотирьох карток.
        DataPanel(title = "Електричні параметри", subtitle = "Миттєві вимірювання") {
            val power = telemetryNumber(full, "grid", "power")
            val voltage = telemetryNumber(full, "grid", "voltage_l1")
            val frequency = telemetryNumber(full, "grid", "frequency")
            val current = telemetryNumber(full, "grid", "current")
            InlineMetric(
                label = "Активна потужність",
                value = gridParameterValue(power, "W", gridStatus(full, "grid", "power")),
                accent = if (power == null) colors.textTertiary else colors.grid
            )
            InlineMetric(
                label = "Напруга",
                value = gridParameterValue(voltage, "V", gridStatus(full, "grid", "voltage_l1")),
                accent = if (voltage == null) colors.textTertiary else colors.grid
            )
            InlineMetric(
                label = "Частота",
                value = gridParameterValue(frequency, "Гц", gridStatus(full, "grid", "frequency")),
                accent = if (frequency == null) colors.textTertiary else colors.grid
            )
            InlineMetric(
                label = "Струм",
                value = gridParameterValue(current, "A", gridStatus(full, "grid", "current")),
                accent = if (current == null) colors.textTertiary else colors.grid
            )
        }

        // Обмін з мережею за добу — компактні плитки в одній панелі.
        DataPanel(title = "Обмін з мережею за добу", subtitle = "Сьогодні") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                StatTile(
                    label = "Імпорт",
                    value = telemetryNumber(full, "grid", "daily_import_energy"),
                    unit = "kWh",
                    accent = colors.gridImport,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Експорт",
                    value = telemetryNumber(full, "grid", "daily_export_energy"),
                    unit = "kWh",
                    accent = colors.gridExport,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stitch: клікабельний рядок переходу до діагностики якості телеметрії.
        DataPanel(
            title = "Діагностика телеметрії",
            subtitle = "Перевірити якість даних",
            modifier = Modifier.clickable { onNavigate("diagnostics") },
            trailing = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        ) {
            InlineMetric("Перевірити якість телеметрії", "Відкрити", colors.primary)
        }
    }
}

private fun gridStatus(full: com.sunlink.svitgrid.data.dto.TelemetryFullDto?, category: String, field: String): String {
    val item = telemetryValue(full, category, field)
    return if (item == null) "Недоступно" else telemetryLabel(item)
}

/** Відсутнє значення — окремий чесний стан, а не нуль і не підроблений підпис. */
private fun gridParameterValue(value: Float?, unit: String, status: String): String =
    if (value == null) "Недоступно" else "${gridValue(value, unit)} · $status"

private fun gridValue(value: Float?, unit: String): String = when {
    value == null -> "—"
    unit == "W" && (value <= -1000f || value >= 1000f) -> "%.2f kW".format(value / 1000f)
    unit == "W" -> "%.0f W".format(value)
    unit == "V" -> "%.1f V".format(value)
    unit == "A" -> "%.2f A".format(value)
    unit == "Гц" -> "%.2f Гц".format(value)
    else -> "%.2f %s".format(value, unit)
}
