package com.sunlink.svitgrid.ui.telemetry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.ui.designsystem.BatterySOCIndicator
import com.sunlink.svitgrid.ui.designsystem.LastUpdatedLabel
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.PowerValue
import com.sunlink.svitgrid.ui.designsystem.SectionHeader
import com.sunlink.svitgrid.ui.designsystem.SunLinkDimensions
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.TelemetryChart
import com.sunlink.svitgrid.viewmodel.MainViewModel

private fun value(dto: TelemetryFullDto?, category: String, field: String): MeasurementValueDto? =
    dto?.categories?.get(category)?.get(field)

@Composable
private fun ValueCard(
    label: String,
    item: MeasurementValueDto?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(SunLinkDimensions.spaceMd),
            verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceXs)
        ) {
            Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
            PowerValue(
                value = item?.normalized_value?.toFloat(),
                unit = item?.unit.orEmpty(),
                decimals = 2,
                color = accent
            )
            Text(
                text = when {
                    item == null -> stringResource(R.string.dash_no_data)
                    item.confidence == "calculated" -> stringResource(R.string.value_calculated)
                    item.quality == "stale" -> stringResource(R.string.state_stale)
                    else -> item.confidence ?: stringResource(R.string.dash_no_data)
                },
                style = SunLinkTypography.caption,
                color = if (item?.quality == "stale") colors.warning else colors.textTertiary
            )
        }
    }
}

@Composable
private fun CategoryScaffold(
    title: String,
    vm: MainViewModel,
    content: @Composable (TelemetryFullDto?) -> Unit
) {
    val full by vm.fullTelemetry.collectAsState()
    val loadError by vm.fullTelemetryError.collectAsState()
    val loadingFull by vm.fullTelemetryLoading.collectAsState()
    val colors = LocalSunLinkColors.current
    LaunchedEffect(Unit) { vm.loadFullTelemetry() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = SunLinkDimensions.spaceXl),
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
    ) {
        SectionHeader(title, Modifier.padding(horizontal = SunLinkDimensions.spaceLg))
        if (loadingFull && full == null) {
            CircularProgressIndicator(modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg))
        } else if (full == null) {
            Text(
                loadError ?: stringResource(R.string.telemetry_unavailable),
                style = SunLinkTypography.body,
                color = if (loadError != null) colors.error else colors.textSecondary,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            androidx.compose.material3.TextButton(
                onClick = { vm.loadFullTelemetry() },
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            ) { Text(stringResource(R.string.refresh)) }
        } else {
            LastUpdatedLabel(full!!.timestamp)
            if (full!!.stale == true) {
                Text(
                    stringResource(R.string.dash_stale_banner),
                    style = SunLinkTypography.caption,
                    color = colors.warning,
                    modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
                )
            }
            content(full)
        }
    }
}

@Composable
fun BatteryDetailScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    CategoryScaffold(stringResource(R.string.screen_battery), vm) { dto ->
        val soc = value(dto, "battery", "soc")?.normalized_value?.toFloat()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SunLinkDimensions.spaceLg),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceLg)
        ) {
            BatterySOCIndicator(soc, size = 96.dp)
            Column {
                Text(stringResource(R.string.metric_soc), style = SunLinkTypography.title, color = colors.textPrimary)
                val status = when {
                    value(dto, "battery", "power")?.normalized_value == null -> stringResource(R.string.state_unknown)
                    (value(dto, "battery", "power")!!.normalized_value ?: 0.0) > 0 -> stringResource(R.string.battery_charging)
                    else -> stringResource(R.string.battery_discharging)
                }
                Text(status, style = SunLinkTypography.body, color = colors.textSecondary)
            }
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.metric_voltage), value(dto, "battery", "voltage"), colors.battery, Modifier.weight(1f))
            ValueCard(stringResource(R.string.metric_current), value(dto, "battery", "current"), colors.battery, Modifier.weight(1f))
            ValueCard(stringResource(R.string.metric_power), value(dto, "battery", "power"), colors.battery, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.battery_temperature), value(dto, "battery", "temperature"), colors.warning, Modifier.weight(1f))
            ValueCard(stringResource(R.string.battery_daily_charge), value(dto, "battery", "daily_charge_energy"), colors.success, Modifier.weight(1f))
            ValueCard(stringResource(R.string.battery_daily_discharge), value(dto, "battery", "daily_discharge_energy"), colors.load, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.battery_status), value(dto, "battery", "status"), colors.battery, Modifier.weight(1f))
            ValueCard(stringResource(R.string.battery_total_charge), value(dto, "battery", "total_charge_energy"), colors.success, Modifier.weight(1f))
            ValueCard(stringResource(R.string.battery_total_discharge), value(dto, "battery", "total_discharge_energy"), colors.load, Modifier.weight(1f))
        }
    }
}

@Composable
fun PvDetailScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    CategoryScaffold(stringResource(R.string.screen_pv), vm) { dto ->
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.pv_total), value(dto, "pv", "total_power"), colors.solar, Modifier.weight(1f))
            ValueCard(stringResource(R.string.pv_daily), value(dto, "pv", "daily_energy"), colors.solar, Modifier.weight(1f))
            ValueCard(stringResource(R.string.pv_total_energy), value(dto, "pv", "total_energy"), colors.solar, Modifier.weight(1f))
            ValueCard(stringResource(R.string.pv_status), value(dto, "pv", "status"), colors.solar, Modifier.weight(1f))
        }
        listOf("pv1", "pv2", "pv3").forEach { mppt ->
            SectionHeader(mppt.uppercase(), Modifier.padding(horizontal = SunLinkDimensions.spaceLg))
            Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
                ValueCard(stringResource(R.string.metric_voltage), value(dto, "pv", "${mppt}_voltage"), colors.solar, Modifier.weight(1f))
                ValueCard(stringResource(R.string.metric_current), value(dto, "pv", "${mppt}_current"), colors.solar, Modifier.weight(1f))
                ValueCard(stringResource(R.string.metric_power), value(dto, "pv", "${mppt}_power"), colors.solar, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GridDetailScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    CategoryScaffold(stringResource(R.string.screen_grid), vm) { dto ->
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.metric_power), value(dto, "grid", "power"), colors.grid, Modifier.weight(1f))
            ValueCard(stringResource(R.string.metric_voltage), value(dto, "grid", "voltage_l1"), colors.grid, Modifier.weight(1f))
            ValueCard(stringResource(R.string.grid_frequency), value(dto, "grid", "frequency"), colors.grid, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.grid_import), value(dto, "grid", "import_energy"), colors.gridImport, Modifier.weight(1f))
            ValueCard(stringResource(R.string.grid_export), value(dto, "grid", "export_energy"), colors.gridExport, Modifier.weight(1f))
            ValueCard(stringResource(R.string.grid_connection), value(dto, "grid", "connected"), colors.grid, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.metric_current) + " L1", value(dto, "grid", "current_l1"), colors.grid, Modifier.weight(1f))
            ValueCard(stringResource(R.string.metric_current) + " L2", value(dto, "grid", "current_l2"), colors.grid, Modifier.weight(1f))
            ValueCard(stringResource(R.string.grid_import_power), value(dto, "grid", "import_power"), colors.gridImport, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.grid_export_power), value(dto, "grid", "export_power"), colors.gridExport, Modifier.weight(1f))
            ValueCard(stringResource(R.string.grid_voltage) + " L2", value(dto, "grid", "voltage_l2"), colors.grid, Modifier.weight(1f))
            ValueCard(stringResource(R.string.grid_ct), value(dto, "grid", "internal_ct_l1"), colors.grid, Modifier.weight(1f))
        }
    }
}

@Composable
fun LoadDetailScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    CategoryScaffold(stringResource(R.string.screen_load), vm) { dto ->
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.metric_power), value(dto, "load", "power"), colors.load, Modifier.weight(1f))
            ValueCard(stringResource(R.string.metric_voltage), value(dto, "load", "voltage"), colors.load, Modifier.weight(1f))
            ValueCard(stringResource(R.string.metric_current), value(dto, "load", "current"), colors.load, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.load_daily_energy), value(dto, "load", "energy"), colors.load, Modifier.weight(1f))
            ValueCard(stringResource(R.string.load_frequency), value(dto, "load", "frequency"), colors.load, Modifier.weight(1f))
            ValueCard(stringResource(R.string.load_power_factor), value(dto, "load", "power_factor"), colors.load, Modifier.weight(1f))
        }
    }
}

@Composable
fun EnergyFlowScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    CategoryScaffold(stringResource(R.string.screen_energy_flow), vm) { dto ->
        fun num(category: String, field: String): Float? =
            value(dto, category, field)?.normalized_value?.toFloat()

        Text(
            stringResource(R.string.flow_hint),
            style = SunLinkTypography.body,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
        )
        com.sunlink.svitgrid.ui.designsystem.EnergyFlowDiagram(
            pvPower = num("pv", "total_power"),
            pv1Power = num("pv", "pv1_power"),
            pv2Power = num("pv", "pv2_power"),
            pv3Power = num("pv", "pv3_power"),
            batteryPower = num("battery", "power"),
            gridPower = num("grid", "power"),
            loadPower = num("load", "power"),
            soc = num("battery", "soc"),
            state = com.sunlink.svitgrid.ui.designsystem.DashboardState.ONLINE,
            modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
        )
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.node_solar), value(dto, "pv", "total_power"), colors.solar, Modifier.weight(1f))
            ValueCard(stringResource(R.string.node_battery), value(dto, "battery", "power"), colors.battery, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = SunLinkDimensions.spaceLg), horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
            ValueCard(stringResource(R.string.node_grid), value(dto, "grid", "power"), colors.grid, Modifier.weight(1f))
            ValueCard(stringResource(R.string.node_home), value(dto, "load", "power"), colors.load, Modifier.weight(1f))
        }
    }
}

@Composable
fun InverterDetailScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    CategoryScaffold(stringResource(R.string.screen_inverter), vm) { dto ->
        val fields = listOf(
            R.string.inverter_operating_state to "operating_state",
            R.string.inverter_dc_voltage to "dc_voltage",
            R.string.inverter_ac_voltage to "ac_voltage",
            R.string.inverter_ac_current to "ac_current",
            R.string.inverter_ac_frequency to "ac_frequency",
            R.string.inverter_active_power to "active_power",
            R.string.inverter_reactive_power to "reactive_power",
            R.string.inverter_apparent_power to "apparent_power",
            R.string.inverter_power_factor to "power_factor",
            R.string.inverter_dc_temperature to "dc_temperature",
            R.string.inverter_ac_temperature to "ac_temperature",
            R.string.inverter_efficiency to "efficiency",
            R.string.inverter_fault_code to "fault_code",
            R.string.inverter_warning_code to "warning_code",
            R.string.inverter_communication_status to "communication_status"
        )
        fields.chunked(3).forEach { row ->
            Row(
                Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
            ) {
                row.forEach { (label, field) ->
                    ValueCard(stringResource(label), value(dto, "inverter", field), colors.primary, Modifier.weight(1f))
                }
            }
        }
    }
}
