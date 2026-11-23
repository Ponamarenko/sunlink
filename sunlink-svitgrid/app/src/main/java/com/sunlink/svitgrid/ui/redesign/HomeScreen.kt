package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
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

    val success = state as? InverterUiState.Success
    val stale = full?.stale == true || quality?.stale == true || success?.stale == true
    val online = success?.status?.online == true
    val freshness = when {
        error != null -> Freshness.OFFLINE
        stale -> Freshness.STALE
        online && full != null -> Freshness.FRESH
        online -> Freshness.PROVISIONAL
        else -> Freshness.UNAVAILABLE
    }
    val freshnessLabel = when (freshness) {
        Freshness.FRESH -> stringResource(R.string.state_online)
        Freshness.STALE -> stringResource(R.string.state_stale)
        Freshness.PROVISIONAL -> stringResource(R.string.state_connecting)
        Freshness.OFFLINE -> stringResource(R.string.state_error)
        Freshness.UNAVAILABLE -> stringResource(R.string.state_offline)
    }

    val pvItem = telemetryValue(full, "pv", "total_power")
    val batteryItem = telemetryValue(full, "battery", "power")
    val gridItem = telemetryValue(full, "grid", "power")
    val loadItem = telemetryValue(full, "load", "power")
    val pvValue = pvItem?.normalized_value?.toFloat()
    val batteryValue = batteryItem?.normalized_value?.toFloat()
    val gridValue = gridItem?.normalized_value?.toFloat()
    val loadValue = loadItem?.normalized_value?.toFloat()

    val pvNode = FlowNode(pvValue, isConfirmed(pvItem, stale), pvFlowDirection(pvValue, isConfirmed(pvItem, stale)))
    val gridNode = FlowNode(gridValue, isConfirmed(gridItem, stale), gridFlowDirection(gridValue, isConfirmed(gridItem, stale)))
    val loadNode = FlowNode(loadValue, isConfirmed(loadItem, stale), loadFlowDirection(loadValue, isConfirmed(loadItem, stale)))
    // Напрямок батареї сервер не підтверджує (конфлікт reg. 190) — показуємо лише активність.
    val batteryNode = FlowNode(batteryValue, isConfirmed(batteryItem, stale), batteryFlowDirection())

    SunLinkScreen(
        title = stringResource(R.string.nav_home),
        subtitle = "$freshnessLabel · ${stringResource(R.string.dash_last_update, formatTelemetryTimestamp(full?.timestamp))}",
        refreshing = loading,
        onRefresh = { vm.loadData(); vm.loadFullTelemetry() }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataFreshnessIndicator(freshness, full?.timestamp, Modifier.weight(1f), compact = true)
            TransportPill(transport, compact = true)
        }
        error?.let { Text(it, color = colors.error, style = SunLinkTypography.caption) }

        PremiumCard(accent = colors.primary) {
            PremiumSectionHeader(stringResource(R.string.energy_balance), stringResource(R.string.flow_hint))
            PremiumEnergyFlow(pvNode, batteryNode, gridNode, loadNode)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "PV Power", pvValue, "W", Icons.Default.SolarPower, colors.solar,
                Modifier.weight(1f), emphasize = true,
                caption = telemetryLabel(pvItem),
                sparkline = series["pv_total_power"]?.points?.map { p -> if (p.gap) null else p.value?.toFloat() } ?: emptyList()
            )
            MetricCard(
                "Battery SOC", telemetryNumber(full, "battery", "soc"), "%",
                Icons.Default.BatteryChargingFull, colors.battery, Modifier.weight(1f),
                emphasize = true,
                sparkline = series["battery_soc"]?.points?.map { p -> if (p.gap) null else p.value?.toFloat() } ?: emptyList()
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Battery", batteryValue, "W", null, colors.batteryCharge, Modifier.weight(1f))
            MetricCard("Grid", gridValue, "W", null, colors.grid, Modifier.weight(1f))
            MetricCard("Load", loadValue, "W", Icons.Default.Power, colors.load, Modifier.weight(1f))
        }

        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader(stringResource(R.string.energy_today))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyItem("Solar", telemetryNumber(full, "pv", "daily_energy"), colors.solar, Modifier.weight(1f))
                DailyItem("Import", telemetryNumber(full, "grid", "daily_import_energy"), colors.gridImport, Modifier.weight(1f))
                DailyItem("Export", telemetryNumber(full, "grid", "daily_export_energy"), colors.gridExport, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyItem("Load", telemetryNumber(full, "load", "daily_energy"), colors.load, Modifier.weight(1f))
                DailyItem("Charge", telemetryNumber(full, "battery", "daily_charge_energy"), colors.batteryCharge, Modifier.weight(1f))
                DailyItem("Discharge", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.batteryDischarge, Modifier.weight(1f))
            }
        }

        quality?.let {
            StatusPill("${it.available_parameters}/${it.known_parameters} parameters", colors.primary)
        }
    }
}

@Composable
private fun TransportPill(
    transport: TelemetryWebSocketClient.WsTransportState,
    compact: Boolean = false
) {
    val colors = LocalSunLinkColors.current
    val connected = transport == TelemetryWebSocketClient.WsTransportState.OPEN
    StatusPill(transportLabel(transport), if (connected) colors.success else colors.warning)
}

@Composable
private fun DailyItem(
    label: String,
    value: Float?,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .padding(vertical = PremiumTokens.spaceXs)
    ) {
        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        Text(
            if (value == null) "—" else "%.1f kWh".format(value),
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else color
        )
    }
}
