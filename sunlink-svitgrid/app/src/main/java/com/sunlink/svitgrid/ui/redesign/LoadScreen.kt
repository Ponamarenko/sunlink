package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val LoadPowerHistoryParam = "load_power"

/**
 * Stitch «Навантаження»: hero з поточним споживанням, чотири компактні метрики
 * та area-графік історії. Знак reg.175 (S_WORD) зберігається як є:
 * displayLoadPower не застосовує abs(). Відсутнє — «—», а не нуль.
 */
@Composable
fun LoadScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val stats by vm.stats.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(LoadPowerHistoryParam, "24h")
        val (start, end) = isoRange24h()
        vm.loadStats(LoadPowerHistoryParam, start, end)
    }

    val powerItem = telemetryValue(full, "load", "power")
    val power = displayLoadPower(telemetryNumber(full, "load", "power"))
    val confirmed = isConfirmed(powerItem, full?.stale == true)
    val dailyEnergy = telemetryNumber(full, "load", "daily_energy")
    val loadSeries = series[LoadPowerHistoryParam]
    val values = loadChartValues(loadSeries)

    val statsMatch = stats?.parameter == LoadPowerHistoryParam
    val dailyMax = if (statsMatch) stats?.max?.toFloat() else null
    val dailyAvg = if (statsMatch) stats?.avg?.toFloat() else null

    val present = values.filterNotNull()
    val seriesMin = present.minOrNull()
    val seriesMax = present.maxOrNull()
    val timePart = hhmm(full?.timestamp) ?: "—"
    val peakAt = peakTime(loadSeries)
    val averagedHours = seriesHours(loadSeries)
    val (heroValue, heroUnit) = loadPowerParts(power)

    SunLinkScreen(
        title = "Навантаження",
        subtitle = "Споживання будинку",
        refreshing = refreshing,
        onRefresh = {
            vm.loadFullTelemetry()
            vm.loadSeriesFor(LoadPowerHistoryParam, "24h")
            val (start, end) = isoRange24h()
            vm.loadStats(LoadPowerHistoryParam, start, end)
        }
    ) {
        LoadHeroCard(powerItem, power, confirmed, heroValue, heroUnit)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val (maxValue, maxUnit) = loadPowerParts(dailyMax)
            LoadMetricCard(
                label = "Спожито за добу",
                value = if (dailyEnergy == null) "—" else "%.1f".format(dailyEnergy),
                unit = if (dailyEnergy == null) "" else "kWh",
                caption = if (dailyEnergy == null) "Недоступно" else "З 00:00 до $timePart",
                valueColor = colors.load,
                modifier = Modifier.weight(1f)
            )
            LoadMetricCard(
                label = "Максимум за добу",
                value = maxValue,
                unit = maxUnit,
                caption = if (dailyMax == null) {
                    "Недоступно"
                } else if (peakAt != null) {
                    "о $peakAt"
                } else {
                    "За останні 24 год"
                },
                valueColor = colors.textPrimary,
                captionMono = peakAt != null && dailyMax != null,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val (avgValue, avgUnit) = loadPowerParts(dailyAvg)
            LoadMetricCard(
                label = "Середнє за добу",
                value = avgValue,
                unit = avgUnit,
                caption = if (dailyAvg == null) {
                    "Недоступно"
                } else if (averagedHours != null) {
                    "Оцінка за $averagedHours год"
                } else {
                    "Оцінка за добу"
                },
                valueColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            LoadMetricCard(
                label = "Резервний вихід",
                value = "—",
                unit = "",
                caption = "Недоступно",
                valueColor = colors.textTertiary,
                low = true,
                modifier = Modifier.weight(1f)
            )
        }

        LoadHistoryCard(loadSeries, values, seriesMin, seriesMax)
    }
}

/** Stitch: hero-картка навантаження з підтвердженим станом і збереженим знаком. */
@Composable
private fun LoadHeroCard(
    item: com.sunlink.svitgrid.data.dto.MeasurementValueDto?,
    power: Float?,
    confirmed: Boolean,
    value: String,
    unit: String
) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(20.dp)
    val live = power != null && confirmed
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.load.copy(alpha = 0.35f), shape)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = if (live) "Активно" else "Недоступно",
                        color = if (live) colors.load else colors.offline
                    )
                    Text(
                        if (power == null) "Недоступно" else if (confirmed) "Підтверджено" else telemetryLabel(item),
                        style = SunLinkTypography.caption,
                        color = if (live) colors.load else colors.textTertiary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        style = SunLinkTypography.display,
                        color = if (power == null) colors.textTertiary else colors.textPrimary
                    )
                    if (unit.isNotBlank()) {
                        Text(
                            unit,
                            style = SunLinkTypography.title,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 5.dp, start = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Поточне споживання будинку",
                    style = SunLinkTypography.caption,
                    color = colors.textSecondary
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.load.copy(alpha = 0.12f))
                    .border(1.dp, colors.load.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = colors.load,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/** Stitch: плитка метрики з великим значенням і чесним підписом. */
@Composable
private fun LoadMetricCard(
    label: String,
    value: String,
    unit: String,
    caption: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    captionMono: Boolean = false,
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
            .padding(14.dp)
    ) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = SunLinkTypography.headline,
                color = if (value == "—") colors.textTertiary else valueColor
            )
            if (unit.isNotBlank()) {
                Text(
                    unit,
                    style = SunLinkTypography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            caption,
            style = if (captionMono) SunLinkTypography.mono else SunLinkTypography.caption,
            color = colors.textTertiary
        )
    }
}

/** Stitch: area-графік історії з реальними мін/пік і підписами осі. */
@Composable
private fun LoadHistoryCard(
    series: HistorySeriesDto?,
    values: List<Float?>,
    min: Float?,
    max: Float?
) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
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
                "Історія навантаження",
                style = SunLinkTypography.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (min != null && max != null) {
                Text(
                    "Мін ${loadPowerText(min)} · Пік ${loadPowerText(max)}",
                    style = SunLinkTypography.mono,
                    color = colors.load
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (values.filterNotNull().isEmpty()) {
            EmptyState("Немає даних")
        } else {
            AreaChart(
                values = values,
                color = colors.load,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                showGrid = true,
                marker = true
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                chartTimeLabels(series).forEach { label ->
                    Text(label, style = SunLinkTypography.mono, color = colors.textSecondary)
                }
            }
        }
    }
}

/** Підписи осі беруться з реальних timestamp-ів ряду, а не з фіксованих заглушок. */
private fun chartTimeLabels(series: HistorySeriesDto?): List<String> {
    val stamps = series?.points?.map { it.timestamp }.orEmpty()
    if (stamps.isEmpty()) return listOf(hhmm(series?.start) ?: "—", hhmm(series?.end) ?: "—")
    val last = stamps.lastIndex
    return listOf(0, last / 3, last * 2 / 3, last).map { hhmm(stamps[it]) ?: "—" }.distinct()
}

/** gap == true або value == null стає null — графік розриває лінію, а не з'єднує через нуль. */
private fun loadChartValues(series: HistorySeriesDto?): List<Float?> =
    series?.points?.map { point ->
        if (point.gap || point.value == null) null else point.value!!.toFloat()
    } ?: emptyList()

/** Час реального піку рядy — для підпису «о HH:mm», без вигаданих значень. */
private fun peakTime(series: HistorySeriesDto?): String? =
    series?.points
        ?.filter { !it.gap && it.value != null }
        ?.maxByOrNull { it.value!! }
        ?.let { hhmm(it.timestamp) }

/** Тривалість ряду в годинах для чесного підпису «Оцінка за N год». */
private fun seriesHours(series: HistorySeriesDto?): Int? {
    val s = series ?: return null
    fun parse(iso: String?): Instant? = try {
        iso?.let { Instant.parse(it) }
    } catch (e: Exception) {
        null
    }
    val start = parse(s.start) ?: s.points.firstOrNull()?.let { parse(it.timestamp) }
    val end = parse(s.end) ?: s.points.lastOrNull()?.let { parse(it.timestamp) }
    if (start == null || end == null) return null
    val hours = Duration.between(start, end).toHours().toInt()
    return if (hours > 0) hours else null
}

/** Знак зберігається: від'ємна потужність залишається від'ємною. */
private fun loadPowerText(value: Float?): String = when {
    value == null -> "—"
    value <= -1000f || value >= 1000f -> "%.2f kW".format(value / 1000f)
    else -> "%.0f W".format(value)
}

/** Розділення числа й одиниці для дводоколірного рядка, як у Stitch. */
private fun loadPowerParts(value: Float?): Pair<String, String> {
    if (value == null) return "—" to ""
    val text = loadPowerText(value)
    val split = text.indexOf(' ')
    return if (split < 0) text to "" else text.substring(0, split) to text.substring(split + 1)
}

private fun isoRange24h(): Pair<String, String> {
    val end = Instant.now()
    return end.minus(24, ChronoUnit.HOURS).toString() to end.toString()
}

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
