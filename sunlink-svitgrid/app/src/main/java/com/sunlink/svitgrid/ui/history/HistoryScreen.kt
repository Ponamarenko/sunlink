package com.sunlink.svitgrid.ui.history

import com.sunlink.svitgrid.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.model.HistoryPoint
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.viewmodel.HistoryUiState
import com.sunlink.svitgrid.viewmodel.MainViewModel

@Composable
fun HistoryScreen(vm: MainViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val historyState by vm.historyState.collectAsState()
    val loading by vm.refreshing.collectAsState()

    val uiStateValue = uiState
    val currentData = when (uiStateValue) {
        is InverterUiState.Success -> uiStateValue.data
        else -> InverterData()
    }
    val historyStateValue = historyState

    var selectedParameter by remember { mutableStateOf("battery_soc") }
    val periods = listOf(
        "1h" to java.time.Duration.ofHours(1),
        "6h" to java.time.Duration.ofHours(6),
        "24h" to java.time.Duration.ofDays(1),
        "7d" to java.time.Duration.ofDays(7),
        "30d" to java.time.Duration.ofDays(30)
    )
    var selectedPeriod by remember { mutableStateOf(periods[2]) }
    val end = remember { java.time.Instant.now().toString() }
    val start = remember(selectedPeriod) {
        java.time.Instant.now().minus(selectedPeriod.second).toString()
    }

    LaunchedEffect(Unit) { vm.loadData() }
    LaunchedEffect(selectedParameter, selectedPeriod) {
        vm.loadHistory(selectedParameter, start, end)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    vm.loadData()
                    vm.loadHistory(selectedParameter, start, end)
                },
                enabled = !loading,
                modifier = Modifier.weight(1f)
            ) {
                Text("🔄")
            }
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        val currentValue = currentParameterValue(currentData, selectedParameter)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = parameterLabel(selectedParameter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = parameterUnit(selectedParameter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Text(
            text = stringResource(R.string.history_period),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            periods.forEach { (label, duration) ->
                FilterChip(
                    selected = selectedPeriod.second == duration,
                    onClick = { selectedPeriod = label to duration },
                    label = { Text(label) }
                )
            }
        }

        Text(
            text = stringResource(R.string.history_parameter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("battery_soc", "pv1_power", "pv2_power", "pv3_power", "battery_power", "battery_voltage", "battery_current", "dc_temperature").forEach { param ->
                Button(
                    onClick = { selectedParameter = param },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(parameterShortLabel(param), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        when (historyStateValue) {
            is HistoryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is HistoryUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Помилка завантаження історії", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(historyStateValue.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            HistoryUiState.Empty -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚡",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.history_no_data),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ендпоінт /api/measurements/history не повернув точок для параметру $selectedParameter. Можливо, сервер ще не накопичив вимірювань або параметр не підтримується.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            is HistoryUiState.Success -> {
                val points = historyStateValue.points
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${parameterLabel(selectedParameter)} —/${parameterUnit(selectedParameter)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Точок: ${points.size} • період: ${formatPeriod(points)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Список точок замість Canvas-графіка
                    Column(
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    ) {
                        points.forEachIndexed { index, point ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = point.timestamp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(120.dp)
                                )
                                Text(
                                    text = "${point.value?.toString() ?: "—"} ${point.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (index < points.lastIndex) Divider()
                        }
                    }
                }
            }
        }

        when (uiStateValue) {
            is InverterUiState.Success -> {
                Text(
                    text = "Джерело: ${uiStateValue.data.source} • якість: ${uiStateValue.data.quality} • достовірність: ${uiStateValue.data.confidence}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is InverterUiState.Error -> {
                Text(
                    text = "Помилка: ${uiStateValue.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}

private fun currentParameterValue(data: InverterData, parameter: String): String {
    return when (parameter) {
        "battery_soc" -> data.batterySoc?.let { "${it.toInt()}%" } ?: "—"
        "pv1_power" -> data.pv1Power?.let { "${it.toInt()} Вт" } ?: "—"
        "pv2_power" -> data.pv2Power?.let { "${it.toInt()} Вт" } ?: "—"
        "pv3_power" -> data.pv3Power?.let { "${it.toInt()} Вт" } ?: "—"
        "battery_power" -> data.pBatt?.let { "${it.toInt()} Вт" } ?: "—"
        "battery_voltage" -> data.batteryVoltage?.let { String.format("%.1f", it) } ?: "—"
        "battery_current" -> data.batteryCurrent?.let { String.format("%.2f", it) } ?: "—"
        "dc_temperature" -> data.dcTemperature?.let { String.format("%.1f", it) } ?: "—"
        else -> "—"
    }
}

private fun parameterLabel(parameter: String): String {
    return when (parameter) {
        "battery_soc" -> "Заряд батареї (SOC)"
        "pv1_power" -> "Потужність PV1"
        "pv2_power" -> "Потужність PV2"
        "pv3_power" -> "Потужність PV3"
        "battery_power" -> "Потужність батареї"
        "battery_voltage" -> "Напруга батареї"
        "battery_current" -> "Ток батареї"
        "dc_temperature" -> "Температура DC"
        else -> parameter
    }
}

private fun parameterShortLabel(parameter: String): String {
    return when (parameter) {
        "battery_soc" -> "SOC"
        "pv1_power" -> "PV1"
        "pv2_power" -> "PV2"
        "pv3_power" -> "PV3"
        "battery_power" -> "Batt"
        "battery_voltage" -> "Batt V"
        "battery_current" -> "Batt I"
        "dc_temperature" -> "Темп."
        else -> parameter
    }
}

private fun parameterUnit(parameter: String): String {
    return when (parameter) {
        "battery_soc" -> "%"
        "pv1_power", "pv2_power", "pv3_power", "battery_power" -> "Вт"
        "battery_voltage" -> "В"
        "battery_current" -> "А"
        "dc_temperature" -> "°C"
        else -> ""
    }
}

private fun formatPeriod(points: List<HistoryPoint>): String {
    return if (points.isEmpty()) "—" else "${points.first().timestamp} → ${points.last().timestamp}"
}
