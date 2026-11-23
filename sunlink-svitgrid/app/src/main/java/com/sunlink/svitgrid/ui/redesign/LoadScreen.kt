package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
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

private const val LoadPowerHistoryParam = "load_power"

@Composable
fun LoadScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    var period by remember { mutableStateOf("24h") }

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(LoadPowerHistoryParam, period)
    }

    val powerItem = telemetryValue(full, "load", "power")
    val power = telemetryNumber(full, "load", "power")
    val freshness = freshnessOf(full?.stale, powerItem?.quality, powerItem?.confidence, full?.connection_status)
    val loadSeries = series[LoadPowerHistoryParam]
    val values = loadChartValues(loadSeries)

    SunLinkScreen(
        title = "Навантаження",
        subtitle = "Споживання · ${telemetryLabel(powerItem)}",
        refreshing = refreshing,
        onRefresh = {
            vm.loadFullTelemetry()
            vm.loadSeriesFor(LoadPowerHistoryParam, period)
        }
    ) {
        DataFreshnessIndicator(freshness, full?.timestamp)
        StatusPill(
            text = if (power == null) "Немає даних" else "Поточне споживання",
            color = if (power == null) colors.offline else colors.load
        )

        // Hero: миттєва потужність (W) з реальним спарклайном — не накопичена енергія.
        MetricCard(
            title = "Поточне навантаження",
            value = power,
            unit = "W",
            icon = Icons.Default.Power,
            accent = colors.load,
            modifier = Modifier.fillMaxWidth(),
            caption = telemetryLabel(powerItem),
            sparkline = values,
            emphasize = true
        )

        PremiumCard(accent = colors.load) {
            PremiumSectionHeader("Електричні параметри", "Миттєві вимірювання")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                MetricCard(
                    "Напруга", telemetryNumber(full, "load", "voltage"), "V",
                    Icons.Default.Bolt, colors.load, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "load", "voltage"))
                )
                MetricCard(
                    "Струм", telemetryNumber(full, "load", "current"), "A",
                    null, colors.load, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "load", "current"))
                )
            }
        }

        PremiumCard(accent = colors.load) {
            PremiumSectionHeader("Накопичена енергія", "кВт·год — не миттєва потужність")
            EnergyRow("За день", full, "load", "daily_energy", colors.load)
            EnergyRow("Всього", full, "load", "energy", colors.load)
        }

        PremiumCard(accent = colors.load) {
            PremiumSectionHeader("Історія навантаження", "load_power")
            PeriodSelector(period) { selected ->
                period = selected
                vm.loadSeriesFor(LoadPowerHistoryParam, selected)
            }
            if (values.all { it == null }) {
                EmptyState("Немає даних")
            } else {
                AnalyticsChart(values, colors.load, Modifier.fillMaxWidth().height(120.dp))
                Text(
                    text = "Пропуски: ${loadSeries?.missing_points ?: 0}",
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** gap == true або value == null стає null — графік розриває лінію, а не з'єднує через нуль. */
private fun loadChartValues(series: HistorySeriesDto?): List<Float?> =
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
