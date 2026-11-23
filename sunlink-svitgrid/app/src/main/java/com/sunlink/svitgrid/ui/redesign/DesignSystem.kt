package com.sunlink.svitgrid.ui.redesign

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkDimensions
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import kotlin.math.abs

private val CardShape = RoundedCornerShape(20.dp)

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
                    listOf(colors.background, colors.surface.copy(alpha = 0.55f))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = SunLinkTypography.display, color = colors.textPrimary)
                if (subtitle != null) {
                    Text(subtitle, style = SunLinkTypography.caption, color = colors.textTertiary)
                }
            }
            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
            } else if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
            }
        }
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun SunLinkCard(
    modifier: Modifier = Modifier,
    accent: Color = LocalSunLinkColors.current.primary,
    content: @Composable () -> Unit
) {
    val colors = LocalSunLinkColors.current
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accent.copy(alpha = 0.7f))
        )
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
    sparkline: List<Float?> = emptyList()
) {
    val colors = LocalSunLinkColors.current
    SunLinkCard(modifier = modifier, accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(title, style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (value == null) "—" else if (abs(value) >= 1000f) "%.2f".format(value / 1000f) else "%.1f".format(value),
                style = SunLinkTypography.display,
                color = if (value == null) colors.textTertiary else colors.textPrimary
            )
            Text(
                text = if (value == null) "" else if (abs(value) >= 1000f) "kW" else unit,
                style = SunLinkTypography.label,
                color = colors.textTertiary,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }
        if (sparkline.isNotEmpty()) {
            Sparkline(sparkline, accent, Modifier.fillMaxWidth().height(28.dp))
        }
        if (caption != null) {
            Text(caption, style = SunLinkTypography.caption, color = colors.textTertiary)
        }
    }
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
                val x = step * index
                val y = size.height - ((v - min) / span) * (size.height - 6.dp.toPx()) - 3.dp.toPx()
                Offset(x, y)
            }
            for (i in 0 until points.size - 1) {
                drawLine(color, points[i], points[i + 1], 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
fun ArcGauge(value: Float?, modifier: Modifier = Modifier, color: Color = LocalSunLinkColors.current.battery) {
    val colors = LocalSunLinkColors.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            drawArc(colors.surfaceElevated, 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            if (value != null) {
                drawArc(color, 135f, 270f * (value.coerceIn(0f, 100f) / 100f), false, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (value == null) "—" else "${value.toInt()}%", style = SunLinkTypography.display, color = colors.textPrimary)
            Text("SOC", style = SunLinkTypography.caption, color = colors.textTertiary)
        }
    }
}

@Composable
fun EnergyFlowCanvas(
    pv: Float?,
    battery: Float?,
    grid: Float?,
    load: Float?,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    val active = listOf(pv, battery, grid, load).any { it != null && abs(it) > 1f }
    val transition = rememberInfiniteTransition(label = "flow")
    val progress by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "flow-progress"
    )
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val nodes = listOf(
            Offset(size.width * 0.5f, size.height * 0.12f) to colors.solar,
            Offset(size.width * 0.12f, cy) to colors.battery,
            Offset(size.width * 0.88f, cy) to colors.grid,
            Offset(size.width * 0.5f, size.height * 0.88f) to colors.load
        )
        val values = listOf(pv, battery, grid, load)
        nodes.forEachIndexed { index, (point, color) ->
            val v = values[index]
            val path = Path().apply { moveTo(cx, cy); lineTo(point.x, point.y) }
            val strength = v?.let { (abs(it) / 5000f).coerceIn(0.15f, 1f) } ?: 0f
            drawPath(
                path,
                color.copy(alpha = if (v == null) 0.12f else 0.2f + 0.35f * strength),
                style = Stroke((2f + 2f * strength).dp.toPx(), cap = StrokeCap.Round)
            )
            // Імпульс позначає лише підтверджену активність, а не напрямок потоку:
            // сервер не підтверджує напрямок для всіх вузлів.
            if (active && v != null && abs(v) > 1f) {
                val pulse = 6.dp.toPx() + (3.dp.toPx() * kotlin.math.sin(progress * 2f * kotlin.math.PI).toFloat())
                drawCircle(color.copy(alpha = 0.25f), pulse + 4.dp.toPx(), point)
                drawCircle(color, pulse, point)
            }
            drawCircle(color.copy(alpha = 0.16f), 26.dp.toPx(), point)
            drawCircle(color, 3.dp.toPx(), point)
        }
        drawCircle(colors.primary, 34.dp.toPx(), Offset(cx, cy))
    }
}

@Composable
fun EmptyHint(text: String) {
    val colors = LocalSunLinkColors.current
    Text(
        text,
        style = SunLinkTypography.caption,
        color = colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    )
}
