package com.sunlink.svitgrid.ui.inverters

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
import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.viewmodel.MainViewModel

/** Екран детальної інформації про інвертор. */
@Composable
fun InverterDetailsScreen(vm: MainViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val loading by vm.refreshing.collectAsState()

    val uiStateValue = uiState
    val data = when (uiStateValue) {
        is InverterUiState.Success -> uiStateValue.data
        else -> InverterData()
    }
    val status = when (uiStateValue) {
        is InverterUiState.Success -> uiStateValue.status
        else -> InverterStatus()
    }

    LaunchedEffect(Unit) { vm.loadData() }

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
                text = stringResource(R.string.inverter_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { vm.loadData() },
                enabled = !loading
            ) {
                Text(stringResource(R.string.refresh))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = status.inverterId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Deye SUN-12K-SG02LP1-EU-AM3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.width(1.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (status.online) "✅" else "❌",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = if (status.online) "Онлайн" else "Офлайн",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Реєстрів: ${status.availableRegisters}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                status.lastUpdate?.let {
                    Text(
                        text = "Оновлено: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
                status.error?.let {
                    Text(
                        text = "Помилка: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (data.pv1Power != null || data.pv2Power != null || data.pv3Power != null) {
            ParameterGroup(
                title = "Сонячні панелі (PV)",
                icon = { Text("☀️") },
                items = listOf(
                    DetailItem("PV1", data.pv1Power, "Вт", data.pv1Voltage, data.pv1Current),
                    DetailItem("PV2", data.pv2Power, "Вт", data.pv2Voltage, data.pv2Current),
                    DetailItem("PV3", data.pv3Power, "Вт", data.pv3Voltage, data.pv3Current),
                ).filter { it.value != null || it.voltage != null || it.current != null },
                source = "pv1_power/pv2_power/pv3_power (reg 186/187/188)",
                quality = data.quality,
                confidence = data.confidence
            )
        }

        if (data.batterySoc != null || data.batteryVoltage != null || data.batteryCurrent != null || data.pBatt != null) {
            ParameterGroup(
                title = stringResource(R.string.screen_battery),
                icon = { Text("🔋") },
                items = listOf(
                    DetailItem("SOC", data.batterySoc, "%"),
                    DetailItem("Напруга", data.batteryVoltage, "В"),
                    DetailItem("Ток", data.batteryCurrent, "А"),
                    DetailItem("Потужність", data.pBatt, "Вт"),
                    DetailItem("Температура (DC)", data.dcTemperature, "°C"),
                    DetailItem("Температура (AC)", data.acTemperature, "°C"),
                ).filter { it.value != null },
                source = "battery_soc/battery_voltage/battery_current/battery_power (reg 184/183/191/190)",
                quality = data.quality,
                confidence = data.confidence,
                note = data.batteryDirection
            )
        }

        if (data.pGrid != null || data.gridVoltageL1 != null) {
            ParameterGroup(
                title = "Електрос мережа",
                icon = { Text("⚡") },
                items = listOf(
                    DetailItem("Потужність", data.pGrid, "Вт"),
                    DetailItem("Напруга L1", data.gridVoltageL1, "В"),
                ).filter { it.value != null },
                source = "p_grid/grid_voltage_l1",
                quality = data.quality,
                confidence = data.confidence,
                note = data.gridDirection
            )
        }

        if (data.pLoad != null) {
            ParameterGroup(
                title = stringResource(R.string.inverter_load),
                icon = { Text("🏠") },
                items = listOf(DetailItem("Потужність", data.pLoad, "Вт")),
                source = "p_load (calculate)",
                quality = data.quality,
                confidence = data.confidence
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Якість даних", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Divider()
                Text("Джерело: ${data.source}", style = MaterialTheme.typography.bodyMedium)
                Text("Якість: ${data.quality}", style = MaterialTheme.typography.bodyMedium)
                Text("Достовірність: ${data.confidence}", style = MaterialTheme.typography.bodyMedium)
                Text("Час: ${data.timestamp ?: "—"}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (!data.hasMeasurements && !status.online) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📡",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Дані інвертора недоступні",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Перевірте підключення до backend у Налаштуваннях.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

data class DetailItem(
    val label: String,
    val value: Float?,
    val unit: String,
    val voltage: Float? = null,
    val current: Float? = null
)

@Composable
private fun ParameterGroup(
    title: String,
    icon: @Composable () -> Unit,
    items: List<DetailItem>,
    source: String,
    quality: String,
    confidence: String,
    note: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.width(1.dp), verticalAlignment = Alignment.CenterVertically) {
                    icon()
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Divider()
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.voltage != null || item.current != null) {
                            Text(
                                text = "${item.voltage?.let { String.format("%.1f", it) ?: "—"} } В · ${item.current?.let { String.format("%.2f", it) ?: "—"} } А",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Row(modifier = Modifier.width(1.dp), verticalAlignment = Alignment.CenterVertically) {
                        val v = item.value
                        Text(
                            text = if (v != null) String.format("%.2f", v) else "—",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Divider()
            Text(
                text = "Джерело: $source • якість: $quality • достовірність: $confidence${note?.let { " • $note" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
