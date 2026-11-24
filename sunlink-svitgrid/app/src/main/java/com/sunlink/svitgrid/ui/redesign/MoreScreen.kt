package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography

private data class MoreItem(
    val route: String,
    val label: String,
    val subtitle: String? = null,
    val monoSubtitle: Boolean = false,
    val icon: ImageVector,
    val accent: Color? = null,
    val compact: Boolean = false
)

/**
 * Екран «Більше» (Stitch sunlink_ux_v2_9).
 * Одна згрупована картка на розділ, розділювачі між рядками, 16px поля, 12px проміжки.
 * Усі кольори — з темної семантичної палітри застосунку.
 */
@Composable
fun MoreScreen(currentRoute: String, onNavigate: (String) -> Unit) {
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
        MoreItem("analytics", "Аналітика", icon = Icons.Default.BarChart, accent = colors.solar, compact = true),
        MoreItem("grid", "Мережа", icon = Icons.Default.ElectricalServices, accent = colors.grid, compact = true),
        MoreItem("load", "Навантаження", icon = Icons.Default.Home, accent = colors.load, compact = true),
        MoreItem("diagnostics", "Діагностика", icon = Icons.Default.Troubleshoot, accent = colors.battery, compact = true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.background, colors.surface.copy(alpha = 0.65f)))
            )
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Більше", style = SunLinkTypography.headline, color = colors.textPrimary)
                Text(
                    "Додаткові розділи та система",
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary
                )
            }
        }

        SectionLabel("Вторинні функції")
        GroupCard {
            secondary.forEachIndexed { index, item ->
                if (index > 0) GroupDivider()
                MoreRow(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }
        }

        SectionLabel("Детальний моніторинг")
        GroupCard {
            monitoring.forEachIndexed { index, item ->
                if (index > 0) GroupDivider()
                MoreRow(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalSunLinkColors.current
    Text(
        text,
        style = SunLinkTypography.caption,
        color = colors.textSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalSunLinkColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = { Column(content = content) }
    )
}

@Composable
private fun GroupDivider() {
    val colors = LocalSunLinkColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border.copy(alpha = 0.6f))
    )
}

@Composable
private fun MoreRow(item: MoreItem, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSunLinkColors.current
    val horizontal = if (item.compact) 16.dp else PremiumTokens.spaceLg
    val vertical = if (item.compact) 12.dp else PremiumTokens.spaceLg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = horizontal, vertical = vertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (item.compact) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.accent ?: colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
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
                        tint = item.accent ?: colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    item.label,
                    style = if (item.compact) SunLinkTypography.body else SunLinkTypography.title,
                    color = if (selected) colors.primary else colors.textPrimary
                )
                if (item.subtitle != null) {
                    Text(
                        item.subtitle,
                        style = if (item.monoSubtitle) SunLinkTypography.mono else SunLinkTypography.caption,
                        color = colors.textTertiary
                    )
                }
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
