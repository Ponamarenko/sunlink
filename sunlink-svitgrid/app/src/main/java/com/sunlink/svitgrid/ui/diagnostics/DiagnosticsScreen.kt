package com.sunlink.svitgrid.ui.diagnostics

import com.sunlink.svitgrid.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.viewmodel.MainViewModel
import com.sunlink.svitgrid.network.NetworkDiagnostics

@Composable
fun DiagnosticsScreen(vm: MainViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val events by NetworkDiagnostics.events.collectAsState()
    val lastSuccess by NetworkDiagnostics.lastSuccessUtc.collectAsState()
    val backendHealth by vm.backendHealth.collectAsState()
    val backendVersion by vm.backendVersion.collectAsState()
    val quality by vm.telemetryQuality.collectAsState()

    val uiStateValue = uiState
    val data = when (uiStateValue) {
        is InverterUiState.Success -> uiStateValue.data
        else -> InverterData()
    }
    val status = when (uiStateValue) {
        is InverterUiState.Success -> uiStateValue.status
        else -> InverterStatus()
    }

    LaunchedEffect(Unit) {
        vm.loadData()
        vm.loadBackendInfo()
        vm.loadTelemetryQuality()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.diagnostics_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = { vm.checkConnection() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.diagnostics_checking))
            } else {
                Text(stringResource(R.string.diagnostics_check_server))
            }
        }

        DiagnosticCard(
            title = stringResource(R.string.diagnostics_server),
            status = if (status.online) stringResource(R.string.diagnostics_available) else stringResource(R.string.diagnostics_unavailable),
            statusColor = if (status.online) Color(0xFF66BB6A) else Color(0xFFEF5350),
            details = listOf(
                "Інвертор: ${status.inverterId}",
                "Останнє оновлення: ${status.lastUpdate ?: "—"}",
                "Реєстрів: ${status.availableRegisters}",
                "Помилка: ${status.error ?: "немає"}"
            )
        )

        DiagnosticCard(
            title = "Мережеві запити (UPDATE / REST / WEBSOCKET)",
            status = "Останній успіх: ${lastSuccess ?: "—"}",
            statusColor = Color(0xFF66BB6A),
            details = if (events.isEmpty()) listOf(stringResource(R.string.diagnostics_no_requests))
            else events.take(10).map { e ->
                "${e.type} · ${e.stage} · ${e.endpoint} · HTTP ${e.httpCode ?: "—"} · ${e.errorType ?: "OK"} · ${e.atUtc.take(19)}"
            }
        )

        DiagnosticCard(
            title = "Backend",
            status = backendHealth?.status ?: "—",
            statusColor = if (backendHealth?.status == "ok") Color(0xFF66BB6A) else Color(0xFF90A4AE),
            details = listOf(
                "API: ${backendVersion?.api ?: "—"} · Gateway: ${backendVersion?.gateway ?: "—"}",
                "Протокол: ${backendVersion?.protocol ?: "—"}",
                "Source: ${backendHealth?.source ?: "—"}",
                "Останній успіх (мережа): ${lastSuccess ?: "—"}"
            )
        )

        DiagnosticCard(
            title = stringResource(R.string.quality_title),
            status = quality?.source ?: "—",
            statusColor = if (quality?.stale == true) Color(0xFFFFCA28) else Color(0xFF66BB6A),
            details = listOf(
                stringResource(R.string.quality_known, quality?.known_parameters ?: 0),
                stringResource(R.string.quality_available, quality?.available_parameters ?: 0),
                stringResource(R.string.quality_unavailable, quality?.unavailable_parameters ?: 0),
                stringResource(R.string.quality_active, quality?.active_registers?.joinToString() ?: "—")
            )
        )

        DiagnosticCard(
            title = stringResource(R.string.diagnostics_quality),
            status = data.quality,
            statusColor = when (data.quality) {
                "valid" -> Color(0xFF66BB6A)
                "unavailable" -> Color(0xFFEF5350)
                "stale" -> Color(0xFFFFCA28)
                "error" -> Color(0xFFEF5350)
                else -> Color(0xFF90A4AE)
            },
            details = listOf(
                "Джерело: ${data.source}",
                "Достовірність: ${data.confidence}",
                "Тип вимірювання: ${data.timestamp?.let { "реальний" } ?: "немає даних"}",
                "Має вимірювання: ${if (data.hasMeasurements) "так" else "ні"}"
            )
        )

        if (data.hasMeasurements) {
            DiagnosticCard(
                title = "Поточні вимірювання",
                status = "Оновлено",
                statusColor = Color(0xFF66BB6A),
                details = listOf(
                    "PV (загальна): ${data.pSun?.let { String.format("%.0f", it) } ?: "—"} Вт",
                    "SOC батареї: ${data.batterySoc?.let { String.format("%.0f", it) } ?: "—"} %",
                    "Напруга батареї: ${data.batteryVoltage?.let { String.format("%.1f", it) } ?: "—"} В",
                    "Потужність мережі: ${data.pGrid?.let { String.format("%.0f", it) } ?: "—"} Вт",
                    "Навантаження: ${data.pLoad?.let { String.format("%.0f", it) } ?: "—"} Вт"
                )
            )
        } else {
            DiagnosticCard(
                title = "Поточні вимірювання",
                status = "Немає даних",
                statusColor = Color(0xFFEF5350),
                details = listOf("Вимірювання не отримано з сервера")
            )
        }

        if (uiStateValue is InverterUiState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Остання помилка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Divider()
                    Text(uiStateValue.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Як читати статуси", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Divider()
                Text("• Онлайн / Офлайн — чи відповідає сервер на запити /api/live/raw", style = MaterialTheme.typography.bodySmall)
                Text("• Якість даних: valid (дійсні), unavailable (недоступні), stale (застарілі), error (помилка), unverified (неперевірені)", style = MaterialTheme.typography.bodySmall)
                Text("• Достовірність: corroborated (підтверджені), provisional (предварітні), verified (верифіковані), unverified", style = MaterialTheme.typography.bodySmall)
                Text("• Джерело: modbus_register (реальний Modbus), api_fallback (резервний API)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    status: String,
    statusColor: Color,
    details: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            Divider()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                details.forEach { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
