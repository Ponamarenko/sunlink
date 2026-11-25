package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

/**
 * Stitch «Мережа»: статусна картка підключення, чотири чесні параметри
 * електричної лінії, добовий обмін та посилання на діагностику.
 * Значення, яких інвертор не віддає, залишаються «— / Недоступно», а не нулями.
 */
@Composable
fun GridScreen(vm: MainViewModel = viewModel(), onNavigate: (String) -> Unit = {}) {
    val full by vm.fullTelemetry.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) { vm.loadFullTelemetry() }

    val gridFields = listOf("power", "voltage_l1", "frequency", "current_l1")
    val hasGridData = gridFields.any { telemetryNumber(full, "grid", it) != null }
    val accent = if (hasGridData) colors.warning else colors.grid
    val powerItem = telemetryValue(full, "grid", "power")
    val freshness = freshnessOf(
        full?.stale, powerItem?.quality, powerItem?.confidence, full?.connection_status
    )

    SunLinkScreen(
        title = "Мережа",
        subtitle = "Параметри зовнішньої лінії",
        refreshing = refreshing,
        onRefresh = { vm.loadFullTelemetry() }
    ) {
        GridStatusCard(hasGridData, accent, freshness, full?.timestamp)

        val parameters = listOf(
            "Активна потужність" to "power",
            "Напруга" to "voltage_l1",
            "Частота" to "frequency",
            "Струм" to "current_l1"
        )
        parameters.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { (label, field) ->
                    GridParameterCard(
                        label = label,
                        value = gridParameterValue(telemetryNumber(full, "grid", field), field),
                        caption = gridParameterCaption(full, field),
                        muted = telemetryNumber(full, "grid", field) == null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        GridExchangeCard(full)
        GridDiagnosticLink(onNavigate)
    }
}

/** Stitch: сеція стану — іконка в квадраті, заголовок і чесне пояснення. */
@Composable
private fun GridStatusCard(
    hasGridData: Boolean,
    accent: Color,
    freshness: Freshness,
    timestamp: String?
) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ElectricalServices,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasGridData) "Дані мережі частково доступні" else "Дані мережі недоступні",
                    style = SunLinkTypography.title,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasGridData) {
                        "Частина параметрів мережі надходить від інвертора."
                    } else {
                        "Потужність, напруга та частота мережі зараз не надходять від інвертора."
                    },
                    style = SunLinkTypography.caption,
                    color = colors.textSecondary
                )
            }
        }
        if (hasGridData) {
            Spacer(Modifier.height(8.dp))
            DataFreshnessIndicator(freshness, timestamp, compact = true)
        }
    }
}

/** Stitch: окрема плитка параметра з підписом «— Недоступно». */
@Composable
private fun GridParameterCard(
    label: String,
    value: String,
    caption: String,
    muted: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.55f))
            .border(1.dp, colors.border.copy(alpha = 0.6f), shape)
            .padding(14.dp)
    ) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = SunLinkTypography.headline,
            color = if (muted) colors.textTertiary else colors.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(caption, style = SunLinkTypography.caption, color = colors.textTertiary)
    }
}

/** Stitch: «Обмін з мережею за добу / Сьогодні» з розділювачем між імпортом і експортом. */
@Composable
private fun GridExchangeCard(full: TelemetryFullDto?) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Обмін з мережею за добу",
                style = SunLinkTypography.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text("Сьогодні", style = SunLinkTypography.caption, color = colors.textSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            GridExchangeCell(
                label = "Імпорт",
                value = energyValue(telemetryNumber(full, "grid", "import_energy")),
                color = colors.gridImport,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(52.dp)
                    .background(colors.border)
            )
            GridExchangeCell(
                label = "Експорт",
                value = energyValue(telemetryNumber(full, "grid", "export_energy")),
                color = colors.gridExport,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun GridExchangeCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalSunLinkColors.current
    Column(modifier) {
        Text(label, style = SunLinkTypography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = SunLinkTypography.title,
            color = if (value == "—") colors.textTertiary else color
        )
    }
}

/** Stitch: клікабельний рядок переходу до діагностики з табличкою та стрілкою. */
@Composable
private fun GridDiagnosticLink(onNavigate: (String) -> Unit) {
    val colors = LocalSunLinkColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable { onNavigate("diagnostics") }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Troubleshoot,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                "Перевірити якість телеметрії",
                style = SunLinkTypography.body,
                color = colors.textPrimary
            )
            Text(
                "Відкрити діагностику",
                style = SunLinkTypography.caption,
                color = colors.textSecondary
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun gridParameterValue(value: Float?, field: String): String {
    if (value == null) return "—"
    return when (field) {
        "power" -> gridValue(value, "W")
        "voltage_l1" -> gridValue(value, "V")
        "frequency" -> gridValue(value, "Гц")
        "current_l1" -> gridValue(value, "A")
        else -> "%.2f".format(value)
    }
}

private fun gridParameterCaption(full: TelemetryFullDto?, field: String): String {
    if (telemetryNumber(full, "grid", field) == null) return "Недоступно"
    return telemetryLabel(telemetryValue(full, "grid", field))
}

/** Відсутнє значення — окремий чесний стан, а не нуль і не підроблений підпис. */
private fun gridValue(value: Float?, unit: String): String = when {
    value == null -> "—"
    unit == "W" && (value <= -1000f || value >= 1000f) -> "%.2f kW".format(value / 1000f)
    unit == "W" -> "%.0f W".format(value)
    unit == "V" -> "%.1f V".format(value)
    unit == "A" -> "%.2f A".format(value)
    unit == "Гц" -> "%.2f Гц".format(value)
    else -> "%.2f %s".format(value, unit)
}

private fun energyValue(value: Float?): String =
    if (value == null) "—" else "%.2f kWh".format(value)
