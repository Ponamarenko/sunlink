package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.NetworkDiagnostics
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

@Composable
fun DiagnosticsScreen(vm: MainViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val events by NetworkDiagnostics.events.collectAsState()
    val lastSuccess by NetworkDiagnostics.lastSuccessUtc.collectAsState()
    val backendHealth by vm.backendHealth.collectAsState()
    val backendVersion by vm.backendVersion.collectAsState()
    val quality by vm.telemetryQuality.collectAsState()
    val transport by vm.liveTransport.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadData()
        vm.loadBackendInfo()
        vm.loadTelemetryQuality()
    }

    val success = uiState as? InverterUiState.Success
    val online = success?.status?.online == true
    val stale = success?.stale == true || quality?.stale == true
    val error = (uiState as? InverterUiState.Error)?.message

    SunLinkScreen(
        title = stringResource(R.string.nav_diagnostics),
        subtitle = "REST ${com.sunlink.svitgrid.network.RetrofitClient.getBaseUrl()} · WS $transport",
        refreshing = loading,
        onRefresh = { vm.loadBackendInfo(); vm.loadTelemetryQuality() }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                if (error != null) stringResource(R.string.state_error)
                else if (stale) stringResource(R.string.state_stale)
                else if (online) stringResource(R.string.state_online)
                else stringResource(R.string.state_offline),
                if (error != null) colors.error else if (stale) colors.warning else if (online) colors.success else colors.offline,
                Modifier.weight(1f)
            )
            StatusPill("WS $transport", if (transport == TelemetryWebSocketClient.WsTransportState.OPEN) colors.success else colors.warning, Modifier.weight(1f))
        }

        error?.let {
            SunLinkCard(accent = colors.error) {
                Text("Остання помилка", style = SunLinkTypography.label, color = colors.error)
                Text(it, style = SunLinkTypography.caption, color = colors.textSecondary)
            }
        }

        Button(onClick = { vm.checkConnection() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) stringResource(R.string.diagnostics_checking) else stringResource(R.string.diagnostics_check_server))
        }

        SunLinkCard(accent = colors.primary) {
            Text(stringResource(R.string.diagnostics_server), style = SunLinkTypography.label, color = colors.textSecondary)
            InfoRow("Останнє оновлення", success?.status?.lastUpdate ?: "—")
            InfoRow("Інвертор", success?.status?.inverterId ?: "—")
            InfoRow("Доступних реєстрів", success?.status?.availableRegisters?.toString() ?: "—")
            InfoRow("Останній успішний запит", lastSuccess ?: "—")
        }

        SunLinkCard(accent = colors.grid) {
            Text("Backend", style = SunLinkTypography.label, color = colors.textSecondary)
            InfoRow("Стан", backendHealth?.status ?: "—")
            InfoRow("API / Gateway", listOfNotNull(backendVersion?.api, backendVersion?.gateway).joinToString(" / ").ifBlank { "—" })
            InfoRow("Протокол", backendVersion?.protocol ?: "—")
            InfoRow("Джерело", backendHealth?.source ?: "—")
        }

        SunLinkCard(accent = colors.solar) {
            Text(stringResource(R.string.quality_title), style = SunLinkTypography.label, color = colors.textSecondary)
            InfoRow(stringResource(R.string.quality_known, quality?.known_parameters ?: 0), "")
            InfoRow(stringResource(R.string.quality_available, quality?.available_parameters ?: 0), "")
            InfoRow(stringResource(R.string.quality_unavailable, quality?.unavailable_parameters ?: 0), "")
            InfoRow(stringResource(R.string.quality_active, quality?.active_registers?.joinToString()?.ifBlank { "—" } ?: "—"), "")
            InfoRow("Polling", if (quality?.polling_running == true) "ON" else "OFF")
            InfoRow("Cycles / failed / missed", "${quality?.cycles ?: 0} / ${quality?.failed_cycles ?: 0} / ${quality?.missed_cycles ?: 0}")
            InfoRow("Last cycle", quality?.last_cycle_ms?.let { "%.0f ms".format(it) } ?: "—")
        }

        SunLinkCard(accent = colors.load) {
            Text("Мережеві події (UPDATE / REST / WS)", style = SunLinkTypography.label, color = colors.textSecondary)
            if (events.isEmpty()) {
                Text(stringResource(R.string.diagnostics_no_requests), style = SunLinkTypography.caption, color = colors.textTertiary)
            } else {
                events.take(10).forEach { e ->
                    Text(
                        "${e.type} · ${e.stage} · ${e.endpoint} · HTTP ${e.httpCode ?: "—"} · ${e.errorType ?: "OK"}",
                        style = SunLinkTypography.caption,
                        color = colors.textSecondary
                    )
                }
            }
        }

        SunLinkCard(accent = colors.border) {
            Text("Як читати статуси", style = SunLinkTypography.label, color = colors.textSecondary)
            Text("• Онлайн / Офлайн — чи відповідає сервер на запити /api/live/raw.", style = SunLinkTypography.caption, color = colors.textTertiary)
            Text("• verified / corroborated — підтверджені значення; provisional — попередні; calculated — розраховані.", style = SunLinkTypography.caption, color = colors.textTertiary)
            Text("• stale — застарілі дані; unavailable — відсутні значення (показуються як «—», не як 0).", style = SunLinkTypography.caption, color = colors.textTertiary)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        Text(value.ifBlank { "—" }, style = SunLinkTypography.label, color = colors.textPrimary)
    }
}
