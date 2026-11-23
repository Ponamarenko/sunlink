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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

private const val GridVoltageHistoryParam = "grid_voltage_l1"

@Composable
fun GridScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    var period by remember { mutableStateOf("24h") }

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(GridVoltageHistoryParam, period)
    }

    val powerItem = telemetryValue(full, "grid", "power")
    val power = telemetryNumber(full, "grid", "power")
    val freshness = freshnessOf(full?.stale, powerItem?.quality, powerItem?.confidence, full?.connection_status)
    val voltageSeries = series[GridVoltageHistoryParam]
    val voltageValues = gridChartValues(voltageSeries)

    SunLinkScreen(
        title = "Мережа",
        subtitle = "Обмін енергією · ${telemetryLabel(powerItem)}",
        refreshing = refreshing,
        onRefresh = {
            vm.loadFullTelemetry()
            vm.loadSeriesFor(GridVoltageHistoryParam, period)
        }
    ) {
        DataFreshnessIndicator(freshness, full?.timestamp)
        StatusPill(
            text = gridPowerLabel(power),
            color = when {
                power == null -> colors.offline
                power > 0f -> colors.gridImport
                power < 0f -> colors.gridExport
                else -> colors.grid
            }
        )

        // Hero: напрямок і потужність обміну. Відсутнє значення — «—», а не нуль.
        MetricCard(
            title = "Потужність мережі",
            value = power,
            unit = "W",
            icon = Icons.Default.Bolt,
            accent = colors.grid,
            modifier = Modifier.fillMaxWidth(),
            caption = telemetryLabel(powerItem),
            emphasize = true
        )

        PremiumCard(accent = colors.grid) {
            PremiumSectionHeader("Параметри мережі", "Миттєві вимірювання")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                MetricCard(
                    "Напруга L1", telemetryNumber(full, "grid", "voltage_l1"), "V",
                    Icons.Default.Bolt, colors.grid, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "grid", "voltage_l1"))
                )
                MetricCard(
                    "Частота", telemetryNumber(full, "grid", "frequency"), "Гц",
                    Icons.Default.GridOn, colors.grid, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "grid", "frequency"))
                )
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            MetricCard(
                "Струм", telemetryNumber(full, "grid", "current"), "A",
                Icons.Default.Power, colors.grid, Modifier.fillMaxWidth(),
                caption = telemetryLabel(telemetryValue(full, "grid", "current"))
            )
        }

        PremiumCard(accent = colors.gridImport) {
            PremiumSectionHeader("Енергія", "Імпорт / експорт (кВт·год)")
            EnergyRow("Імпорт за день", full, "grid", "daily_import_energy", colors.gridImport)
            EnergyRow("Експорт за день", full, "grid", "daily_export_energy", colors.gridExport)
            EnergyRow("Імпорт всього", full, "grid", "import_energy", colors.gridImport)
            EnergyRow("Експорт всього", full, "grid", "export_energy", colors.gridExport)
        }

        PremiumCard(accent = colors.grid) {
            PremiumSectionHeader("Історія напруги", "grid_voltage_l1")
            PeriodSelector(period) { selected ->
                period = selected
                vm.loadSeriesFor(GridVoltageHistoryParam, selected)
            }
            if (voltageValues.all { it == null }) {
                EmptyState("Немає даних")
            } else {
                AnalyticsChart(voltageValues, colors.grid, Modifier.fillMaxWidth().height(120.dp))
                Text(
                    text = "Пропуски: ${voltageSeries?.missing_points ?: 0}",
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

/** gap == true або value == null стає null — графік розриває лінію, а не з'єднує через нуль. */
private fun gridChartValues(series: HistorySeriesDto?): List<Float?> =
    series?.points?.map { point ->
        if (point.gap || point.value == null) null else point.value!!.toFloat()
    } ?: emptyList()

@Composable
private fun EnergyRow(label: String, full: TelemetryFullDto?, category: String, field: String, color: Color) {
    val colors = LocalSunLinkColors.current
    val value = telemetryNumber(full, category, field)
    val item = telemetryValue(full, category, field)
    Column(Modifier.fillMaxWidth().padding(vertical = PremiumTokens.spaceXs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = SunLinkTypography.body, color = colors.textSecondary)
            Text(
                text = if (value == null) "—" else "%.2f kWh".format(value),
                style = SunLinkTypography.label,
                color = if (value == null) colors.textTertiary else color
            )
        }
        Text(telemetryLabel(item), style = SunLinkTypography.caption, color = colors.textTertiary)
    }
}

@Composable
private fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("24h", "7d", "30d").forEach { value ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.primary else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    value,
                    style = SunLinkTypography.label,
                    color = if (active) colors.onPrimary else colors.textSecondary
                )
            }
        }
    }
}
