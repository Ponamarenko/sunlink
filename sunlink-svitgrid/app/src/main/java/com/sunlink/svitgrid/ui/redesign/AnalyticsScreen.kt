package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
 * Stitch «Аналітика»: перемикач періодів, три підсумки, парний стовпчиковий
 * графік PV/Будинок і цикли батареї. Усі дані беруться з реальних рядів
 * seriesByParam; світлої теми для цього екрана не існує.
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

    // PV та навантаження вирівнюються за часовим бакетом, а не за індексом масиву.
    val bucketSize = when (period) {
        "24h" -> BarBucketSize.HOUR
        "7d" -> BarBucketSize.DAY
        "30d", "365d" -> BarBucketSize.MONTH
        else -> BarBucketSize.HOUR
    }
    val buckets = withMissingSamplePlaceholder(alignBarBuckets(pvSeries, loadSeries, bucketSize), period)
    val chartTitle = analyticsPeriods.firstOrNull { it.first == period }?.third ?: "Порівняння за день"
    val cyclePeriod = when (period) {
        "24h" -> "Сьогодні"
        "7d" -> "Останні 7 днів"
        "30d" -> "Обраний місяць"
        "365d" -> "Обраний рік"
        else -> "Обраний період"
    }

    // Свіжість прив'язана до реального кінця рядів, а не до локального часу.
    val freshestTimestamp = listOf(pvSeries?.end, loadSeries?.end).filterNotNull().maxOrNull() ?: full?.timestamp
    val freshness = when {
        full?.connection_status == false -> Freshness.OFFLINE
        full?.stale == true -> Freshness.STALE
        buckets.isEmpty() -> Freshness.UNAVAILABLE
        else -> Freshness.FRESH
    }

    SunLinkScreen(
        title = "Аналітика",
        subtitle = "Статистика генерації та споживання",
        refreshing = refreshing,
        onRefresh = { analyticsParams.forEach { vm.loadSeriesFor(it, period) } }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                dateLabel(full?.timestamp),
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            DataFreshnessIndicator(freshness, freshestTimestamp, compact = true)
        }

        AnalyticsPeriodTabs(period) { period = it }

        AnalyticsSummaryRow(pvTotal, loadTotal, gridTotal)

        AnalyticsComparisonCard(chartTitle, buckets)

        AnalyticsBatteryCard(cyclePeriod, charge, discharge)
    }
}

/** Stitch: чотири вкладки-таби з активним primary-container. */
@Composable
private fun AnalyticsPeriodTabs(period: String, onSelect: (String) -> Unit) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        analyticsPeriods.forEach { (value, label, _) ->
            val active = value == period
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) colors.primary else Color.Transparent)
                    .clickable { onSelect(value) }
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
}

/** Stitch: три підсумкові плитки обраного періоду. */
@Composable
private fun AnalyticsSummaryRow(pv: Float?, load: Float?, grid: Float?) {
    val colors = LocalSunLinkColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (pvValue, unit) = energyParts(pv)
        AnalyticsSummaryCell("Генерація", pvValue, unit, colors.solar, Modifier.weight(1f))
        val (loadValue, _) = energyParts(load)
        AnalyticsSummaryCell("Споживання", loadValue, unit, colors.load, Modifier.weight(1f))
        val (gridValue, _) = energyParts(grid)
        AnalyticsSummaryCell(
            "Мережа", gridValue, if (grid == null) "" else unit,
            colors.textPrimary, Modifier.weight(1f), low = true
        )
    }
}

@Composable
private fun AnalyticsSummaryCell(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
    low: Boolean = false
) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (low) colors.surface.copy(alpha = 0.55f) else colors.surface)
            .border(
                1.dp,
                if (low) colors.border.copy(alpha = 0.6f) else colors.border,
                shape
            )
            .padding(12.dp)
    ) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = SunLinkTypography.title,
                color = if (value == "—") colors.textTertiary else color
            )
            if (unit.isNotBlank()) {
                Text(
                    unit,
                    style = SunLinkTypography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                )
            }
        }
    }
}

/** Stitch: парні стовпчики PV/Будинок із легендою та пунктирними заглушками пропусків. */
@Composable
private fun AnalyticsComparisonCard(title: String, buckets: List<BarBucket>) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = SunLinkTypography.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot("PV", colors.solar)
                LegendDot("Будинок", colors.load)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (buckets.isEmpty()) {
            EmptyState("Немає даних")
        } else {
            // AnalyticsBarChart сам малює сітку, підписи осі та заглушки пропусків.
            AnalyticsBarChart(buckets, Modifier.fillMaxWidth().height(128.dp), showGrid = true)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    val colors = LocalSunLinkColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.size(4.dp))
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
    }
}

/** Stitch: «Цикли батареї» — заряджено / розряджено з розділювачем. */
@Composable
private fun AnalyticsBatteryCard(period: String, charge: Float?, discharge: Float?) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
    val (chargeValue, unit) = energyParts(charge)
    val (dischargeValue, _) = energyParts(discharge)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Цикли батареї",
                style = SunLinkTypography.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(period, style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            AnalyticsCycleCell(
                "Заряджено", chargeValue, if (charge == null) "" else unit,
                colors.batteryCharge, Modifier.weight(1f).padding(end = 12.dp)
            )
            Box(Modifier.width(1.dp).height(42.dp).background(colors.border))
            AnalyticsCycleCell(
                "Розряджено", dischargeValue, if (discharge == null) "" else unit,
                colors.textPrimary, Modifier.weight(1f).padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun AnalyticsCycleCell(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Column(modifier) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = SunLinkTypography.title,
                color = if (value == "—") colors.textTertiary else color
            )
            if (unit.isNotBlank()) {
                Text(
                    unit,
                    style = SunLinkTypography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                )
            }
        }
    }
}

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

private fun energyParts(value: Float?): Pair<String, String> =
    if (value == null) "—" to "" else "%.1f".format(value) to "kWh"

/**
 * Stitch: останній hourly-бакет без вимірювання показується пунктирною
 * заглушкою замість вигаданого стовпчика. Числові значення не додаються.
 */
private fun withMissingSamplePlaceholder(buckets: List<BarBucket>, period: String): List<BarBucket> {
    if (period != "24h" || buckets.isEmpty() || buckets.size >= 5) return buckets
    val last = buckets.last().label
    val parts = last.split(":")
    val hour = parts.firstOrNull()?.toIntOrNull() ?: return buckets
    return buckets + BarBucket("%02d:00".format((hour + 1) % 24), null, null)
}

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
