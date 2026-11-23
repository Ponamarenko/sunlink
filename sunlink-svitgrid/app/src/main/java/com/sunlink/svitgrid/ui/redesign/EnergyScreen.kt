package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
fun EnergyScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val quality by vm.telemetryQuality.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current
    val stale = full?.stale == true || quality?.stale == true

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadTelemetryQuality()
        vm.loadSeriesFor("pv_total_power")
        vm.loadSeriesFor("load_power")
        vm.loadData()
    }

    val pv = telemetryValue(full, "pv", "total_power")
    val battery = telemetryValue(full, "battery", "power")
    val grid = telemetryValue(full, "grid", "power")
    val load = telemetryValue(full, "load", "power")

    SunLinkScreen(
        title = stringResource(R.string.nav_energy),
        subtitle = stringResource(R.string.energy_subtitle),
        refreshing = loading,
        onRefresh = { vm.loadFullTelemetry(); vm.loadTelemetryQuality() }
    ) {
        SunLinkCard(accent = colors.primary) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.energy_balance), style = SunLinkTypography.label, color = colors.textSecondary)
            }
            EnergyFlowCanvas(
                pv = pv?.normalized_value?.takeIf { isConfirmed(pv, stale) }?.toFloat(),
                battery = battery?.normalized_value?.takeIf { isConfirmed(battery, stale) }?.toFloat(),
                grid = grid?.normalized_value?.takeIf { isConfirmed(grid, stale) }?.toFloat(),
                load = load?.normalized_value?.takeIf { isConfirmed(load, stale) }?.toFloat(),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
            Text(stringResource(R.string.flow_hint), style = SunLinkTypography.caption, color = colors.textTertiary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusPill("PV ${wattText(pv, stale)}", colors.solar)
                StatusPill("Battery ${wattText(battery, stale)}", colors.battery)
                StatusPill("Grid ${wattText(grid, stale)}", colors.grid)
                StatusPill("Load ${wattText(load, stale)}", colors.load)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("PV", pv?.normalized_value?.toFloat(), "W", Icons.Default.SolarPower, colors.solar, Modifier.weight(1f), caption = telemetryLabel(pv))
            MetricCard("Load", load?.normalized_value?.toFloat(), "W", Icons.Default.Power, colors.load, Modifier.weight(1f), caption = telemetryLabel(load))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Battery", battery?.normalized_value?.toFloat(), "W", Icons.Default.BatteryChargingFull, colors.battery, Modifier.weight(1f), caption = telemetryLabel(battery))
            MetricCard("Grid", grid?.normalized_value?.toFloat(), "W", Icons.Default.Bolt, colors.grid, Modifier.weight(1f), caption = telemetryLabel(grid))
        }

        SunLinkCard(accent = colors.solar) {
            Text(stringResource(R.string.energy_today), style = SunLinkTypography.label, color = colors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Генерація", style = SunLinkTypography.caption, color = colors.textTertiary)
                Text(telemetryNumber(full, "pv", "daily_energy")?.let { "%.1f kWh".format(it) } ?: "—", style = SunLinkTypography.label, color = colors.solar)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Імпорт / Експорт", style = SunLinkTypography.caption, color = colors.textTertiary)
                Text(
                    listOfNotNull(
                        telemetryNumber(full, "grid", "daily_import_energy")?.let { "↓ %.1f".format(it) },
                        telemetryNumber(full, "grid", "daily_export_energy")?.let { "↑ %.1f".format(it) }
                    ).joinToString(" / ").ifBlank { "—" },
                    style = SunLinkTypography.label, color = colors.grid
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Заряд / Розряд", style = SunLinkTypography.caption, color = colors.textTertiary)
                Text(
                    listOfNotNull(
                        telemetryNumber(full, "battery", "daily_charge_energy")?.let { "+ %.1f".format(it) },
                        telemetryNumber(full, "battery", "daily_discharge_energy")?.let { "- %.1f".format(it) }
                    ).joinToString(" / ").ifBlank { "—" },
                    style = SunLinkTypography.label, color = colors.battery
                )
            }
        }

        if (stale) {
            StatusPill(stringResource(R.string.state_stale), colors.warning)
        }
        Text(stringResource(R.string.gap_notice), style = SunLinkTypography.caption, color = colors.textTertiary)
    }
}
