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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel

// Stitch dark surface tokens that have no semantic alias in SunLinkColors.
private val EnergySurfaceLow = Color(0xFF0B111E)   // dark-surface-container-low
private val EnergySurfaceHigh = Color(0xFF182235)  // dark-surface-container-high
private val EnergySolarContainer = Color(0xFF3A2D0C)
private val EnergyBatteryContainer = Color(0xFF093524)
private val EnergyLoadContainer = Color(0xFF3B2408)

private val EnergyTitleMd = SunLinkTypography.title.copy(fontFeatureSettings = "tnum") // 16/22 600
private val EnergyBodySm = SunLinkTypography.body.copy(fontSize = 12.sp, lineHeight = 16.sp)
private val EnergyLabelSm = SunLinkTypography.label.copy(fontSize = 11.sp, lineHeight = 14.sp)
private val EnergyMonoSm = SunLinkTypography.mono.copy(fontSize = 11.sp, lineHeight = 14.sp)

/**
 * Енергія — прямий порт Stitch-екрану «Енергія»: компактний заголовок із пілюлею
 * «Баланс», пропорційна смуга покриття навантаження, чотири окремі рядки потоків
 * і сітка 2×3 добових підсумків. Усі значення — лише підтверджена телеметрія;
 * відсутні значення залишаються «—».
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

    val pvItem = telemetryValue(full, "pv", "total_power")
    val loadItem = telemetryValue(full, "load", "power")
    val pvValue = pvItem?.normalized_value?.takeIf { isConfirmed(pvItem, stale) }?.toFloat()
    val loadValue = displayLoadPower(telemetryNumber(full, "load", "power"))
        ?.takeIf { isConfirmed(loadItem, stale) }
    val batteryVoltage = telemetryNumber(full, "battery", "voltage")
    val batteryCurrent = telemetryNumber(full, "battery", "current")

    val solarPct = if (pvValue != null && loadValue != null && loadValue > 0f)
        ((pvValue / loadValue) * 100f).toInt().coerceIn(0, 100)
    else null
    val otherValue = if (pvValue != null && loadValue != null && loadValue > pvValue)
        loadValue - pvValue
    else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EnergyHeader(full?.timestamp, loading, onNavigate)
        CoverageCard(solarPct, pvValue, otherValue, loadValue)
        FlowsCard(pvItem, loadItem, stale, pvValue, loadValue, batteryVoltage, batteryCurrent)
        DailyEnergyCard(full, onNavigate)
    }
}

/** Compact Header (Root Tab): icon tile + title + status badge + analytics action. */
@Composable
private fun EnergyHeader(timestamp: String?, loading: Boolean, onNavigate: (String) -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.nav_energy), style = SunLinkTypography.headline, color = colors.textPrimary)
                    Row(
                        modifier = Modifier
                            .background(EnergyBatteryContainer, RoundedCornerShape(50))
                            .border(1.dp, colors.battery.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(6.dp).background(colors.battery, CircleShape))
                        Text("Баланс", style = EnergyLabelSm, color = colors.battery)
                    }
                }
                Text(
                    "Потоки потужності · Оновлено ${formatTelemetryTimestamp(timestamp)}",
                    style = EnergyBodySm,
                    color = colors.textSecondary
                )
            }
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
                .clickable { onNavigate("analytics") },
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
            } else {
                Icon(Icons.Default.Bolt, contentDescription = "Аналітика енергії", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Live Energy Balance Summary: rounded-xl surface, proportional bar, 2-column legend. */
@Composable
private fun CoverageCard(solarPct: Int?, pvValue: Float?, otherValue: Float?, loadValue: Float?) {
    val colors = LocalSunLinkColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Покриття навантаження", style = SunLinkTypography.title, color = colors.textPrimary)
            Text(
                if (loadValue == null) "—" else "${loadValue.toInt()} W зараз",
                style = EnergyMonoSm,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.height(12.dp))

        // h-3 track, p-0.5, gap-0.5; solar fills the left share, the rest is reserve.
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(EnergySurfaceHigh)
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .padding(2.dp)
        ) {
            if (solarPct != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        Modifier
                            .weight(solarPct.coerceIn(1, 99).toFloat())
                            .height(8.dp)
                            .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                            .background(colors.solar)
                    )
                    Box(
                        Modifier
                            .weight((100 - solarPct).coerceIn(1, 99).toFloat())
                            .height(8.dp)
                            .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                            .background(colors.battery.copy(alpha = 0.6f))
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoverageLegend(
                dotColor = colors.solar,
                label = if (solarPct == null) "Від сонця" else "Від сонця ($solarPct%)",
                value = pvValue,
                valueColor = colors.solar,
                modifier = Modifier.weight(1f)
            )
            CoverageLegend(
                dotColor = colors.battery.copy(alpha = 0.7f),
                label = "Інші джерела",
                value = otherValue,
                valueColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoverageLegend(dotColor: Color, label: String, value: Float?, valueColor: Color, modifier: Modifier) {
    val colors = LocalSunLinkColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(dotColor, CircleShape))
        Column {
            Text(label, style = EnergyLabelSm, color = colors.textSecondary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (value == null) "—" else value.toInt().toString(),
                    style = EnergyTitleMd,
                    color = if (value == null) colors.textTertiary else valueColor
                )
                if (value != null) {
                    Text("W", style = EnergyBodySm, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 1.dp))
                }
            }
        }
    }
}

/** Confirmed Power Flows Breakdown: four distinct rows with tinted 32×6 icon tiles. */
@Composable
private fun FlowsCard(
    pvItem: com.sunlink.svitgrid.data.dto.MeasurementValueDto?,
    loadItem: com.sunlink.svitgrid.data.dto.MeasurementValueDto?,
    stale: Boolean,
    pvValue: Float?,
    loadValue: Float?,
    batteryVoltage: Float?,
    batteryCurrent: Float?
) {
    val colors = LocalSunLinkColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text("Потоки потужності", style = SunLinkTypography.title, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))

        val pvOk = isConfirmed(pvItem, stale)
        val loadOk = isConfirmed(loadItem, stale)
        FlowRow(
            icon = Icons.Default.SolarPower,
            iconBg = EnergySolarContainer,
            iconBorder = colors.solar.copy(alpha = 0.4f),
            iconColor = colors.solar,
            title = "Сонце → Інвертор",
            status = if (pvOk) "Підтверджено" else "Немає даних",
            statusColor = if (pvOk) colors.battery else colors.textTertiary,
            value = if (pvValue == null) "—" else "${pvValue.toInt()} W",
            valueStyle = EnergyTitleMd,
            valueColor = colors.solar
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.6f)))
        FlowRow(
            icon = Icons.Default.Home,
            iconBg = EnergyLoadContainer,
            iconBorder = colors.load.copy(alpha = 0.4f),
            iconColor = colors.load,
            title = "Інвертор → Будинок",
            status = if (loadOk) "Підтверджено" else "Немає даних",
            statusColor = if (loadOk) colors.battery else colors.textTertiary,
            value = if (loadValue == null) "—" else "${loadValue.toInt()} W",
            valueStyle = EnergyTitleMd,
            valueColor = colors.load
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.6f)))
        FlowRow(
            icon = Icons.Default.BatteryChargingFull,
            iconBg = EnergyBatteryContainer,
            iconBorder = colors.battery.copy(alpha = 0.4f),
            iconColor = colors.battery,
            title = "Батарея ↔ Інвертор",
            status = "Напрямок не підтверджено",
            statusColor = colors.textSecondary,
            value = voltageCurrentText(batteryVoltage, batteryCurrent),
            valueStyle = EnergyMonoSm,
            valueColor = colors.textSecondary
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.6f)))
        FlowRow(
            icon = Icons.Default.ElectricalServices,
            iconBg = EnergySurfaceHigh,
            iconBorder = colors.border,
            iconColor = colors.grid,
            title = "Мережа ↔ Інвертор",
            status = "Телеметрія недоступна",
            statusColor = colors.textSecondary,
            value = "—",
            valueStyle = EnergyTitleMd,
            valueColor = colors.textSecondary,
            topPaddingOnly = true
        )
    }
}

@Composable
private fun FlowRow(
    icon: ImageVector,
    iconBg: Color,
    iconBorder: Color,
    iconColor: Color,
    title: String,
    status: String,
    statusColor: Color,
    value: String,
    valueStyle: TextStyle,
    valueColor: Color,
    topPaddingOnly: Boolean = false
) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (topPaddingOnly) 8.dp else 0.dp, bottom = if (topPaddingOnly) 0.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(iconBg)
                    .border(1.dp, iconBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, style = SunLinkTypography.body, color = colors.textPrimary)
                Text(status, style = EnergyLabelSm, color = statusColor)
            }
        }
        Text(value, style = valueStyle, color = valueColor)
    }
}

/** Daily Energy Totals: 2×3 separated, lightly bordered tiles. */
@Composable
private fun DailyEnergyCard(full: com.sunlink.svitgrid.data.dto.TelemetryFullDto?, onNavigate: (String) -> Unit) {
    val colors = LocalSunLinkColors.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Енергія за добу", style = SunLinkTypography.title, color = colors.textPrimary)
            Text("Сьогодні", style = EnergyLabelSm, color = colors.textSecondary)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DailyTile("Генерація", telemetryNumber(full, "pv", "daily_energy"), colors.solar, "solar", onNavigate, Modifier.weight(1f))
                DailyTile("Споживання", telemetryNumber(full, "load", "daily_energy"), colors.load, "load", onNavigate, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DailyTile("Заряджено", telemetryNumber(full, "battery", "daily_charge_energy"), colors.battery, "battery", onNavigate, Modifier.weight(1f))
                DailyTile("Розряджено", telemetryNumber(full, "battery", "daily_discharge_energy"), colors.textPrimary, "battery", onNavigate, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DailyTile("Імпорт з мережі", telemetryNumber(full, "grid", "daily_import_energy"), colors.gridImport, "grid", onNavigate, Modifier.weight(1f))
                DailyTile("Експорт у мережу", telemetryNumber(full, "grid", "daily_export_energy"), colors.gridExport, "grid", onNavigate, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DailyTile(label: String, value: Float?, color: Color, route: String, onNavigate: (String) -> Unit, modifier: Modifier) {
    val colors = LocalSunLinkColors.current
    val available = value != null
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (available) colors.surface else EnergySurfaceLow)
            .border(
                1.dp,
                if (available) colors.border else colors.border.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onNavigate(route) }
            .padding(12.dp)
    ) {
        Text(label, style = EnergyLabelSm, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (value == null) "—" else "%.1f".format(value),
                style = SunLinkTypography.headline,
                color = if (available) color else colors.textTertiary
            )
            if (available) {
                Text("kWh", style = EnergyBodySm, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 3.dp))
            }
        }
    }
}

private fun voltageCurrentText(voltage: Float?, current: Float?): String {
    if (voltage == null && current == null) return "—"
    val v = voltage?.let { "%.1f".format(it) } ?: "—"
    val a = current?.let { "%.1f".format(it) } ?: "—"
    return "$v V · $a A"
}
