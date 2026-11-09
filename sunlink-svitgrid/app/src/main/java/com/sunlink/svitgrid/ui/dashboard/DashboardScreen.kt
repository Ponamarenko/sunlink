package com.sunlink.svitgrid.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
        PVSection(d)
        FlowSchemeSection(d, Modifier.weight(1f))
        // Нижню навігацію прибрано (сендвіч-меню / drawer)
    }
}

@Composable
private fun HeaderSection(loading: Boolean, hasData: Boolean, error: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "HOME",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
                color = Color.White,
                fontSize = 18.sp
            )
            val status = when {
                loading -> "Оновлення..."
                hasData -> "Є зв'язок"
                error != null -> "Немає зв'язку"
                else -> "Очікування..."
            }
            Text(status, color = if (hasData) AccentGreen else if (error != null) Color(0xFFE57373) else Color.Gray, fontFamily = Mono, fontSize = 10.sp)
            if (!hasData && error != null) {
                Text(
                    error.take(90),
                    color = Color(0xFFEF9A9A),
                    fontFamily = Mono,
                    fontSize = 8.sp,
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFCDDC39)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = "Без світла 16 хв · на акумуляторі",
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
            color = Color.Black,
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "☀ ${if (pSun != null) fmt(pSun / 1000f, 1, " кВт") else "— кВт"}",
                color = Color.White,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    val powerKw = row.w?.div(1000f) ?: if (row.v != null && row.a != null) (row.v * row.a / 1000f) else null
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        LottieNode("solar.json", Modifier.size(44.dp))
                        Text(
                            "${fmtInt(row.v, " В")} · ${fmt(row.a, 1, " А")}",
                            color = Color.Gray, fontFamily = Mono, fontSize = 9.sp, textAlign = TextAlign.Center
                        )
                        Text(
                            "${row.name} · ${fmt(powerKw, 1)}",
                            color = Color.LightGray, fontFamily = Mono, fontSize = 9.sp
                        )
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pv = Offset(w * 0.5f, h * 0.12f)
            val inv = Offset(w * 0.5f, h * 0.42f)
            val batt = Offset(w * 0.2f, h * 0.72f)
            val grid = Offset(w * 0.8f, h * 0.72f)
            val home = Offset(w * 0.5f, h * 0.88f)

            // Bezier-лінії (cubicTo) для всіх 4 потоків
            fun bezier(from: Offset, to: Offset, bend: Float = 0f): Path = Path().apply {
                moveTo(from.x, from.y)
                val cx = (from.x + to.x) / 2f + bend
                val cy = (from.y + to.y) / 2f
                cubicTo(cx, from.y, cx, to.y, to.x, to.y)
            }

            data class Flow(val path: Path, val color: Color, val power: Float?, val reverse: Boolean = false)
            val flows = listOf(
                Flow(bezier(pv, inv), AccentOrange, pSun?.let { abs(it) }),
                Flow(bezier(inv, batt, -30f), if ((pBatt ?: 0f) >= 0f) AccentGreen else AccentBlue, pBatt?.let { abs(it) }, (pBatt ?: 0f) < 0f),
                Flow(bezier(inv, grid, 30f), AccentBlue, pGrid?.let { abs(it) }, (pGrid ?: 0f) < 0f),
                Flow(bezier(inv, home), Color(0xFF9C27B0), pLoad?.let { abs(it) }),
            )

            flows.forEach { f ->
                val power = f.power ?: 0f
                if (power <= 1f) return@forEach
                drawPath(f.path, color = f.color.copy(alpha = 0.35f), style = Stroke(width = 3f, cap = StrokeCap.Round))
                val measure = PathMeasure()
                measure.setPath(f.path, false)
                val len = measure.length
                if (len <= 0f) return@forEach
                val speedFactor = max(0.4f, (power / 2000f).coerceIn(0.4f, 2.5f))
                val count = if (power > 500f) 3 else 2
                for (i in 0 until count) {
                    val base = (particleProgress * speedFactor + i.toFloat() / count) % 1f
                    val t = if (f.reverse) 1f - base else base
                    val pos = measure.getPosition(t * len)
                    drawCircle(color = f.color, radius = 4.5f, center = pos)
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 2f, center = pos)
                }
            }
        }

        // PV (top) — Lottie
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp).clickable { selectedNode = "PV" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("solar.json", Modifier.size(52.dp))
            Text(if (pSun != null) fmt(pSun / 1000f, 1, " кВт") else "— кВт", color = Color.White, fontFamily = Mono, fontSize = 11.sp)
        }

        // Inverter (center) — Lottie
        Column(
            modifier = Modifier.align(Alignment.Center).clickable { selectedNode = "Інвертор" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("inverter.json", Modifier.size(62.dp))
            Text("Інвертор", color = Color.LightGray, fontFamily = Mono, fontSize = 10.sp)
        }

        // Battery (left) — Lottie
        val battStatus = when {
            pBatt == null -> "—"
            pBatt > 5f -> "Заряд"
            pBatt < -5f -> "Розряд"
            else -> "Очікування"
        }
        Column(
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 4.dp, y = 44.dp).clickable { selectedNode = "Батарея" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("battery.json", Modifier.size(56.dp))
            Text(if (battSoc != null) "${battSoc.toInt()}%" else "—%", color = Color.White, fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "$battStatus${if (pBatt != null && abs(pBatt) > 5f) " · ${abs(pBatt).toInt()} Вт" else ""}",
                color = Color.Gray, fontFamily = Mono, fontSize = 9.sp
            )
        }

        // Grid (right) — Lottie transformer (стовб)
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-4).dp, y = 44.dp).clickable { selectedNode = "Мережа" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("transformer.json", Modifier.size(56.dp))
            Text("Мережа", color = Color.White, fontFamily = Mono, fontSize = 11.sp)
            Text(if (pGrid != null) "${abs(pGrid).toInt()} Вт" else "— Вт", color = Color.Gray, fontFamily = Mono, fontSize = 9.sp)
        }

        // Home (bottom) — Lottie
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).clickable { selectedNode = "Дім" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("home_sun.json", Modifier.size(60.dp))
            Text("Дім · ${if (pLoad != null) fmt(pLoad / 1000f, 1, " кВт") else "— кВт"}", color = Color.White, fontFamily = Mono, fontSize = 11.sp)
        }

        // Bottom sheet-like info card on node tap
        selectedNode?.let { node ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clickable { selectedNode = null },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2633)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(node, color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    val detail = when (node) {
                        "PV" -> "Потужність: ${if (pSun != null) fmt(pSun / 1000f, 2, " кВт") else "—"} (p_sun/power_dc)"
                        "Інвертор" -> "Deye SUN-12K · Modbus TCP · дані з API"
                        "Батарея" -> "SOC: ${fmtInt(battSoc, "%")} · V: ${fmt(battV, 1)} В · I: ${fmt(battA, 2)} А · T: ${fmt(battT, 0)} °C · $battStatus ${if (pBatt != null) "${abs(pBatt).toInt()} Вт" else "—"} (batt_soc/battery_*/p_batt)"
                        "Мережа" -> "Потік: ${if (pGrid != null) "${pGrid.toInt()} Вт" else "—"} · V: ${fmt(gridV, 0)} В (p_grid/grid_voltage_l1)"
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
