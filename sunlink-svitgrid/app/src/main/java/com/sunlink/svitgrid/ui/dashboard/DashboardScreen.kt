package com.sunlink.svitgrid.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.foundation.clickable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.ui.designsystem.BatterySOCIndicator
import com.sunlink.svitgrid.ui.designsystem.ConnectionStatusBadge
import com.sunlink.svitgrid.ui.designsystem.DashboardState
import com.sunlink.svitgrid.ui.designsystem.DashboardStateMapper
import com.sunlink.svitgrid.ui.designsystem.EnergyFlowDiagram
import com.sunlink.svitgrid.ui.designsystem.EnergyMetricCard
import com.sunlink.svitgrid.ui.designsystem.ErrorState
import com.sunlink.svitgrid.ui.designsystem.LastUpdatedLabel
import com.sunlink.svitgrid.ui.designsystem.LoadingState
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.OfflineState
import com.sunlink.svitgrid.ui.designsystem.PowerValue
import com.sunlink.svitgrid.ui.designsystem.SectionHeader
import com.sunlink.svitgrid.ui.designsystem.SunLinkBackground
import com.sunlink.svitgrid.ui.designsystem.SunLinkDimensions
import com.sunlink.svitgrid.ui.designsystem.SunLinkTopBar
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.TelemetryValue
import com.sunlink.svitgrid.viewmodel.MainViewModel

@Composable
fun DashboardScreen(vm: MainViewModel = viewModel(), onNavigate: (String) -> Unit = {}) {
    val uiState by vm.uiState.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val transport by vm.liveTransport.collectAsState()
    DashboardContent(
        uiState = uiState,
        refreshing = refreshing,
        transport = transport,
        onRefresh = { vm.refresh() },
        onNavigate = onNavigate
    )
}

@Composable
internal fun DashboardContent(
    uiState: InverterUiState,
    refreshing: Boolean,
    transport: com.sunlink.svitgrid.network.TelemetryWebSocketClient.WsTransportState,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val state = DashboardStateMapper.map(uiState, refreshing, transport)
    val colors = LocalSunLinkColors.current

    val data: InverterData = when (uiState) {
        is InverterUiState.Success -> (uiState as InverterUiState.Success).data
        is InverterUiState.Error -> (uiState as InverterUiState.Error).data ?: InverterData()
        else -> InverterData()
    }
    val errorMessage = (uiState as? InverterUiState.Error)?.message

    SunLinkBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = SunLinkDimensions.spaceXl),
            verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
        ) {
            SunLinkTopBar(
                title = stringResource(R.string.app_name),
                subtitle = "Deye SUN-12K-SG02LP1-EU-AM3",
                state = state,
                lastUpdate = data.timestamp
            )

            if (state == DashboardState.STALE) {
                StatusBanner(stringResource(R.string.dash_stale_banner), colors.warning)
            }
            if (state == DashboardState.ERROR && errorMessage != null) {
                StatusBanner(stringResource(R.string.dash_error_banner), colors.error)
            }
            if (data.source == "demo_simulator" || data.source == "mock") {
                StatusBanner(stringResource(R.string.dash_demo_warning), colors.warning)
            }

            if (!data.hasMeasurements) {
                when (state) {
                    DashboardState.LOADING, DashboardState.CONNECTING -> LoadingState()
                    DashboardState.ERROR -> ErrorState(
                        message = errorMessage ?: stringResource(R.string.dash_error_banner),
                        onRetry = onRefresh
                    )
                    else -> OfflineState(
                        message = stringResource(R.string.dash_no_data_hint),
                        onRetry = onRefresh
                    )
                }
            } else {
            // Головний енергетичний блок — тільки підтверджені поля.
            OverviewCard(data = data, state = state)

            SectionHeader(stringResource(R.string.dash_flow_title))
            EnergyFlowDiagram(
                pvPower = data.pSun,
                pv1Power = data.pv1Power,
                pv2Power = data.pv2Power,
                pv3Power = data.pv3Power,
                batteryPower = data.pBatt,
                gridPower = data.pGrid,
                loadPower = data.pLoad,
                soc = data.batterySoc,
                state = state,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SunLinkDimensions.spaceLg)
            ) {
                val cards = @Composable { listOf(
                    stringResource(R.string.metric_pv) to (data.pSun to (R.drawable.sunlink_solar_panel to colors.solar)),
                    stringResource(R.string.metric_battery) to (data.pBatt to (R.drawable.sunlink_battery to colors.battery)),
                    stringResource(R.string.metric_grid) to (data.pGrid to (R.drawable.sunlink_grid_pole to colors.grid)),
                    stringResource(R.string.metric_load) to (data.pLoad to (R.drawable.sunlink_house to colors.load))
                ) }
                val unit = stringResource(R.string.unit_watt)
                if (maxWidth < 360.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
                        listOf(cards().take(2), cards().drop(2)).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
                                pair.forEach { (title, info) ->
                                    EnergyMetricCard(
                                        title, info.first, unit, info.second.first, info.second.second,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)) {
                        cards().forEach { (title, info) ->
                            EnergyMetricCard(
                                title, info.first, unit, info.second.first, info.second.second,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.quick_access))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SunLinkDimensions.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
            ) {
                listOf(
                    "battery" to R.string.screen_battery,
                    "pv" to R.string.screen_pv,
                    "grid" to R.string.screen_grid,
                    "load" to R.string.screen_load,
                    "energy-flow" to R.string.screen_energy_flow,
                    "statistics" to R.string.statistics
                ).forEach { (route, label) ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onNavigate(route) },
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Text(
                            stringResource(label), style = SunLinkTypography.caption,
                            color = colors.primary,
                            modifier = Modifier.padding(SunLinkDimensions.spaceSm),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            SectionHeader(
                stringResource(R.string.dash_telemetry_title),
                Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            RealTelemetrySection(data = data)

            Text(
                text = stringResource(R.string.dash_source, data.source),
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            LastUpdatedLabel(
                data.timestamp,
                Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            }
        }
    }
}

@Composable
private fun StatusBanner(message: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SunLinkDimensions.spaceLg),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Text(
            message,
            style = SunLinkTypography.caption,
            color = color,
            modifier = Modifier.padding(SunLinkDimensions.spaceMd)
        )
    }
}

@Composable
private fun OverviewCard(data: InverterData, state: DashboardState) {
    val colors = LocalSunLinkColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SunLinkDimensions.spaceLg),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier.padding(SunLinkDimensions.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
        ) {
            BatterySOCIndicator(data.batterySoc, size = 68.dp)
            Column(modifier = Modifier.weight(1f)) {
                ConnectionStatusBadge(state = state)
                Spacer(Modifier.height(SunLinkDimensions.spaceXs))
                Text(
                    text = stringResource(
                        when (DashboardStateMapper.inverterState(data, state)) {
                            com.sunlink.svitgrid.ui.designsystem.InverterPresentation.ONLINE -> R.string.dash_inverter_online
                            com.sunlink.svitgrid.ui.designsystem.InverterPresentation.OFFLINE -> R.string.dash_inverter_offline
                            com.sunlink.svitgrid.ui.designsystem.InverterPresentation.WAITING -> R.string.dash_inverter_waiting
                            com.sunlink.svitgrid.ui.designsystem.InverterPresentation.NO_DATA -> R.string.dash_inverter_no_data
                        }
                    ),
                    style = SunLinkTypography.label,
                    color = colors.textSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                PowerValue(data.pSun, stringResource(R.string.unit_watt), color = colors.solar)
                PowerValue(data.pLoad, stringResource(R.string.unit_watt), color = colors.load)
            }
        }
    }
}

@Composable
private fun RealTelemetrySection(data: InverterData) {
    val colors = LocalSunLinkColors.current
    val provisional = data.provisionalParameters
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SunLinkDimensions.spaceLg),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(SunLinkDimensions.spaceLg),
            verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceLg)) {
                TelemetryValue("PV1", data.pv1Power, stringResource(R.string.unit_watt), 0, colors.solar, "pv1_power" in provisional, Modifier.weight(1f))
                TelemetryValue("PV2", data.pv2Power, stringResource(R.string.unit_watt), 0, colors.solar, "pv2_power" in provisional, Modifier.weight(1f))
                TelemetryValue("PV3", data.pv3Power, stringResource(R.string.unit_watt), 0, colors.solar, "pv3_power" in provisional, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceLg)) {
                TelemetryValue(stringResource(R.string.metric_voltage), data.batteryVoltage, stringResource(R.string.unit_volt), 2, colors.battery, "battery_voltage" in provisional, Modifier.weight(1f))
                TelemetryValue(stringResource(R.string.metric_current), data.batteryCurrent, stringResource(R.string.unit_amp), 2, colors.battery, "battery_current" in provisional, Modifier.weight(1f))
                TelemetryValue(stringResource(R.string.metric_power), data.pBatt, stringResource(R.string.unit_watt), 0, colors.battery, "battery_power" in provisional, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceLg)) {
                TelemetryValue("T90", data.dcTemperature, stringResource(R.string.unit_celsius), 1, colors.warning, "dc_temperature" in provisional, Modifier.weight(1f))
                TelemetryValue("T91", data.acTemperature, stringResource(R.string.unit_celsius), 1, colors.warning, "ac_temperature" in provisional, Modifier.weight(1f))
                TelemetryValue("SOC", data.batterySoc, stringResource(R.string.unit_percent), 0, colors.success, false, Modifier.weight(1f))
            }
            if (provisional.isNotEmpty()) {
                Text(
                    stringResource(R.string.dash_provisional_note),
                    style = SunLinkTypography.caption,
                    color = colors.warning
                )
            }
        }
    }
}
