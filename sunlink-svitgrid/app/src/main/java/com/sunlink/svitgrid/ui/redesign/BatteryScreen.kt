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

    SunLinkScreen(
        title = "Батарея",
        subtitle = "Стан акумулятора · ${telemetryLabel(socItem)}",
        refreshing = loading,
        onRefresh = {
            vm.loadFullTelemetry()
            reloadHistory(period)
        }
    ) {
        if (error != null) {
            Text(error!!, color = colors.error, style = SunLinkTypography.caption)
        }

        // Hero: великий індикатор SOC. Відсутнє значення — «—», а не нуль.
        SunLinkCard(accent = colors.battery) {
            Text("РІВЕНЬ ЗАРЯДУ (SOC)", style = SunLinkTypography.label, color = colors.textSecondary)
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                ArcGauge(soc, Modifier.size(180.dp), colors.battery)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill(telemetryLabel(socItem), confidenceColor(socItem, colors))
                Text(full?.timestamp ?: "—", style = SunLinkTypography.caption, color = colors.textTertiary)
            }
        }

        Text("ЕЛЕКТРИЧНІ ПАРАМЕТРИ", style = SunLinkTypography.label, color = colors.textSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Потужність", telemetryNumber(full, "battery", "power"), "W",
                Icons.Default.BatteryChargingFull, colors.batteryCharge, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "battery", "power"))
            )
            MetricCard(
                "Температура", telemetryNumber(full, "battery", "temperature"), "°C",
                null, colors.warning, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "battery", "temperature"))
            )
        }

        Text("ЕНЕРГІЯ", style = SunLinkTypography.label, color = colors.textSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Заряд за день", telemetryNumber(full, "battery", "daily_charge_energy"), "kWh",
                null, colors.batteryCharge, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "battery", "daily_charge_energy"))
            )
            MetricCard(
                "Розряд за день", telemetryNumber(full, "battery", "daily_discharge_energy"), "kWh",
                null, colors.batteryDischarge, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "battery", "daily_discharge_energy"))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Заряд всього", telemetryNumber(full, "battery", "total_charge_energy"), "kWh",
                null, colors.batteryCharge, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "battery", "total_charge_energy"))
            )
            MetricCard(
                "Розряд всього", telemetryNumber(full, "battery", "total_discharge_energy"), "kWh",
                null, colors.batteryDischarge, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "battery", "total_discharge_energy"))
            )
        }

        SunLinkCard(accent = colors.primary) {
            Text("ІСТОРІЯ", style = SunLinkTypography.label, color = colors.textSecondary)
            PeriodSelector(period) { selected ->
                period = selected
                reloadHistory(selected)
            }
            HistoryChart("SOC", series["battery_soc"], "%", colors.battery)
            HistoryChart("Напруга", series["battery_voltage"], "V", colors.battery)
            HistoryChart("Струм", series["battery_current"], "A", colors.battery)
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
private fun HistoryChart(title: String, series: HistorySeriesDto?, unit: String, color: Color) {
    val colors = LocalSunLinkColors.current
    val points = series?.points.orEmpty()
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = SunLinkTypography.label, color = colors.textSecondary)
            Text(series?.unit ?: unit, style = SunLinkTypography.caption, color = colors.textTertiary)
        }
        if (points.isEmpty()) {
            EmptyHint("Немає даних")
        } else {
            // gap == true або value == null перетворюємо в null — Sparkline розриває лінію,
            // а не з'єднує точки через нуль.
            Sparkline(
                points.map { point -> if (point.gap || point.value == null) null else point.value!!.toFloat() },
                color,
                Modifier.fillMaxWidth().height(64.dp)
            )
        }
    }
}
