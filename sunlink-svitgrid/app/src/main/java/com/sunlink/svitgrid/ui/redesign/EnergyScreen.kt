package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
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
 *
 * Композиція: один HeroPanel (баланс + схема потоків), далі DataPanel з рядками
 * потоків і DataPanel із добовою енергією та посиланням на Аналітику.
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
    val gridItem = telemetryValue(full, "grid", "power")
    val batteryItem = telemetryValue(full, "battery", "power")
    val batteryVoltage = telemetryNumber(full, "battery", "voltage")
    val batteryCurrent = telemetryNumber(full, "battery", "current")

    val pvConfirmed = isConfirmed(pv, stale)
    val loadConfirmed = isConfirmed(load, stale)
    val gridConfirmed = isConfirmed(gridItem, stale)
    val batteryConfirmed = isConfirmed(batteryItem, stale)

    // Лише підтверджені значення потрапляють у розрахунки та потоки.
    val pvValue = pv?.normalized_value?.takeIf { pvConfirmed }?.toFloat()
    val loadValue = displayLoadPower(telemetryNumber(full, "load", "power"))?.takeIf { loadConfirmed }
    val gridValue = gridItem?.normalized_value?.takeIf { gridConfirmed }?.toFloat()
    val batteryValue = batteryItem?.normalized_value?.takeIf { batteryConfirmed }?.toFloat()

    val pvNode = FlowNode(pvValue, pvConfirmed, pvFlowDirection(pvValue, pvConfirmed))
    val loadNode = FlowNode(loadValue, loadConfirmed, loadFlowDirection(loadValue, loadConfirmed))
    val gridNode = FlowNode(gridValue, gridConfirmed, gridFlowDirection(gridValue, gridConfirmed))
    val batteryNode = FlowNode(batteryValue, batteryConfirmed, batteryFlowDirection())

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

        // 1. Одна hero-панель: покриття навантаження + схема потоків із DEYE.
        HeroPanel(accent = colors.primary) {
            Column(verticalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                PremiumSectionHeader(
                    title = "Баланс і потоки",
                    subtitle = if (isBalanceComplete(listOf(pvValue, batteryValue, gridValue, loadValue)))
                        stringResource(R.string.flow_hint)
                    else stringResource(R.string.balance_partial)
                )
                PremiumEnergyFlow(pvNode, batteryNode, gridNode, loadNode)
                Coverage(pvValue, loadValue)
            }
        }

        // 2. Рядки потоків — одна згрупована панель, без окремих карток.
        DataPanel(title = "Потоки потужності") {
            FlowDetailRow(
                icon = Icons.Default.SolarPower, color = colors.solar,
                title = "Сонце → Інвертор",
                status = if (pvConfirmed) telemetryLabel(pv) else "Немає даних",
                statusOk = pvConfirmed,
                value = wattText(pvValue), valueColor = colors.solar
            )
            FlowDetailRow(
                icon = Icons.Default.Home, color = colors.load,
                title = "Інвертор → Будинок",
                status = if (loadConfirmed) telemetryLabel(load) else "Немає даних",
                statusOk = loadConfirmed,
                value = wattText(loadValue), valueColor = colors.load
            )
            FlowDetailRow(
                icon = Icons.Default.BatteryChargingFull, color = colors.battery,
                title = "Батарея ↔ Інвертор",
                status = "Напрямок не підтверджено",
                statusOk = false,
                value = voltageCurrentText(batteryVoltage, batteryCurrent), valueColor = colors.textPrimary
            )
            FlowDetailRow(
                icon = Icons.Default.GridOn, color = colors.grid,
                title = "Мережа ↔ Інвертор",
                status = "Телеметрія недоступна",
                statusOk = false,
                value = null, valueColor = colors.textTertiary
            )
        }

        // 3. Добова енергія + Аналітика (без дублювання компактних карток Home).
        DataPanel(
            title = "Енергія за добу",
            subtitle = "Сьогодні",
            trailing = {
                Row(
                    modifier = Modifier.clickable { onNavigate("analytics") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Аналітика", style = SunLinkTypography.caption, color = colors.primary)
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                }
            }
        ) {
            DailyMetric("Генерація", telemetryNumber(full, "pv", "daily_energy"), colors.solar)
            DailyMetric("Споживання", telemetryNumber(full, "load", "daily_energy"), colors.load)
            DailyMetric("Заряджено", telemetryNumber(full, "battery", "daily_charge_energy"), colors.batteryCharge)
            DailyMetric("Розряджено", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.batteryDischarge)
            DailyMetric("Імпорт з мережі", telemetryNumber(full, "grid", "daily_import_energy"), colors.gridImport)
            DailyMetric("Експорт у мережу", telemetryNumber(full, "grid", "daily_export_energy"), colors.gridExport)
        }
    }
}

@Composable
private fun Coverage(pvValue: Float?, loadValue: Float?) {
    val colors = LocalSunLinkColors.current
    if (pvValue == null || loadValue == null || loadValue <= 0f) {
        EmptyState("Немає підтверджених даних для балансу")
        return
    }
    val fraction = (pvValue / loadValue).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()
    val other = loadValue - pvValue
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(colors.surfaceElevated, RoundedCornerShape(50))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .background(colors.solar, RoundedCornerShape(50))
        )
    }
    InlineMetric("Від сонця ($percent%)", "${pvValue.toInt()} W", colors.solar)
    InlineMetric(
        "Інші джерела",
        if (other >= 0f) "${other.toInt()} W" else "—",
        if (other >= 0f) colors.battery else null
    )
}

private fun wattText(value: Float?): String = value?.let { "${it.toInt()} W" } ?: "—"

private fun kwhText(value: Float?): String = value?.let { "%.1f kWh".format(it) } ?: "—"

private fun voltageCurrentText(voltage: Float?, current: Float?): String {
    if (voltage == null && current == null) return "—"
    val v = voltage?.let { "%.1f".format(it) } ?: "—"
    val a = current?.let { "%.1f".format(it) } ?: "—"
    return "$v V · $a A"
}

@Composable
private fun DailyMetric(label: String, value: Float?, color: Color) {
    InlineMetric(label, kwhText(value), if (value == null) null else color)
}

@Composable
private fun FlowDetailRow(
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
        androidx.compose.foundation.layout.Spacer(Modifier.size(PremiumTokens.spaceSm))
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
