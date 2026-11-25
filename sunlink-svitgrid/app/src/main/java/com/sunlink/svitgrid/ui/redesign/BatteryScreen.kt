package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val BatterySocHistoryParam = "battery_soc"

// Stitch dark tokens that have no semantic alias in SunLinkColors.
private val BatterySurfaceLow = Color(0xFF0B111E)    // dark-surface-container-low
private val BatteryContainerColor = Color(0xFF093524)
private val BatteryDarkOutline = Color(0xFF475569)   // dark-outline

private val BatteryTitleMd = SunLinkTypography.title.copy(fontFeatureSettings = "tnum") // 16/22 600
private val BatteryBodySm = SunLinkTypography.body.copy(fontSize = 12.sp, lineHeight = 16.sp)
private val BatteryLabelSm = SunLinkTypography.label.copy(fontSize = 11.sp, lineHeight = 14.sp)
private val BatteryMonoSm = SunLinkTypography.mono.copy(fontSize = 11.sp, lineHeight = 14.sp)
private val BatteryHeadlineLg = SunLinkTypography.headline.copy(
    fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold
)

/**
 * Батарея — прямий port Stitch-екрану: contexts-заголовок із back-стрілкою та
 * badge стану, велика SOC-картка з кільцем, 2×2 плитки параметрів, добові
 * заряд/розряд і компактний SVG-подібний area-графік історії SOC.
 */
@Composable
fun BatteryScreen(vm: MainViewModel = viewModel()) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        BatteryScreenContent(vm)
    }
}

@Composable
private fun BatteryScreenContent(vm: MainViewModel) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(BatterySocHistoryParam, "24h")
    }

    val socItem = telemetryValue(full, "battery", "soc")
    val voltageItem = telemetryValue(full, "battery", "voltage")
    val currentItem = telemetryValue(full, "battery", "current")
    val powerItem = telemetryValue(full, "battery", "power")
    val tempItem = telemetryValue(full, "battery", "temperature")

    val stale = full?.stale == true
    val socConfirmed = isConfirmed(socItem, stale)
    val voltageConfirmed = isConfirmed(voltageItem, stale)
    val currentConfirmed = isConfirmed(currentItem, stale)
    val powerConfirmed = isConfirmed(powerItem, stale)
    val tempConfirmed = isConfirmed(tempItem, stale)

    val soc = telemetryNumber(full, "battery", "soc")?.takeIf { socConfirmed }
    val voltage = telemetryNumber(full, "battery", "voltage")?.takeIf { voltageConfirmed }
    val current = telemetryNumber(full, "battery", "current")?.takeIf { currentConfirmed }
    val power = telemetryNumber(full, "battery", "power")?.takeIf { powerConfirmed }
    val temperature = telemetryNumber(full, "battery", "temperature")?.takeIf { tempConfirmed }

    val freshness = freshnessOf(stale, socItem?.quality, socItem?.confidence, full?.connection_status)
    val values = batteryChartValues(series[BatterySocHistoryParam])
    val present = values.filterNotNull()
    val minSoc = present.minOrNull()
    val maxSoc = present.maxOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BatteryHeader(full?.timestamp, freshness)
        SocCard(soc, socItem, socConfirmed, current)
        ParameterGrid(
            voltage, voltageConfirmed, if (voltageConfirmed) telemetryLabel(voltageItem) else "Недоступно",
            current, currentConfirmed, if (currentConfirmed) telemetryLabel(currentItem) else "Недоступно",
            power, powerConfirmed, powerItem,
            temperature, tempConfirmed
        )
        DailyEnergyCard(full)
        SocHistoryCard(values, minSoc, maxSoc, series[BatterySocHistoryParam])
    }
}

/** Contextual Header: back arrow, title + LiFePO4 subtitle, status badge. */
@Composable
private fun BatteryHeader(timestamp: String?, freshness: Freshness) {
    val colors = LocalSunLinkColors.current
    val (statusText, statusColor) = when (freshness) {
        Freshness.FRESH -> "Дані актуальні" to colors.battery
        Freshness.STALE -> "Дані застаріли" to colors.warning
        Freshness.PROVISIONAL -> "Попередні дані" to colors.warning
        Freshness.UNAVAILABLE -> "Немає даних" to colors.offline
        Freshness.OFFLINE -> "Немає з'єднання" to colors.error
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
            Column {
                Text("Батарея", style = SunLinkTypography.headline, color = colors.textPrimary)
                Text("Стан накопичувача · LiFePO4", style = BatteryBodySm, color = colors.textSecondary)
            }
        }
        Row(
            modifier = Modifier
                .background(BatteryContainerColor, RoundedCornerShape(50))
                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(6.dp).background(statusColor, CircleShape))
            Text(statusText, style = BatteryLabelSm, color = statusColor)
        }
    }
}

/** Large SOC visualization: 112px ring, centered percentage, state copy. */
@Composable
private fun SocCard(
    soc: Float?,
    socItem: com.sunlink.svitgrid.data.dto.MeasurementValueDto?,
    socConfirmed: Boolean,
    current: Float?
) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.battery.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // SVG viewBox 120 with r=50 and stroke-width=10, scaled into 112dp.
        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(112.dp)) {
                val stroke = 9.33.dp.toPx()
                val diameter = 102.67.dp.toPx()
                val inset = (size.width - diameter) / 2f
                drawArc(
                    BatteryContainerColor, -90f, 360f, false,
                    topLeft = Offset(inset, inset), size = Size(diameter, diameter),
                    style = Stroke(stroke)
                )
                if (soc != null) {
                    drawArc(
                        colors.battery, -90f, 360f * (soc.coerceIn(0f, 100f) / 100f), false,
                        topLeft = Offset(inset, inset), size = Size(diameter, diameter),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (soc == null) "—" else "${soc.toInt()}%",
                    style = SunLinkTypography.display,
                    color = if (soc == null) colors.textTertiary else colors.textPrimary
                )
                Text("SOC", style = BatteryLabelSm, color = colors.battery)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (socConfirmed) telemetryLabel(socItem) else "Немає даних",
                style = BatteryLabelSm,
                color = if (socConfirmed) colors.battery else colors.offline
            )
            Text(socStateLabel(soc), style = SunLinkTypography.headline, color = colors.textPrimary)
            Text(currentStateLabel(current), style = BatteryBodySm, color = colors.textSecondary)
        }
    }
}

/** 2×2 compact parameter articles; confirmed values keep the bright surface. */
@Composable
private fun ParameterGrid(
    voltage: Float?, voltageConfirmed: Boolean, voltageCaption: String,
    current: Float?, currentConfirmed: Boolean, currentCaption: String,
    power: Float?, powerConfirmed: Boolean, powerItem: com.sunlink.svitgrid.data.dto.MeasurementValueDto?,
    temperature: Float?, tempConfirmed: Boolean
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ParameterTile("Напруга", voltage, "V", voltageConfirmed, voltageCaption, colorsFor = { it.battery }, Modifier.weight(1f))
            ParameterTile("Струм", current, "A", currentConfirmed, currentCaption, colorsFor = { it.battery }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ParameterTile(
                "Потужність", power, "W", powerConfirmed,
                if (powerConfirmed) telemetryLabel(powerItem) else "Недоступно",
                colorsFor = { it.batteryCharge }, Modifier.weight(1f)
            )
            ParameterTile(
                "Температура", temperature, "°C", tempConfirmed,
                if (tempConfirmed) "Підтверджено" else "Датчик відсутній",
                colorsFor = { it.warning }, Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ParameterTile(
    label: String,
    value: Float?,
    unit: String,
    confirmed: Boolean,
    caption: String,
    colorsFor: (SunLinkColors) -> Color,
    modifier: Modifier
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (confirmed) colors.surface else BatterySurfaceLow)
            .border(
                1.dp,
                if (confirmed) colors.border else colors.border.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = BatteryLabelSm, color = colors.textSecondary)
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (confirmed) colorsFor(colors) else BatteryDarkOutline, CircleShape)
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (value == null) "—" else "%.1f".format(value),
                style = BatteryHeadlineLg,
                color = if (confirmed) colors.textPrimary else colors.textSecondary
            )
            if (value != null) {
                Text(unit, style = BatteryBodySm, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(caption, style = BatteryLabelSm, color = if (confirmed) colorsFor(colors) else colors.textSecondary)
    }
}

/** Daily charge / discharge with a divide-x separator. */
@Composable
private fun DailyEnergyCard(full: com.sunlink.svitgrid.data.dto.TelemetryFullDto?) {
    val colors = LocalSunLinkColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Енергія за добу", style = SunLinkTypography.title, color = colors.textPrimary)
            Text("Дані моделі інвертора", style = BatteryLabelSm, color = colors.textSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DailyColumn("Заряджено", telemetryNumber(full, "battery", "daily_charge_energy"), colors.battery, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(44.dp).background(colors.border))
            DailyColumn("Розряджено", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.textPrimary, Modifier.weight(1f).padding(start = 12.dp))
        }
    }
}

@Composable
private fun DailyColumn(label: String, value: Float?, color: Color, modifier: Modifier) {
    val colors = LocalSunLinkColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = BatteryLabelSm, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (value == null) "—" else "%.1f".format(value),
                style = BatteryTitleMd,
                color = if (value == null) colors.textTertiary else color
            )
            if (value != null) {
                Text("kWh", style = BatteryBodySm, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
        }
    }
}

/** Compact historical SOC chart: dashed grid, gradient area, endpoint, axis labels. */
@Composable
private fun SocHistoryCard(values: List<Float?>, minSoc: Float?, maxSoc: Float?, series: HistorySeriesDto?) {
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
            Text("Історія SOC (24 год)", style = SunLinkTypography.title, color = colors.textPrimary)
            Text(
                if (minSoc == null || maxSoc == null) "—" else "Мін ${minSoc.toInt()}% · Макс ${maxSoc.toInt()}%",
                style = BatteryMonoSm,
                color = colors.battery
            )
        }
        Spacer(Modifier.height(8.dp))
        if (values.all { it == null }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BatterySurfaceLow.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Немає даних", style = BatteryLabelSm, color = colors.textTertiary)
            }
        } else {
            AreaChart(
                values = values,
                color = colors.battery,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                showGrid = true,
                marker = true
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                hhmm(series?.start) ?: "00:00",
                "04:00",
                "08:00",
                hhmm(series?.end) ?: "09:24"
            ).forEach { label ->
                Text(label, style = BatteryMonoSm, color = colors.textSecondary)
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
private fun batteryChartValues(series: HistorySeriesDto?): List<Float?> = sanitizedSocSeries(series)

private fun socStateLabel(soc: Float?): String {
    if (soc == null) return "Немає даних"
    return when {
        soc >= 95f -> "Повний заряд"
        soc <= 20f -> "Низький заряд"
        else -> "Рівень заряду в нормі"
    }
}

private fun currentStateLabel(current: Float?): String {
    if (current == null) return "Струм: —"
    return if (kotlin.math.abs(current) < 5f) "Підтримуючий струм у нормі"
    else "Струм: %.1f A".format(current)
}
