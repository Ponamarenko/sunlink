package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

private fun wattText(item: MeasurementValueDto?, stale: Boolean): String {
    if (!isConfirmed(item, stale)) return "—"
    val v = item?.normalized_value ?: return "—"
    return "${v.toInt()} W"
}

@Composable
fun EnergyScreen(vm: MainViewModel = viewModel(), onNavigate: (String) -> Unit = {}) {
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
    val battery = telemetryValue(full, "battery", "power")
    val grid = telemetryValue(full, "grid", "power")
    val load = telemetryValue(full, "load", "power")

    val pvConfirmed = isConfirmed(pv, stale)
    val batteryConfirmed = isConfirmed(battery, stale)
    val gridConfirmed = isConfirmed(grid, stale)
    val loadConfirmed = isConfirmed(load, stale)

    // Лише підтверджені значення потрапляють у потоки — жодних демонстраційних значень.
    val pvValue = pv?.normalized_value?.takeIf { pvConfirmed }?.toFloat()
    val batteryValue = battery?.normalized_value?.takeIf { batteryConfirmed }?.toFloat()
    val gridValue = grid?.normalized_value?.takeIf { gridConfirmed }?.toFloat()
    val loadValue = load?.normalized_value?.takeIf { loadConfirmed }?.toFloat()

    val freshness = freshnessOf(
        stale = stale,
        quality = pv?.quality,
        confidence = pv?.confidence,
        online = full?.connection_status
    )

    SunLinkScreen(
        title = stringResource(R.string.nav_energy),
        subtitle = stringResource(R.string.energy_subtitle),
        refreshing = loading,
        onRefresh = { vm.loadFullTelemetry(); vm.loadTelemetryQuality() }
    ) {
        PremiumCard(accent = colors.primary) {
            PremiumSectionHeader(
                title = stringResource(R.string.energy_balance),
                trailing = { DataFreshnessIndicator(freshness = freshness, timestamp = full?.timestamp) }
            )
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            PremiumEnergyFlow(
                pv = FlowNode(pvValue, pvConfirmed, pvFlowDirection(pvValue, pvConfirmed)),
                // Напрямок батареї сервер не підтверджує (конфлікт масштабу reg. 190).
                battery = FlowNode(batteryValue, batteryConfirmed, batteryFlowDirection()),
                grid = FlowNode(gridValue, gridConfirmed, gridFlowDirection(gridValue, gridConfirmed)),
                load = FlowNode(loadValue, loadConfirmed, loadFlowDirection(loadValue, loadConfirmed)),
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.flow_hint), style = SunLinkTypography.caption, color = colors.textTertiary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusPill("PV ${wattText(pv, stale)}", colors.solar)
                StatusPill("Battery ${wattText(battery, stale)}", colors.battery)
                StatusPill("Grid ${wattText(grid, stale)}", colors.grid)
                StatusPill("Load ${wattText(load, stale)}", colors.load)
            }
        }

        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader(title = stringResource(R.string.energy_today))
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            EnergyBalanceRow("Генерація (PV)", telemetryNumber(full, "pv", "daily_energy"), colors.solar)
            EnergyBalanceRow("Споживання (Load)", telemetryNumber(full, "load", "daily_energy"), colors.load)
            EnergyBalanceRow("Імпорт", telemetryNumber(full, "grid", "daily_import_energy"), colors.gridImport)
            EnergyBalanceRow("Експорт", telemetryNumber(full, "grid", "daily_export_energy"), colors.gridExport)
            EnergyBalanceRow("Заряд батареї", telemetryNumber(full, "battery", "daily_charge_energy"), colors.batteryCharge)
            EnergyBalanceRow("Розряд батареї", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.batteryDischarge)
        }

        // Детальні потужні значення вже є на Home та в чіпах Energy Flow —
        // тут не дублюємо їх великими картками.
        PremiumCard(
            modifier = Modifier.clickable { onNavigate("analytics") },
            accent = colors.primary
        ) {
            PremiumSectionHeader(
                title = "Історія та статистика",
                subtitle = "Daily / Weekly / Monthly / Yearly"
            )
        }
    }
}

@Composable
private fun EnergyBalanceRow(label: String, value: Float?, color: Color) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        Text(
            text = value?.let { "%.1f kWh".format(it) } ?: "—",
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else color
        )
    }
}
