package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

/**
 * Мережа — чесний стан зовнішньої лінії. Інвертор зараз не віддає grid-канали,
 * тому відсутні значення показуються як «—» / «Недоступно», а не як нулі.
 * Уся палітра приходить з кореневого sunLinkDarkColors() — екран завжди темний.
 */
@Composable
fun GridScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) { vm.loadFullTelemetry() }

    val powerItem = telemetryValue(full, "grid", "power")
    val hasGridData = listOf("power", "voltage_l1", "frequency", "current")
        .any { telemetryNumber(full, "grid", it) != null }

    SunLinkScreen(
        title = "Мережа",
        subtitle = "Параметри зовнішньої лінії",
        refreshing = refreshing,
        onRefresh = { vm.loadFullTelemetry() }
    ) {
        StatusPill(
            text = if (hasGridData) "Частково доступно" else "Недоступно",
            color = if (hasGridData) colors.warning else colors.offline
        )

        // Банер стану підключення мережі.
        PremiumCard(accent = if (hasGridData) colors.warning else colors.grid) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(colors.surfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (hasGridData) colors.warning else colors.grid,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.size(PremiumTokens.spaceMd))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (hasGridData) "Дані мережі частково доступні" else "Дані мережі недоступні",
                        style = SunLinkTypography.title,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceXs))
                    Text(
                        if (hasGridData) {
                            "Частина параметрів мережі надходить від інвертора."
                        } else {
                            "Потужність, напруга та частота мережі зараз не надходять від інвертора."
                        },
                        style = SunLinkTypography.caption,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Електричні параметри — 4 чесні стани.
        Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            GridMetricCard(
                label = "Активна потужність",
                value = telemetryNumber(full, "grid", "power"),
                unit = "W",
                status = if (powerItem == null) "Недоступно" else telemetryLabel(powerItem),
                color = colors.grid,
                modifier = Modifier.weight(1f)
            )
            GridMetricCard(
                label = "Напруга",
                value = telemetryNumber(full, "grid", "voltage_l1"),
                unit = "V",
                status = gridStatus(full, "grid", "voltage_l1"),
                color = colors.grid,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            GridMetricCard(
                label = "Частота",
                value = telemetryNumber(full, "grid", "frequency"),
                unit = "Гц",
                status = gridStatus(full, "grid", "frequency"),
                color = colors.grid,
                modifier = Modifier.weight(1f)
            )
            GridMetricCard(
                label = "Струм",
                value = telemetryNumber(full, "grid", "current"),
                unit = "A",
                status = gridStatus(full, "grid", "current"),
                color = colors.grid,
                modifier = Modifier.weight(1f)
            )
        }

        // Обмін з мережею за добу.
        PremiumCard(accent = colors.gridImport) {
            PremiumSectionHeader(title = "Обмін з мережею за добу", subtitle = "Сьогодні")
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Імпорт", style = SunLinkTypography.caption, color = colors.textSecondary)
                    Spacer(Modifier.height(PremiumTokens.spaceXs))
                    Text(
                        gridEnergy(telemetryNumber(full, "grid", "daily_import_energy")),
                        style = SunLinkTypography.title,
                        color = colors.gridImport
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Експорт", style = SunLinkTypography.caption, color = colors.textSecondary)
                    Spacer(Modifier.height(PremiumTokens.spaceXs))
                    Text(
                        gridEnergy(telemetryNumber(full, "grid", "daily_export_energy")),
                        style = SunLinkTypography.title,
                        color = colors.gridExport
                    )
                }
            }
        }

        // Не клікаємо: навігації на цьому екрані немає. Це чесна текстова підказка.
        PremiumCard(accent = colors.primary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Troubleshoot,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(PremiumTokens.spaceSm))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Перевірити якість телеметрії",
                        style = SunLinkTypography.body,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Відкрити діагностику",
                        style = SunLinkTypography.caption,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

private fun gridStatus(full: com.sunlink.svitgrid.data.dto.TelemetryFullDto?, category: String, field: String): String {
    val item = telemetryValue(full, category, field)
    return if (item == null) "Недоступно" else telemetryLabel(item)
}

@Composable
private fun GridMetricCard(
    label: String,
    value: Float?,
    unit: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    PremiumCard(modifier = modifier, accent = if (value == null) colors.border else color) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(PremiumTokens.spaceSm))
        Text(
            gridValue(value, unit),
            style = SunLinkTypography.metric,
            color = if (value == null) colors.textTertiary else colors.textPrimary
        )
        Spacer(Modifier.height(PremiumTokens.spaceXs))
        Text(
            status,
            style = SunLinkTypography.caption,
            color = if (value == null) colors.textTertiary else color
        )
    }
}

private fun gridValue(value: Float?, unit: String): String = when {
    value == null -> "—"
    unit == "W" && (value <= -1000f || value >= 1000f) -> "%.2f kW".format(value / 1000f)
    unit == "W" -> "%.0f W".format(value)
    unit == "V" -> "%.1f V".format(value)
    unit == "A" -> "%.2f A".format(value)
    unit == "Гц" -> "%.2f Гц".format(value)
    else -> "%.2f %s".format(value, unit)
}

private fun gridEnergy(value: Float?): String =
    if (value == null) "—" else "%.2f kWh".format(value)
