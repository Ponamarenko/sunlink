package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
 * Прямий порт Stitch node_375653363625559668: компактний заголовок, три
 * секції (зʼєднання, якість телеметрії, стан помилок) і технічний блок нижче.
 * Кожна секція має власні примітиви, а не узагальнені Hero/Stat/Data/Inline.
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

    val reload = {
        vm.loadData()
        vm.loadFullTelemetry()
        vm.loadTelemetryQuality()
    }

    val success = uiState as? InverterUiState.Success
    val errorState = uiState as? InverterUiState.Error
    val wsConnected = transport == TelemetryWebSocketClient.WsTransportState.OPEN
    val online = success?.status?.online == true || wsConnected
    val stale = success?.stale == true || quality?.stale == true || full?.stale == true
    val hasTelemetry = success != null || full != null

    val errorEvents = events.filter { event ->
        event.errorType != null || (event.httpCode ?: 200) >= 400
    }
    val hasErrors = errorState != null ||
        !success?.status?.error.isNullOrBlank() ||
        errorEvents.isNotEmpty()

    val freshness = freshnessOf(
        stale = stale,
        quality = if (hasTelemetry) "verified" else "unavailable",
        confidence = if (hasTelemetry) "verified" else null,
        online = if (errorState != null) false else if (hasTelemetry) online else null
    )
    val freshnessColor = when (freshness) {
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
        online && !stale -> colors.success
        stale -> colors.warning
        else -> colors.offline
    }

    val known = quality?.known_parameters ?: 0
    val available = quality?.available_parameters ?: 0
    val channels = if (quality == null || known <= 0) "—" else "$available / $known каналів"
    val lastPacket = lastPacketLabel(lastSuccess ?: full?.timestamp ?: success?.status?.lastUpdate)
    val hasFreshTelemetry = !stale && (
        telemetryValue(full, "pv", "total_power") != null ||
            telemetryValue(full, "load", "power") != null
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiagnosticsHeader(
            status = statusLabel,
            statusColor = statusColor,
            refreshing = refreshing,
            onRefresh = reload
        )

        // 1. Зʼєднання з інвертором
        DiagnosticsCard(
            title = "Зʼєднання з інвертором",
            trailing = if (online) "Онлайн" else "Офлайн",
            trailingColor = if (online) colors.battery else colors.offline,
            monoTrailing = true
        ) {
            DiagInfoRow(
                label = "Канал WebSocket",
                value = if (wsConnected) "Підключено" else transportLabel(transport),
                valueColor = if (wsConnected) colors.grid else colors.textSecondary
            )
            DiagInfoRow("Останній пакет", lastPacket, colors.textPrimary, mono = true)
            DiagInfoRow("Інтервал опитування", "${vm.pollIntervalSeconds} сек", colors.textPrimary, mono = true)
            DiagInfoRow("Актуальність даних", freshnessLabel(freshness), freshnessColor)
        }

        // 2. Якість телеметрії
        DiagnosticsCard(title = "Якість телеметрії", trailing = channels, trailingColor = colors.textSecondary, monoTrailing = true) {
            QualityBandRow(
                label = "Сонце (PV1) та навантаження",
                status = qualityStatus(
                    listOf(
                        telemetryValue(full, "pv", "pv1_power"),
                        telemetryValue(full, "load", "power")
                    ),
                    "Недоступно", stale
                )
            )
            QualityBandRow(
                label = "Батарея (SOC, напруга, струм)",
                status = qualityStatus(
                    listOf(
                        telemetryValue(full, "battery", "soc"),
                        telemetryValue(full, "battery", "voltage"),
                        telemetryValue(full, "battery", "current")
                    ),
                    "Недоступно", stale
                )
            )
            QualityBandRow(
                label = "Мережа та імпорт/експорт",
                status = qualityStatus(
                    listOf(
                        telemetryValue(full, "grid", "power"),
                        telemetryValue(full, "grid", "import_energy"),
                        telemetryValue(full, "grid", "export_energy")
                    ),
                    "Недоступно", stale
                )
            )
            QualityBandRow(
                label = "Датчик температури батареї",
                status = qualityStatus(
                    listOf(telemetryValue(full, "battery", "temperature")),
                    "Відсутній", stale
                )
            )
        }

        // 3. Стан помилок
        DiagnosticsCard(title = "Стан помилок") {
            if (!hasErrors) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.battery,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Активних аварій або збоїв інвертора не зафіксовано.",
                        style = SunLinkTypography.caption,
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
                    Text(message, style = SunLinkTypography.caption, color = colors.error)
                }
            }
        }

        // Компактна довідка: REST/provenance/поллінг без статусної картки.
        TechnicalBlock(
            rest = telemetryErrorNote(restError, hasFreshTelemetry) ?: "—",
            provenance = listOf(
                "SOC ${telemetryProvenance(telemetryValue(full, "battery", "soc"))}",
                "PV ${telemetryProvenance(telemetryValue(full, "pv", "total_power"))}",
                "Load ${telemetryProvenance(telemetryValue(full, "load", "power"))}",
                "Grid ${telemetryProvenance(telemetryValue(full, "grid", "power"))}"
            ).joinToString(" · "),
            transport = "${transportLabel(transport)} · ${quality?.cycles ?: 0}/${quality?.failed_cycles ?: 0}/${quality?.missed_cycles ?: 0}"
        )
    }
}

@Composable
private fun DiagnosticsHeader(status: String, statusColor: Color, refreshing: Boolean, onRefresh: () -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Діагностика", style = SunLinkTypography.headline, color = colors.textPrimary)
            Text("Звʼязок та якість телеметрії", style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        DiagnosticsStatusBadge(status, statusColor)
        if (refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).padding(start = 8.dp),
                strokeWidth = 2.dp,
                color = colors.primary
            )
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Оновити", tint = colors.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticsStatusBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, RoundedCornerShape(50)))
        Text(text, style = SunLinkTypography.caption, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DiagnosticsCard(
    title: String,
    trailing: String? = null,
    trailingColor: Color? = null,
    monoTrailing: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(20.dp))
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = SunLinkTypography.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
            if (trailing != null) {
                Text(
                    trailing,
                    style = if (monoTrailing) SunLinkTypography.mono else SunLinkTypography.caption,
                    color = trailingColor ?: colors.textSecondary
                )
            }
        }
        content()
    }
}

@Composable
private fun DiagInfoRow(label: String, value: String, valueColor: Color, mono: Boolean = false) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = if (mono) SunLinkTypography.mono else SunLinkTypography.label, color = valueColor)
    }
}

@Composable
private fun QualityBandRow(label: String, status: String) {
    val colors = LocalSunLinkColors.current
    val confirmed = status == "Підтверджено"
    val partial = status == "Частково"
    val shape = RoundedCornerShape(6.dp)
    val statusColor = when {
        confirmed -> colors.battery
        partial -> colors.warning
        else -> colors.textSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (confirmed) colors.surfaceElevated else colors.background, shape)
            .then(
                if (confirmed) Modifier
                else Modifier.border(1.dp, colors.border.copy(alpha = 0.6f), shape)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = SunLinkTypography.caption,
            color = if (confirmed) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(status, style = SunLinkTypography.label, color = statusColor)
    }
}

@Composable
private fun TechnicalBlock(rest: String, provenance: String, transport: String) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Технічні деталі · REST / provenance / polling", style = SunLinkTypography.caption, color = colors.textTertiary)
        TechnicalLine("REST", rest)
        TechnicalLine("Provenance", provenance)
        TechnicalLine("Транспорт / цикли", transport)
    }
}

@Composable
private fun TechnicalLine(label: String, value: String) {
    val colors = LocalSunLinkColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = SunLinkTypography.mono, color = colors.textTertiary)
        Text(value.ifBlank { "—" }, style = SunLinkTypography.mono, color = colors.textSecondary, modifier = Modifier.weight(1f))
    }
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
