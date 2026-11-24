package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.NetworkDiagnostics
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Екран «Інвертор» (Stitch sunlink_ux_v2_11): паспорт пристрою, стан зв'язку
 * та чесні заглушки для даних, яких backend не передає.
 */
@Composable
fun InverterDetailsScreen(
    vm: MainViewModel = viewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val uiState by vm.uiState.collectAsState()
    val full by vm.fullTelemetry.collectAsState()
    val transport by vm.liveTransport.collectAsState()
    val lastSuccess by NetworkDiagnostics.lastSuccessUtc.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadData()
        vm.loadFullTelemetry()
    }

    val success = uiState as? InverterUiState.Success
    val online = success?.status?.online == true ||
        transport == TelemetryWebSocketClient.WsTransportState.OPEN
    val stale = full?.stale == true || success?.stale == true
    val freshness = freshnessOf(
        stale = stale,
        quality = if (online) "verified" else "unavailable",
        confidence = if (online) "verified" else null,
        online = online
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.background, colors.surface.copy(alpha = 0.65f)))
            )
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surface, RoundedCornerShape(50))
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .clickable { onNavigate("more") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад до меню Більше",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Інвертор", style = SunLinkTypography.headline, color = colors.textPrimary)
                Text(
                    "Паспорт пристрою та звʼязок",
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary
                )
            }
            StatusPill(
                text = if (online) "Онлайн" else "Офлайн",
                color = if (online) colors.battery else colors.error
            )
        }

        // 1. Виробник і модель.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.surfaceElevated, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Виробник: Deye",
                        style = SunLinkTypography.caption,
                        color = colors.textTertiary
                    )
                    Text(
                        "SUN-12K-SG02LP1-EU-AM3",
                        style = SunLinkTypography.title,
                        color = colors.textPrimary
                    )
                    Text(
                        "Гібридний сонячний інвертор",
                        style = SunLinkTypography.caption,
                        color = colors.textTertiary
                    )
                }
            }
        }

        // 2. Стан підключення.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            DetailRow(
                label = "Тип підключення",
                value = "WebSocket · Локальний шлюз",
                valueColor = colors.textPrimary
            )
            DetailDivider()
            DetailRow(
                label = "Поточний стан",
                value = if (online) "Підключено" else "Відключено",
                valueColor = if (online) colors.battery else colors.error,
                bold = true
            )
            DetailDivider()
            DetailRow(
                label = "Останній успішний обмін",
                value = lastExchangeLabel(lastSuccess),
                valueColor = colors.textPrimary,
                mono = true
            )
        }

        DataFreshnessIndicator(freshness, full?.timestamp, compact = true)

        // 3. Ідентифікація та прошивка — тільки чесні заглушки, без вигаданих серійних номерів.
        SectionCaption("Ідентифікація та прошивка")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            DetailRow("Серійний номер", "— · Не передається", colors.textTertiary, mono = true)
            DetailDivider()
            DetailRow("Ідентифікатор логера", "— · Не передається", colors.textTertiary, mono = true)
            DetailDivider()
            DetailRow("Версія прошивки", "— · Недоступно", colors.textTertiary, mono = true)
        }

        // 4. Перехід до діагностики.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .clickable { onNavigate("diagnostics") },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Troubleshoot,
                    contentDescription = null,
                    tint = colors.battery,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Діагностика звʼязку",
                        style = SunLinkTypography.body,
                        color = colors.textPrimary
                    )
                    Text(
                        "Переглянути стан каналів і пакетів",
                        style = SunLinkTypography.caption,
                        color = colors.textTertiary
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    val colors = LocalSunLinkColors.current
    Text(
        text,
        style = SunLinkTypography.caption,
        color = colors.textSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun DetailDivider() {
    val colors = LocalSunLinkColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border.copy(alpha = 0.6f))
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color,
    mono: Boolean = false,
    bold: Boolean = false
) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = SunLinkTypography.caption,
            color = colors.textTertiary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = if (mono) SunLinkTypography.mono else SunLinkTypography.caption,
            color = valueColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** Час останнього успішного обміну без вигадування дати. */
private fun lastExchangeLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val zoned = Instant.parse(iso).atZone(ZoneId.systemDefault())
        val time = DateTimeFormatter.ofPattern("HH:mm").format(zoned)
        if (zoned.toLocalDate() == LocalDate.now()) {
            "Сьогодні, $time"
        } else {
            DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()).format(zoned)
        }
    } catch (e: Exception) {
        formatTelemetryTimestamp(iso)
    }
}
