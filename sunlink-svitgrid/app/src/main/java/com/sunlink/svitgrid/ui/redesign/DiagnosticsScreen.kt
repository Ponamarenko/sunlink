package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.NetworkDiagnostics
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Діагностика — один hero-стан, згруповані панелі та тонкі рядки.
 * Сирі REST-помилки й provenance винесені в компактний технічний блок.
 * Усі стани темні (кореневий sunLinkDarkColors).
 */
@Composable
fun DiagnosticsScreen(vm: MainViewModel = viewModel()) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        DiagnosticsScreenContent(vm)
    }
}

@Composable
private fun DiagnosticsScreenContent(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val events by NetworkDiagnostics.events.collectAsState()
    val lastSuccess by NetworkDiagnostics.lastSuccessUtc.collectAsState()
    val quality by vm.telemetryQuality.collectAsState()
    val transport by vm.liveTransport.collectAsState()
    val full by vm.fullTelemetry.collectAsState()
    val restError by vm.fullTelemetryError.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadData()
        vm.loadFullTelemetry()
        vm.loadTelemetryQuality()
    }

    val success = uiState as? InverterUiState.Success
    val errorState = uiState as? InverterUiState.Error
    val online = success?.status?.online == true
    val stale = success?.stale == true || quality?.stale == true || full?.stale == true

    val errorEvents = events.filter { event ->
        event.errorType != null || (event.httpCode ?: 200) >= 400
    }
    val hasErrors = errorState != null ||
        !success?.status?.error.isNullOrBlank() ||
        errorEvents.isNotEmpty()

    val hasTelemetry = success != null || full != null
    val connectionFreshness = freshnessOf(
        stale = stale,
        quality = if (hasTelemetry) "verified" else "unavailable",
        confidence = if (hasTelemetry) "verified" else null,
        online = if (errorState != null) false else if (hasTelemetry) online else null
    )
    val freshnessColor = when (connectionFreshness) {
        Freshness.FRESH -> colors.success
        Freshness.STALE, Freshness.PROVISIONAL -> colors.warning
        Freshness.UNAVAILABLE -> colors.offline
        Freshness.OFFLINE -> colors.error
    }

    val statusLabel = when {
        hasErrors -> "Увага"
        online && !stale -> "Стабільно"
        stale -> "Застарілі дані"
        else -> "Недоступно"
    }
    val statusColor = when {
        hasErrors -> colors.error
        online && !stale -> colors.battery
        stale -> colors.warning
        else -> colors.offline
    }

    val known = quality?.known_parameters ?: 0
    val available = quality?.available_parameters ?: 0
    val channels = if (quality == null || known <= 0) "—" else "$available / $known каналів"
    val knownTile = quality?.known_parameters?.takeIf { it > 0 }?.toFloat()
    val availableTile = knownTile?.let { quality?.available_parameters?.toFloat() }

    val lastPacket = lastPacketLabel(
        lastSuccess ?: full?.timestamp ?: success?.status?.lastUpdate
    )
    val wsConnected = transport == TelemetryWebSocketClient.WsTransportState.OPEN
    val hasFreshTelemetry = !stale && (
        telemetryValue(full, "pv", "total_power") != null ||
            telemetryValue(full, "load", "power") != null
        )

    SunLinkScreen(
        title = "Діагностика",
        subtitle = "Звʼязок та якість телеметрії",
        refreshing = refreshing,
        onRefresh = {
            vm.loadData()
            vm.loadFullTelemetry()
            vm.loadTelemetryQuality()
        }
    ) {
        // Hero: єдиний блок статусу — Fresh / Stale / Unavailable / Offline.
        HeroPanel(accent = statusColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceLg)
            ) {
                Column(Modifier.weight(1f)) {
                    StatusPill(statusLabel, statusColor)
                    Spacer(Modifier.height(PremiumTokens.spaceSm))
                    Text(
                        freshnessLabel(connectionFreshness),
                        style = SunLinkTypography.headline,
                        color = colors.textPrimary
                    )
                    Text(
                        "Останній пакет: $lastPacket",
                        style = SunLinkTypography.body,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceSm))
                    DataFreshnessIndicator(
                        freshness = connectionFreshness,
                        timestamp = lastSuccess ?: full?.timestamp,
                        compact = true
                    )
                }
            }
        }

        // З'єднання: тонкі рядки замість окремої картки на кожне поле.
        DataPanel(
            title = "Зʼєднання з інвертором",
            subtitle = "Канал і опитування",
            trailing = {
                Text(
                    if (online) "Онлайн" else "Офлайн",
                    style = SunLinkTypography.caption,
                    color = if (online) colors.battery else colors.offline
                )
            }
        ) {
            InlineMetric(
                label = "Канал WebSocket",
                value = if (wsConnected) "Підключено" else transportLabel(transport),
                accent = if (wsConnected) colors.grid else colors.textTertiary
            )
            InlineMetric("Останній пакет", lastPacket, colors.textPrimary)
            InlineMetric("Інтервал опитування", "${vm.pollIntervalSeconds} сек", colors.textPrimary)
            InlineMetric("Актуальність даних", freshnessLabel(connectionFreshness), freshnessColor)
        }

        // Якість телеметрії: компактні плитки + рядки станів каналів.
        DataPanel(title = "Якість телеметрії", subtitle = channels) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)
            ) {
                StatTile(
                    label = "Відомо каналів",
                    value = knownTile,
                    unit = "каналів",
                    accent = colors.grid,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Доступно",
                    value = availableTile,
                    unit = "каналів",
                    accent = colors.battery,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            QualityRow(
                "PV1 + навантаження",
                qualityStatus(
                    listOf(
                        telemetryValue(full, "pv", "pv1_power"),
                        telemetryValue(full, "load", "power")
                    ),
                    "Недоступно", stale
                )
            )
            QualityRow(
                "Батарея (SOC, V, I)",
                qualityStatus(
                    listOf(
                        telemetryValue(full, "battery", "soc"),
                        telemetryValue(full, "battery", "voltage"),
                        telemetryValue(full, "battery", "current")
                    ),
                    "Недоступно", stale
                )
            )
            QualityRow(
                "Мережа / імпорт / експорт",
                qualityStatus(
                    listOf(
                        telemetryValue(full, "grid", "power"),
                        telemetryValue(full, "grid", "import_energy"),
                        telemetryValue(full, "grid", "export_energy")
                    ),
                    "Недоступно", stale
                )
            )
            QualityRow(
                "Датчик температури батареї",
                qualityStatus(
                    listOf(telemetryValue(full, "battery", "temperature")),
                    "Відсутній", stale
                )
            )
        }

        // Стан помилок: позитивне повідомлення — тільки якщо помилок немає.
        DataPanel(
            title = "Стан помилок",
            subtitle = if (hasErrors) "Зафіксовані збої та події" else "Активних збоїв немає"
        ) {
            if (!hasErrors) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.battery,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(PremiumTokens.spaceSm))
                    Text(
                        "Активних аварій або збоїв інвертора не зафіксовано",
                        style = SunLinkTypography.body,
                        color = colors.textPrimary
                    )
                }
            } else {
                val messages = buildList {
                    errorState?.message?.let { add(it) }
                    success?.status?.error?.let { add(it) }
                    errorEvents.take(5).forEach { event ->
                        add("${event.type} · ${event.stage} · ${event.errorType ?: "HTTP ${event.httpCode ?: "—"}"}")
                    }
                }.distinct()
                messages.forEach { message ->
                    Text("• $message", style = SunLinkTypography.caption, color = colors.error)
                }
            }
        }

        // Технічні деталі — компактна довідкова панель, не статусна картка.
        DataPanel(title = "Технічні деталі", subtitle = "REST / provenance / polling") {
            DiagRow("REST", telemetryErrorNote(restError, hasFreshTelemetry) ?: "—", colors.textSecondary)
            DiagRow("Provenance SOC", telemetryProvenance(telemetryValue(full, "battery", "soc")), colors.textSecondary)
            DiagRow("Provenance PV", telemetryProvenance(telemetryValue(full, "pv", "total_power")), colors.textSecondary)
            DiagRow("Provenance Load", telemetryProvenance(telemetryValue(full, "load", "power")), colors.textSecondary)
            DiagRow("Provenance Grid", telemetryProvenance(telemetryValue(full, "grid", "power")), colors.textSecondary)
            DiagRow("Транспорт", transportLabel(transport), colors.textSecondary)
            DiagRow(
                "Цикли / збої / пропуски",
                "${quality?.cycles ?: 0} / ${quality?.failed_cycles ?: 0} / ${quality?.missed_cycles ?: 0}",
                colors.textSecondary
            )
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String, valueColor: Color) {
    InlineMetric(label, value.ifBlank { "—" }, valueColor)
}

@Composable
private fun QualityRow(label: String, status: String) {
    val colors = LocalSunLinkColors.current
    InlineMetric(
        label = label,
        value = status,
        accent = if (status == "Підтверджено") colors.success else colors.textTertiary
    )
}

/** Статус групи каналів формується лише з реальних вимірювань. */
private fun qualityStatus(items: List<MeasurementValueDto?>, absentLabel: String, stale: Boolean): String {
    val present = items.filterNotNull()
    if (present.isEmpty()) return absentLabel
    val confirmed = present.count { isConfirmed(it, stale) }
    return when (confirmed) {
        present.size -> "Підтверджено"
        0 -> "Недоступно"
        else -> "Частково"
    }
}

private fun lastPacketLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val zoned = Instant.parse(iso).atZone(ZoneId.systemDefault())
        val time = DateTimeFormatter.ofPattern("HH:mm").format(zoned)
        if (zoned.toLocalDate() == LocalDate.now(ZoneId.systemDefault())) {
            "Сьогодні, $time"
        } else {
            DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("uk")).format(zoned)
        }
    } catch (e: Exception) {
        "—"
    }
}
