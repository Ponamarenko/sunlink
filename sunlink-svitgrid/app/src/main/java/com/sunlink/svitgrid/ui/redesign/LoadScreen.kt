package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val LoadPowerHistoryParam = "load_power"

/**
 * Навантаження — поточне споживання будинку. Знак reg.175 (S_WORD) зберігається
 * як є: displayLoadPower не застосовує abs(). Відсутнє — «—», а не нуль.
 * Одна hero-поверхня, компактні плитки добової статистики й одна панель історії.
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
    val freshness = freshnessOf(
        full?.stale, powerItem?.quality, powerItem?.confidence, full?.connection_status
    )
    val dailyEnergy = telemetryNumber(full, "load", "daily_energy")
    val values = loadChartValues(series[LoadPowerHistoryParam])

    // Статистика належить саме load_power — інакше показуємо «—».
    val statsMatch = stats?.parameter == LoadPowerHistoryParam
    val dailyMax = if (statsMatch) stats?.max?.toFloat() else null
    val dailyAvg = if (statsMatch) stats?.avg?.toFloat() else null

    val present = values.filterNotNull()
    val seriesMin = present.minOrNull()
    val seriesMax = present.maxOrNull()
    val timePart = hhmm(full?.timestamp) ?: "—"

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
        // Hero: миттєва потужність зі збереженим знаком і чесним станом.
        HeroPanel(accent = colors.load) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    StatusPill(
                        text = if (power != null && confirmed) telemetryLabel(powerItem) else "Недоступно",
                        color = if (power != null && confirmed) colors.load else colors.offline
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceSm))
                    Text(
                        loadPowerText(power),
                        style = SunLinkTypography.display,
                        color = if (power == null) colors.textTertiary else colors.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Поточне споживання будинку",
                        style = SunLinkTypography.caption,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceSm))
                    DataFreshnessIndicator(freshness, full?.timestamp, compact = true)
                }
                Box(
                    Modifier
                        .size(52.dp)
                        .background(colors.surfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = colors.load,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Компактні добові статистики: лише реальні значення, відсутнє — «—».
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            StatTile(
                label = "Спожито за добу",
                value = dailyEnergy,
                unit = "kWh",
                accent = colors.load,
                modifier = Modifier.weight(1f),
                caption = if (dailyEnergy == null) "Недоступно" else "З 00:00 до $timePart"
            )
            StatTile(
                label = "Максимум за добу",
                value = dailyMax,
                unit = "W",
                accent = colors.load,
                modifier = Modifier.weight(1f),
                caption = if (dailyMax == null) "Недоступно" else "За останні 24 год"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            StatTile(
                label = "Середнє за добу",
                value = dailyAvg,
                unit = "W",
                accent = colors.load,
                modifier = Modifier.weight(1f),
                caption = if (dailyAvg == null) "Недоступно" else "За останні 24 год"
            )
            StatTile(
                label = "Резервний вихід",
                value = null,
                unit = "W",
                accent = colors.offline,
                modifier = Modifier.weight(1f),
                caption = "Недоступно"
            )
        }

        // Історія навантаження — чесні розриви, реальні мін/пік, одна панель.
        DataPanel(
            title = "Історія навантаження",
            subtitle = if (seriesMin != null && seriesMax != null) {
                "Мін ${loadPowerText(seriesMin)} · Пік ${loadPowerText(seriesMax)}"
            } else {
                null
            }
        ) {
            if (present.isEmpty()) {
                EmptyState("Немає даних")
            } else {
                AreaChart(
                    values = values,
                    color = colors.load,
                    modifier = Modifier.fillMaxWidth().height(124.dp),
                    showGrid = true,
                    marker = true
                )
                Spacer(Modifier.height(PremiumTokens.spaceXs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    chartTimeLabels(series[LoadPowerHistoryParam]).forEach { label ->
                        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
                    }
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

/** Знак зберігається: від'ємна потужність залишається від'ємною. */
private fun loadPowerText(value: Float?): String = when {
    value == null -> "—"
    value <= -1000f || value >= 1000f -> "%.2f kW".format(value / 1000f)
    else -> "%.0f W".format(value)
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
