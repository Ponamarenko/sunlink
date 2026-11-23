package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.StatsSummaryDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.theme.PremiumAnalyticsTheme
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit

/**Chip-мітки відповідають perіодам backend: 24h / 7d / 30d / 365d. */
private val analyticsPeriods = listOf(
    "24h" to "Daily",
    "7d" to "Weekly",
    "30d" to "Monthly",
    "365d" to "Yearly"
)

private val chartCards = listOf(
    "pv_total_power" to "PV · потужність",
    "load_power" to "Навантаження · потужність",
    "battery_soc" to "Батарея · SOC",
    "daily_grid_import" to "Мережа · імпорт",
    "daily_grid_export" to "Мережа · експорт",
    "daily_battery_charge" to "Батарея · заряд",
    "daily_battery_discharge" to "Батарея · розряд"
)

private val statsParams = listOf(
    "pv_total_power" to "PV",
    "load_power" to "Навантаження",
    "battery_soc" to "SOC"
)

@Composable
fun AnalyticsScreen(vm: MainViewModel = viewModel()) {
    // Увесь екран — у локальній світлій темі; решта застосунку залишається темною.
    PremiumAnalyticsTheme {
        val series by vm.seriesByParam.collectAsState()
        val stats by vm.stats.collectAsState()
        val refreshing by vm.refreshing.collectAsState()
        val colors = LocalSunLinkColors.current

        var period by remember { mutableStateOf("24h") }
        var statsParam by remember { mutableStateOf("pv_total_power") }

        LaunchedEffect(Unit) {
            vm.loadFullTelemetry()
        }

        LaunchedEffect(period) {
            chartCards.forEach { (param, _) -> vm.loadSeriesFor(param, period) }
            val (start, end) = isoRange(period)
            vm.loadStats(statsParam, start, end)
        }

        LaunchedEffect(statsParam) {
            val (start, end) = isoRange(period)
            vm.loadStats(statsParam, start, end)
        }

        SunLinkScreen(
            title = "Аналітика",
            subtitle = "Історія та статистика · ${periodLabel(period)}",
            refreshing = refreshing,
            onRefresh = {
                chartCards.forEach { (param, _) -> vm.loadSeriesFor(param, period) }
                val (start, end) = isoRange(period)
                vm.loadStats(statsParam, start, end)
            }
        ) {
            PremiumCard(accent = colors.primary) {
                PremiumSectionHeader(title = "ПЕРІОД")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    analyticsPeriods.forEach { (value, label) ->
                        FilterChip(
                            selected = period == value,
                            onClick = { period = value },
                            label = { Text(label) }
                        )
                    }
                }
            }

            chartCards.forEach { (param, title) ->
                SeriesChartCard(title, series[param], chartColor(param))
            }

            StatsSummaryCard(stats, statsParam, period) { selected -> statsParam = selected }
        }
    }
}

@Composable
private fun chartColor(param: String): Color {
    val colors = LocalSunLinkColors.current
    return when {
        param.startsWith("pv_") -> colors.solar
        param == "load_power" -> colors.load
        param == "battery_soc" || param.startsWith("daily_battery") -> colors.battery
        param.startsWith("daily_grid_import") -> colors.gridImport
        param.startsWith("daily_grid_export") -> colors.gridExport
        else -> colors.primary
    }
}

private fun periodLabel(period: String): String = when (period) {
    "7d" -> "Тиждень"
    "30d" -> "Місяць"
    "365d" -> "Рік"
    else -> "Доба"
}

/** ISO-8601 діапазон для запиту статистики за обраний період. */
private fun isoRange(period: String): Pair<String, String> {
    val end = Instant.now()
    val start = when (period) {
        "7d" -> end.minus(7, ChronoUnit.DAYS)
        "30d" -> end.minus(30, ChronoUnit.DAYS)
        "365d" -> end.minus(365, ChronoUnit.DAYS)
        else -> end.minus(24, ChronoUnit.HOURS)
    }
    return start.toString() to end.toString()
}

@Composable
private fun SeriesChartCard(
    title: String,
    data: HistorySeriesDto?,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    PremiumCard(accent = color, modifier = modifier) {
        PremiumSectionHeader(
            title = title,
            trailing = { Text(data?.unit ?: "", style = SunLinkTypography.caption, color = colors.textTertiary) }
        )
        // gap / null перетворені на null — AnalyticsChart малює розрив, а не нуль.
        val values = data?.points?.map { point -> if (point.gap) null else point.value?.toFloat() } ?: emptyList()
        if (values.isEmpty() || values.all { it == null }) {
            EmptyState("Немає даних")
        } else {
            AnalyticsChart(values, color, Modifier.fillMaxWidth().height(110.dp), showGrid = true)
            Text(
                text = "Доступно: ${data?.available_points ?: 0} · Пропуски: ${data?.missing_points ?: 0}",
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatsSummaryCard(
    stats: StatsSummaryDto?,
    expectedParam: String,
    period: String,
    onParamChange: (String) -> Unit
) {
    val colors = LocalSunLinkColors.current
    PremiumCard(accent = colors.primary) {
        PremiumSectionHeader(title = "СТАТИСТИКА")
        Text("Параметр", style = SunLinkTypography.caption, color = colors.textTertiary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            statsParams.forEach { (value, label) ->
                FilterChip(
                    selected = expectedParam == value,
                    onClick = { onParamChange(value) },
                    label = { Text(label) }
                )
            }
        }

        val matching = stats?.parameter == expectedParam && hasAnyValue(stats)
        if (!matching) {
            EmptyState("Немає даних")
        } else {
            Text(
                text = "Період: ${periodLabel(period)} · Вимірювань: ${stats!!.count}",
                style = SunLinkTypography.caption,
                color = colors.textTertiary
            )
            StatRow("Мінімум", stats.min, colors.primary)
            StatRow("Максимум", stats.max, colors.primary)
            StatRow("Середнє", stats.avg, colors.primary)
            StatRow("Сума", stats.sum, colors.primary)
        }
    }
}

private fun hasAnyValue(stats: StatsSummaryDto?): Boolean =
    stats?.min != null || stats?.max != null || stats?.avg != null || stats?.sum != null

@Composable
private fun StatRow(label: String, value: Double?, color: Color) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = SunLinkTypography.body, color = colors.textSecondary)
        Text(
            text = value?.toFloat()?.let { "%.2f".format(it) } ?: "—",
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else color
        )
    }
}
