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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors
import com.sunlink.svitgrid.viewmodel.MainViewModel

enum class HomeWeather { SUN, CLOUDS, RAIN, SNOW, FOG }

private fun weatherRes(weather: HomeWeather): Int = when (weather) {
    HomeWeather.SUN -> R.raw.weather_effect_sun
    HomeWeather.CLOUDS -> R.raw.weather_effect_clouds
    HomeWeather.RAIN -> R.raw.weather_effect_rain
    HomeWeather.SNOW -> R.raw.weather_effect_snow
    HomeWeather.FOG -> R.raw.weather_effect_fog
}

/** Direct Compose port of Stitch node_12456895449774030424. */
@Composable
fun HomeScreen(
    vm: MainViewModel = viewModel(),
    onNavigate: (String) -> Unit = {},
    weather: HomeWeather = HomeWeather.SUN
) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        val full by vm.fullTelemetry.collectAsState()
        val error by vm.fullTelemetryError.collectAsState()
        val loading by vm.refreshing.collectAsState()
        val state by vm.uiState.collectAsState()
        val transport by vm.liveTransport.collectAsState()
        val colors = LocalSunLinkColors.current

        LaunchedEffect(Unit) { vm.loadFullTelemetry(); vm.loadData() }

        val success = state as? InverterUiState.Success
        val stale = full?.stale == true || success?.stale == true
        val online = success?.status?.online == true
        val freshness = when {
            error != null && full == null -> Freshness.OFFLINE
            stale -> Freshness.STALE
            online && full != null -> Freshness.FRESH
            full != null -> Freshness.PROVISIONAL
            else -> Freshness.UNAVAILABLE
        }
        val pv = telemetryValue(full, "pv", "total_power")
        val batterySoc = telemetryValue(full, "battery", "soc")
        val batteryVoltage = telemetryNumber(full, "battery", "voltage")
        val batteryCurrent = telemetryNumber(full, "battery", "current")
        val batteryPowerItem = telemetryValue(full, "battery", "power")
        val gridItem = telemetryValue(full, "grid", "power")
        val loadItem = telemetryValue(full, "load", "power")
        val batteryPower = telemetryNumber(full, "battery", "power")
        val gridPower = telemetryNumber(full, "grid", "power")
        val loadPower = displayLoadPower(telemetryNumber(full, "load", "power"))

        val pvNode = FlowNode(pv?.normalized_value?.takeIf { isConfirmed(pv, stale) }?.toFloat(), isConfirmed(pv, stale), pvFlowDirection(pv?.normalized_value?.toFloat(), isConfirmed(pv, stale)))
        val batteryNode = FlowNode(
            batteryPower, isConfirmed(batteryPowerItem, stale), batteryFlowDirection(),
            secondary = listOfNotNull(
                batteryVoltage?.let { "%.1f V".format(it) },
                batteryCurrent?.let { "%.1f A".format(it) }
            ).joinToString(" · ").ifBlank { null }
        )
        val gridNode = FlowNode(
            gridPower, isConfirmed(gridItem, stale), gridFlowDirection(gridPower, isConfirmed(gridItem, stale)),
            status = if (isConfirmed(gridItem, stale)) null else "Недоступно"
        )
        val loadNode = FlowNode(loadPower, isConfirmed(loadItem, stale), loadFlowDirection(loadPower, isConfirmed(loadItem, stale)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A. Compact header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(colors.surfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Tune, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Головна", style = SunLinkTypography.headline, color = colors.textPrimary)
                        StatusPill(if (online) "Онлайн" else if (stale) "Застаріло" else "Офлайн",
                            if (online && !stale) colors.battery else colors.warning)
                    }
                    Text(
                        "SunLink · Оновлено ${formatTelemetryTimestamp(full?.timestamp)}",
                        style = SunLinkTypography.body, color = colors.textTertiary
                    )
                }
                Box(
                    Modifier.size(44.dp).background(colors.surface, RoundedCornerShape(50))
                        .border(1.dp, colors.border, RoundedCornerShape(50)).clickable { vm.loadFullTelemetry(); vm.loadData() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Refresh, contentDescription = "Оновити", tint = colors.textPrimary) }
            }

            // B. Separate freshness / WebSocket row
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (freshness == Freshness.FRESH) colors.battery else colors.warning, RoundedCornerShape(50)))
                Spacer(Modifier.width(6.dp))
                Text(freshnessLabel(freshness), style = SunLinkTypography.label, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                Text(
                    "WebSocket: ${if (transport == TelemetryWebSocketClient.WsTransportState.OPEN) "підключено" else "підключення"}",
                    style = SunLinkTypography.mono, color = colors.textSecondary
                )
            }
            telemetryErrorNote(error, full != null && !stale)?.let {
                Text(it, style = SunLinkTypography.caption, color = if (full != null && !stale) colors.warning else colors.error)
            }

            // C. Main energy flow, exact Stitch proportions
            Box(
                Modifier.fillMaxWidth().height(356.dp)
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .clickable { onNavigate("energy") }
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(weatherRes(weather)))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever
                )
                val house = ImageBitmap.imageResource(R.drawable.house_reference)
                // Each weather variant is a different crop of the user's original photo.
                val crop = when (weather) {
                    HomeWeather.SUN -> IntOffset(0, 0) to IntSize((house.width * 0.34f).toInt(), (house.height * 0.50f).toInt())
                    HomeWeather.CLOUDS -> IntOffset((house.width * 0.34f).toInt(), 0) to IntSize((house.width * 0.34f).toInt(), (house.height * 0.50f).toInt())
                    HomeWeather.RAIN -> IntOffset((house.width * 0.68f).toInt(), 0) to IntSize((house.width * 0.32f).toInt(), (house.height * 0.50f).toInt())
                    HomeWeather.SNOW -> IntOffset(0, (house.height * 0.50f).toInt()) to IntSize((house.width * 0.50f).toInt(), (house.height * 0.50f).toInt())
                    HomeWeather.FOG -> IntOffset((house.width * 0.50f).toInt(), (house.height * 0.50f).toInt()) to IntSize((house.width * 0.50f).toInt(), (house.height * 0.50f).toInt())
                }
                Canvas(Modifier.matchParentSize()) {
                    drawImage(
                        image = house,
                        srcOffset = crop.first,
                        srcSize = crop.second,
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(colors.background.copy(alpha = 0.30f), colors.background.copy(alpha = 0.68f))
                        ),
                        size = size
                    )
                }
                LottieAnimation(
                    composition,
                    { progress },
                    modifier = Modifier.matchParentSize(),
                    alignment = Alignment.TopCenter
                )
                PremiumEnergyFlow(pvNode, batteryNode, gridNode, loadNode, drawBackdrop = false)
            }

            // D. Three compact key metrics (no repeated card frames)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StitchMetricTile("Потужність PV", telemetryNumber(full, "pv", "total_power"), "W", colors.solar,
                    telemetryLabel(pv), Modifier.weight(1f).clickable { onNavigate("solar") })
                StitchMetricTile("Батарея SOC", telemetryNumber(full, "battery", "soc"), "%", colors.battery,
                    "${telemetryLabel(batterySoc)}", Modifier.weight(1f).clickable { onNavigate("battery") })
                StitchMetricTile("Навантаження", loadPower, "W", colors.load,
                    telemetryLabel(loadItem), Modifier.weight(1f).clickable { onNavigate("load") })
            }

            // E. Compact daily summary — one grouped panel, 3 x 2
            Column(
                Modifier.fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("За сьогодні", style = SunLinkTypography.title, color = colors.textPrimary)
                    Text("Аналітика →", style = SunLinkTypography.label, color = colors.primary,
                        modifier = Modifier.clickable { onNavigate("analytics") })
                }
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Генерація" to telemetryNumber(full, "pv", "daily_energy") to colors.solar,
                    "Споживання" to telemetryNumber(full, "load", "daily_energy") to colors.load,
                    "Імпорт" to telemetryNumber(full, "grid", "import_energy") to colors.grid,
                    "Експорт" to telemetryNumber(full, "grid", "export_energy") to colors.gridExport,
                    "Заряд" to telemetryNumber(full, "battery", "daily_charge_energy") to colors.batteryCharge,
                    "Розряд" to telemetryNumber(full, "battery", "daily_discharge_energy") to colors.batteryDischarge
                ).chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (pair, color) ->
                            val (label, value) = pair
                            Column(Modifier.weight(1f)) {
                                Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
                                Text(value?.let { "%.1f kWh".format(it) } ?: "—", style = SunLinkTypography.label, color = color)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StitchMetricTile(
    label: String, value: Float?, unit: String, color: androidx.compose.ui.graphics.Color,
    caption: String?, modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value?.let { "%.0f".format(it) } ?: "—", style = SunLinkTypography.title, color = if (value == null) colors.textTertiary else color)
            Text(" $unit", style = SunLinkTypography.caption, color = colors.textTertiary)
        }
        Text(caption ?: "", style = SunLinkTypography.caption, color = colors.textTertiary)
    }
}
