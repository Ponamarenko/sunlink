package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

private val gridPeriods = listOf("24h" to "24 год", "7d" to "7 дн", "30d" to "30 дн")

@Composable
fun GridScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    var period by remember { mutableStateOf("24h") }

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor("grid_voltage_l1", period)
    }

    val power = telemetryNumber(full, "grid", "power")

    SunLinkScreen(
        title = "Мережа",
        subtitle = "Потік енергії · ${full?.timestamp ?: "—"}",
        refreshing = refreshing,
        onRefresh = { vm.loadFullTelemetry(); vm.loadSeriesFor("grid_voltage_l1", period) }
    ) {
        StatusPill(
            text = gridPowerLabel(power),
            color = when {
                power == null -> colors.offline
                power > 0f -> colors.gridImport
                else -> colors.gridExport
            }
        )

        SunLinkCard(accent = colors.grid) {
            Text("ПОТУЖНІСТЬ МЕРЕЖІ", style = SunLinkTypography.label, color = colors.textSecondary)
            Text(
                text = if (power == null) "—" else "%.1f Вт".format(power),
                style = SunLinkTypography.display,
                color = colors.textPrimary
            )
            Text(
                text = gridPowerCaption(power),
                style = SunLinkTypography.caption,
                color = colors.textTertiary
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Напруга", telemetryNumber(full, "grid", "voltage"), "В",
                Icons.Default.Bolt, colors.grid, Modifier.weight(1f)
            )
            MetricCard(
                "Струм", telemetryNumber(full, "grid", "current"), "А",
                Icons.Default.Power, colors.grid, Modifier.weight(1f)
            )
            MetricCard(
                "Частота", telemetryNumber(full, "grid", "frequency"), "Гц",
                Icons.Default.GridOn, colors.grid, Modifier.weight(1f)
            )
        }

        SunLinkCard(accent = colors.gridImport) {
            Text("ЕНЕРГІЯ", style = SunLinkTypography.label, color = colors.textSecondary)
            EnergyRow("Імпорт за день", telemetryNumber(full, "grid", "daily_import_energy"), colors.gridImport)
            EnergyRow("Експорт за день", telemetryNumber(full, "grid", "daily_export_energy"), colors.gridExport)
            EnergyRow("Імпорт всього", telemetryNumber(full, "grid", "import_energy"), colors.gridImport)
            EnergyRow("Експорт всього", telemetryNumber(full, "grid", "export_energy"), colors.gridExport)
        }

        SunLinkCard(accent = colors.grid) {
            Text("ІСТОРІЯ НАПРУГИ МЕРЕЖІ", style = SunLinkTypography.label, color = colors.textSecondary)
            PeriodSelector(period, gridPeriods) { selected ->
                period = selected
                vm.loadSeriesFor("grid_voltage_l1", selected)
            }
            val points = series["grid_voltage_l1"]?.points.orEmpty()
            val values = points.map { point -> if (point.gap) null else point.value?.toFloat() }
            if (values.all { it == null }) {
                EmptyHint("Немає даних")
            } else {
                Sparkline(values, colors.grid, Modifier.fillMaxWidth().height(120.dp))
                Text(
                    text = "Пропуски: ${series["grid_voltage_l1"]?.missing_points ?: 0}",
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun gridPowerLabel(power: Float?): String = when {
    power == null -> "Немає даних"
    power > 0f -> "Імпорт з мережі"
    power < 0f -> "Експорт у мережу"
    else -> "Баланс"
}

private fun gridPowerCaption(power: Float?): String = when {
    power == null -> "Немає даних"
    power > 0f -> "+" + "%.1f Вт споживається з мережі".format(power)
    power < 0f -> "%.1f Вт повертається в мережу".format(power)
    else -> "Баланс"
}

@Composable
private fun EnergyRow(label: String, value: Float?, color: androidx.compose.ui.graphics.Color) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = SunLinkTypography.body, color = colors.textSecondary)
        Text(
            text = if (value == null) "—" else "%.2f кВт·год".format(value),
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else color
        )
    }
}

@Composable
private fun PeriodSelector(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}
