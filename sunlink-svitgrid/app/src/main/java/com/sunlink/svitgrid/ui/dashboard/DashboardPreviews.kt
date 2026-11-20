package com.sunlink.svitgrid.ui.dashboard

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.theme.SvitGridTheme
import com.sunlink.svitgrid.ui.theme.ThemeMode

/**
 * Stage 31 — Preview使用 ізольованих тестових даних.
 * УВАГА: ці значення НЕ є телеметрією і ніколи не потрапляють у production ViewModel.
 */
private object PreviewTelemetry {
    const val SOC = 90f
    const val PV = 1240f
    const val LOAD = 860f
    const val BATTERY = 240f
    const val GRID = -140f
    const val TS = "2026-11-20T10:00:00+00:00"

    fun online() = InverterData(
        batterySoc = SOC, pSun = PV, pLoad = LOAD, pBatt = BATTERY, pGrid = GRID,
        pv1Power = 700f, pv2Power = 540f,
        batteryVoltage = 52.4f, batteryCurrent = 4.6f,
        dcTemperature = 31.2f, acTemperature = 38.4f,
        timestamp = TS, connectionStatus = true, source = "PREVIEW_ONLY",
        quality = "valid", confidence = "corroborated"
    )

    fun partial() = InverterData(
        batterySoc = SOC, timestamp = TS, connectionStatus = true,
        source = "PREVIEW_ONLY", quality = "valid", confidence = "corroborated"
    )

    fun status() = InverterStatus(
        online = true, lastUpdate = TS, inverterId = "deye_sg02lp1_am3", availableRegisters = 9
    )
}

@Composable
private fun PreviewHost(mode: ThemeMode, content: @Composable () -> Unit) {
    SvitGridTheme(themeMode = mode) { Surface(content = content) }
}

@Preview(showBackground = true, name = "Dashboard Dark — Online")
@Composable
fun PreviewDashboardDarkOnline() = PreviewHost(ThemeMode.DARK) {
    DashboardContent(
        uiState = InverterUiState.Success(PreviewTelemetry.online(), PreviewTelemetry.status(), false),
        refreshing = false,
        transport = TelemetryWebSocketClient.WsTransportState.OPEN,
        onRefresh = {}
    )
}

@Preview(showBackground = true, name = "Dashboard Light — Online")
@Composable
fun PreviewDashboardLightOnline() = PreviewHost(ThemeMode.LIGHT) {
    DashboardContent(
        uiState = InverterUiState.Success(PreviewTelemetry.online(), PreviewTelemetry.status(), false),
        refreshing = false,
        transport = TelemetryWebSocketClient.WsTransportState.OPEN,
        onRefresh = {}
    )
}

@Preview(showBackground = true, name = "Dashboard Offline")
@Composable
fun PreviewDashboardOffline() = PreviewHost(ThemeMode.DARK) {
    DashboardContent(
        uiState = InverterUiState.Error("WebSocket недоступний", null, null, false),
        refreshing = false,
        transport = TelemetryWebSocketClient.WsTransportState.RECONNECTING,
        onRefresh = {}
    )
}

@Preview(showBackground = true, name = "Dashboard Stale")
@Composable
fun PreviewDashboardStale() = PreviewHost(ThemeMode.DARK) {
    DashboardContent(
        uiState = InverterUiState.Success(PreviewTelemetry.partial(), PreviewTelemetry.status(), true),
        refreshing = false,
        transport = TelemetryWebSocketClient.WsTransportState.RECONNECTING,
        onRefresh = {}
    )
}

@Preview(showBackground = true, name = "Dashboard Loading")
@Composable
fun PreviewDashboardLoading() = PreviewHost(ThemeMode.DARK) {
    DashboardContent(
        uiState = InverterUiState.Loading,
        refreshing = true,
        transport = TelemetryWebSocketClient.WsTransportState.CONNECTING,
        onRefresh = {}
    )
}

@Preview(showBackground = true, name = "Dashboard partial telemetry")
@Composable
fun PreviewDashboardPartial() = PreviewHost(ThemeMode.LIGHT) {
    DashboardContent(
        uiState = InverterUiState.Success(PreviewTelemetry.partial(), PreviewTelemetry.status(), false),
        refreshing = false,
        transport = TelemetryWebSocketClient.WsTransportState.OPEN,
        onRefresh = {}
    )
}
