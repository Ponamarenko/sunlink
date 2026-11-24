package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sunlink.svitgrid.data.dto.HistorySeriesDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Stitch: linearGradient stop-opacity 0.28 → 0.0 for area charts. */
private const val AREA_TOP_ALPHA = 0.28f
private const val AREA_BOTTOM_ALPHA = 0f

/**
 * Area chart matching the Stitch SVG structure:
 * dashed gridlines, closed gradient area, 2.2px line, last-point marker.
 * Null / gap points break the line instead of being drawn as zeros.
 */
@Composable
fun AreaChart(
    values: List<Float?>,
    color: Color,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    marker: Boolean = true
) {
    val colors = LocalSunLinkColors.current
    Canvas(modifier.clip(RoundedCornerShape(12.dp))) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
        val topPad = 8.dp.toPx()
        val bottomPad = 8.dp.toPx()
        val sidePad = 4.dp.toPx()
        val gridFractions = listOf(0.21f, 0.54f, 0.875f)

        if (showGrid) {
            gridFractions.forEachIndexed { index, fraction ->
                val y = size.height * fraction
                drawLine(
                    color = colors.border,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (index < gridFractions.size - 1) dash else null
                )
            }
        }

        val present = values.filterNotNull()
        if (present.isEmpty()) return@Canvas
        val min = present.minOrNull()!!
        val max = present.maxOrNull()!!
        val span = if (max == min) 1f else max - min
        val step = (size.width - sidePad * 2) / maxOf(1, values.size - 1)
        val bottomY = size.height - bottomPad

        fun pointOf(index: Int, value: Float): Offset = Offset(
            sidePad + step * index,
            bottomY - ((value - min) / span) * (size.height - topPad - bottomPad)
        )

        val segments = seriesSegments(values)
        segments.forEach { segment ->
            if (segment.size < 2) return@forEach
            val points = segment.map { (index, value) -> pointOf(index, value) }
            val area = Path().apply {
                moveTo(points.first().x, bottomY)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, bottomY)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = AREA_TOP_ALPHA),
                        color.copy(alpha = AREA_BOTTOM_ALPHA)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
            points.zipWithNext().forEach { (a, b) ->
                drawLine(
                    color = color.copy(alpha = 0.16f),
                    start = a,
                    end = b,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = a,
                    end = b,
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            if (marker) {
                val last = points.last()
                drawCircle(color.copy(alpha = 0.2f), 7.dp.toPx(), last)
                drawCircle(color, 3.5.dp.toPx(), last)
            }
        }
        // Single valid point: show only a marker, never a fabricated line.
        if (segments.all { it.size < 2 } && marker) {
            val single = values.indexOfFirst { it != null }
            if (single >= 0) {
                val p = pointOf(single, values[single]!!)
                drawCircle(color.copy(alpha = 0.2f), 7.dp.toPx(), p)
                drawCircle(color, 3.5.dp.toPx(), p)
            }
        }
    }
}

data class BarBucket(val label: String, val pv: Float?, val load: Float?)

/** Bucket size for the selected Stitch period. */
enum class BarBucketSize { HOUR, DAY, MONTH }

private fun bucketKey(iso: String, size: BarBucketSize): String? = try {
    val t = Instant.parse(iso).atZone(ZoneId.systemDefault())
    when (size) {
        BarBucketSize.HOUR -> t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH", Locale.US))
        BarBucketSize.DAY -> t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
        BarBucketSize.MONTH -> t.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))
    }
} catch (e: Exception) { null }

private fun bucketLabel(key: String, size: BarBucketSize): String = try {
    when (size) {
        BarBucketSize.HOUR -> key.substring(11, 13) + ":00"
        BarBucketSize.DAY -> key.substring(5, 10).replace('-', '.')
        BarBucketSize.MONTH -> java.time.YearMonth.parse(key).format(DateTimeFormatter.ofPattern("LLL", Locale("uk")))
    }
} catch (e: Exception) { key }

/**
 * Aligns PV and Load by the SAME timestamp bucket, never by array index.
 * Missing buckets remain null and are rendered as dashed placeholders.
 */
fun alignBarBuckets(
    pvSeries: HistorySeriesDto?,
    loadSeries: HistorySeriesDto?,
    size: BarBucketSize,
    limit: Int = 5
): List<BarBucket> {
    fun mapOf(series: HistorySeriesDto?): Map<String, Float> = buildMap {
        series?.points?.forEach { point ->
            val value = point.value?.toFloat()
            if (point.gap || value == null || value.isNaN() || value.isInfinite()) return@forEach
            bucketKey(point.timestamp, size)?.let { key -> put(key, value) }
        }
    }
    val pvMap = mapOf(pvSeries)
    val loadMap = mapOf(loadSeries)
    return (pvMap.keys + loadMap.keys)
        .distinct()
        .sorted()
        .takeLast(limit)
        .map { key -> BarBucket(bucketLabel(key, size), pvMap[key], loadMap[key]) }
}

/**
 * Stitch Analytics: paired rounded bars (PV gold / Load orange), dashed gridlines
 * and a dashed placeholder for a bucket with no measurements.
 */
@Composable
fun AnalyticsBarChart(
    buckets: List<BarBucket>,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true
) {
    val colors = LocalSunLinkColors.current
    val solar = colors.solar
    val load = colors.load
    Canvas(modifier.clip(RoundedCornerShape(12.dp))) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
        if (showGrid) {
            listOf(0.18f, 0.49f, 0.79f).forEachIndexed { index, fraction ->
                val y = size.height * fraction
                drawLine(
                    color = colors.border,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (index < 2) dash else null
                )
            }
        }
        if (buckets.isEmpty()) return@Canvas
        val values = buckets.flatMap { listOfNotNull(it.pv, it.load) }
        if (values.isEmpty()) return@Canvas
        val max = values.maxOrNull()!!.coerceAtLeast(0.001f)
        val bottom = size.height - 4.dp.toPx()
        val groupW = size.width / buckets.size
        val barW = (groupW * 0.24f).coerceAtMost(14.dp.toPx())
        val gap = 2.dp.toPx()
        val radius = CornerRadius(3.dp.toPx())

        buckets.forEachIndexed { index, bucket ->
            val center = groupW * index + groupW / 2
            val pvX = center - barW - gap / 2
            val loadX = center + gap / 2
            listOf(bucket.pv to pvX to solar, bucket.load to loadX to load).forEach { (pair, color) ->
                val (value, x) = pair
                if (value == null) {
                    drawRoundRect(
                        color = colors.border,
                        topLeft = Offset(x, bottom - 12.dp.toPx()),
                        size = Size(barW * 2 + gap, 12.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
                    )
                    return@forEach
                }
                val h = ((value / max) * (size.height * 0.72f)).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, bottom - h),
                    size = Size(barW, h),
                    cornerRadius = radius
                )
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        buckets.forEach { bucket ->
            Text(
                bucket.label,
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
