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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PvPower = "pv_total_power"
private const val LoadPower = "load_power"
private const val GridImport = "daily_grid_import"
private const val GridExport = "daily_grid_export"
private const val BatteryCharge = "daily_battery_charge"
private const val BatteryDischarge = "daily_battery_discharge"

/** Мітка чіпа, backend-період і заголовок порівняльного графіка. */
private val analyticsPeriods = listOf(
    Triple("24h", "День", "Порівняння за день"),
    Triple("7d", "Тиждень", "Порівняння за тиждень"),
    Triple("30d", "Місяць", "Порівняння за місяць"),
    Triple("365d", "Рік", "Порівняння за рік")
)

private val analyticsParams = listOf(
    PvPower, LoadPower, GridImport, GridExport, BatteryCharge, BatteryDischarge
)

/**
 * Аналітика — генерація/споживання лише з реальних рядів. Усі стани темні
 * (кореневий sunLinkDarkColors); світлої теми для цього екрана не існує.
 */
@Composable
fun AnalyticsScreen(vm: MainViewModel = viewModel()) {
    val series by vm.seriesByParam.collectAsState()
    val full by vm.fullTelemetry.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    var period by rememberSaveable { mutableStateOf("24h") }

    LaunchedEffect(Unit) { vm.loadFullTelemetry() }
    LaunchedEffect(period) {
        analyticsParams.forEach { vm.loadSeriesFor(it, period) }
    }

    val pvSeries = series[PvPower]
    val loadSeries = series[LoadPower]

    // Енергія = інтегрування реальної потужності за відомим інтервалом.
    // Для доби, якщо ряд відсутній, використовуємо добову енергію з телеметрії.
    val pvTotal = energyKwh(pvSeries)
        ?: if (period == "24h") telemetryNumber(full, "pv", "daily_energy") else null
    val loadTotal = energyKwh(loadSeries)
        ?: if (period == "24h") telemetryNumber(full, "load", "daily_energy") else null
    val gridTotal = run {
        val imp = sumPresent(series[GridImport])
        val exp = sumPresent(series[GridExport])
        when {
            imp != null && exp != null -> imp - exp
            imp != null -> imp
            exp != null -> -exp
            else -> null
        }
    }

    val charge = if (period == "24h") {
        telemetryNumber(full, "battery", "daily_charge_energy") ?: sumPresent(series[BatteryCharge])
    } else {
        sumPresent(series[BatteryCharge])
    }
    val discharge = if (period == "24h") {
        telemetryNumber(full, "battery", "daily_discharge_energy") ?: sumPresent(series[BatteryDischarge])
    } else {
        sumPresent(series[BatteryDischarge])
    }

    val pvValues = seriesValues(pvSeries)
    val loadValues = seriesValues(loadSeries)
    val chartTitle = analyticsPeriods.firstOrNull { it.first == period }?.third ?: "Порівняння за день"

    SunLinkScreen(
        title = "Аналітика",
        subtitle = "Статистика генерації та споживання",
        refreshing = refreshing,
        onRefresh = { analyticsParams.forEach { vm.loadSeriesFor(it, period) } }
    ) {
        Text(
            dateLabel(full?.timestamp),
            style = SunLinkTypography.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Період: День / Тиждень / Місяць / Рік.
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            analyticsPeriods.forEach { (value, label, _) ->
                val active = value == period
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) colors.primary else Color.Transparent)
                        .clickable { period = value }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = SunLinkTypography.caption,
                        color = if (active) colors.onPrimary else colors.textSecondary
                    )
                }
            }
        }

        // Підсумки періоду.
        Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            SummaryTile("Генерація", pvTotal, colors.solar, Modifier.weight(1f))
            SummaryTile("Споживання", loadTotal, colors.load, Modifier.weight(1f))
            SummaryTile("Мережа", gridTotal, colors.grid, Modifier.weight(1f))
        }

        // Порівняння PV vs Будинок — два незалежні gap-aware шари з сіткою.
        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader(
                title = chartTitle,
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                        LegendDot("PV", colors.solar)
                        LegendDot("Будинок", colors.load)
                    }
                }
            )
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            if (pvValues.all { it == null } && loadValues.all { it == null }) {
                EmptyState("Немає даних")
            } else {
                Box(Modifier.fillMaxWidth().height(132.dp)) {
                    AnalyticsChart(pvValues, colors.solar, Modifier.matchParentSize(), showGrid = true)
                    AnalyticsChart(loadValues, colors.load, Modifier.matchParentSize(), showGrid = false)
                }
                Spacer(Modifier.height(PremiumTokens.spaceXs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    axisLabels(pvSeries, period).forEach { label ->
                        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
                    }
                }
            }
        }

        // Цикли батареї.
        PremiumCard(accent = colors.battery) {
            PremiumSectionHeader(title = "Цикли батареї", subtitle = if (period == "24h") "Сьогодні" else "Обраний період")
            Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceMd)) {
                CycleTile("Заряджено", charge, colors.batteryCharge, Modifier.weight(1f))
                CycleTile("Розряджено", discharge, colors.batteryDischarge, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: Float?, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    PremiumCard(modifier = modifier, accent = if (value == null) colors.border else color) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(PremiumTokens.spaceXs))
        Text(
            energyText(value),
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else color
        )
    }
}

@Composable
private fun CycleTile(label: String, value: Float?, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(modifier) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(PremiumTokens.spaceXs))
        Text(
            energyText(value),
            style = SunLinkTypography.title,
            color = if (value == null) colors.textTertiary else color
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    val colors = LocalSunLinkColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.size(PremiumTokens.spaceXs))
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
    }
}

/** gap / null → null: AnalyticsChart малює розрив, а не нульову точку. */
private fun seriesValues(series: HistorySeriesDto?): List<Float?> =
    series?.points?.map { point -> if (point.gap) null else point.value?.toFloat() } ?: emptyList()

/** Сума реальних точок енергетичного ряду (кВт·год). */
private fun sumPresent(series: HistorySeriesDto?): Float? {
    val present = series?.points?.filter { !it.gap && it.value != null } ?: return null
    if (present.isEmpty()) return null
    return present.sumOf { it.value!! }.toFloat()
}

/** Перетворюємо ряд потужності (W) у кВт·год за відомим інтервалом. */
private fun energyKwh(series: HistorySeriesDto?): Float? {
    val s = series ?: return null
    if (s.interval_seconds <= 0) return null
    val present = s.points.filter { !it.gap && it.value != null }
    if (present.isEmpty()) return null
    val wattSeconds = present.sumOf { it.value!! } * s.interval_seconds
    return (wattSeconds / 3_600_000.0).toFloat()
}

private fun energyText(value: Float?): String =
    if (value == null) "—" else "%.1f kWh".format(value)

/** Дата з реальної телеметрії; якщо її немає — поточний день. Не мок. */
private fun dateLabel(iso: String?): String {
    val date = iso?.let {
        try {
            Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (e: Exception) {
            null
        }
    } ?: LocalDate.now(ZoneId.systemDefault())
    return try {
        DateTimeFormatter.ofPattern("d MMM", Locale("uk")).format(date)
    } catch (e: Exception) {
        "—"
    }
}

private fun axisLabels(series: HistorySeriesDto?, period: String, count: Int = 5): List<String> {
    val startIso = series?.start ?: return List(count) { "—" }
    val endIso = series.end ?: return List(count) { "—" }
    return try {
        val start = Instant.parse(startIso).toEpochMilli()
        val end = Instant.parse(endIso).toEpochMilli()
        if (end <= start) return List(count) { "—" }
        val pattern = if (period == "24h") "HH:mm" else "d MMM"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale("uk")).withZone(ZoneId.systemDefault())
        (0 until count).map { i ->
            formatter.format(Instant.ofEpochMilli(start + (end - start) * i / (count - 1)))
        }
    } catch (e: Exception) {
        List(count) { "—" }
    }
}
