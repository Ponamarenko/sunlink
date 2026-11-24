package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val BatterySocHistoryParam = "battery_soc"

/**
 * Батарея — темний екран стану накопичувача. Потужність і температура не
 * виводяться, якщо сервер не підтвердив значення: «—» + зрозумілий стан.
 */
@Composable
fun BatteryScreen(vm: MainViewModel = viewModel()) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        BatteryScreenContent(vm)
    }
}

@Composable
private fun BatteryScreenContent(vm: MainViewModel) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current
    val stale = full?.stale == true
    val hasFreshTelemetry = full != null && !stale

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor(BatterySocHistoryParam, "24h")
    }

    val socItem = telemetryValue(full, "battery", "soc")
    val voltageItem = telemetryValue(full, "battery", "voltage")
    val currentItem = telemetryValue(full, "battery", "current")
    val powerItem = telemetryValue(full, "battery", "power")
    val tempItem = telemetryValue(full, "battery", "temperature")

    val socConfirmed = isConfirmed(socItem, stale)
    val voltageConfirmed = isConfirmed(voltageItem, stale)
    val currentConfirmed = isConfirmed(currentItem, stale)
    val powerConfirmed = isConfirmed(powerItem, stale)
    val tempConfirmed = isConfirmed(tempItem, stale)

    val soc = telemetryNumber(full, "battery", "soc")?.takeIf { socConfirmed }
    val voltage = telemetryNumber(full, "battery", "voltage")?.takeIf { voltageConfirmed }
    val current = telemetryNumber(full, "battery", "current")?.takeIf { currentConfirmed }
    val power = telemetryNumber(full, "battery", "power")?.takeIf { powerConfirmed }
    val temperature = telemetryNumber(full, "battery", "temperature")?.takeIf { tempConfirmed }

    val freshness = freshnessOf(full?.stale, socItem?.quality, socItem?.confidence, full?.connection_status)
    val socValues = batteryChartValues(series[BatterySocHistoryParam])
    val presentSoc = socValues.filterNotNull()
    val minSoc = presentSoc.minOrNull()
    val maxSoc = presentSoc.maxOrNull()
    val socRange = if (minSoc != null && maxSoc != null)
        "Мін ${minSoc.toInt()}% · Макс ${maxSoc.toInt()}%"
    else "—"

    SunLinkScreen(
        title = "Батарея",
        subtitle = "Стан накопичувача · LiFePO4",
        refreshing = loading,
        onRefresh = {
            vm.loadFullTelemetry()
            vm.loadSeriesFor(BatterySocHistoryParam, "24h")
        }
    ) {
        DataFreshnessIndicator(freshness, full?.timestamp)
        telemetryErrorNote(error, hasFreshTelemetry)?.let {
            Text(it, color = if (hasFreshTelemetry) colors.warning else colors.error, style = SunLinkTypography.caption)
        }

        // Hero: SOC-ринг і підписи стану, похідні від реальних вимірювань.
        PremiumCard(accent = colors.battery) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceLg)
            ) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    BatterySocIndicator(soc, Modifier.size(112.dp), colors.battery)
                }
                Column(Modifier.weight(1f)) {
                    StatusPill(
                        text = if (socConfirmed) telemetryLabel(socItem) else "Немає даних",
                        color = if (socConfirmed) colors.success else colors.offline
                    )
                    Spacer(Modifier.height(PremiumTokens.spaceSm))
                    Text(socStateLabel(soc), style = SunLinkTypography.headline, color = colors.textPrimary)
                    Text(currentStateLabel(current), style = SunLinkTypography.body, color = colors.textSecondary)
                }
            }
        }

        // Електричні параметри: 2×2. Відсутні значення — «—», а не нулі.
        PremiumCard(accent = colors.battery) {
            PremiumSectionHeader("Електричні параметри", "Миттєві вимірювання")
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                MetricCard(
                    title = "Напруга", value = voltage, unit = "V", icon = null,
                    accent = colors.battery, modifier = Modifier.weight(1f),
                    caption = if (voltageConfirmed) telemetryLabel(voltageItem) else "Недоступно"
                )
                MetricCard(
                    title = "Струм", value = current, unit = "A", icon = null,
                    accent = colors.battery, modifier = Modifier.weight(1f),
                    caption = if (currentConfirmed) telemetryLabel(currentItem) else "Недоступно"
                )
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                MetricCard(
                    title = "Потужність", value = power, unit = "W", icon = null,
                    accent = colors.batteryCharge, modifier = Modifier.weight(1f),
                    caption = if (powerConfirmed) telemetryLabel(powerItem) else "Недоступно"
                )
                MetricCard(
                    title = "Температура", value = temperature, unit = "°C", icon = null,
                    accent = colors.warning, modifier = Modifier.weight(1f),
                    caption = if (tempConfirmed) telemetryLabel(tempItem) else "Датчик відсутній"
                )
            }
        }

        // Добова енергія — дані моделі інвертора.
        PremiumCard(accent = colors.batteryCharge) {
            PremiumSectionHeader("Енергія за добу", "Дані моделі інвертора")
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                DailyEnergyCell("Заряджено", telemetryNumber(full, "battery", "daily_charge_energy"), colors.batteryCharge, Modifier.weight(1f))
                DailyEnergyCell("Розряджено", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.batteryDischarge, Modifier.weight(1f))
            }
        }

        // Історія SOC за 24 год із чесними розривами.
        PremiumCard(accent = colors.battery) {
            PremiumSectionHeader("Історія SOC (24 год)", socRange)
            if (presentSoc.isEmpty()) {
                EmptyState("Немає даних")
            } else {
                AreaChart(
                    values = socValues,
                    color = colors.battery,
                    modifier = Modifier.fillMaxWidth().height(124.dp),
                    showGrid = true,
                    marker = true
                )
                Spacer(Modifier.height(PremiumTokens.spaceXs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(
                        hhmm(series[BatterySocHistoryParam]?.start) ?: "00:00",
                        "04:00",
                        "08:00",
                        hhmm(series[BatterySocHistoryParam]?.end) ?: "09:24"
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
private fun batteryChartValues(series: HistorySeriesDto?): List<Float?> =
    series?.points?.map { point ->
        if (point.gap || point.value == null) null else point.value.toFloat()
    } ?: emptyList()

private fun socStateLabel(soc: Float?): String {
    if (soc == null) return "Немає даних"
    return when {
        soc >= 95f -> "Повний заряд"
        soc <= 20f -> "Низький заряд"
        else -> "Рівень заряду в нормі"
    }
}

private fun currentStateLabel(current: Float?): String {
    if (current == null) return "Струм: —"
    return if (kotlin.math.abs(current) < 5f) "Підтримуючий струм у нормі"
    else "Струм: %.1f A".format(current)
}

@Composable
private fun DailyEnergyCell(label: String, value: Float?, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(modifier) {
        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        Text(
            text = value?.let { "%.1f kWh".format(it) } ?: "—",
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else color
        )
    }
}
