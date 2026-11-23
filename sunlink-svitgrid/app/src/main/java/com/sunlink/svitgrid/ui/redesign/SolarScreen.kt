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
    val dailyEnergy = telemetryNumber(full, "pv", "daily_energy")
    val totalEnergy = telemetryNumber(full, "pv", "total_energy")

    SunLinkScreen(
        title = "Сонячна енергія",
        subtitle = "Фотовольтаїка · ${telemetryLabel(totalItem)}",
        refreshing = loading,
        onRefresh = {
            vm.loadFullTelemetry()
            reloadHistory(period)
        }
    ) {
        if (error != null) {
            Text(error!!, color = colors.error, style = SunLinkTypography.caption)
        }

        // Hero: загальна потужність PV. Відсутнє значення — «—», а не нуль.
        SunLinkCard(accent = colors.solar) {
            Text("ЗАГАЛЬНА ПОТУЖНІСТЬ PV", style = SunLinkTypography.label, color = colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (totalPower == null) "—" else "%.1f".format(totalPower),
                    style = SunLinkTypography.display,
                    color = if (totalPower == null) colors.textTertiary else colors.textPrimary
                )
                Text(
                    if (totalPower == null) "" else "W",
                    style = SunLinkTypography.label,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }
            val heroPoints = series[SolarHeroParam]?.points.orEmpty()
            if (heroPoints.isEmpty()) {
                EmptyHint("Немає даних")
            } else {
                Sparkline(
                    heroPoints.map { point -> if (point.gap || point.value == null) null else point.value!!.toFloat() },
                    colors.solar,
                    Modifier.fillMaxWidth().height(72.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill(telemetryLabel(totalItem), confidenceColor(totalItem, colors))
                Text(
                    "Сьогодні: ${if (dailyEnergy == null) "—" else "%.1f kWh".format(dailyEnergy)}",
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary
                )
            }
        }

        Text("ГЕНЕРАЦІЯ", style = SunLinkTypography.label, color = colors.textSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "За день", dailyEnergy, "kWh",
                Icons.Default.SolarPower, colors.solar, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "pv", "daily_energy"))
            )
            MetricCard(
                "Всього", totalEnergy, "kWh",
                null, colors.solar, Modifier.weight(1f),
                caption = telemetryLabel(telemetryValue(full, "pv", "total_energy"))
            )
        }

        Text("КАНАЛИ PV", style = SunLinkTypography.label, color = colors.textSecondary)
        PvChannelCard("PV1", "pv1", full)
        PvChannelCard("PV2", "pv2", full)
        PvChannelCard("PV3", "pv3", full)

        SunLinkCard(accent = colors.primary) {
            Text("ІСТОРІЯ КАНАЛІВ", style = SunLinkTypography.label, color = colors.textSecondary)
            PeriodSelector(period) { selected ->
                period = selected
                reloadHistory(selected)
            }
            HistoryChart("PV1", series["pv1_power"], "W", colors.solar)
            HistoryChart("PV2", series["pv2_power"], "W", colors.solar)
            HistoryChart("PV3", series["pv3_power"], "W", colors.solar)
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
private fun PvChannelCard(label: String, prefix: String, full: TelemetryFullDto?) {
    val colors = LocalSunLinkColors.current
    val voltage = telemetryNumber(full, "pv", "${prefix}_voltage")
    val current = telemetryNumber(full, "pv", "${prefix}_current")
    val power = telemetryNumber(full, "pv", "${prefix}_power")
    val powerItem = telemetryValue(full, "pv", "${prefix}_power")
    val active = power != null && power > 1f

    SunLinkCard(accent = if (active) colors.solar else colors.offline) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SolarPower,
                    contentDescription = null,
                    tint = if (active) colors.solar else colors.offline,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(label, style = SunLinkTypography.title, color = colors.textPrimary)
            }
            StatusPill(
                if (active) "Активний" else "Немає даних",
                if (active) colors.success else colors.offline
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Напруга", voltage, "V",
                null, colors.solar, Modifier.weight(1f)
            )
            MetricCard(
                "Струм", current, "A",
                null, colors.solar, Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
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
