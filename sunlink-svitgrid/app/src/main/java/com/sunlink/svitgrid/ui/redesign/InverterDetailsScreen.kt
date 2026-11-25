package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
 * Прямий порт Stitch node_1334021055661215071: заголовок із назад, картка
 * паспорта пристрою, стан підключення, чесні заглушки ідентифікації та
 * окремий перехід до діагностики. Без узагальнених Hero/Stat/Data/Inline.
 */
@Composable
fun InverterDetailsScreen(
    vm: MainViewModel = viewModel(),
    onNavigate: (String) -> Unit = {}
) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        InverterDetailsScreenContent(vm, onNavigate)
    }
}

@Composable
private fun InverterDetailsScreenContent(vm: MainViewModel, onNavigate: (String) -> Unit) {
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
    val wsConnected = transport == TelemetryWebSocketClient.WsTransportState.OPEN
    val online = success?.status?.online == true || wsConnected || full?.connection_status == true
    val statusColor = if (online) colors.battery else colors.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InverterHeader(online = online, statusColor = statusColor, onBack = { onNavigate("more") })

        // 1. Виробник і модель
        InverterIdentityCard()

        // 2. Стан підключення
        InverterDetailCard {
            DetailLine("Тип підключення", "WebSocket · Локальний шлюз", colors.textPrimary, mono = false)
            DetailDivider()
            DetailLine(
                "Поточний стан",
                if (online) "Підключено" else "Відключено",
                statusColor
            )
            DetailDivider()
            DetailLine("Останній успішний обмін", lastExchangeLabel(lastSuccess), colors.textPrimary, mono = true)
        }

        // 3. Ідентифікація та прошивка — тільки чесні заглушки.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Ідентифікація та прошивка",
                style = SunLinkTypography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            InverterDetailCard(low = true) {
                DetailLine("Серійний номер", "— · Не передається", colors.textSecondary, mono = true)
                DetailDivider()
                DetailLine("Ідентифікатор логера", "— · Не передається", colors.textSecondary, mono = true)
                DetailDivider()
                DetailLine("Версія прошивки", "— · Недоступно", colors.textSecondary, mono = true)
            }
        }

        // 4. Перехід до діагностики
        InverterDiagnosticsLink(onClick = { onNavigate("diagnostics") })
    }
}

@Composable
private fun InverterHeader(online: Boolean, statusColor: Color, onBack: () -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surface, RoundedCornerShape(50))
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад до меню Більше",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Інвертор", style = SunLinkTypography.headline, color = colors.textPrimary)
            Text("Паспорт пристрою та звʼязок", style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        Row(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(50))
                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(6.dp).background(statusColor, RoundedCornerShape(50)))
            Text(
                if (online) "Онлайн" else "Офлайн",
                style = SunLinkTypography.caption,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun InverterIdentityCard() {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(28.dp))
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .padding(16.dp),
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
            Icon(Icons.Default.Memory, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Виробник: Deye", style = SunLinkTypography.caption, color = colors.textSecondary)
            Text("SUN-12K-SG02LP1-EU-AM3", style = SunLinkTypography.title, color = colors.textPrimary)
            Text("Гібридний сонячний інвертор", style = SunLinkTypography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun InverterDetailCard(low: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (low) colors.background else colors.surface, shape)
            .border(1.dp, colors.border.copy(alpha = if (low) 0.6f else 1f), shape)
            .clip(shape),
        content = content
    )
}

@Composable
private fun DetailLine(label: String, value: String, valueColor: Color, mono: Boolean = false) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = if (mono) SunLinkTypography.mono else SunLinkTypography.label, color = valueColor)
    }
}

@Composable
private fun DetailDivider() {
    val colors = LocalSunLinkColors.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.6f)))
}

@Composable
private fun InverterDiagnosticsLink(onClick: () -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Troubleshoot, contentDescription = null, tint = colors.battery, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Діагностика звʼязку", style = SunLinkTypography.body, color = colors.textPrimary)
            Text("Переглянути стан каналів і пакетів", style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
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
