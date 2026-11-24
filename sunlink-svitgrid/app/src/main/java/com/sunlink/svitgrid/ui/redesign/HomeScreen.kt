package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Power
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel

/**
 * Головна — темний "Hybrid Premium" екран. Усі значення надходять лише з
 * реальної телеметрії (`full` / `series`); відсутні — «—» зі зрозумілим станом.
 */
@Composable
fun HomeScreen(vm: MainViewModel = viewModel(), onNavigate: (String) -> Unit = {}) {
    // Усі 12 екранів мають бути темними незалежно від системної теми.
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        HomeScreenContent(vm, onNavigate)
    }
}

@Composable
private fun HomeScreenContent(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val full by vm.fullTelemetry.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val state by vm.uiState.collectAsState()
    val transport by vm.liveTransport.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadData()
        vm.loadSeriesFor("pv_total_power")
        vm.loadSeriesFor("battery_soc")
    }

    val success = state as? InverterUiState.Success
    val stale = full?.stale == true || success?.stale == true
    val online = success?.status?.online == true
    val freshness = when {
        error != null -> Freshness.OFFLINE
        stale -> Freshness.STALE
        online && full != null -> Freshness.FRESH
        online -> Freshness.PROVISIONAL
        else -> Freshness.UNAVAILABLE
    }
    val headerStatus = when (freshness) {
        Freshness.FRESH -> stringResource(R.string.state_online)
        Freshness.STALE -> stringResource(R.string.state_stale)
        Freshness.PROVISIONAL -> stringResource(R.string.state_connecting)
        Freshness.OFFLINE -> stringResource(R.string.state_error)
        Freshness.UNAVAILABLE -> stringResource(R.string.state_offline)
    }

    val pvItem = telemetryValue(full, "pv", "total_power")
    val loadItem = telemetryValue(full, "load", "power")
    val gridItem = telemetryValue(full, "grid", "power")
    val batteryItem = telemetryValue(full, "battery", "power")
    val socItem = telemetryValue(full, "battery", "soc")

    val pvValue = pvItem?.normalized_value?.toFloat()
    val loadValue = displayLoadPower(telemetryNumber(full, "load", "power"))
    val gridValue = gridItem?.normalized_value?.toFloat()
    val batteryValue = batteryItem?.normalized_value?.toFloat()
    val soc = telemetryNumber(full, "battery", "soc")
    val voltage = telemetryNumber(full, "battery", "voltage")
    val current = telemetryNumber(full, "battery", "current")

    val pvConfirmed = isConfirmed(pvItem, stale)
    val loadConfirmed = isConfirmed(loadItem, stale)
    val gridConfirmed = isConfirmed(gridItem, stale)
    val batteryConfirmed = isConfirmed(batteryItem, stale)
    val socConfirmed = isConfirmed(socItem, stale)

    // Лише підтверджені значення керують напрямком потоків; батарея — UNKNOWN.
    val pvNode = FlowNode(pvValue.takeIf { pvConfirmed }, pvConfirmed, pvFlowDirection(pvValue, pvConfirmed))
    val loadNode = FlowNode(loadValue.takeIf { loadConfirmed }, loadConfirmed, loadFlowDirection(loadValue, loadConfirmed))
    val gridNode = FlowNode(gridValue.takeIf { gridConfirmed }, gridConfirmed, gridFlowDirection(gridValue, gridConfirmed))
    val batteryNode = FlowNode(batteryValue.takeIf { batteryConfirmed }, batteryConfirmed, batteryFlowDirection())

    SunLinkScreen(
        title = stringResource(R.string.nav_home),
        subtitle = "SunLink · Оновлено ${formatTelemetryTimestamp(full?.timestamp)}",
        refreshing = loading,
        onRefresh = { vm.loadFullTelemetry(); vm.loadData() }
    ) {
        // Статус-рядок: окремі пілюлі стану, свіжості та WebSocket.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            StatusPill(headerStatus, freshnessColor(freshness, colors), Modifier.weight(1f))
            DataFreshnessIndicator(freshness, full?.timestamp, Modifier.weight(1f), compact = true)
        }
        StatusPill(
            wsLabel(transport),
            if (transport == TelemetryWebSocketClient.WsTransportState.OPEN) colors.success else colors.warning
        )
        telemetryErrorNote(error, full != null && !stale)?.let {
            Text(it, color = if (full != null && !stale) colors.warning else colors.error, style = SunLinkTypography.caption)
        }

        // Схема потоків: Сонце (вгорі) / Батарея (ліворуч) / Мережа (праворуч) / Будинок (внизу).
        PremiumCard(modifier = Modifier.clickable { onNavigate("energy") }, accent = colors.primary) {
            PremiumSectionHeader(
                title = stringResource(R.string.energy_balance),
                subtitle = if (isBalanceComplete(listOf(pvValue, batteryValue, gridValue, loadValue)))
                    stringResource(R.string.flow_hint)
                else stringResource(R.string.balance_partial)
            )
            PremiumEnergyFlow(pvNode, batteryNode, gridNode, loadNode)
        }

        // Ключові показники: PV · SOC · Навантаження.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
            MetricCard(
                title = "Потужність PV",
                value = pvValue.takeIf { pvConfirmed },
                unit = "W",
                icon = Icons.Default.SolarPower,
                accent = colors.solar,
                modifier = Modifier.weight(1f).clickable { onNavigate("solar") },
                caption = telemetryLabel(pvItem),
                sparkline = series["pv_total_power"]?.points?.map { p -> if (p.gap) null else p.value?.toFloat() } ?: emptyList()
            )
            MetricCard(
                title = "Заряд SOC",
                value = soc.takeIf { socConfirmed },
                unit = "%",
                icon = Icons.Default.BatteryChargingFull,
                accent = colors.battery,
                modifier = Modifier.weight(1f).clickable { onNavigate("battery") },
                caption = voltageCurrentCaption(voltage, current),
                sparkline = series["battery_soc"]?.points?.map { p -> if (p.gap) null else p.value?.toFloat() } ?: emptyList()
            )
            MetricCard(
                title = "Навантаження",
                value = loadValue.takeIf { loadConfirmed },
                unit = "W",
                icon = Icons.Default.Power,
                accent = colors.load,
                modifier = Modifier.weight(1f).clickable { onNavigate("load") },
                caption = telemetryLabel(loadItem)
            )
        }

        // Підсумки за сьогодні — 6 окремих показників + посилання на Аналітику.
        PremiumCard(accent = colors.solar) {
            PremiumSectionHeader(
                title = "За сьогодні",
                trailing = {
                    Row(
                        modifier = Modifier.clickable { onNavigate("analytics") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Аналітика", style = SunLinkTypography.caption, color = colors.primary)
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    }
                }
            )
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                DailyCell("Сонце", telemetryNumber(full, "pv", "daily_energy"), colors.solar, Modifier.weight(1f))
                DailyCell("Імпорт", telemetryNumber(full, "grid", "daily_import_energy"), colors.gridImport, Modifier.weight(1f))
                DailyCell("Експорт", telemetryNumber(full, "grid", "daily_export_energy"), colors.gridExport, Modifier.weight(1f))
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                DailyCell("Навантаження", telemetryNumber(full, "load", "daily_energy"), colors.load, Modifier.weight(1f))
                DailyCell("Заряд", telemetryNumber(full, "battery", "daily_charge_energy"), colors.batteryCharge, Modifier.weight(1f))
                DailyCell("Розряд", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.batteryDischarge, Modifier.weight(1f))
            }
        }
    }
}

private fun freshnessColor(freshness: Freshness, colors: SunLinkColors): Color = when (freshness) {
    Freshness.FRESH -> colors.success
    Freshness.STALE -> colors.warning
    Freshness.PROVISIONAL -> colors.warning
    Freshness.OFFLINE -> colors.error
    Freshness.UNAVAILABLE -> colors.offline
}

private fun wsLabel(state: TelemetryWebSocketClient.WsTransportState): String = when (state) {
    TelemetryWebSocketClient.WsTransportState.OPEN -> "WebSocket: підключено"
    TelemetryWebSocketClient.WsTransportState.CONNECTING,
    TelemetryWebSocketClient.WsTransportState.RECONNECTING -> "WebSocket: підключення"
    TelemetryWebSocketClient.WsTransportState.CLOSED -> "WebSocket: відключено"
}

private fun voltageCurrentCaption(voltage: Float?, current: Float?): String {
    if (voltage == null && current == null) return "Немає даних"
    val v = voltage?.let { "%.1fV".format(it) } ?: "—"
    val a = current?.let { "%.1fA".format(it) } ?: "—"
    return "$v · $a"
}

@Composable
private fun DailyCell(label: String, value: Float?, color: Color, modifier: Modifier = Modifier) {
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
