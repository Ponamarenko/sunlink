package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

@Composable
fun HomeScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val quality by vm.telemetryQuality.collectAsState()
    val state by vm.uiState.collectAsState()
    val transport by vm.liveTransport.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadTelemetryQuality()
        vm.loadData()
        vm.loadSeriesFor("pv_total_power")
        vm.loadSeriesFor("battery_soc")
    }

    val stale = state is InverterUiState.Success && (state as InverterUiState.Success).stale
    val online = state is InverterUiState.Success && (state as InverterUiState.Success).status.online
    val statusText = when {
        error != null -> stringResource(R.string.state_error)
        stale -> stringResource(R.string.state_stale)
        online -> stringResource(R.string.state_online)
        transport == TelemetryWebSocketClient.WsTransportState.RECONNECTING ||
            transport == TelemetryWebSocketClient.WsTransportState.CONNECTING -> stringResource(R.string.state_connecting)
        else -> stringResource(R.string.state_offline)
    }
    val statusColor = when {
        error != null -> colors.error
        stale -> colors.warning
        online -> colors.success
        else -> colors.offline
    }

    SunLinkScreen(
        title = stringResource(R.string.nav_home),
        subtitle = "$statusText · ${stringResource(R.string.dash_last_update, full?.timestamp ?: "—")}",
        refreshing = loading,
        onRefresh = { vm.loadData(); vm.loadFullTelemetry() }
    ) {
        StatusPill(statusText, statusColor)
        if (error != null) androidx.compose.material3.Text(error!!, color = colors.error, style = SunLinkTypography.caption)

        SunLinkCard {
            androidx.compose.material3.Text("ЕНЕРГЕТИЧНИЙ ПОТІК", style = SunLinkTypography.label, color = colors.textSecondary)
            EnergyFlowCanvas(
                pv = telemetryNumber(full, "pv", "total_power"),
                battery = telemetryNumber(full, "battery", "power"),
                grid = telemetryNumber(full, "grid", "power"),
                load = telemetryNumber(full, "load", "power"),
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusPill("PV", colors.solar)
                StatusPill("Battery", colors.battery)
                StatusPill("Grid", colors.grid)
                StatusPill("Load", colors.load)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "PV Power", telemetryNumber(full, "pv", "total_power"), "W",
                Icons.Default.SolarPower, colors.solar, Modifier.weight(1f),
                sparkline = series["pv_total_power"]?.points?.map { point -> if (point.gap) null else point.value?.toFloat() } ?: emptyList()
            )
            MetricCard(
                "SOC", telemetryNumber(full, "battery", "soc"), "%",
                Icons.Default.BatteryChargingFull, colors.battery, Modifier.weight(1f),
                sparkline = series["battery_soc"]?.points?.map { point -> if (point.gap) null else point.value?.toFloat() } ?: emptyList()
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Battery", telemetryNumber(full, "battery", "power"), "W", null, colors.batteryCharge, Modifier.weight(1f))
            MetricCard("Grid", telemetryNumber(full, "grid", "power"), "W", null, colors.grid, Modifier.weight(1f))
            MetricCard("Load", telemetryNumber(full, "load", "power"), "W", Icons.Default.Power, colors.load, Modifier.weight(1f))
        }

        SunLinkCard {
            androidx.compose.material3.Text("ЕНЕРГІЯ ЗА ДЕНЬ", style = SunLinkTypography.label, color = colors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyItem("Solar", telemetryNumber(full, "pv", "daily_energy"), colors.solar, Modifier.weight(1f))
                DailyItem("Import", telemetryNumber(full, "grid", "import_energy"), colors.gridImport, Modifier.weight(1f))
                DailyItem("Export", telemetryNumber(full, "grid", "export_energy"), colors.gridExport, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyItem("Load", telemetryNumber(full, "load", "energy"), colors.load, Modifier.weight(1f))
                DailyItem("Charge", telemetryNumber(full, "battery", "daily_charge_energy"), colors.batteryCharge, Modifier.weight(1f))
                DailyItem("Discharge", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.batteryDischarge, Modifier.weight(1f))
            }
        }

        quality?.let {
            StatusPill("Параметрів: ${it.available_parameters}/${it.known_parameters}", colors.primary)
        }
    }
}

@Composable
private fun DailyItem(label: String, value: Float?, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        androidx.compose.material3.Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        androidx.compose.material3.Text(
            if (value == null) "—" else "%.1f kWh".format(value),
            style = SunLinkTypography.label, color = if (value == null) colors.textTertiary else color
        )
    }
}
