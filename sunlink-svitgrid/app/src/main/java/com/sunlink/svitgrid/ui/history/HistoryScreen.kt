package com.sunlink.svitgrid.ui.history

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.PowerValue
import com.sunlink.svitgrid.ui.designsystem.SunLinkDimensions
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.TelemetryChart
import com.sunlink.svitgrid.viewmodel.MainViewModel

private val PERIODS = listOf("24h", "7d", "30d")
private val PARAMETERS = listOf(
    "battery_soc", "battery_voltage", "battery_current", "battery_power",
    "pv1_power", "pv2_power", "pv3_power", "daily_pv_energy",
    "grid_voltage_l1", "grid_frequency", "load_power",
    "daily_grid_import", "daily_grid_export",
    "daily_battery_charge", "daily_battery_discharge"
)

@Composable
fun HistoryScreen(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val colors = LocalSunLinkColors.current
    val uiState by vm.uiState.collectAsState()
    val series by vm.series.collectAsState()
    val seriesLoading by vm.seriesLoading.collectAsState()
    val seriesError by vm.seriesError.collectAsState()

    var selectedParameter by remember { mutableStateOf(PARAMETERS.first()) }
    var selectedPeriod by remember { mutableStateOf(PERIODS.first()) }
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { vm.loadData() }
    LaunchedEffect(selectedParameter, selectedPeriod) {
        selectedIndex = 0
        vm.loadSeries(selectedParameter, selectedPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = SunLinkDimensions.spaceXl),
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
    ) {
        Text(
            stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
        )
        Row(
            Modifier.padding(horizontal = SunLinkDimensions.spaceLg).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
        ) {
            PERIODS.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(period) }
                )
            }
        }
        Row(
            Modifier.padding(horizontal = SunLinkDimensions.spaceLg).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
        ) {
            PARAMETERS.forEach { parameter ->
                FilterChip(
                    selected = selectedParameter == parameter,
                    onClick = { selectedParameter = parameter },
                    label = { Text(parameterShortLabel(context, parameter)) }
                )
            }
        }

        when {
            seriesLoading -> CircularProgressIndicator(
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            seriesError != null -> Text(
                seriesError!!,
                color = colors.error,
                style = SunLinkTypography.body,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            series == null || series!!.points.isEmpty() -> Text(
                stringResource(R.string.history_no_data),
                color = colors.textSecondary,
                style = SunLinkTypography.body,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            else -> SeriesContent(
                series = series!!,
                parameter = selectedParameter,
                selectedIndex = selectedIndex,
                onIndexChange = { selectedIndex = it },
                context = context
            )
        }

        when (val state = uiState) {
            is InverterUiState.Success -> Text(
                stringResource(R.string.history_source, state.data.source, state.data.quality),
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            else -> {}
        }
    }
}

@Composable
private fun SeriesContent(
    series: HistorySeriesDto,
    parameter: String,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    context: Context
) {
    val colors = LocalSunLinkColors.current
    val values = series.points.map { it.value?.toFloat() }
    val safeIndex = selectedIndex.coerceIn(0, values.lastIndex)
    val selected = series.points[safeIndex]

    Card(
        modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(SunLinkDimensions.spaceMd)) {
            Text(
                "${parameterLabel(context, parameter)} · ${series.period}",
                style = SunLinkTypography.title,
                color = colors.textPrimary
            )
            TelemetryChart(values, parameterLabel(context, parameter))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected.timestamp,
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary
                )
                PowerValue(
                    value = selected.value?.toFloat(),
                    unit = parameterUnit(context, parameter),
                    decimals = 2,
                    color = colors.primary,
                    style = SunLinkTypography.label
                )
            }
            Slider(
                value = safeIndex.toFloat(),
                valueRange = 0f..values.lastIndex.coerceAtLeast(1).toFloat(),
                steps = values.lastIndex.coerceAtLeast(1),
                onValueChange = { onIndexChange(it.toInt()) }
            )
            Text(
                stringResource(
                    R.string.history_points,
                    series.available_points,
                    series.missing_points
                ),
                style = SunLinkTypography.caption,
                color = colors.textTertiary
            )
        }
    }
}

private fun labelRes(parameter: String): Int = when (parameter) {
    "battery_soc" -> R.string.hist_soc
    "battery_voltage" -> R.string.hist_batt_voltage
    "battery_current" -> R.string.hist_batt_current
    "battery_power" -> R.string.hist_batt_power
    "pv1_power" -> R.string.hist_pv1
    "pv2_power" -> R.string.hist_pv2
    "pv3_power" -> R.string.hist_pv3
    "daily_pv_energy" -> R.string.hist_daily_pv
    "grid_voltage_l1" -> R.string.hist_grid_voltage
    "grid_frequency" -> R.string.hist_grid_freq
    "load_power" -> R.string.hist_load_power
    "daily_grid_import" -> R.string.hist_daily_import
    "daily_grid_export" -> R.string.hist_daily_export
    "daily_battery_charge" -> R.string.hist_daily_charge
    "daily_battery_discharge" -> R.string.hist_daily_discharge
    else -> R.string.history_parameter
}

private fun parameterLabel(context: Context, parameter: String): String = context.getString(labelRes(parameter))
private fun parameterShortLabel(context: Context, parameter: String): String = parameterLabel(context, parameter)
private fun parameterUnit(context: Context, parameter: String): String = when (parameter) {
    "battery_soc" -> context.getString(R.string.unit_percent)
    "battery_voltage", "grid_voltage_l1" -> context.getString(R.string.unit_volt)
    "battery_current" -> context.getString(R.string.unit_amp)
    "grid_frequency" -> context.getString(R.string.unit_hz)
    "dc_temperature" -> context.getString(R.string.unit_celsius)
    "daily_pv_energy", "daily_grid_import", "daily_grid_export",
    "daily_battery_charge", "daily_battery_discharge" -> context.getString(R.string.unit_kwh)
    else -> context.getString(R.string.unit_watt)
}
