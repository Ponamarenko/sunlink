package com.sunlink.svitgrid.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Простий графік із чесними пропусками: сегмент малюється тільки між двома
 * сусідніми не-null значеннями. Великі проміжки без даних НЕ з'єднуються.
 */
@Composable
fun TelemetryChart(
    values: List<Float?>,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = LocalSunLinkColors.current.primary
) {
    val colors = LocalSunLinkColors.current
    val present = values.filterNotNull()
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = SunLinkDimensions.spaceLg)) {
        Text(label, style = SunLinkTypography.label, color = colors.textSecondary)
        val min = present.minOrNull() ?: 0f
        val max = present.maxOrNull() ?: 1f
        val span = if (max == min) 1f else max - min
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val width = size.width
            val height = size.height
            drawLine(
                color = colors.border,
                start = Offset(0f, height - 1f),
                end = Offset(width, height - 1f),
                strokeWidth = 1f
            )
            if (values.isEmpty()) return@Canvas
            val stepX = width / maxOf(1, values.size - 1)

            fun point(index: Int, value: Float): Offset = Offset(
                x = stepX * index,
                y = height - ((value - min) / span) * (height - 12.dp.toPx()) - 6.dp.toPx()
            )

            var segment = mutableListOf<Offset>()
            values.forEachIndexed { index, value ->
                if (value == null) {
                    if (segment.size > 1) {
                        for (i in 0 until segment.size - 1) {
                            drawLine(color, segment[i], segment[i + 1], strokeWidth = 2.dp.toPx())
                        }
                    }
                    segment = mutableListOf()
                } else {
                    segment.add(point(index, value))
                    drawCircle(color, radius = 2.dp.toPx(), center = point(index, value))
                }
            }
            if (segment.size > 1) {
                for (i in 0 until segment.size - 1) {
                    drawLine(color, segment[i], segment[i + 1], strokeWidth = 2.dp.toPx())
                }
            }
        }
        Text(
            text = if (present.isEmpty()) "—" else "min %.1f · max %.1f".format(min, max),
            style = SunLinkTypography.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
