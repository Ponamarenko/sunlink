package com.sunlink.svitgrid.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.PowerValue
import com.sunlink.svitgrid.ui.designsystem.SectionHeader
import com.sunlink.svitgrid.ui.designsystem.SunLinkDimensions
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.TelemetryChart
import com.sunlink.svitgrid.viewmodel.HistoryUiState
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Duration
import java.time.Instant

private data class Period(val days: Long, val label: String)
private val PERIODS = listOf(
    Period(1, "24h"), Period(7, "7d"), Period(30, "30d"), Period(365, "365d")
)
private val PARAMETERS = listOf(
    "battery_soc", "pv1_power", "battery_power", "grid_power", "load_power"
)

@Composable
fun StatisticsScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    val stats by vm.stats.collectAsState()
    val history by vm.historyState.collectAsState()
    var parameter by remember { mutableStateOf(PARAMETERS.first()) }
    var period by remember { mutableStateOf(PERIODS.first()) }

    val end = remember { Instant.now().toString() }
    val start = remember(period) { Instant.now().minus(Duration.ofDays(period.days)).toString() }

    LaunchedEffect(parameter, period) {
        vm.loadStats(parameter, start, end)
        vm.loadHistory(parameter, start, end)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = SunLinkDimensions.spaceXl),
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
    ) {
        SectionHeader(
            stringResource(R.string.statistics_title),
            Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
        )
        Row(
            Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
            horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
        ) {
            PERIODS.forEach { p ->
                FilterChip(
                    selected = p == period,
                    onClick = { period = p },
                    label = { Text(p.label) }
                )
            }
        }
        Row(
            Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
            horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
        ) {
            PARAMETERS.take(3).forEach { p ->
                FilterChip(
                    selected = p == parameter,
                    onClick = { parameter = p },
                    label = { Text(p) }
                )
            }
        }
        Card(
            modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier.padding(SunLinkDimensions.spaceMd),
                verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
            ) {
                Text(parameter, style = SunLinkTypography.title, color = colors.textPrimary)
                if (stats == null || stats!!.count == 0) {
                    Text(
                        stringResource(R.string.statistics_no_data),
                        style = SunLinkTypography.body,
                        color = colors.textSecondary
                    )
                } else {
                    Text(
                        stringResource(R.string.statistics_count, stats!!.count),
                        style = SunLinkTypography.caption,
                        color = colors.textTertiary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceLg)) {
                        PowerValue(stats!!.min?.toFloat(), "", 1, colors.textPrimary, SunLinkTypography.label, Modifier.weight(1f))
                        PowerValue(stats!!.max?.toFloat(), "", 1, colors.textPrimary, SunLinkTypography.label, Modifier.weight(1f))
                        PowerValue(stats!!.avg?.toFloat(), "", 1, colors.primary, SunLinkTypography.label, Modifier.weight(1f))
                        PowerValue(stats!!.sum?.toFloat(), "", 1, colors.textPrimary, SunLinkTypography.label, Modifier.weight(1f))
                    }
                }
            }
        }
        val points = when (val state = history) {
            is HistoryUiState.Success -> state.points.map { it.value }
            else -> emptyList()
        }
        if (points.isNotEmpty()) {
            TelemetryChart(points, parameter)
        }
    }
}
