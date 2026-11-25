package com.sunlink.svitgrid.ui.redesign

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SolarPower
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathEffect
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
    // Stitch: rounded surface-container with a hairline border and a soft shadow.
    // Hero cards use a tinted border; ordinary cards keep the neutral outline.
    val borderColor = if (accent == colors.primary) colors.border.copy(alpha = 0.7f)
                      else accent.copy(alpha = 0.35f)
    Card(
        modifier = modifier,
        shape = PremiumTokens.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(PremiumTokens.spaceLg)) { content() }
    }
}

/** Зворотна сумісність з Stage49; тепер це преміальна картка. */
@Composable
fun SunLinkCard(
    modifier: Modifier = Modifier,
    accent: Color = LocalSunLinkColors.current.primary,
    content: @Composable () -> Unit
) = PremiumCard(modifier, accent, content)

/** Hero surface: layered gradient with tinted hairline, used once per screen. */
@Composable
fun HeroPanel(
    modifier: Modifier = Modifier,
    accent: Color = LocalSunLinkColors.current.primary,
    content: @Composable () -> Unit
) {
    val colors = LocalSunLinkColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(colors.surfaceElevated, colors.surface)))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(PremiumTokens.spaceLg)
    ) { content() }
}

/** Compact metric tile: tinted tonal surface, no heavy frame. */
@Composable
fun StatTile(
    label: String,
    value: Float?,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
    icon: ImageVector? = null,
    emphasized: Boolean = false
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = if (emphasized) 0.13f else 0.08f))
            .padding(if (emphasized) PremiumTokens.spaceLg else PremiumTokens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(PremiumTokens.spaceXs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (value == null) "—" else if (abs(value) >= 1000f) "%.2f".format(value / 1000f) else "%.1f".format(value),
                style = if (emphasized) SunLinkTypography.display else SunLinkTypography.metric,
                color = if (value == null) colors.textTertiary else colors.textPrimary
            )
            Text(
                if (value == null) "" else if (abs(value) >= 1000f) "kW" else unit,
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(bottom = if (emphasized) 5.dp else 3.dp, start = 4.dp)
            )
        }
        if (caption != null) Text(caption, style = SunLinkTypography.caption, color = colors.textTertiary)
    }
}

/** Grouped data surface: one panel with thin dividers instead of many boxes. */
@Composable
fun DataPanel(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface.copy(alpha = 0.72f))
            .border(1.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(PremiumTokens.spaceLg)
    ) {
        PremiumSectionHeader(title, subtitle, trailing)
        Spacer(Modifier.height(PremiumTokens.spaceSm))
        content()
    }
}

@Composable
fun InlineMetric(
    label: String,
    value: String,
    accent: Color? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = PremiumTokens.spaceXs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = SunLinkTypography.body, color = colors.textSecondary)
        Text(value, style = SunLinkTypography.label, color = accent ?: colors.textPrimary)
    }
}

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
        Freshness.FRESH -> freshnessLabel(Freshness.FRESH) to colors.success
        Freshness.STALE -> freshnessLabel(Freshness.STALE) to colors.warning
        Freshness.PROVISIONAL -> freshnessLabel(Freshness.PROVISIONAL) to colors.warning
        Freshness.UNAVAILABLE -> freshnessLabel(Freshness.UNAVAILABLE) to colors.offline
        Freshness.OFFLINE -> freshnessLabel(Freshness.OFFLINE) to colors.error
    }
    val timeLabel = formatTelemetryTimestamp(timestamp)
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
        if (!timestamp.isNullOrBlank() && !compact && timeLabel != "—") {
            Text(timeLabel, style = SunLinkTypography.caption, color = colors.textTertiary)
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
    val direction: FlowDirection = FlowDirection.UNKNOWN,
    val secondary: String? = null,
    val status: String? = null
)

private data class FlowEdge(
    val node: FlowNode,
    val color: Color,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val nx: Float,
    val ny: Float,
    val dashedWhenUnknown: Boolean = false
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
    // Stitch viewBox 334 x 332, scaled to the Compose container.
    val edges = listOf(
        FlowEdge(pv, colors.solar, "Сонце (PV)", Icons.Default.SolarPower, 0.50f, 74f / 332f),
        FlowEdge(battery, colors.battery, "Батарея", Icons.Default.BatteryChargingFull, 104f / 334f, 0.50f, true),
        FlowEdge(grid, colors.grid, "Мережа", Icons.Default.ElectricalServices, 230f / 334f, 0.50f, true),
        FlowEdge(load, colors.load, "Будинок", Icons.Default.Home, 0.50f, 256f / 332f)
    )

    Canvas(modifier) {
        drawRect(brush = Brush.verticalGradient(listOf(colors.surface, colors.background)), size = size)
        // Stitch uses a dusk photo here. It is not obtainable via MCP, so a restrained
        // horizon glow approximates the depth without overlapping the diagram.
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xFFF59E0B).copy(alpha = 0.06f))
            ),
            topLeft = Offset(0f, size.height * 0.72f),
            size = Size(size.width, size.height * 0.28f)
        )
        drawLine(colors.border.copy(alpha = 0.5f), Offset(0f, size.height * 0.86f), Offset(size.width, size.height * 0.86f), 1.dp.toPx())

        val hub = Offset(size.width * 0.5f, size.height * 0.5f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
        edges.forEach { edge ->
            val node = Offset(size.width * edge.nx, size.height * edge.ny)
            val active = edge.node.confirmed && edge.node.value != null && abs(edge.node.value!!) > 1f
            val (start, end) = when (edge.node.direction) {
                FlowDirection.TO_HUB -> node to hub
                else -> hub to node
            }
            val effect = if (edge.dashedWhenUnknown && !active) dash else null
            drawLine(edge.color.copy(alpha = if (active) 0.22f else 0.08f), start, end, 4.dp.toPx(), StrokeCap.Round, effect)
            drawLine(edge.color.copy(alpha = if (active) 0.9f else 0.3f), start, end, 2.2.dp.toPx(), StrokeCap.Round, effect)
            if (active && edge.node.direction != FlowDirection.UNKNOWN) {
                val dx = end.x - start.x
                val dy = end.y - start.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                if (len > 0f) {
                    val ux = dx / len
                    val uy = dy / len
                    val tip = if (edge.node.direction == FlowDirection.TO_HUB)
                        Offset(hub.x + ux * 30.dp.toPx(), hub.y + uy * 30.dp.toPx())
                    else Offset(node.x - ux * 14.dp.toPx(), node.y - uy * 14.dp.toPx())
                    val base = Offset(tip.x - ux * 9.dp.toPx(), tip.y - uy * 9.dp.toPx())
                    val perp = Offset(-uy * 4.dp.toPx(), ux * 4.dp.toPx())
                    drawPath(Path().apply {
                        moveTo(tip.x, tip.y); lineTo(base.x + perp.x, base.y + perp.y); lineTo(base.x - perp.x, base.y - perp.y); close()
                    }, edge.color)
                    val measure = PathMeasure().apply { setPath(Path().apply { moveTo(start.x, start.y); lineTo(end.x, end.y) }, false) }
                    val pos = measure.getPosition(if (edge.node.direction == FlowDirection.TO_HUB) 1f - phase else phase * measure.length)
                    drawCircle(edge.color, 3.dp.toPx(), pos)
                }
            }
        }
        val hubSize = 64.dp.toPx()
        drawRoundRect(colors.surfaceElevated, Offset(hub.x - hubSize / 2, hub.y - hubSize / 2), Size(hubSize, hubSize), CornerRadius(6.dp.toPx()))
        drawRoundRect(colors.border, Offset(hub.x - hubSize / 2, hub.y - hubSize / 2), Size(hubSize, hubSize), CornerRadius(6.dp.toPx()), style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun EnergyFlowCanvas(
    pv: Float?, battery: Float?, grid: Float?, load: Float?, modifier: Modifier = Modifier
) = EnergyFlowCanvas(FlowNode(pv), FlowNode(battery), FlowNode(grid), FlowNode(load), modifier)

@Composable
fun PremiumEnergyFlow(
    pv: FlowNode,
    battery: FlowNode,
    grid: FlowNode,
    load: FlowNode,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Box(modifier.fillMaxWidth().height(332.dp)) {
        EnergyFlowCanvas(pv, battery, grid, load, Modifier.matchParentSize())
        StitchNode(pv, "Сонце (PV)", colors.solar, Icons.Default.SolarPower, Modifier.align(Alignment.TopCenter).width(160.dp))
        StitchNode(battery, "Батарея", colors.battery, Icons.Default.BatteryChargingFull, Modifier.align(Alignment.CenterStart).width(104.dp))
        StitchNode(grid, "Мережа", colors.grid, Icons.Default.ElectricalServices, Modifier.align(Alignment.CenterEnd).width(104.dp))
        StitchNode(load, "Будинок", colors.load, Icons.Default.Home, Modifier.align(Alignment.BottomCenter).width(160.dp))
        Box(
            Modifier.align(Alignment.Center).size(64.dp).clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceElevated).border(1.dp, colors.border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Bolt, contentDescription = "Deye inverter", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                Text("DEYE", style = SunLinkTypography.caption, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun StitchNode(
    node: FlowNode,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier
            .background(colors.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(PremiumTokens.spaceSm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        Text(
            if (node.value == null) "—" else "${node.value!!.toInt()} W",
            style = SunLinkTypography.title,
            color = if (node.value == null) colors.textTertiary else color
        )
        when {
            node.status != null -> Text(node.status!!, style = SunLinkTypography.caption, color = colors.textTertiary)
            node.secondary != null -> Text(node.secondary!!, style = SunLinkTypography.mono, color = colors.textTertiary)
        }
    }
}
