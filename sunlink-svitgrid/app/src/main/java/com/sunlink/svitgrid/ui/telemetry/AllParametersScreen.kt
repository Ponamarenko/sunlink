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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private enum class ParameterFilter {
    ALL, AVAILABLE, VERIFIED, UNVERIFIED, UNAVAILABLE, STALE
}

@Composable
fun AllParametersScreen(vm: MainViewModel = viewModel()) {
    val colors = LocalSunLinkColors.current
    val full by vm.fullTelemetry.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ParameterFilter.ALL) }

    LaunchedEffect(Unit) { vm.loadFullTelemetry() }

    val categories = full?.categories.orEmpty()
    val all = categories.flatMap { (category, fields) ->
        fields.orEmpty().map { (name, value) -> Triple(category, name, value) }
    }.sortedWith(compareBy({ it.first }, { it.second }))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = SunLinkDimensions.spaceXl),
        verticalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceMd)
    ) {
        SectionHeader(stringResource(R.string.all_parameters), Modifier.padding(horizontal = SunLinkDimensions.spaceLg))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.all_parameters_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = SunLinkDimensions.spaceLg)
        )
        Row(
            Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
            horizontalArrangement = Arrangement.spacedBy(SunLinkDimensions.spaceSm)
        ) {
            ParameterFilter.values().forEach { f ->
                FilterChip(
                    selected = f == filter,
                    onClick = { filter = f },
                    label = { Text(stringResource(filterLabel(f))) }
                )
            }
        }
        error?.let {
            Text(it, color = colors.error, style = SunLinkTypography.caption,
                modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg))
        }
        full?.let { LastUpdatedLabel(it.timestamp) }
        all.filter { (category, name, _) ->
            query.isBlank() || "$category $name".contains(query, ignoreCase = true)
        }.filter { (_, _, value) ->
            when (filter) {
                ParameterFilter.ALL -> true
                ParameterFilter.AVAILABLE -> value?.normalized_value != null
                ParameterFilter.VERIFIED -> value?.confidence == "corroborated"
                ParameterFilter.UNVERIFIED -> value?.confidence == "provisional" || value?.confidence == "calculated"
                ParameterFilter.UNAVAILABLE -> value == null || value.normalized_value == null
                ParameterFilter.STALE -> value?.quality == "stale" || full?.stale == true
            }
        }.forEach { (category, name, value) ->
            ParameterRow(category, name, value)
        }
    }
}

private fun filterLabel(filter: ParameterFilter) = when (filter) {
    ParameterFilter.ALL -> R.string.filter_all
    ParameterFilter.AVAILABLE -> R.string.filter_available
    ParameterFilter.VERIFIED -> R.string.filter_verified
    ParameterFilter.UNVERIFIED -> R.string.filter_unverified
    ParameterFilter.UNAVAILABLE -> R.string.filter_unavailable
    ParameterFilter.STALE -> R.string.state_stale
}

@Composable
private fun ParameterRow(category: String, name: String, value: MeasurementValueDto?) {
    val colors = LocalSunLinkColors.current
    Card(
        modifier = Modifier.padding(horizontal = SunLinkDimensions.spaceLg),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SunLinkDimensions.spaceMd),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$category.$name", style = SunLinkTypography.label, color = colors.textPrimary)
                Text(
                    value?.confidence ?: stringResource(R.string.dash_no_data),
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary
                )
            }
            PowerValue(
                value = value?.normalized_value?.toFloat(),
                unit = value?.unit.orEmpty(),
                decimals = 2,
                style = SunLinkTypography.label,
                color = if (value?.quality == "stale") colors.warning else colors.textPrimary
            )
        }
    }
}
