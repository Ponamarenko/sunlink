package com.sunlink.svitgrid.ui.designsystem

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunlink.svitgrid.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/* ------------------------------------------------------------------ */
/* Basics                                                              */
/* ------------------------------------------------------------------ */

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
private fun formatUpdateTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return "—"
    return remember(timestamp) {
        try {
            val normalized = if (timestamp.endsWith("Z")) timestamp else timestamp.replace("+00:00", "Z")
            DateTimeFormatter.ofPattern("dd.MM HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(normalized))
        } catch (e: Exception) {
            timestamp.take(19)
        }
    }
}

@Composable
fun SunLinkBackground(content: @Composable () -> Unit) {
    val colors = LocalSunLinkColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) { content() }
}

@Composable
fun SunLinkTopBar(
    title: String,
    subtitle: String,
    state: DashboardState,
    lastUpdate: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = SunLinkDimensions.spaceLg, vertical = SunLinkDimensions.spaceMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.sunlink_inverter),
                contentDescription = title,
                modifier = Modifier.size(SunLinkDimensions.iconLg),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(SunLinkDimensions.spaceSm))
            Column {
                Text(title, style = SunLinkTypography.headline, color = colors.textPrimary)
                Text(subtitle, style = SunLinkTypography.caption, color = colors.textTertiary)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            ConnectionStatusBadge(state = state)
            LastUpdatedLabel(lastUpdate)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Text(
        text = title,
        style = SunLinkTypography.label,
        color = colors.textTertiary,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

@Composable
fun PowerValue(
    value: Float?,
    unit: String,
    decimals: Int = 0,
    color: Color = LocalSunLinkColors.current.textPrimary,
    style: androidx.compose.ui.text.TextStyle = SunLinkTypography.metric,
    modifier: Modifier = Modifier
) {
    val text = if (value == null) "—" else if (decimals == 0) {
        "%d %s".format(value.toInt(), unit)
    } else {
        "%,.${decimals}f %s".format(value, unit)
    }
    val noDataLabel = stringResource(R.string.dash_no_data)
    Text(
        text = text,
        style = style,
        color = if (value == null) LocalSunLinkColors.current.textTertiary else color,
        modifier = modifier.semantics {
            contentDescription = if (value == null) noDataLabel else text
        }
    )
}

@Composable
fun StatusIndicator(state: DashboardState, label: String, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    val color = when (state) {
        DashboardState.ONLINE -> colors.success
        DashboardState.CONNECTING, DashboardState.LOADING -> colors.primary
        DashboardState.STALE, DashboardState.ERROR -> colors.warning
        DashboardState.OFFLINE -> colors.offline
        DashboardState.UNKNOWN -> colors.disabled
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = SunLinkTypography.label, color = color)
    }
}

@Composable
fun ConnectionStatusBadge(state: DashboardState, modifier: Modifier = Modifier) {
    StatusIndicator(
        state = state,
        label = stringResource(state.labelRes),
        modifier = modifier
    )
}

@Composable
fun LastUpdatedLabel(timestamp: String?, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Text(
        text = stringResource(R.string.dash_last_update, formatUpdateTime(timestamp)),
        style = SunLinkTypography.caption,
        color = colors.textTertiary,
        modifier = modifier
    )
}

@Composable
fun BatterySOCIndicator(soc: Float?, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val colors = LocalSunLinkColors.current
    val color = when {
        soc == null -> colors.disabled
        soc < 20f -> colors.error
        soc < 50f -> colors.warning
        else -> colors.success
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            drawArc(colors.surfaceElevated, 0f, 360f, false, style = Stroke(stroke))
            if (soc != null) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * (soc.coerceIn(0f, 100f) / 100f),
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = if (soc == null) "—" else "${soc.toInt()}%",
            style = SunLinkTypography.label,
            color = if (soc == null) colors.textTertiary else colors.textPrimary
        )
    }
}

/* ------------------------------------------------------------------ */
/* Cards and states                                                    */
/* ------------------------------------------------------------------ */

@Composable
fun EnergyMetricCard(
    title: String,
    value: Float?,
    unit: String,
    iconRes: Int,
    accent: Color,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(SunLinkDimensions.radiusMd)
    Card(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = SunLinkDimensions.elevationCard)
    ) {
        Column(
            modifier = Modifier.padding(SunLinkDimensions.spaceMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceXs)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(SunLinkDimensions.iconMd),
                contentScale = ContentScale.Fit
            )
            Text(title, style = SunLinkTypography.label, color = colors.textSecondary)
            PowerValue(value, unit, color = accent)
            Text(
                text = supporting ?: stringResource(R.string.dash_no_data_hint),
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TelemetryValue(
    label: String,
    value: Float?,
    unit: String,
    decimals: Int = 1,
    accent: Color = LocalSunLinkColors.current.textPrimary,
    provisional: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    Column(modifier = modifier) {
        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        Row(verticalAlignment = Alignment.Bottom) {
            PowerValue(value, unit, decimals, if (provisional) colors.warning else accent, SunLinkTypography.title)
            if (provisional) {
                Text(" *", style = SunLinkTypography.caption, color = colors.warning)
            }
        }
    }
}

@Composable
fun EmptyTelemetryState(message: String, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(SunLinkDimensions.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = SunLinkTypography.body, color = colors.textSecondary)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(SunLinkDimensions.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
    ) {
        CircularProgressIndicator(color = colors.primary)
        Text(
            stringResource(R.string.state_loading),
            style = SunLinkTypography.body,
            color = colors.textSecondary
        )
    }
}

@Composable
fun OfflineState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(SunLinkDimensions.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
    ) {
        StatusIndicator(DashboardState.OFFLINE, stringResource(R.string.state_offline))
        Text(message, style = SunLinkTypography.body, color = colors.textSecondary)
        RetryButton(onRetry)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(SunLinkDimensions.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
    ) {
        StatusIndicator(DashboardState.ERROR, stringResource(R.string.state_error))
        Text(message, style = SunLinkTypography.body, color = colors.error)
        RetryButton(onRetry)
    }
}

@Composable
private fun RetryButton(onRetry: () -> Unit) {
    val colors = LocalSunLinkColors.current
    Box(
        modifier = Modifier
            .height(SunLinkDimensions.touchTarget)
            .clip(RoundedCornerShape(SunLinkDimensions.radiusSm))
            .background(colors.primarySoft)
            .clickable { onRetry() }
            .padding(horizontal = SunLinkDimensions.spaceLg),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(android.R.string.ok), style = SunLinkTypography.label, color = colors.primary)
    }
}

/* ------------------------------------------------------------------ */
/* Energy flow diagram                                                 */
/* ------------------------------------------------------------------ */

private data class Point(val fx: Float, val fy: Float)

private val pvPoints = listOf(Point(0.16f, 0.12f), Point(0.5f, 0.12f), Point(0.84f, 0.12f))
private val inverterPoint = Point(0.5f, 0.5f)
private val batteryPoint = Point(0.14f, 0.5f)
private val gridPoint = Point(0.86f, 0.5f)
private val homePoint = Point(0.5f, 0.88f)

private fun DrawScope.drawEnergyFlows(
    connections: List<EnergyFlowConnection>,
    progress: Float,
    colors: SunLinkColors
) {
    fun offset(p: Point) = Offset(p.fx * size.width, p.fy * size.height)
    connections.forEach { flow ->
        val from = offset(flowNodeFrom(flow.key))
        val to = offset(flowNodeTo(flow.key))
        val path = Path().apply { moveTo(from.x, from.y); lineTo(to.x, to.y) }
        drawPath(
            path,
            color = if (flow.isActive) flow.color.copy(alpha = 0.55f) else colors.flowIdle.copy(alpha = 0.45f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        if (!flow.isActive) return@forEach
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val t = if (flow.reverse) 1f - progress else progress
        val pos = measure.getPosition(t * length)
        drawCircle(flow.color, radius = 5.dp.toPx(), center = pos)
        drawCircle(Color.White.copy(alpha = 0.6f), radius = 2.dp.toPx(), center = pos)
    }
}

private fun flowNodeFrom(key: String): Point = when (key) {
    "pv1" -> pvPoints[0]
    "pv2" -> pvPoints[1]
    "pv3" -> pvPoints[2]
    "battery" -> inverterPoint
    "grid" -> inverterPoint
    "home" -> inverterPoint
    else -> inverterPoint
}

private fun flowNodeTo(key: String): Point = when (key) {
    "pv1" -> inverterPoint
    "pv2" -> inverterPoint
    "pv3" -> inverterPoint
    "battery" -> batteryPoint
    "grid" -> gridPoint
    "home" -> homePoint
    else -> inverterPoint
}

@Composable
fun EnergyFlowNode(
    label: String,
    iconRes: Int,
    value: Float?,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = modifier
            .size(SunLinkDimensions.nodeSize)
            .clip(RoundedCornerShape(SunLinkDimensions.radiusMd))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(SunLinkDimensions.radiusMd))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(26.dp),
            contentScale = ContentScale.Fit
        )
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary, maxLines = 1)
        PowerValue(value, unit, color = accent, style = SunLinkTypography.caption)
    }
}

/**
 * Пять узлов + направленные соединения. Анимация включается только при
 * подтверждённых активных потоках и выключенном Reduce Motion.
 */
@Composable
fun EnergyFlowDiagram(
    pvPower: Float?,
    pv1Power: Float?,
    pv2Power: Float?,
    pv3Power: Float?,
    batteryPower: Float?,
    gridPower: Float?,
    loadPower: Float?,
    soc: Float?,
    state: DashboardState,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    val reduceMotion = rememberReduceMotion()
    val activeAllowed = state.isActiveFlowAllowed

    // Направление не угадываем: без подтверждённой семантики знака поток нейтральный.
    val connections = listOf(
        EnergyFlowConnection("pv1", pv1Power, false, colors.solar),
        EnergyFlowConnection("pv2", pv2Power, false, colors.solar),
        EnergyFlowConnection("pv3", pv3Power, false, colors.solar),
        EnergyFlowConnection("battery", batteryPower, false, colors.battery),
        EnergyFlowConnection("grid", gridPower, false, colors.grid),
        EnergyFlowConnection("home", loadPower, false, colors.load)
    )
    val hasActive = activeAllowed && connections.any { it.isActive } && !reduceMotion

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SunLinkDimensions.flowHeight)
            .clip(RoundedCornerShape(SunLinkDimensions.radiusMd))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(SunLinkDimensions.radiusMd))
    ) {
        val node = SunLinkDimensions.nodeSize
        fun place(p: Point) = Modifier.offset(
            x = maxWidth * p.fx - node / 2,
            y = maxHeight * p.fy - node / 2
        )

        if (hasActive) {
            val transition = rememberInfiniteTransition(label = "energy-flow")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(SunLinkDimensions.animationFlowMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "progress"
            )
            Canvas(Modifier.fillMaxSize()) { drawEnergyFlows(connections, progress, colors) }
        } else {
            Canvas(Modifier.fillMaxSize()) { drawEnergyFlows(connections, 0f, colors) }
        }

        EnergyFlowNode(
            stringResource(R.string.node_solar), R.drawable.sunlink_solar_panel,
            pvPower, stringResource(R.string.unit_watt), colors.solar, place(pvPoints[0])
        )
        EnergyFlowNode(
            stringResource(R.string.node_solar), R.drawable.sunlink_solar_panel,
            pv1Power, stringResource(R.string.unit_watt), colors.solar, place(pvPoints[1])
        )
        EnergyFlowNode(
            stringResource(R.string.node_solar), R.drawable.sunlink_solar_panel,
            pv2Power, stringResource(R.string.unit_watt), colors.solar, place(pvPoints[2])
        )
        EnergyFlowNode(
            stringResource(R.string.node_inverter), R.drawable.sunlink_inverter,
            null, "", colors.primary, place(inverterPoint)
        )
        EnergyFlowNode(
            stringResource(R.string.node_battery), R.drawable.sunlink_battery,
            batteryPower, stringResource(R.string.unit_watt), colors.battery, place(batteryPoint)
        )
        EnergyFlowNode(
            stringResource(R.string.node_grid), R.drawable.sunlink_grid_pole,
            gridPower, stringResource(R.string.unit_watt), colors.grid, place(gridPoint)
        )
        EnergyFlowNode(
            stringResource(R.string.node_home), R.drawable.sunlink_house,
            loadPower, stringResource(R.string.unit_watt), colors.load, place(homePoint)
        )
        Box(modifier = place(Point(0.5f, 0.72f)), contentAlignment = Alignment.Center) {
            BatterySOCIndicator(soc, size = 44.dp)
        }
    }
}
