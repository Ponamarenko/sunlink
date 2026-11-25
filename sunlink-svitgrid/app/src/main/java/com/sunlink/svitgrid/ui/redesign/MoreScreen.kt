package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.designsystem.sunLinkDarkColors

private data class MoreItem(
    val route: String,
    val label: String,
    val subtitle: String? = null,
    val monoSubtitle: Boolean = false,
    val icon: ImageVector,
    val accent: Color? = null
)

/**
 * Прямий порт Stitch node_2967584733526758360: компактний заголовок із tune,
 * група вторинних функцій із 36px плитками та група детального моніторингу
 * з компактними рядками. Активний маршрут підсвічується всередині списку.
 */
@Composable
fun MoreScreen(currentRoute: String, onNavigate: (String) -> Unit) {
    CompositionLocalProvider(LocalSunLinkColors provides sunLinkDarkColors()) {
        MoreScreenContent(currentRoute, onNavigate)
    }
}

@Composable
private fun MoreScreenContent(currentRoute: String, onNavigate: (String) -> Unit) {
    val colors = LocalSunLinkColors.current

    val secondary = listOf(
        MoreItem(
            route = "settings",
            label = "Налаштування",
            subtitle = "Сповіщення, інтервал оновлення",
            icon = Icons.Default.Settings
        ),
        MoreItem(
            route = "inverter-details",
            label = "Дані інвертора",
            subtitle = "DEYE SUN-12K-SG02LP1",
            monoSubtitle = true,
            icon = Icons.Default.Memory,
            accent = colors.primary
        ),
        MoreItem(
            route = "all-parameters",
            label = "Усі параметри",
            subtitle = "Повний перелік регістрів",
            icon = Icons.Default.ListAlt
        )
    )

    val monitoring = listOf(
        MoreItem("analytics", "Аналітика", icon = Icons.Default.BarChart, accent = colors.solar),
        MoreItem("grid", "Мережа", icon = Icons.Default.ElectricalServices, accent = colors.grid),
        MoreItem("load", "Навантаження", icon = Icons.Default.Home, accent = colors.load),
        MoreItem("diagnostics", "Діагностика", icon = Icons.Default.Troubleshoot, accent = colors.battery)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MoreHeader()

        // Вторинні функції: Settings / Inverter / All parameters.
        MoreGroupCard {
            secondary.forEachIndexed { index, item ->
                if (index > 0) MoreDivider()
                MoreLargeRow(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }
        }

        // Детальний моніторинг: компактні рядки з кольоровими іконками.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Детальний моніторинг",
                style = SunLinkTypography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            MoreGroupCard {
                monitoring.forEachIndexed { index, item ->
                    if (index > 0) MoreDivider()
                    MoreCompactRow(
                        item = item,
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreHeader() {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surface, RoundedCornerShape(16.dp))
                .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Більше", style = SunLinkTypography.headline, color = colors.textPrimary)
            Text("Додаткові розділи та система", style = SunLinkTypography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun MoreGroupCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalSunLinkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(20.dp))
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun MoreDivider() {
    val colors = LocalSunLinkColors.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.6f)))
}

@Composable
private fun MoreLargeRow(item: MoreItem, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.primarySoft else Color.Transparent)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.surfaceElevated, RoundedCornerShape(6.dp))
                .border(1.dp, colors.border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = if (selected) colors.primary else item.accent ?: colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.label,
                style = SunLinkTypography.title,
                color = if (selected) colors.primary else colors.textPrimary
            )
            if (item.subtitle != null) {
                Text(
                    item.subtitle,
                    style = if (item.monoSubtitle) SunLinkTypography.mono else SunLinkTypography.caption,
                    color = colors.textSecondary
                )
            }
        }
        MoreChevron()
    }
}

@Composable
private fun MoreCompactRow(item: MoreItem, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.primarySoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = if (selected) colors.primary else item.accent ?: colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            item.label,
            style = SunLinkTypography.body,
            color = if (selected) colors.primary else colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        MoreChevron()
    }
}

@Composable
private fun MoreChevron() {
    val colors = LocalSunLinkColors.current
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = colors.textSecondary,
        modifier = Modifier.size(20.dp)
    )
}
