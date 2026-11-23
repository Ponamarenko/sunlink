package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography

private data class MoreItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MoreScreen(currentRoute: String, onNavigate: (String) -> Unit) {
    val colors = LocalSunLinkColors.current
    val groups = listOf(
        stringResource(R.string.drawer_monitoring) to listOf(
            MoreItem("grid", stringResource(R.string.screen_grid), Icons.Default.GridOn),
            MoreItem("load", stringResource(R.string.screen_load), Icons.Default.Power),
            MoreItem("energy-flow", stringResource(R.string.screen_energy_flow), Icons.Default.Analytics)
        ),
        stringResource(R.string.drawer_analytics) to listOf(
            MoreItem("analytics", stringResource(R.string.statistics), Icons.Default.Analytics),
            MoreItem("history", stringResource(R.string.nav_history), Icons.Default.History),
            MoreItem("statistics", stringResource(R.string.nav_statistics), Icons.Default.List)
        ),
        stringResource(R.string.drawer_system) to listOf(
            MoreItem("diagnostics", stringResource(R.string.nav_diagnostics), Icons.Default.Memory),
            MoreItem("settings", stringResource(R.string.nav_settings), Icons.Default.Settings),
            MoreItem("about", stringResource(R.string.nav_about), Icons.Default.Info)
        ),
        stringResource(R.string.drawer_other) to listOf(
            MoreItem("inverter-details", stringResource(R.string.nav_inverter), Icons.Default.Memory),
            MoreItem("telemetry", stringResource(R.string.nav_telemetry), Icons.Default.List),
            MoreItem("inverter-status", stringResource(R.string.screen_inverter), Icons.Default.Memory),
            MoreItem("all-parameters", stringResource(R.string.all_parameters), Icons.Default.List),
            MoreItem("live", stringResource(R.string.live_monitoring), Icons.Default.Stream),
            MoreItem("forecast", stringResource(R.string.nav_forecast), Icons.Default.DateRange),
            MoreItem("debug", stringResource(R.string.nav_debug), Icons.Default.BugReport)
        )
    )

    SunLinkScreen(
        title = stringResource(R.string.nav_more),
        subtitle = stringResource(R.string.more_subtitle)
    ) {
        groups.forEach { (title, items) ->
            Text(title, style = SunLinkTypography.caption, color = colors.primary, modifier = Modifier.padding(top = 4.dp))
            items.forEach { item ->
                SunLinkCard(
                    modifier = Modifier.clickable { onNavigate(item.route) },
                    accent = if (currentRoute == item.route) colors.primary else colors.border
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(item.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        Text(item.label, style = SunLinkTypography.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
