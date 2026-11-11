package com.sunlink.svitgrid.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.max

private val BgDark = Color(0xFF0A0D12)
private val BgCard = Color(0xFF131A24)
private val AccentBlue = Color(0xFF2196F3)
private val AccentOrange = Color(0xFFFF9800)
private val AccentGreen = Color(0xFF66BB6A)
private val Mono = FontFamily.Monospace
private val GlassCard = Color.White.copy(alpha = 0.14f)
private val GlassBorder = Color.White.copy(alpha = 0.2f)
private val GlassShape = RoundedCornerShape(16.dp)

private fun num(data: Map<String, Any>, key: String): Float? = when (val v = data[key]) {
    is Number -> v.toFloat()
    is String -> v.toFloatOrNull()
    is Map<*, *> -> (v["value"] as? Number)?.toFloat()
        ?: (v["value"] as? String)?.toFloatOrNull()
    else -> null
}

private fun firstNum(data: Map<String, Any>, vararg keys: String): Float? {
    for (k in keys) num(data, k)?.let { return it }
    return null
}

private fun fmt(v: Float?, decimals: Int = 1, suffix: String = ""): String {
    if (v == null) return "—"
    return "%.${decimals}f%s".format(v, suffix)
}

private fun fmtInt(v: Float?, suffix: String = ""): String {
    if (v == null) return "—"
    return "${v.toInt()}$suffix"
}

@Composable
fun DashboardScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) { vm.loadLive() }

    // Реальні дані з API (без testData). Порожня мапа, якщо даних ще немає.
    val d = data ?: emptyMap()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        HeaderSection(loading = loading, hasData = data != null, error = error)
        BlackoutSection()
        TimelineSection()
        FlowSchemeSection(d, Modifier.weight(1f))
        // Нижню навігацію прибрано (сендвіч-меню / drawer)
    }
}

@Composable
private fun HeaderSection(loading: Boolean, hasData: Boolean, error: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "HOME",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
                color = Color.White,
                fontSize = 22.sp
            )
            val status = when {
                loading -> "Оновлення..."
                hasData -> "Є зв'язок"
                error != null -> "Немає зв'язку"
                else -> "Очікування..."
            }
            val connected = hasData && !loading
            Text(
                status,
                color = if (connected) AccentGreen else if (error != null) Color(0xFFE57373) else Color.Gray,
                fontFamily = Mono,
                fontSize = 12.sp,
                fontWeight = if (connected) FontWeight.Bold else FontWeight.Normal,
                style = if (connected) TextStyle(
                    shadow = Shadow(color = AccentGreen.copy(alpha = 0.9f), blurRadius = 14f)
                ) else TextStyle.Default
            )
            if (!hasData && error != null) {
                Text(
                    error.take(90),
                    color = Color(0xFFEF9A9A),
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    maxLines = 2
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AccentBlue)
            Text("14° Мряка", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = Mono)
        }
    }
}

@Composable
private fun BlackoutSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFCDDC39).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Text(
            text = "Без світла 16 хв · на акумуляторі",
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
            color = Color(0xFF111111),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimelineSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0..23) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i % 3 == 0) AccentGreen else Color(0xFFE74C3C))
            )
        }
    }
}

private data class PvRow(val name: String, val v: Float?, val a: Float?, val w: Float?)

@Composable
private fun PVSection(data: Map<String, Any>) {
    val pSun = firstNum(data, "p_sun", "power_dc")
    val rows = listOf(
        PvRow(
            "PV1",
            firstNum(data, "pv1_voltage", "pv_voltage_1", "voltage_pv1"),
            firstNum(data, "pv1_current", "pv_current_1", "current_pv1"),
            firstNum(data, "p_string1", "pv1_power", "pv_power_1", "power_pv1")
        ),
        PvRow(
            "PV2",
            firstNum(data, "pv2_voltage", "pv_voltage_2", "voltage_pv2"),
            firstNum(data, "pv2_current", "pv_current_2", "current_pv2"),
            firstNum(data, "p_string2", "pv2_power", "pv_power_2", "power_pv2")
        ),
        PvRow(
            "PV3",
            firstNum(data, "pv3_voltage", "pv_voltage_3", "voltage_pv3"),
            firstNum(data, "pv3_current", "pv_current_3", "current_pv3"),
            firstNum(data, "p_string3", "pv3_power", "pv_power_3", "power_pv3")
        ),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(8.dp, GlassShape),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = GlassShape,
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(20.dp)
                    .background(Color.White.copy(alpha = 0.03f))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("☀️", fontSize = 20.sp)
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = if (pSun != null) fmt(pSun / 1000f, 2, " кВт") else "— кВт",
                        color = Color.White,
                        fontFamily = Mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        val powerW = row.w ?: if (row.v != null && row.a != null) row.v * row.a else null
                        val powerText = when {
                            powerW == null -> "—"
                            abs(powerW) >= 1000f -> "${String.format("%.2f", powerW / 1000f)} кВт"
                            else -> "${powerW.toInt()} Вт"
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☀️", fontSize = 14.sp)
                            LottieNode("solar.json", Modifier.size(48.dp))
                            Text(
                                "${fmtInt(row.v, " В")} · ${fmt(row.a, 1, " А")}",
                                color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 10.sp, textAlign = TextAlign.Center
                            )
                            Text(
                                row.name,
                                color = Color.Gray, fontFamily = Mono, fontSize = 10.sp
                            )
                            Text(
                                powerText,
                                color = Color.White, fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LottieNode(asset: String, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/$asset"))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier
    )
}

@Composable
private fun FlowSchemeSection(data: Map<String, Any>, modifier: Modifier = Modifier) {
    val battSoc = firstNum(data, "batt_soc", "battery_soc", "soc")
    val battV = firstNum(data, "battery_voltage", "batt_voltage", "bat_voltage")
    val battA = firstNum(data, "battery_current", "batt_current", "bat_current")
    val battT = firstNum(data, "battery_temp", "batt_temp", "bat_temp", "temperature")
    val pBatt = firstNum(data, "p_batt", "battery_power", "bat_power")
    val pGrid = firstNum(data, "p_grid", "grid_power")
    val gridV = firstNum(data, "grid_voltage_l1", "grid_voltage", "voltage_ac")
    val pLoad = firstNum(data, "p_load", "load_power", "home_power", "power_ac")
    val pSun = firstNum(data, "p_sun", "power_dc")
    val pvRows = listOf(
        PvRow("PV1", firstNum(data, "pv1_voltage", "pv_voltage_1", "voltage_pv1"), firstNum(data, "pv1_current", "pv_current_1", "current_pv1"), firstNum(data, "p_string1", "pv1_power", "pv_power_1", "power_pv1")),
        PvRow("PV2", firstNum(data, "pv2_voltage", "pv_voltage_2", "voltage_pv2"), firstNum(data, "pv2_current", "pv_current_2", "current_pv2"), firstNum(data, "p_string2", "pv2_power", "pv_power_2", "power_pv2")),
        PvRow("PV3", firstNum(data, "pv3_voltage", "pv_voltage_3", "voltage_pv3"), firstNum(data, "pv3_current", "pv_current_3", "current_pv3"), firstNum(data, "p_string3", "pv3_power", "pv_power_3", "power_pv3")),
    )
    val battStatus = when {
        pBatt == null -> "Очікування"
        pBatt > 5f -> "Заряд · ${abs(pBatt).toInt()} Вт"
        pBatt < -5f -> "Розряд · ${abs(pBatt).toInt()} Вт"
        else -> "Очікування"
    }

    var selectedNode by remember { mutableStateOf<String?>(null) }

    val transition = rememberInfiniteTransition(label = "flow")
    val particleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(8.dp, GlassShape)
            .clip(GlassShape)
            .background(Brush.verticalGradient(listOf(GlassCard, Color(0x220A0D12))))
            .border(BorderStroke(1.dp, GlassBorder), GlassShape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(18.dp)
                .background(Color.White.copy(alpha = 0.02f))
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // layout: PV row top, inverter center, battery left / grid right same level, home bottom
            val inv = Offset(w * 0.5f, h * 0.46f)
            val pvY = h * 0.18f
            val pv1 = Offset(w * 0.17f, pvY)
            val pv2 = Offset(w * 0.5f, pvY)
            val pv3 = Offset(w * 0.83f, pvY)
            val sideY = h * 0.46f
            val batt = Offset(w * 0.16f, sideY)
            val grid = Offset(w * 0.84f, sideY)
            val home = Offset(w * 0.5f, h * 0.86f)

            fun bezier(from: Offset, to: Offset, c1: Offset, c2: Offset): Path = Path().apply {
                moveTo(from.x, from.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, to.x, to.y)
            }

            data class Flow(val path: Path, val color: Color, val power: Float?, val reverse: Boolean = false, val particles: Int = 3)
            val pvPowers = listOf(
                pvRows[0].w ?: if (pvRows[0].v != null && pvRows[0].a != null) pvRows[0].v!! * pvRows[0].a!! else null,
                pvRows[1].w ?: if (pvRows[1].v != null && pvRows[1].a != null) pvRows[1].v!! * pvRows[1].a!! else null,
                pvRows[2].w ?: if (pvRows[2].v != null && pvRows[2].a != null) pvRows[2].v!! * pvRows[2].a!! else null,
            )
            val invTop = Offset(inv.x, inv.y - h * 0.06f)
            val invLeft = Offset(inv.x - w * 0.08f, inv.y)
            val invRight = Offset(inv.x + w * 0.08f, inv.y)
            val invBottom = Offset(inv.x, inv.y + h * 0.08f)

            fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
            fun nearStraight(from: Offset, to: Offset, curve: Float = 0.08f): Path {
                val c1 = Offset(lerp(from.x, to.x, 0.33f), lerp(from.y, to.y, 0.33f - curve * 0.15f))
                val c2 = Offset(lerp(from.x, to.x, 0.66f), lerp(from.y, to.y, 0.66f - curve * 0.15f))
                return bezier(from, to, c1, c2)
            }

            val flows = mutableListOf<Flow>()
            // each PV panel -> inverter (converge), almost straight
            val pvStarts = listOf(pv1, pv2, pv3)
            pvPowers.forEachIndexed { i, p ->
                val from = Offset(pvStarts[i].x, pvStarts[i].y + h * 0.06f)
                val to = invTop
                val c1 = Offset(lerp(from.x, to.x, 0.35f), lerp(from.y, to.y, 0.35f))
                val c2 = Offset(lerp(from.x, to.x, 0.70f), lerp(from.y, to.y, 0.70f))
                flows += Flow(bezier(from, to, c1, c2), AccentOrange, p?.let { abs(it) }, particles = 4)
            }
            val battTo = Offset(batt.x + w * 0.07f, batt.y)
            flows += Flow(
                nearStraight(invLeft, battTo),
                if ((pBatt ?: 0f) >= 0f) AccentGreen else AccentBlue, pBatt?.let { abs(it) }, reverse = (pBatt ?: 0f) < 0f
            )
            val gridTo = Offset(grid.x - w * 0.07f, grid.y)
            flows += Flow(
                nearStraight(invRight, gridTo),
                AccentBlue, pGrid?.let { abs(it) }, reverse = (pGrid ?: 0f) < 0f
            )
            val homeTo = Offset(home.x, home.y - h * 0.06f)
            flows += Flow(
                nearStraight(invBottom, homeTo),
                Color(0xFF9C27B0), pLoad?.let { abs(it) }, particles = 4
            )

            flows.forEach { f ->
                val power = f.power ?: 0f
                if (power <= 0.1f) return@forEach
                drawPath(f.path, color = f.color.copy(alpha = 0.38f), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
                val measure = PathMeasure()
                measure.setPath(f.path, false)
                val len = measure.length
                if (len <= 0f) return@forEach
                val speedFactor = max(0.45f, (power / 2200f).coerceIn(0.45f, 2.6f))
                val count = f.particles
                for (i in 0 until count) {
                    val base = (particleProgress * speedFactor + i.toFloat() / count) % 1f
                    val t = if (f.reverse) 1f - base else base
                    val pos = measure.getPosition(t * len)
                    drawCircle(color = f.color, radius = 5f, center = pos)
                    drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 2.2f, center = pos)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp)) {
            // PV row (each panel separate with own line start)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                pvRows.forEachIndexed { idx, row ->
                    val powerW = row.w ?: if (row.v != null && row.a != null) row.v * row.a else null
                    val pvVal = if (powerW == null) "—" else if (abs(powerW) >= 1000f) fmt(powerW / 1000f, 2) else powerW.toInt().toString()
                    val pvUnit = if (powerW == null) "" else if (abs(powerW) >= 1000f) "кВт" else "Вт"
                    Column(
                        modifier = Modifier.weight(1f).clickable { selectedNode = "PV" },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("☀️", fontSize = 16.sp)
                        LottieNode("solar.json", Modifier.size(44.dp))
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                            Text(pvVal, color = Color.White, fontFamily = Mono, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (pvUnit.isNotEmpty()) Text(" $pvUnit", color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${fmtInt(row.v, "В")}·${fmt(row.a, 1, "А")}", color = Color(0xFF90A4AE), fontFamily = Mono, fontSize = 9.sp)
                        Text(row.name, color = Color.Gray, fontFamily = Mono, fontSize = 10.sp)
                    }
                }
            }

            // middle: inverter center, battery left, grid right (same level)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).clickable { selectedNode = "Інвертор" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LottieNode("inverter.json", Modifier.size(82.dp))
                    Text("Інвертор", color = Color(0xFFCFD8DC), fontFamily = Mono, fontSize = 11.sp)
                }

                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp).clickable { selectedNode = "Батарея" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(10.dp, RoundedCornerShape(18.dp))
                            .background(Brush.radialGradient(listOf(AccentGreen.copy(alpha = 0.25f), Color.Transparent)), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) { LottieNode("battery.json", Modifier.size(58.dp)) }
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                        Text(if (battSoc != null) "${battSoc.toInt()}" else "—", color = Color.White, fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("%", color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(" · ", color = Color.White, fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(if (battV != null) fmt(battV, 2) else "—", color = Color.White, fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("V", color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(battStatus, color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
                }

                val gridV2 = firstNum(data, "grid_voltage_l2", "voltage_l2")
                val gridV3 = firstNum(data, "grid_voltage_l3", "voltage_l3")
                val gridVoltageText = when {
                    gridV != null && gridV2 != null && gridV3 != null -> "${fmtInt(gridV)}/${fmtInt(gridV2)}/${fmtInt(gridV3)} V"
                    gridV != null -> "${fmt(gridV, 1, "V")}"
                    else -> "—V"
                }
                val gridDir = when {
                    pGrid == null -> null
                    pGrid > 0.5f -> "Експорт"
                    pGrid < -0.5f -> "Імпорт"
                    else -> "Баланс"
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp).clickable { selectedNode = "Мережа" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LottieNode("transformer.json", Modifier.size(64.dp))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                        Text(if (pGrid != null) "${abs(pGrid).toInt()}" else "—", color = Color.White, fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" Вт", color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(gridVoltageText, color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 11.sp)
                    Text(
                        gridDir ?: "—",
                        color = when (gridDir) { "Експорт" -> Color(0xFFE57373); "Імпорт" -> AccentBlue; else -> Color(0xFFB0BEC5) },
                        fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Home bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (selectedNode != null) 70.dp else 6.dp)
                    .clickable { selectedNode = "Дім" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LottieNode("home_sun.json", Modifier.size(64.dp))
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                    val homeVal = if (pLoad != null) fmt(pLoad / 1000f, 2) else "—"
                    Text(homeVal, color = Color.White, fontFamily = Mono, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(" кВт", color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("Споживання", color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 11.sp)
            }
        }

        // Bottom sheet-like info card on node tap
        selectedNode?.let { node ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .shadow(6.dp, GlassShape)
                    .clickable { selectedNode = null },
                colors = CardDefaults.cardColors(containerColor = GlassCard.copy(alpha = 0.75f)),
                shape = GlassShape,
                border = BorderStroke(1.dp, GlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(node, color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    val detail = when (node) {
                        "PV" -> "PV1: ${fmt(pvRows[0].v,0)}В ${fmt(pvRows[0].a,1)}А · PV2: ${fmt(pvRows[1].v,0)}В ${fmt(pvRows[1].a,1)}А · PV3: ${fmt(pvRows[2].v,0)}В ${fmt(pvRows[2].a,1)}А · Σ ${if (pSun!=null) fmt(pSun/1000f,2," кВт") else "—"}"
                        "Інвертор" -> "Deye SUN-12K · Modbus TCP · дані з API"
                        "Батарея" -> "SOC: ${fmtInt(battSoc, "%")} · V: ${fmt(battV, 2)} В · I: ${fmt(battA, 2)} А · T: ${fmt(battT, 0)} °C · $battStatus (batt_soc/battery_*/p_batt)"
                        "Мережа" -> {
                            val dir = when { pGrid == null -> "—"; pGrid > 0.5f -> "Експорт"; pGrid < -0.5f -> "Імпорт"; else -> "Баланс" }
                            "Потік: ${if (pGrid != null) "${pGrid.toInt()} Вт ($dir)" else "—"} · V: ${fmt(gridV, 0)} В (p_grid/grid_voltage_l1)"
                        }
                        "Дім" -> "Навантаження: ${if (pLoad != null) fmt(pLoad / 1000f, 2, " кВт") else "—"} (p_load)"
                        else -> ""
                    }
                    Text(detail, color = Color.LightGray, fontFamily = Mono, fontSize = 12.sp)
                    Text("Торкніться щоб закрити", color = Color.Gray, fontFamily = Mono, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
