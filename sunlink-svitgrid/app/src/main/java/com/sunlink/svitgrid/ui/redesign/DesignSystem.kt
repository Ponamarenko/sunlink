package com.sunlink.svitgrid.ui.redesign

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object PremiumTokens {
    val card = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(50)
    val chip = RoundedCornerShape(12.dp)
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
}

@Composable
fun SunLinkScreen(
    title: String,
    subtitle: String? = null,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.surface.copy(alpha = 0.65f))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(
                start = PremiumTokens.spaceLg,
                end = PremiumTokens.spaceLg,
                top = PremiumTokens.spaceMd,
                bottom = PremiumTokens.spaceXl
            ),
        verticalArrangement = Arrangement.spacedBy(PremiumTokens.spaceMd)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = SunLinkTypography.headline, color = colors.textPrimary)
                if (subtitle != null) {
                    Text(subtitle, style = SunLinkTypography.caption, color = colors.textTertiary)
                }
            }
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary
                )
            } else if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
            }
        }
        content()
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    accent: Color = LocalSunLinkColors.current.primary,
    content: @Composable () -> Unit
) {
    val colors = LocalSunLinkColors.current
    Card(
        modifier = modifier,
        shape = PremiumTokens.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.15f), accent, accent.copy(alpha = 0.15f))
                        )
                    )
            )
            Column(Modifier.padding(PremiumTokens.spaceLg)) { content() }
        }
    }
}

/** Зворотна сумісність з Stage49; тепер це преміальна картка. */
@Composable
fun SunLinkCard(
    modifier: Modifier = Modifier,
    accent: Color = LocalSunLinkColors.current.primary,
    content: @Composable () -> Unit
) = PremiumCard(modifier, accent, content)

@Composable
fun PremiumSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = SunLinkTypography.label, color = colors.textSecondary)
            if (subtitle != null) {
                Text(subtitle, style = SunLinkTypography.caption, color = colors.textTertiary)
            }
        }
        trailing?.invoke()
    }
}

enum class Freshness { FRESH, STALE, PROVISIONAL, UNAVAILABLE, OFFLINE }

fun freshnessOf(
    stale: Boolean?,
    quality: String?,
    confidence: String?,
    online: Boolean? = null
): Freshness = when {
    online == false -> Freshness.OFFLINE
    stale == true || quality == "stale" -> Freshness.STALE
    quality == "unavailable" || confidence == null -> Freshness.UNAVAILABLE
    confidence == "provisional" || confidence == "unverified" -> Freshness.PROVISIONAL
    else -> Freshness.FRESH
}

@Composable
fun DataFreshnessIndicator(
    freshness: Freshness,
    timestamp: String? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = LocalSunLinkColors.current
    val (label, color) = when (freshness) {
        Freshness.FRESH -> "Fresh" to colors.success
        Freshness.STALE -> "Stale" to colors.warning
        Freshness.PROVISIONAL -> "Provisional" to colors.warning
        Freshness.UNAVAILABLE -> "Unavailable" to colors.offline
        Freshness.OFFLINE -> "Offline" to colors.error
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), PremiumTokens.pill)
            .padding(
                horizontal = if (compact) PremiumTokens.spaceSm else PremiumTokens.spaceSm,
                vertical = if (compact) 2.dp else PremiumTokens.spaceXs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceXs)
    ) {
        Box(Modifier.size(if (compact) 6.dp else 7.dp).background(color, CircleShape))
        Text(label, style = SunLinkTypography.caption, color = color, fontWeight = FontWeight.SemiBold)
        if (!timestamp.isNullOrBlank() && !compact) {
            Text(timestamp, style = SunLinkTypography.caption, color = colors.textTertiary)
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.14f), PremiumTokens.pill)
            .border(1.dp, color.copy(alpha = 0.35f), PremiumTokens.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, style = SunLinkTypography.caption, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MetricCard(
    title: String,
    value: Float?,
    unit: String,
    icon: ImageVector? = null,
    accent: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
    sparkline: List<Float?> = emptyList(),
    emphasize: Boolean = false
) {
    val colors = LocalSunLinkColors.current
    PremiumCard(modifier = modifier, accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon, contentDescription = null, tint = accent,
                    modifier = Modifier.size(if (emphasize) 22.dp else 18.dp)
                )
                Spacer(Modifier.size(PremiumTokens.spaceSm))
            }
            Text(title, style = SunLinkTypography.label, color = colors.textSecondary)
        }
        Spacer(Modifier.height(PremiumTokens.spaceSm))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (value == null) "—" else if (abs(value) >= 1000f) "%.2f".format(value / 1000f) else "%.1f".format(value),
                style = if (emphasize) SunLinkTypography.display else SunLinkTypography.metric,
                color = if (value == null) colors.textTertiary else colors.textPrimary
            )
            Text(
                text = if (value == null) "" else if (abs(value) >= 1000f) "kW" else unit,
                style = SunLinkTypography.label,
                color = colors.textTertiary,
                modifier = Modifier.padding(bottom = if (emphasize) 8.dp else 5.dp, start = 5.dp)
            )
        }
        if (sparkline.isNotEmpty()) {
            Sparkline(sparkline, accent, Modifier.fillMaxWidth().height(30.dp))
        }
        if (caption != null) {
            Spacer(Modifier.height(PremiumTokens.spaceXs))
            Text(caption, style = SunLinkTypography.caption, color = colors.textTertiary)
        }
    }
}

@Composable
fun PowerPill(label: String, value: Float?, color: Color, modifier: Modifier = Modifier) {
    StatusPill(
        text = if (value == null) "$label —" else "$label ${value.toInt()} W",
        color = color,
        modifier = modifier
    )
}

@Composable
fun Sparkline(values: List<Float?>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val present = values.filterNotNull()
        if (present.isEmpty()) return@Canvas
        val min = present.minOrNull()!!
        val max = present.maxOrNull()!!
        val span = if (max == min) 1f else max - min
        val step = size.width / maxOf(1, values.size - 1)
        seriesSegments(values).forEach { segment ->
            val points = segment.map { (index, v) ->
                Offset(
                    step * index,
                    size.height - ((v - min) / span) * (size.height - 6.dp.toPx()) - 3.dp.toPx()
                )
            }
            for (i in 0 until points.size - 1) {
                drawLine(color, points[i], points[i + 1], 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

/** Графік з сіткою, підписами та чесними розривами. */
@Composable
fun AnalyticsChart(
    values: List<Float?>,
    color: Color,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true
) {
    val colors = LocalSunLinkColors.current
    Canvas(modifier) {
        if (showGrid) {
            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(
                    colors.border.copy(alpha = 0.5f),
                    Offset(0f, y),
                    Offset(size.width, y),
                    1.dp.toPx()
                )
            }
        }
        val present = values.filterNotNull()
        if (present.isEmpty()) return@Canvas
        val min = present.minOrNull()!!
        val max = present.maxOrNull()!!
        val span = if (max == min) 1f else max - min
        val step = size.width / maxOf(1, values.size - 1)
        seriesSegments(values).forEach { segment ->
            val points = segment.map { (index, v) ->
                Offset(
                    step * index,
                    size.height - 8.dp.toPx() - ((v - min) / span) * (size.height - 16.dp.toPx())
                )
            }
            for (i in 0 until points.size - 1) {
                drawLine(color.copy(alpha = 0.18f), points[i], points[i + 1], 6.dp.toPx(), StrokeCap.Round)
                drawLine(color, points[i], points[i + 1], 2.5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
fun BatterySocIndicator(
    soc: Float?,
    modifier: Modifier = Modifier,
    color: Color = LocalSunLinkColors.current.battery
) {
    val colors = LocalSunLinkColors.current
    val transition = rememberInfiniteTransition(label = "soc")
    val shimmer by transition.animateFloat(
        0.85f, 1f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "soc-shimmer"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            drawArc(colors.surfaceElevated, 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            if (soc != null) {
                drawArc(
                    color.copy(alpha = shimmer), 135f,
                    270f * (soc.coerceIn(0f, 100f) / 100f), false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (soc == null) "—" else "${soc.toInt()}%",
                style = SunLinkTypography.display,
                color = if (soc == null) colors.textTertiary else colors.textPrimary
            )
            Text("SOC", style = SunLinkTypography.caption, color = colors.textTertiary)
        }
    }
}

@Composable
fun ArcGauge(value: Float?, modifier: Modifier = Modifier, color: Color = LocalSunLinkColors.current.battery) =
    BatterySocIndicator(value, modifier, color)

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Box(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated.copy(alpha = 0.5f), PremiumTokens.chip)
            .padding(PremiumTokens.spaceLg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = SunLinkTypography.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyHint(text: String) = EmptyState(text)

data class PremiumNavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun PremiumBottomNavigation(
    items: List<PremiumNavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(colors.surface)
            .padding(vertical = PremiumTokens.spaceSm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(item.route) }
                    .padding(vertical = PremiumTokens.spaceXs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PremiumTokens.spaceXs)
            ) {
                Box(
                    Modifier
                        .size(if (selected) 40.dp else 34.dp)
                        .background(
                            if (selected) colors.primarySoft else Color.Transparent,
                            PremiumTokens.pill
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selected) colors.primary else colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    item.label,
                    style = SunLinkTypography.caption,
                    color = if (selected) colors.primary else colors.textTertiary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

enum class FlowDirection { TO_HUB, FROM_HUB, UNKNOWN }

data class FlowNode(
    val value: Float?,
    val confirmed: Boolean = value != null,
    val direction: FlowDirection = FlowDirection.UNKNOWN
)

private data class FlowEdge(
    val node: FlowNode,
    val color: Color,
    val angleRad: Float,
    val label: String
)

@Composable
fun EnergyFlowCanvas(
    pv: FlowNode,
    battery: FlowNode,
    grid: FlowNode,
    load: FlowNode,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    val transition = rememberInfiniteTransition(label = "flow")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "flow-phase"
    )
    val edges = listOf(
        FlowEdge(pv, colors.solar, -Math.PI.toFloat() / 2f, "PV"),
        FlowEdge(battery, colors.battery, Math.PI.toFloat(), "BATTERY"),
        FlowEdge(grid, colors.grid, 0f, "GRID"),
        FlowEdge(load, colors.load, Math.PI.toFloat() / 2f, "HOME")
    )
    val hasActivity = edges.any { it.node.confirmed && it.node.value != null && abs(it.node.value!!) > 1f }

    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = minOf(size.width, size.height) * 0.38f

        // Атмосфера: м'яке світіння навколо хаба.
        drawCircle(colors.primary.copy(alpha = 0.06f), radius * 1.25f, Offset(cx, cy))
        drawCircle(colors.primary.copy(alpha = 0.05f), radius * 0.85f, Offset(cx, cy))

        edges.forEach { edge ->
            val end = Offset(cx + cos(edge.angleRad) * radius, cy + sin(edge.angleRad) * radius)
            val control = Offset(
                cx + cos(edge.angleRad) * radius * 0.45f - sin(edge.angleRad) * radius * 0.18f,
                cy + sin(edge.angleRad) * radius * 0.45f + cos(edge.angleRad) * radius * 0.18f
            )
            val path = Path().apply {
                moveTo(cx, cy)
                quadraticBezierTo(control.x, control.y, end.x, end.y)
            }
            val active = edge.node.confirmed && edge.node.value != null && abs(edge.node.value!!) > 1f
            val strength = edge.node.value?.let { (abs(it) / 3000f).coerceIn(0.2f, 1f) } ?: 0f

            drawPath(path, edge.color.copy(alpha = if (active) 0.12f else 0.06f), style = Stroke(12.dp.toPx()))
            drawPath(
                path,
                edge.color.copy(alpha = if (active) 0.85f else 0.22f),
                style = Stroke((2f + 2f * strength).dp.toPx(), cap = StrokeCap.Round)
            )

            if (active && edge.node.direction != FlowDirection.UNKNOWN) {
                val measure = PathMeasure().apply { setPath(path, false) }
                val t = if (edge.node.direction == FlowDirection.TO_HUB) 1f - phase else phase
                val pos = measure.getPosition(t * measure.length)
                drawCircle(edge.color.copy(alpha = 0.25f), 9.dp.toPx(), pos)
                drawCircle(edge.color, 4.dp.toPx(), pos)
            }

            drawCircle(edge.color.copy(alpha = 0.14f), 22.dp.toPx(), end)
            drawCircle(edge.color, 5.dp.toPx(), end)
        }

        // Центральний хаб інвертора.
        drawCircle(colors.surfaceElevated, 38.dp.toPx(), Offset(cx, cy))
        drawCircle(colors.primary.copy(alpha = if (hasActivity) 0.22f else 0.10f), 30.dp.toPx(), Offset(cx, cy))
        drawCircle(colors.primary, 8.dp.toPx(), Offset(cx, cy))
    }
}

@Composable
fun EnergyFlowCanvas(
    pv: Float?,
    battery: Float?,
    grid: Float?,
    load: Float?,
    modifier: Modifier = Modifier
) = EnergyFlowCanvas(
    FlowNode(pv), FlowNode(battery), FlowNode(grid), FlowNode(load), modifier
)

@Composable
fun PremiumEnergyFlow(
    pv: FlowNode,
    battery: FlowNode,
    grid: FlowNode,
    load: FlowNode,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Box(modifier.fillMaxWidth().height(260.dp)) {
        EnergyFlowCanvas(pv, battery, grid, load, Modifier.matchParentSize())
        FlowChip("PV", pv.value, colors.solar, Modifier.align(Alignment.TopCenter))
        FlowChip("BATTERY", battery.value, colors.battery, Modifier.align(Alignment.CenterStart))
        FlowChip("GRID", grid.value, colors.grid, Modifier.align(Alignment.CenterEnd))
        FlowChip("HOME", load.value, colors.load, Modifier.align(Alignment.BottomCenter))
        Box(
            Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .background(colors.surface, CircleShape)
                .border(1.dp, colors.primary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bolt, contentDescription = "Inverter", tint = colors.primary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun FlowChip(label: String, value: Float?, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier
            .background(colors.surface.copy(alpha = 0.92f), PremiumTokens.chip)
            .border(1.dp, color.copy(alpha = 0.45f), PremiumTokens.chip)
            .padding(horizontal = PremiumTokens.spaceSm, vertical = PremiumTokens.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = SunLinkTypography.caption, color = color, fontWeight = FontWeight.SemiBold)
        Text(
            if (value == null) "—" else "${value.toInt()} W",
            style = SunLinkTypography.label,
            color = if (value == null) colors.textTertiary else colors.textPrimary
        )
    }
}
