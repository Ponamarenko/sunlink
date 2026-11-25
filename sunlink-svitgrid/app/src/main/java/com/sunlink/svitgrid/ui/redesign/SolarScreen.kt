package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SolarPowerHistoryParam = "pv_total_power"

// Stitch dark tokens that have no semantic alias in SunLinkColors.
private val SolarSurfaceLow = Color(0xFF0B111E)    // dark-surface-container-low
private val SolarContainerColor = Color(0xFF3A2D0C)
private val SolarDarkOutline = Color(0xFF475569)   // dark-outline

private val SolarTitleMd = SunLinkTypography.title.copy(fontFeatureSettings = "tnum") // 16/22 600
private val SolarBodySm = SunLinkTypography.body.copy(fontSize = 12.sp, lineHeight = 16.sp)
private val SolarLabelSm = SunLinkTypography.label.copy(fontSize = 11.sp, lineHeight = 14.sp)
private val SolarMonoSm = SunLinkTypography.mono.copy(fontSize = 11.sp, lineHeight = 14.sp)
private val SolarTitleMdTabular = SolarTitleMd

/**
 * Сонце — прямий port Stitch-екрану: compact-заголовок із потужністю праворуч,
 * три візуально різні PV-картки (підтверджена / очікування / недоступна) та
 * панель генерації з area-графіком. Відсутні значення не підміняються нулем.
 */
@Composable
fun SolarScreen(vm: MainViewModel = viewModel()) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        SolarScreenContent(vm)
    }
}

@Composable
private fun SolarScreenContent(vm: MainViewModel) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(SolarPowerHistoryParam, "24h")
    }

    val totalItem = telemetryValue(full, "pv", "total_power")
    val stale = full?.stale == true
    val totalConfirmed = isConfirmed(totalItem, stale)
    val totalPower = telemetryNumber(full, "pv", "total_power")?.takeIf { totalConfirmed }

    val values = solarChartValues(series[SolarPowerHistoryParam])
    val peak = values.filterNotNull().maxOrNull()
    val dailyEnergy = telemetryNumber(full, "pv", "daily_energy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SolarHeader(full?.timestamp, totalPower)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PvInputCard("Вхід PV1", "Підтверджено", full, stale, PVVariant.CONFIRMED)
            PvInputCard("Вхід PV2", "Очікування", full, stale, PVVariant.STANDBY)
            PvInputCard("Вхід PV3", "Недоступно", full, stale, PVVariant.UNAVAILABLE)
        }
        GenerationCard(totalPower, dailyEnergy, peak, series[SolarPowerHistoryParam], values)
    }
}

/** Compact Header: solar tile, title + badge, right-aligned current generation. */
@Composable
private fun SolarHeader(timestamp: String?, totalPower: Float?) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SolarContainerColor)
                    .border(1.dp, colors.solar.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SolarPower, contentDescription = null, tint = colors.solar, modifier = Modifier.size(22.dp))
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Сонце", style = SunLinkTypography.headline, color = colors.textPrimary)
                    Row(
                        modifier = Modifier
                            .background(SolarContainerColor, RoundedCornerShape(50))
                            .border(1.dp, colors.solar.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(6.dp).background(colors.solar, CircleShape))
                        Text("Генерація", style = SolarLabelSm, color = colors.solar)
                    }
                }
                Text(
                    "Входи PV1–PV3 · Оновлено ${formatTelemetryTimestamp(timestamp)}",
                    style = SolarBodySm,
                    color = colors.textSecondary
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (totalPower == null) "—" else totalPower.toInt().toString(),
                style = SunLinkTypography.headline,
                color = if (totalPower == null) colors.textTertiary else colors.solar
            )
            if (totalPower != null) {
                Text("W", style = SolarBodySm, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
        }
    }
}

/** Stitch renders PV1, PV2 and PV3 with three distinct surface treatments. */
private enum class PVVariant { CONFIRMED, STANDBY, UNAVAILABLE }

@Composable
private fun PvInputCard(title: String, fallbackStatus: String, full: TelemetryFullDto?, stale: Boolean, variant: PVVariant) {
    val colors = LocalSunLinkColors.current
    val prefix = when (variant) {
        PVVariant.CONFIRMED -> "pv1"
        PVVariant.STANDBY -> "pv2"
        PVVariant.UNAVAILABLE -> "pv3"
    }
    val powerItem = telemetryValue(full, "pv", "${prefix}_power")
    val voltageItem = telemetryValue(full, "pv", "${prefix}_voltage")
    val currentItem = telemetryValue(full, "pv", "${prefix}_current")

    val confirmed = isConfirmed(powerItem, stale)
    val hasData = powerItem != null || voltageItem != null || currentItem != null
    val status = when {
        confirmed && (powerItem?.normalized_value ?: 0.0) > 1.0 -> "Підтверджено"
        hasData -> "Очікування"
        else -> "Недоступно"
    }

    // Значення підміняються лише тоді, канал підтверджено; інакше чесна «—».
    val power = if (status == "Підтверджено") telemetryNumber(full, "pv", "${prefix}_power") else null
    val voltage = if (status == "Підтверджено") telemetryNumber(full, "pv", "${prefix}_voltage") else null
    val current = if (status == "Підтверджено") telemetryNumber(full, "pv", "${prefix}_current") else null

    val corner: Dp
    val container: Color
    val borderColor: Color
    val elevation: Dp
    val dotColor: Color
    val statusColor: Color
    val titleColor: Color
    val valueStyle: TextStyle
    val powerColor: Color
    val divider: Color
    when (variant) {
        PVVariant.CONFIRMED -> {
            corner = 28.dp; container = colors.surface; borderColor = colors.solar.copy(alpha = 0.4f)
            elevation = 8.dp; dotColor = colors.solar; statusColor = colors.battery
            titleColor = colors.textPrimary; valueStyle = SunLinkTypography.headline
            powerColor = colors.solar; divider = colors.border
        }
        PVVariant.STANDBY -> {
            corner = 12.dp; container = colors.surface; borderColor = colors.border
            elevation = 0.dp; dotColor = colors.textSecondary; statusColor = colors.textSecondary
            titleColor = colors.textPrimary; valueStyle = SolarTitleMd
            powerColor = colors.textSecondary; divider = colors.border
        }
        PVVariant.UNAVAILABLE -> {
            corner = 12.dp; container = SolarSurfaceLow; borderColor = colors.border.copy(alpha = 0.6f)
            elevation = 0.dp; dotColor = SolarDarkOutline; statusColor = colors.textSecondary
            titleColor = colors.textSecondary; valueStyle = SolarTitleMd
            powerColor = colors.textSecondary; divider = colors.border.copy(alpha = 0.6f)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(corner))
            .padding(if (variant == PVVariant.CONFIRMED) 16.dp else 14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                Text(title, style = SunLinkTypography.title, color = titleColor)
            }
            Text(status, style = SolarLabelSm, color = if (status == "Підтверджено") colors.battery else colors.textSecondary)
        }
        Spacer(Modifier.height(if (variant == PVVariant.CONFIRMED) 12.dp else 10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PvMetric("Потужність", power, "W", valueStyle, powerColor, divider, leadingPadding = 0.dp, rowModifier = Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(divider))
            PvMetric("Напруга", voltage, "V", valueStyle, colors.textPrimary, divider, leadingPadding = 12.dp, rowModifier = Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(divider))
            PvMetric("Струм", current, "A", valueStyle, colors.textPrimary, divider, leadingPadding = 12.dp, rowModifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PvMetric(
    label: String,
    value: Float?,
    unit: String,
    style: TextStyle,
    valueColor: Color,
    divider: Color,
    leadingPadding: Dp,
    rowModifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Column(rowModifier.padding(start = leadingPadding), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = SolarLabelSm, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (value == null) "—" else if (unit == "A") "%.1f".format(value) else value.toInt().toString(),
                style = style,
                color = if (value == null) colors.textSecondary else valueColor
            )
            if (value != null) {
                Text(unit, style = SolarBodySm, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
        }
    }
}

/** Production History: daily total + peak in the header, area chart below. */
@Composable
private fun GenerationCard(
    totalPower: Float?,
    dailyEnergy: Float?,
    peak: Float?,
    series: HistorySeriesDto?,
    values: List<Float?>
) {
    val colors = LocalSunLinkColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Генерація сьогодні", style = SunLinkTypography.title, color = colors.textPrimary)
                Text(
                    "За добу: ${dailyEnergy?.let { "%.1f kWh".format(it) } ?: "—"} · " +
                        "Пік ${peak?.let { "${it.toInt()} W" } ?: "—"}",
                    style = SolarLabelSm,
                    color = colors.textSecondary
                )
            }
            Text(
                if (totalPower == null) "—" else "${totalPower.toInt()} W",
                style = SolarMonoSm,
                color = if (totalPower == null) colors.textTertiary else colors.solar
            )
        }
        Spacer(Modifier.height(8.dp))
        if (values.all { it == null }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SolarSurfaceLow.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Немає даних", style = SolarLabelSm, color = colors.textTertiary)
            }
        } else {
            AreaChart(
                values = values,
                color = colors.solar,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                showGrid = true,
                marker = true
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                hhmm(series?.start) ?: "05:00",
                "07:00",
                "08:30",
                hhmm(series?.end) ?: "09:24"
            ).forEach { label ->
                Text(label, style = SolarMonoSm, color = colors.textSecondary)
            }
        }
    }
}

/** Часова підпис осі з реального ISO-timestamp; Stitch-значення — лише fallback. */
private fun hhmm(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        null
    }
}

/** gap == true або value == null перетворюється на null — лінія розривається. */
private fun solarChartValues(series: HistorySeriesDto?): List<Float?> =
    series?.points?.map { point ->
        if (point.gap || point.value == null) null else point.value.toFloat()
    } ?: emptyList()
