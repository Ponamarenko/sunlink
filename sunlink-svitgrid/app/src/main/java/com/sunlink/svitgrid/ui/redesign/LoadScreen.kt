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
import androidx.compose.ui.graphics.Color
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
        StatusPill(
            text = if (power != null && confirmed) "Активно" else "Недоступно",
            color = if (power != null && confirmed) colors.load else colors.offline
        )

        // Hero: миттєва потужність зі збереженим знаком.
        PremiumCard(accent = colors.load) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (power == null) "Немає даних" else telemetryLabel(powerItem),
                        style = SunLinkTypography.caption,
                        color = if (power == null) colors.offline else colors.load
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceXs))
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

        // Доступні метрики навантаження.
        Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            LoadMetricCard(
                label = "Спожито за добу",
                valueText = energyText(dailyEnergy),
                caption = if (dailyEnergy == null) "Недоступно" else "З 00:00 до $timePart",
                valueColor = colors.load,
                hasValue = dailyEnergy != null,
                modifier = Modifier.weight(1f)
            )
            LoadMetricCard(
                label = "Максимум за добу",
                valueText = loadPowerText(dailyMax),
                caption = if (dailyMax == null) "Недоступно" else "За останні 24 год",
                valueColor = colors.textPrimary,
                hasValue = dailyMax != null,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            LoadMetricCard(
                label = "Середнє за добу",
                valueText = loadPowerText(dailyAvg),
                caption = if (dailyAvg == null) "Недоступно" else "За останні 24 год",
                valueColor = colors.textPrimary,
                hasValue = dailyAvg != null,
                modifier = Modifier.weight(1f)
            )
            LoadMetricCard(
                label = "Резервний вихід",
                valueText = "—",
                caption = "Недоступно",
                valueColor = colors.textTertiary,
                hasValue = false,
                modifier = Modifier.weight(1f)
            )
        }

        // Історія навантаження — чесні розриви, реальні мін/пік.
        PremiumCard(accent = colors.load) {
            PremiumSectionHeader(
                title = "Історія навантаження",
                trailing = {
                    Text(
                        if (seriesMin != null && seriesMax != null) {
                            "Мін ${loadPowerText(seriesMin)} · Пік ${loadPowerText(seriesMax)}"
                        } else {
                            "—"
                        },
                        style = SunLinkTypography.caption,
                        color = colors.load
                    )
                }
            )
            if (present.isEmpty()) {
                EmptyState("Немає даних")
            } else {
                AnalyticsChart(
                    values,
                    colors.load,
                    Modifier.fillMaxWidth().height(96.dp),
                    showGrid = true
                )
                Spacer(Modifier.height(PremiumTokens.spaceXs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(hhmm(series[LoadPowerHistoryParam]?.start) ?: "—", style = SunLinkTypography.caption, color = colors.textTertiary)
                    Text(hhmm(series[LoadPowerHistoryParam]?.end) ?: "—", style = SunLinkTypography.caption, color = colors.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun LoadMetricCard(
    label: String,
    valueText: String,
    caption: String,
    valueColor: Color,
    hasValue: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    PremiumCard(modifier = modifier, accent = if (hasValue) valueColor else colors.border) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(PremiumTokens.spaceSm))
        Text(valueText, style = SunLinkTypography.metric, color = valueColor)
        Spacer(Modifier.height(PremiumTokens.spaceXs))
        Text(caption, style = SunLinkTypography.caption, color = colors.textTertiary)
    }
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

private fun energyText(value: Float?): String =
    if (value == null) "—" else "%.1f kWh".format(value)

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
