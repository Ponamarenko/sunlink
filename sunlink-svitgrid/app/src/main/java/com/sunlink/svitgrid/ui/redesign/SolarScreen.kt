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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

private val SolarHeroParam = "pv_total_power"
private val SolarHistoryParams = listOf("pv1_power", "pv2_power", "pv3_power")

@Composable
fun SolarScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    var period by remember { mutableStateOf("24h") }

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(SolarHeroParam, "24h")
        SolarHistoryParams.forEach { vm.loadSeriesFor(it, "24h") }
    }

    fun reloadHistory(newPeriod: String) {
        vm.loadSeriesFor(SolarHeroParam, newPeriod)
        SolarHistoryParams.forEach { vm.loadSeriesFor(it, newPeriod) }
    }

    val totalItem = telemetryValue(full, "pv", "total_power")
    val totalPower = telemetryNumber(full, "pv", "total_power")
    val freshness = freshnessOf(full?.stale, totalItem?.quality, totalItem?.confidence, full?.connection_status)

    SunLinkScreen(
        title = "Сонячна енергія",
        subtitle = "Фотовольтаїка · ${telemetryLabel(totalItem)}",
        refreshing = loading,
        onRefresh = {
            vm.loadFullTelemetry()
            reloadHistory(period)
        }
    ) {
        DataFreshnessIndicator(freshness, full?.timestamp)
        error?.let { Text(it, color = colors.error, style = SunLinkTypography.caption) }

        // Hero: загальна генерація з реальним спарклайном. Відсутнє значення — «—», а не нуль.
        MetricCard(
            title = "Загальна генерація PV",
            value = totalPower,
            unit = "W",
            icon = Icons.Default.SolarPower,
            accent = colors.solar,
            modifier = Modifier.fillMaxWidth(),
            caption = telemetryLabel(totalItem),
            sparkline = solarChartValues(series[SolarHeroParam]),
            emphasize = true
        )

        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader("Генерація", "Накопичена енергія (кВт·год)")
            EnergyRow("За день", full, "pv", "daily_energy", colors.solar)
            EnergyRow("Всього", full, "pv", "total_energy", colors.solar)
        }

        PremiumSectionHeader("Канали PV", "Кожен рядок окремо — без об'єднання")
        PvChannelCard("PV1", "pv1", full)
        PvChannelCard("PV2", "pv2", full)
        PvChannelCard("PV3", "pv3", full)

        PremiumCard(accent = colors.primary) {
            PremiumSectionHeader("Історія каналів", "PV1 · PV2 · PV3")
            PeriodSelector(period) { selected ->
                period = selected
                reloadHistory(selected)
            }
            HistoryChart("PV1", series["pv1_power"], colors.solar)
            HistoryChart("PV2", series["pv2_power"], colors.solar)
            HistoryChart("PV3", series["pv3_power"], colors.solar)
        }
    }
}

/** Підпис довіри з TelemetryHelpers, розфарбований за рівнем довіри. */
private fun confidenceColor(item: MeasurementValueDto?, colors: SunLinkColors): Color = when {
    item == null -> colors.offline
    item.confidence == "corroborated" -> colors.success
    item.confidence == "calculated" -> colors.warning
    item.confidence == "provisional" -> colors.warning
    else -> colors.textTertiary
}

/** gap == true або value == null стає null — графік розриває лінію, а не з'єднує через нуль. */
private fun solarChartValues(series: HistorySeriesDto?): List<Float?> =
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
private fun PvChannelCard(label: String, prefix: String, full: TelemetryFullDto?) {
    val colors = LocalSunLinkColors.current
    val voltage = telemetryNumber(full, "pv", "${prefix}_voltage")
    val current = telemetryNumber(full, "pv", "${prefix}_current")
    val power = telemetryNumber(full, "pv", "${prefix}_power")
    val powerItem = telemetryValue(full, "pv", "${prefix}_power")
    val hasData = powerItem != null

    PremiumCard(accent = if (hasData) colors.solar else colors.offline) {
        PremiumSectionHeader(
            title = label,
            subtitle = "Напруга · струм · потужність",
            trailing = { StatusPill(telemetryLabel(powerItem), confidenceColor(powerItem, colors)) }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            MetricCard(
                "Напруга", voltage, "V",
                null, colors.solar, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "pv", "${prefix}_voltage"))
            )
            MetricCard(
                "Струм", current, "A",
                null, colors.solar, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "pv", "${prefix}_current"))
            )
        }
        Spacer(Modifier.height(PremiumTokens.spaceSm))
        MetricCard(
            "Потужність", power, "W",
            Icons.Default.Bolt, colors.solar, Modifier.fillMaxWidth(),
            caption = telemetryLabel(powerItem)
        )
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

@Composable
private fun HistoryChart(title: String, series: HistorySeriesDto?, color: Color) {
    val colors = LocalSunLinkColors.current
    val values = solarChartValues(series)
    Column(Modifier.fillMaxWidth().padding(top = PremiumTokens.spaceSm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = SunLinkTypography.label, color = colors.textSecondary)
            Text(series?.unit.orEmpty(), style = SunLinkTypography.caption, color = colors.textTertiary)
        }
        if (values.all { it == null }) {
            EmptyState("Немає даних")
        } else {
            AnalyticsChart(values, color, Modifier.fillMaxWidth().height(88.dp))
        }
    }
}
