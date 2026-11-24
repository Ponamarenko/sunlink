package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Екран «Інвертор» (Stitch sunlink_ux_v2_11): hero-паспорт пристрою,
 * згрупований стан звʼязку та чесні заглушки для даних, яких backend не передає.
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
    val online = success?.status?.online == true ||
        transport == TelemetryWebSocketClient.WsTransportState.OPEN
    val stale = full?.stale == true || success?.stale == true
    val freshness = freshnessOf(
        stale = stale,
        quality = if (online) "verified" else "unavailable",
        confidence = if (online) "verified" else null,
        online = online
    )

    SunLinkScreen(
        title = "Інвертор",
        subtitle = "Паспорт пристрою та звʼязок"
    ) {
        // Hero: ідентифікація пристрою, стан каналу та повернення в меню.
        HeroPanel(accent = if (online) colors.battery else colors.error) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceElevated, RoundedCornerShape(50))
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
                Spacer(Modifier.size(PremiumTokens.spaceMd))
                Column(Modifier.weight(1f)) {
                    SectionCaption("Паспорт пристрою")
                    Text("Виробник: Deye", style = SunLinkTypography.caption, color = colors.textTertiary)
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
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(PremiumTokens.spaceMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill(
                    text = if (online) "Онлайн" else "Офлайн",
                    color = if (online) colors.battery else colors.error
                )
                DataFreshnessIndicator(freshness, full?.timestamp, compact = true)
            }
        }

        // Стан підключення: тонкі рядки в одній панелі.
        DataPanel(title = "Стан підключення", subtitle = "Локальний шлюз") {
            InlineMetric("Тип підключення", "WebSocket · Локальний шлюз", colors.textPrimary)
            DetailDivider()
            InlineMetric(
                "Поточний стан",
                if (online) "Підключено" else "Відключено",
                if (online) colors.battery else colors.error
            )
            DetailDivider()
            InlineMetric("Останній успішний обмін", lastExchangeLabel(lastSuccess), colors.textPrimary)
        }

        // Ідентифікація та прошивка — тільки чесні заглушки, без вигаданих номерів.
        DataPanel(title = "Ідентифікація та прошивка", subtitle = "Поля не передаються backend") {
            DetailRow("Серійний номер", "— · Не передається", colors.textTertiary, mono = true)
            DetailDivider()
            DetailRow("Ідентифікатор логера", "— · Не передається", colors.textTertiary, mono = true)
            DetailDivider()
            DetailRow("Версія прошивки", "— · Недоступно", colors.textTertiary, mono = true)
        }

        // Перехід до діагностики — окрема дія, не ще одна одинакова картка.
        DataPanel(
            title = "Діагностика звʼязку",
            subtitle = "Стан каналів і пакетів",
            modifier = Modifier.clickable { onNavigate("diagnostics") },
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)
                ) {
                    Icon(
                        Icons.Default.Troubleshoot,
                        contentDescription = null,
                        tint = colors.battery,
                        modifier = Modifier.size(20.dp)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        ) {
            InlineMetric("Дія", "Відкрити діагностику", colors.battery)
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    val colors = LocalSunLinkColors.current
    Text(
        text,
        style = SunLinkTypography.caption,
        color = colors.primary,
        fontWeight = FontWeight.SemiBold
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
            .padding(vertical = PremiumTokens.spaceSm),
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
