package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

private val loadPeriods = listOf("24h" to "24 год", "7d" to "7 дн", "30d" to "30 дн")

@Composable
fun LoadScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val series by vm.seriesByParam.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    var period by remember { mutableStateOf("24h") }

    LaunchedEffect(Unit) {
        vm.loadFullTelemetry()
        vm.loadSeriesFor("load_power", period)
    }

    val loadSeries = series["load_power"]
    val spark = loadSeries?.points?.map { point -> if (point.gap) null else point.value?.toFloat() }
        ?: emptyList()

    SunLinkScreen(
        title = "Навантаження",
        subtitle = "Споживання · ${full?.timestamp ?: "—"}",
        refreshing = refreshing,
        onRefresh = { vm.loadFullTelemetry(); vm.loadSeriesFor("load_power", period) }
    ) {
        StatusPill(
            text = if (telemetryNumber(full, "load", "power") == null) "Немає даних" else "Поточне споживання",
            color = if (telemetryNumber(full, "load", "power") == null) colors.offline else colors.load
        )

        SunLinkCard(accent = colors.load) {
            Text("ПОТУЖНІСТЬ НАВАНТАЖЕННЯ", style = SunLinkTypography.label, color = colors.textSecondary)
            MetricCard(
                "Поточне навантаження",
                telemetryNumber(full, "load", "power"),
                "Вт",
                Icons.Default.Power,
                colors.load,
                sparkline = spark
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Напруга", telemetryNumber(full, "load", "voltage"), "В",
                Icons.Default.Bolt, colors.load, Modifier.weight(1f)
            )
            MetricCard(
                "Струм", telemetryNumber(full, "load", "current"), "А",
                null, colors.load, Modifier.weight(1f)
            )
            MetricCard(
                "Енергія за день", telemetryNumber(full, "load", "daily_energy"), "кВт·год",
                null, colors.load, Modifier.weight(1f)
            )
        }

        SunLinkCard(accent = colors.load) {
            Text("ІСТОРІЯ НАВАНТАЖЕННЯ", style = SunLinkTypography.label, color = colors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                loadPeriods.forEach { (value, label) ->
                    FilterChip(
                        selected = period == value,
                        onClick = {
                            period = value
                            vm.loadSeriesFor("load_power", value)
                        },
                        label = { Text(label) }
                    )
                }
            }
            val values = loadSeries?.points?.map { point -> if (point.gap) null else point.value?.toFloat() }
                ?: emptyList()
            if (values.isEmpty() || values.all { it == null }) {
                EmptyHint("Немає даних")
            } else {
                Sparkline(values, colors.load, Modifier.fillMaxWidth().height(140.dp))
                Text(
                    text = "Пропуски: ${loadSeries?.missing_points ?: 0}",
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SunLinkCard(accent = colors.load) {
            Text("ЕНЕРГІЯ", style = SunLinkTypography.label, color = colors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Накопичена енергія", style = SunLinkTypography.body, color = colors.textSecondary)
                Text(
                    text = telemetryNumber(full, "load", "energy")?.let { "%.2f кВт·год".format(it) } ?: "—",
                    style = SunLinkTypography.label,
                    color = colors.load
                )
            }
        }
    }
}
