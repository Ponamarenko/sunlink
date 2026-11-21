package com.sunlink.svitgrid.ui.telemetry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.ui.designsystem.LastUpdatedLabel
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.PowerValue
import com.sunlink.svitgrid.ui.designsystem.SectionHeader
import com.sunlink.svitgrid.ui.designsystem.SunLinkDimensions
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel

@Composable
fun TelemetryScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val colors = LocalSunLinkColors.current

    LaunchedEffect(Unit) { vm.loadFullTelemetry() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = SunLinkDimensions.spaceXl),
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
    ) {
        SectionHeader(
            stringResource(R.string.telemetry_title),
            Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
        )
        if (full == null) {
            Text(
                stringResource(R.string.telemetry_unavailable),
                style = SunLinkTypography.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
        } else {
            Text(
                stringResource(R.string.dash_source, full!!.source ?: "—"),
                style = SunLinkTypography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
            )
            LastUpdatedLabel(full!!.timestamp)
            if (full!!.stale == true) {
                Text(
                    stringResource(R.string.dash_stale_banner),
                    style = SunLinkTypography.caption,
                    color = colors.warning,
                    modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
                )
            }
            val categories = full!!.categories.orEmpty()
            listOf("pv", "battery", "grid", "load", "inverter", "energy").forEach { key ->
                val fields = categories[key].orEmpty()
                if (fields.isEmpty()) return@forEach
                SectionHeader(
                    key.uppercase(),
                    Modifier.padding(horizontal = SunLinkDimensions.spaceLg)
                )
                Card(
                    modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(SunLinkDimensions.spaceMd),
                        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
                    ) {
                        fields.toSortedMap().forEach { (name, value) ->
                            TelemetryRow(name, value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(name: String, value: MeasurementValueDto?) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = SunLinkTypography.caption, color = colors.textSecondary)
        PowerValue(
            value = value?.normalized_value?.toFloat(),
            unit = value?.unit.orEmpty(),
            decimals = 2,
            style = SunLinkTypography.label,
            color = colors.textPrimary
        )
    }
}
