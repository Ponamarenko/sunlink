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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
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

private val BatteryHistoryParams = listOf("battery_soc", "battery_voltage", "battery_current")

@Composable
fun BatteryScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current
    val hasFreshTelemetry = full != null && full?.stale != true

    var period by remember { mutableStateOf("24h") }

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        BatteryHistoryParams.forEach { vm.loadSeriesFor(it, "24h") }
    }

    fun reloadHistory(newPeriod: String) {
        BatteryHistoryParams.forEach { vm.loadSeriesFor(it, newPeriod) }
    }

    val socItem = telemetryValue(full, "battery", "soc")
    val soc = telemetryNumber(full, "battery", "soc")
    val freshness = freshnessOf(full?.stale, socItem?.quality, socItem?.confidence, full?.connection_status)

    SunLinkScreen(
        title = "Батарея",
        subtitle = "Стан акумулятора · ${telemetryLabel(socItem)}",
        refreshing = loading,
        onRefresh = {
            vm.loadFullTelemetry()
            reloadHistory(period)
        }
    ) {
        DataFreshnessIndicator(freshness, full?.timestamp)
        telemetryErrorNote(error, hasFreshTelemetry)?.let {
            Text(it, color = if (hasFreshTelemetry) colors.warning else colors.error, style = SunLinkTypography.caption)
        }

        // Hero: великий анімований SOC-ринг. Відсутнє значення — «—», а не нуль.
        PremiumCard(accent = colors.battery) {
            PremiumSectionHeader("Рівень заряду (SOC)", telemetryLabel(socItem))
            Box(Modifier.fillMaxWidth().height(196.dp), contentAlignment = Alignment.Center) {
                BatterySocIndicator(soc, Modifier.size(196.dp), colors.battery)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(telemetryLabel(socItem), confidenceColor(socItem, colors))
                Text(formatTelemetryTimestamp(full?.timestamp), style = SunLinkTypography.caption, color = colors.textTertiary)
            }
        }

        PremiumCard(accent = colors.battery) {
            PremiumSectionHeader("Електричні параметри", "Миттєві вимірювання")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                MetricCard(
                    "Напруга", telemetryNumber(full, "battery", "voltage"), "V",
                    Icons.Default.Bolt, colors.battery, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "battery", "voltage"))
                )
                MetricCard(
                    "Струм", telemetryNumber(full, "battery", "current"), "A",
                    null, colors.battery, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "battery", "current"))
                )
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                MetricCard(
                    "Потужність", telemetryNumber(full, "battery", "power"), "W",
                    Icons.Default.BatteryChargingFull, colors.batteryCharge, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "battery", "power")),
                    emphasize = true
                )
                MetricCard(
                    "Температура", telemetryNumber(full, "battery", "temperature"), "°C",
                    null, colors.warning, Modifier.weight(1f),
                    caption = telemetryLabel(telemetryValue(full, "battery", "temperature"))
                )
            }
        }

        PremiumCard(accent = colors.batteryCharge) {
            PremiumSectionHeader("Заряд і розряд", "Накопичена енергія (кВт·год)")
            EnergyRow("Заряд за день", full, "battery", "daily_charge_energy", colors.batteryCharge)
            EnergyRow("Розряд за день", full, "battery", "daily_discharge_energy", colors.batteryDischarge)
            EnergyRow("Заряд всього", full, "battery", "total_charge_energy", colors.batteryCharge)
            EnergyRow("Розряд всього", full, "battery", "total_discharge_energy", colors.batteryDischarge)
        }

        PremiumCard(accent = colors.primary) {
            PremiumSectionHeader("Історія", "SOC · напруга · струм")
            PeriodSelector(period) { selected ->
                period = selected
                reloadHistory(selected)
            }
            HistoryChart("SOC", series["battery_soc"], colors.battery)
            HistoryChart("Напруга", series["battery_voltage"], colors.battery)
            HistoryChart("Струм", series["battery_current"], colors.battery)
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

/** gap == true або value == null стає null —graph розриває лінію, а не з'єднує через нуль. */
private fun batteryChartValues(series: HistorySeriesDto?): List<Float?> =
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

@Composable
private fun HistoryChart(title: String, series: HistorySeriesDto?, color: Color) {
    val colors = LocalSunLinkColors.current
    val values = batteryChartValues(series)
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
