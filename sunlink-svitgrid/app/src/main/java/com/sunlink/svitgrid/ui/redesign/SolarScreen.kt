package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SolarPower
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
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SolarPowerHistoryParam = "pv_total_power"

/**
 * Сонце — темний екран генерації. Стани PV1–PV3 похідні від наявності та
 * підтвердженості вимірювань; відсутні значення ніколи не підміняються нулем.
 */
@Composable
fun SolarScreen(vm: MainViewModel = viewModel()) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        SolarScreenContent(vm)
    }
}

@Composable
private fun SolarScreenContent(vm: MainViewModel) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current
    val stale = full?.stale == true
    val hasFreshTelemetry = full != null && !stale

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(SolarPowerHistoryParam, "24h")
    }

    val totalItem = telemetryValue(full, "pv", "total_power")
    val totalConfirmed = isConfirmed(totalItem, stale)
    val totalPower = telemetryNumber(full, "pv", "total_power")?.takeIf { totalConfirmed }
    val freshness = freshnessOf(stale, totalItem?.quality, totalItem?.confidence, full?.connection_status)

    val chartValues = solarChartValues(series[SolarPowerHistoryParam])
    val peak = chartValues.filterNotNull().maxOrNull()
    val dailyEnergy = telemetryNumber(full, "pv", "daily_energy")

    SunLinkScreen(
        title = "Сонце",
        subtitle = "Входи PV1–PV3 · Оновлено ${formatTelemetryTimestamp(full?.timestamp)}",
        refreshing = loading,
        onRefresh = {
            vm.loadFullTelemetry()
            vm.loadSeriesFor(SolarPowerHistoryParam, "24h")
        }
    ) {
        DataFreshnessIndicator(freshness, full?.timestamp)
        StatusPill("Генерація", colors.solar)
        telemetryErrorNote(error, hasFreshTelemetry)?.let {
            Text(it, color = if (hasFreshTelemetry) colors.warning else colors.error, style = SunLinkTypography.caption)
        }

        // Hero: загальна потужність PV із реальним gap-aware спарклайном.
        MetricCard(
            title = "Загальна генерація PV",
            value = totalPower,
            unit = "W",
            icon = Icons.Default.SolarPower,
            accent = colors.solar,
            modifier = Modifier.fillMaxWidth(),
            caption = telemetryLabel(totalItem),
            sparkline = chartValues,
            emphasize = true
        )

        // Входи PV1–PV3: кожен окремо, без об'єднання невідомих значень.
        PvInputCard("Вхід PV1", "pv1", full, stale, colors)
        PvInputCard("Вхід PV2", "pv2", full, stale, colors)
        PvInputCard("Вхід PV3", "pv3", full, stale, colors)

        // Генерація сьогодні: добова енергія та пік — з реальних даних/ряду.
        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader(
                title = "Генерація сьогодні",
                trailing = {
                    Text(
                        text = totalPower?.let { "${it.toInt()} W" } ?: "—",
                        style = SunLinkTypography.caption,
                        color = if (totalPower == null) colors.textTertiary else colors.solar
                    )
                }
            )
            Text(
                text = "За добу: ${dailyEnergy?.let { "%.1f kWh".format(it) } ?: "—"} · " +
                    "Пік ${peak?.let { "${it.toInt()} W" } ?: "—"}",
                style = SunLinkTypography.body,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            if (chartValues.all { it == null }) {
                EmptyState("Немає даних")
            } else {
                AreaChart(
                    values = chartValues,
                    color = colors.solar,
                    modifier = Modifier.fillMaxWidth().height(124.dp),
                    showGrid = true,
                    marker = true
                )
                Spacer(Modifier.height(PremiumTokens.spaceXs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(
                        hhmm(series[SolarPowerHistoryParam]?.start) ?: "05:00",
                        "07:00",
                        "08:30",
                        hhmm(series[SolarPowerHistoryParam]?.end) ?: "09:24"
                    ).forEach { label ->
                        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
                    }
                }
            }
        }
    }
}

/** Часова підпис осі з реального ISO-timestamp; Stitch-значення — лише fallback. */
private fun hhmm(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        null
    }
}

/** gap == true або value == null перетворюється на null — лінія розривається. */
private fun solarChartValues(series: HistorySeriesDto?): List<Float?> =
    series?.points?.map { point ->
        if (point.gap || point.value == null) null else point.value.toFloat()
    } ?: emptyList()

@Composable
private fun PvInputCard(
    title: String,
    prefix: String,
    full: TelemetryFullDto?,
    stale: Boolean,
    colors: SunLinkColors
) {
    val powerItem = telemetryValue(full, "pv", "${prefix}_power")
    val voltageItem = telemetryValue(full, "pv", "${prefix}_voltage")
    val currentItem = telemetryValue(full, "pv", "${prefix}_current")

    val confirmed = isConfirmed(powerItem, stale)
    val hasData = powerItem != null || voltageItem != null || currentItem != null
    val status = when {
        confirmed && (powerItem?.normalized_value ?: 0.0) > 1.0 -> "Підтверджено"
        hasData -> "Очікування"
        else -> "Недоступно"
    }
    val accent = if (status == "Підтверджено") colors.solar else colors.offline
    val valueColor = if (status == "Підтверджено") colors.solar else colors.textTertiary

    val power = if (status == "Підтверджено") telemetryNumber(full, "pv", "${prefix}_power") else null
    val voltage = if (status == "Підтверджено") telemetryNumber(full, "pv", "${prefix}_voltage") else null
    val current = if (status == "Підтверджено") telemetryNumber(full, "pv", "${prefix}_current") else null

    PremiumCard(accent = accent) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = SunLinkTypography.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
            StatusPill(
                text = status,
                color = when (status) {
                    "Підтверджено" -> colors.success
                    "Очікування" -> colors.warning
                    else -> colors.offline
                }
            )
        }
        Spacer(Modifier.height(PremiumTokens.spaceSm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            PvValueCell("Потужність", power?.let { "%.0f W".format(it) }, valueColor, Modifier.weight(1f))
            PvValueCell("Напруга", voltage?.let { "%.0f V".format(it) }, colors.textPrimary, Modifier.weight(1f))
            PvValueCell("Струм", current?.let { "%.1f A".format(it) }, colors.textPrimary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PvValueCell(label: String, value: String?, valueColor: Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(modifier) {
        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        Text(
            text = value ?: "—",
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else valueColor
        )
    }
}
