package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    val errorState = uiState as? InverterUiState.Error
    val online = success?.status?.online == true
    val stale = success?.stale == true || quality?.stale == true
    val error = errorState?.message ?: success?.status?.error

    // Стани Fresh / Stale / Unavailable / Offline формуються єдиною функцією freshnessOf.
    val connectionFreshness = freshnessOf(
        stale = stale,
        quality = if (success == null) "unavailable" else "verified",
        confidence = if (success == null) null else "verified",
        online = if (error != null) false else if (success == null) null else online
    )
    val healthStatus = backendHealth?.status
    val backendFreshness = freshnessOf(
        stale = false,
        quality = when {
            healthStatus == null -> "unavailable"
            healthStatus.equals("ok", true) || healthStatus.equals("up", true) -> "verified"
            else -> "stale"
        },
        confidence = if (healthStatus == null) null else "verified"
    )
    val qualityFreshness = freshnessOf(
        stale = quality?.stale,
        quality = null,
        confidence = if (quality == null) null else "verified"
    )
    val wsFreshness = freshnessOf(
        stale = false,
        quality = null,
        confidence = if (transport == TelemetryWebSocketClient.WsTransportState.OPEN) "verified" else null
    )

    SunLinkScreen(
        title = stringResource(R.string.nav_diagnostics),
        subtitle = "REST ${com.sunlink.svitgrid.network.RetrofitClient.getBaseUrl()} · WS $transport",
        refreshing = loading,
        onRefresh = { vm.loadBackendInfo(); vm.loadTelemetryQuality() }
    ) {
        PremiumCard(accent = colors.primary) {
            PremiumSectionHeader(title = stringResource(R.string.diagnostics_title))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                DataFreshnessIndicator(connectionFreshness, modifier = Modifier.weight(1f))
                DataFreshnessIndicator(backendFreshness, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(PremiumTokens.spaceXs))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                DataFreshnessIndicator(qualityFreshness, modifier = Modifier.weight(1f))
                DataFreshnessIndicator(wsFreshness, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Button(onClick = { vm.checkConnection() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text(if (loading) stringResource(R.string.diagnostics_checking) else stringResource(R.string.diagnostics_check_server))
            }
        }

        PremiumCard(accent = colors.grid) {
            PremiumSectionHeader(title = "З'єднання", trailing = { DataFreshnessIndicator(connectionFreshness) })
            InfoRow("Останнє оновлення", success?.status?.lastUpdate ?: "—")
            InfoRow("Інвертор", success?.status?.inverterId ?: "—")
            InfoRow("Доступних реєстрів", success?.status?.availableRegisters?.toString() ?: "—")
            InfoRow("Останній успішний запит", lastSuccess ?: "—")
            InfoRow("Транспорт", transport.name)
        }

        PremiumCard(accent = colors.primary) {
            PremiumSectionHeader(title = "Backend", trailing = { DataFreshnessIndicator(backendFreshness) })
            InfoRow("Стан", backendHealth?.status ?: "—")
            InfoRow("API / Gateway", listOfNotNull(backendVersion?.api, backendVersion?.gateway).joinToString(" / ").ifBlank { "—" })
            InfoRow("Протокол", backendVersion?.protocol ?: "—")
            InfoRow("Джерело", backendHealth?.source ?: "—")
        }

        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader(title = stringResource(R.string.quality_title), trailing = { DataFreshnessIndicator(qualityFreshness) })
            InfoRow(stringResource(R.string.quality_known, quality?.known_parameters ?: 0), "")
            InfoRow(stringResource(R.string.quality_available, quality?.available_parameters ?: 0), "")
            InfoRow(stringResource(R.string.quality_unavailable, quality?.unavailable_parameters ?: 0), "")
            InfoRow("Polling", if (quality?.polling_running == true) "ON" else "OFF")
            InfoRow("Cycles / failed / missed", "${quality?.cycles ?: 0} / ${quality?.failed_cycles ?: 0} / ${quality?.missed_cycles ?: 0}")
            InfoRow("Last cycle", quality?.last_cycle_ms?.let { "%.0f ms".format(it) } ?: "—")
        }

        PremiumCard(accent = colors.battery) {
            PremiumSectionHeader(title = "Стан реєстрів")
            InfoRow("Активні", quality?.active_registers?.joinToString()?.ifBlank { "—" } ?: "—")
            InfoRow(
                "Відомо / Доступно / Недоступно",
                "${quality?.known_parameters ?: 0} / ${quality?.available_parameters ?: 0} / ${quality?.unavailable_parameters ?: 0}"
            )
            InfoRow(
                "Розширений polling",
                quality?.extended_polling?.let { "jobs=${it.jobs ?: "—"} · executed=${it.executed.size}" } ?: "—"
            )
        }

        PremiumCard(accent = colors.error) {
            PremiumSectionHeader(title = "Попередження та помилки")
            val messages = mutableListOf<String>().apply {
                errorState?.message?.let { add(it) }
                success?.status?.error?.let { add(it) }
                quality?.by_confidence
                    ?.filter { (key, count) -> key in setOf("provisional", "unverified") && count > 0 }
                    ?.forEach { (key, count) -> add("$key: $count") }
            }
            if (messages.isEmpty()) {
                EmptyState("Помилок і попереджень немає")
            } else {
                messages.distinct().forEach {
                    Text("• $it", style = SunLinkTypography.caption, color = colors.textSecondary)
                }
            }
        }

        PremiumCard(accent = colors.load) {
            PremiumSectionHeader(title = "Мережеві події (UPDATE / REST / WS)")
            if (events.isEmpty()) {
                EmptyState(stringResource(R.string.diagnostics_no_requests))
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

        PremiumCard(accent = colors.border) {
            PremiumSectionHeader(title = "Як читати статуси")
            Text("• Онлайн / Офлайн — чи відповідає сервер на запити /api/live/raw.", style = SunLinkTypography.caption, color = colors.textTertiary)
            Text("• Fresh / Stale / Unavailable / Offline — свіжість і доступність підтверджених значень.", style = SunLinkTypography.caption, color = colors.textTertiary)
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
