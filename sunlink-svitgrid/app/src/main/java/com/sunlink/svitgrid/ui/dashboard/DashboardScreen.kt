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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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

@Composable
fun DashboardScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val testData = remember {
        mapOf(
            "power_dc" to mapOf("value" to 4680.0),
            "power_ac" to mapOf("value" to 4708.0),
            "battery_soc" to mapOf("value" to 65.0),
            "battery_voltage" to mapOf("value" to 48.2),
            "pv1_voltage" to mapOf("value" to 296.0),
            "pv1_current" to mapOf("value" to 2.1),
            "pv2_voltage" to mapOf("value" to 295.0),
            "pv2_current" to mapOf("value" to 2.0),
            "pv3_voltage" to mapOf("value" to 294.0),
            "pv3_current" to mapOf("value" to 1.9),
            "p_sun" to 4680.0,
            "p_load" to 4708.0,
            "p_batt" to 0.0,
            "p_grid" to 28.0
        )
    }

    val d = data ?: testData
    LaunchedEffect(Unit) { vm.loadLive() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        HeaderSection()
        BlackoutSection()
        TimelineSection()
        PVSection(d)
        FlowSchemeSection(d, Modifier.weight(1f))
        BottomNavigationSection()
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HOME",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = Mono,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            fontSize = 20.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("14° Мряка", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontFamily = Mono)
            Text("Щойно", style = MaterialTheme.typography.bodySmall, color = Color.LightGray, fontFamily = Mono)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", color = Color.White, fontFamily = Mono)
            Text("☰", color = Color.White, fontFamily = Mono)
        }
    }
}

@Composable
private fun BlackoutSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFCDDC39)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = "Без світла 16 хв · на акумуляторі",
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0..23) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i % 3 == 0) AccentGreen else Color(0xFFE74C3C))
            )
        }
    }
}

@Composable
private fun PVSection(data: Map<String, Any>) {
    val pSun = (data["p_sun"] as? Number)?.toFloat() ?: 0f
    val pvData = listOf(
        Triple("PV1", (data["pv1_voltage"] as? Number)?.toFloat() ?: 296f, (data["pv1_current"] as? Number)?.toFloat() ?: 2.1f),
        Triple("PV2", (data["pv2_voltage"] as? Number)?.toFloat() ?: 295f, (data["pv2_current"] as? Number)?.toFloat() ?: 2.0f),
        Triple("PV3", (data["pv3_voltage"] as? Number)?.toFloat() ?: 294f, (data["pv3_current"] as? Number)?.toFloat() ?: 1.9f),
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "☀ ${(pSun / 1000f).let { String.format("%.1f", it) }} кВт",
                color = Color.White,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pvData.forEach { (name, v, a) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.svit_panel),
                            contentDescription = name,
                            modifier = Modifier.size(52.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            "${v.toInt()} В · ${String.format("%.1f", a)} А",
                            color = Color.Gray,
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "$name · ${String.format("%.1f", v * a / 1000f)}",
                            color = Color.LightGray,
                            fontFamily = Mono,
                            fontSize = 10.sp
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
    val batterySoc = ((data["battery_soc"] as? Map<*, *>)?.get("value") as? Number)?.toFloat() ?: 65f
    val pBatt = (data["p_batt"] as? Number)?.toFloat() ?: 0f
    val pGrid = (data["p_grid"] as? Number)?.toFloat() ?: 0f
    val pLoad = (data["p_load"] as? Number)?.toFloat() ?: 0f
    val pSun = (data["p_sun"] as? Number)?.toFloat() ?: 0f

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
            .padding(horizontal = 16.dp, vertical = 6.dp)
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

            fun bezier(from: Offset, to: Offset, bend: Float = 0f): Path = Path().apply {
                moveTo(from.x, from.y)
                val cx = (from.x + to.x) / 2f + bend
                val cy = (from.y + to.y) / 2f
                cubicTo(cx, from.y, cx, to.y, to.x, to.y)
            }

            data class Flow(val path: Path, val color: Color, val power: Float, val reverse: Boolean = false)
            val flows = listOf(
                Flow(bezier(pv, inv), AccentOrange, pSun),
                Flow(bezier(inv, batt, -30f), if (pBatt >= 0) AccentGreen else AccentBlue, abs(pBatt), pBatt < 0),
                Flow(bezier(inv, grid, 30f), AccentBlue, abs(pGrid), pGrid < 0),
                Flow(bezier(inv, home), Color(0xFF9C27B0), pLoad),
            )

            flows.forEach { f ->
                if (f.power <= 1f) return@forEach
                drawPath(f.path, color = f.color.copy(alpha = 0.35f), style = Stroke(width = 3f, cap = StrokeCap.Round))
                val measure = PathMeasure()
                measure.setPath(f.path, false)
                val len = measure.length
                if (len <= 0f) return@forEach
                val speedFactor = max(0.4f, (f.power / 2000f).coerceIn(0.4f, 2.5f))
                val count = if (f.power > 500f) 3 else 2
                for (i in 0 until count) {
                    val base = (particleProgress * speedFactor + i.toFloat() / count) % 1f
                    val t = if (f.reverse) 1f - base else base
                    val pos = measure.getPosition(t * len)
                    drawCircle(color = f.color, radius = 4.5f, center = pos)
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 2f, center = pos)
                }
            }
        }

        // PV (top)
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp).clickable { selectedNode = "PV" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("solar.json", Modifier.size(54.dp))
            Text("${(pSun / 1000f).let { String.format("%.1f", it) }} кВт", color = Color.White, fontFamily = Mono, fontSize = 11.sp)
        }

        // Inverter (center)
        Column(
            modifier = Modifier.align(Alignment.Center).clickable { selectedNode = "Інвертор" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("inverter.json", Modifier.size(64.dp))
            Text("Інвертор", color = Color.LightGray, fontFamily = Mono, fontSize = 10.sp)
        }

        // Battery (left)
        Column(
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 8.dp, y = 50.dp).clickable { selectedNode = "Батарея" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("battery.json", Modifier.size(58.dp))
            Text("${batterySoc.toInt()}%", color = Color.White, fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                if (pBatt > 0) "Заряд · ${pBatt.toInt()} Вт" else if (pBatt < 0) "Розряд · ${abs(pBatt).toInt()} Вт" else "Очікування",
                color = Color.Gray, fontFamily = Mono, fontSize = 9.sp
            )
        }

        // Grid (right)
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-8).dp, y = 50.dp).clickable { selectedNode = "Мережа" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.svit_household_grid),
                contentDescription = "Мережа",
                modifier = Modifier.size(58.dp),
                contentScale = ContentScale.Fit
            )
            Text("Мережа", color = Color.White, fontFamily = Mono, fontSize = 11.sp)
            Text("${abs(pGrid).toInt()} Вт", color = Color.Gray, fontFamily = Mono, fontSize = 9.sp)
        }

        // Home (bottom)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).clickable { selectedNode = "Дім" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieNode("home_sun.json", Modifier.size(62.dp))
            Text("Дім · ${(pLoad / 1000f).let { String.format("%.1f", it) }} кВт", color = Color.White, fontFamily = Mono, fontSize = 11.sp)
        }

        // Bottom sheet-like info card on node tap
        selectedNode?.let { node ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable { selectedNode = null },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2633)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(node, color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    val detail = when (node) {
                        "PV" -> "Потужність: ${(pSun / 1000f).let { String.format("%.2f", it) }} кВт"
                        "Інвертор" -> "Deye SUN-12K · гібридний режим"
                        "Батарея" -> "SOC: ${batterySoc.toInt()}% · ${if (pBatt >= 0) "заряд" else "розряд"} ${abs(pBatt).toInt()} Вт · 32 кВт·г"
                        "Мережа" -> "Потік: ${pGrid.toInt()} Вт · Modbus TCP"
                        "Дім" -> "Навантаження: ${(pLoad / 1000f).let { String.format("%.2f", it) }} кВт"
                        else -> ""
                    }
                    Text(detail, color = Color.LightGray, fontFamily = Mono, fontSize = 12.sp)
                    Text("Торкніться щоб закрити", color = Color.Gray, fontFamily = Mono, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationSection() {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color(0xFF111821)
    ) {
        val items = listOf("Дім", "Дані", "Графіки", "Сонце", "Налашт.")
        items.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = index == 0,
                onClick = { },
                icon = {
                    val iconRes = when (index) {
                        0 -> R.drawable.svit_house
                        1 -> R.drawable.svit_household_workshop
                        2 -> R.drawable.svit_household_cabin
                        3 -> R.drawable.svit_panel
                        else -> R.drawable.svit_device_ed1_wifi
                    }
                    Image(painter = painterResource(id = iconRes), contentDescription = label, modifier = Modifier.size(22.dp), contentScale = ContentScale.Fit)
                },
                label = { Text(label, fontFamily = Mono, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF223047)
                )
            )
        }
    }
}
