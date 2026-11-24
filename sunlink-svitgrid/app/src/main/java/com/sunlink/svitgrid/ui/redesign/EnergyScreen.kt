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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SolarPower
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel

/**
 * Енергія — темний екран балансу. «Інші джерела» обчислюється лише тоді, коли
 * і PV, і навантаження підтверджені; інакше — «—». Напрямок батареї невідомий.
 */
@Composable
fun EnergyScreen(vm: MainViewModel = viewModel(), onNavigate: (String) -> Unit = {}) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        EnergyScreenContent(vm, onNavigate)
    }
}

@Composable
private fun EnergyScreenContent(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val full by vm.fullTelemetry.collectAsState()
    val quality by vm.telemetryQuality.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current
    val stale = full?.stale == true || quality?.stale == true

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadTelemetryQuality()
        vm.loadData()
    }

    val pv = telemetryValue(full, "pv", "total_power")
    val load = telemetryValue(full, "load", "power")
    val batteryVoltage = telemetryNumber(full, "battery", "voltage")
    val batteryCurrent = telemetryNumber(full, "battery", "current")

    val pvConfirmed = isConfirmed(pv, stale)
    val loadConfirmed = isConfirmed(load, stale)

    // Лише підтверджені значення потрапляють у розрахунки та потоки.
    val pvValue = pv?.normalized_value?.takeIf { pvConfirmed }?.toFloat()
    val loadValue = displayLoadPower(telemetryNumber(full, "load", "power"))?.takeIf { loadConfirmed }

    val freshness = freshnessOf(
        stale = stale,
        quality = pv?.quality,
        confidence = pv?.confidence,
        online = full?.connection_status
    )

    SunLinkScreen(
        title = stringResource(R.string.nav_energy),
        subtitle = "Потоки потужності · Оновлено ${formatTelemetryTimestamp(full?.timestamp)}",
        refreshing = loading,
        onRefresh = { vm.loadFullTelemetry(); vm.loadTelemetryQuality() }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            StatusPill("Баланс", colors.primary, Modifier.weight(1f))
            DataFreshnessIndicator(freshness, full?.timestamp, Modifier.weight(1f), compact = true)
        }

        // 1. Покриття навантаження.
        PremiumCard(accent = colors.load) {
            PremiumSectionHeader(
                title = "Покриття навантаження",
                trailing = {
                    Text(
                        text = loadValue?.let { "${it.toInt()} W зараз" } ?: "—",
                        style = SunLinkTypography.caption,
                        color = colors.textTertiary
                    )
                }
            )
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            if (pvValue != null && loadValue != null && loadValue > 0f) {
                val solarFraction = (pvValue / loadValue).coerceIn(0f, 1f)
                val percent = (solarFraction * 100).toInt()
                val other = loadValue - pvValue
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(colors.surfaceElevated, PremiumTokens.pill)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(solarFraction)
                            .height(12.dp)
                            .background(colors.solar, PremiumTokens.pill)
                    )
                }
                Spacer(Modifier.height(PremiumTokens.spaceMd))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                    LegendItem("Від сонця ($percent%)", pvValue, colors.solar, Modifier.weight(1f))
                    LegendItem("Інші джерела", if (other >= 0f) other else null, colors.battery, Modifier.weight(1f))
                }
            } else {
                EmptyState("Немає підтверджених даних для балансу")
            }
        }

        // 2. Потоки потужності (напрямки — лише підтверджені).
        PremiumCard(accent = colors.primary) {
            PremiumSectionHeader("Потоки потужності")
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            FlowRow(
                icon = Icons.Default.SolarPower, color = colors.solar,
                title = "Сонце → Інвертор",
                status = if (pvConfirmed) telemetryLabel(pv) else "Немає даних",
                statusOk = pvConfirmed,
                value = wattText(pvValue), valueColor = colors.solar
            )
            FlowRow(
                icon = Icons.Default.Home, color = colors.load,
                title = "Інвертор → Будинок",
                status = if (loadConfirmed) telemetryLabel(load) else "Немає даних",
                statusOk = loadConfirmed,
                value = wattText(loadValue), valueColor = colors.load
            )
            FlowRow(
                icon = Icons.Default.BatteryChargingFull, color = colors.battery,
                title = "Батарея ↔ Інвертор",
                status = "Напрямок не підтверджено",
                statusOk = false,
                value = voltageCurrentText(batteryVoltage, batteryCurrent), valueColor = colors.textPrimary
            )
            FlowRow(
                icon = Icons.Default.GridOn, color = colors.grid,
                title = "Мережа ↔ Інвертор",
                status = "Телеметрія недоступна",
                statusOk = false,
                value = null, valueColor = colors.textTertiary
            )
        }

        // 3. Енергія за добу — шість окремих рядків.
        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader("Енергія за добу", "Сьогодні")
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            EnergyBalanceRow("Генерація", telemetryNumber(full, "pv", "daily_energy"), colors.solar)
            EnergyBalanceRow("Споживання", telemetryNumber(full, "load", "daily_energy"), colors.load)
            EnergyBalanceRow("Заряджено", telemetryNumber(full, "battery", "daily_charge_energy"), colors.batteryCharge)
            EnergyBalanceRow("Розряджено", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.batteryDischarge)
            EnergyBalanceRow("Імпорт з мережі", telemetryNumber(full, "grid", "daily_import_energy"), colors.gridImport)
            EnergyBalanceRow("Експорт у мережу", telemetryNumber(full, "grid", "daily_export_energy"), colors.gridExport)
        }

        // Детальна історія доступна в Аналітиці (без дублювання потоків тут).
        PremiumCard(modifier = Modifier.clickable { onNavigate("analytics") }, accent = colors.primary) {
            PremiumSectionHeader("Історія та статистика", "Daily / Weekly / Monthly / Yearly")
        }
    }
}

private fun wattText(value: Float?): String = value?.let { "${it.toInt()} W" } ?: "—"

private fun voltageCurrentText(voltage: Float?, current: Float?): String {
    if (voltage == null && current == null) return "—"
    val v = voltage?.let { "%.1f".format(it) } ?: "—"
    val a = current?.let { "%.1f".format(it) } ?: "—"
    return "$v V · $a A"
}

@Composable
private fun LegendItem(label: String, value: Float?, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Column {
            Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
            Text(
                text = value?.let { "${it.toInt()} W" } ?: "—",
                style = SunLinkTypography.title,
                color = if (value == null) colors.textTertiary else color
            )
        }
    }
}

@Composable
private fun FlowRow(
    icon: ImageVector,
    color: Color,
    title: String,
    status: String,
    statusOk: Boolean,
    value: String?,
    valueColor: Color
) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = PremiumTokens.spaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(PremiumTokens.spaceSm))
        Column(Modifier.weight(1f)) {
            Text(title, style = SunLinkTypography.body, color = colors.textPrimary)
            Text(
                status,
                style = SunLinkTypography.caption,
                color = if (statusOk) colors.success else colors.textTertiary
            )
        }
        Text(
            text = value ?: "—",
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else valueColor
        )
    }
}

@Composable
private fun EnergyBalanceRow(label: String, value: Float?, color: Color) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SunLinkTypography.body, color = colors.textSecondary)
        Text(
            text = value?.let { "%.1f kWh".format(it) } ?: "—",
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else color
        )
    }
}
