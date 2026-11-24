package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    val accent: Color? = null,
    val compact: Boolean = false
)

/**
 * Екран «Більше» (Stitch sunlink_ux_v2_9): hero-ідентифікатор меню та дві
 * згруповані панелі з тонкими рядками. Головні вкладки не дублюються.
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
        MoreItem("analytics", "Аналітика", icon = Icons.Default.BarChart, accent = colors.solar, compact = true),
        MoreItem("grid", "Мережа", icon = Icons.Default.ElectricalServices, accent = colors.grid, compact = true),
        MoreItem("load", "Навантаження", icon = Icons.Default.Home, accent = colors.load, compact = true),
        MoreItem("diagnostics", "Діагностика", icon = Icons.Default.Troubleshoot, accent = colors.battery, compact = true)
    )

    SunLinkScreen(
        title = "Більше",
        subtitle = "Додаткові розділи та система"
    ) {
        HeroPanel(accent = colors.primary) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceLg)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.primarySoft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    SectionLabel("Меню")
                    Text("Всі розділи застосунку", style = SunLinkTypography.title, color = colors.textPrimary)
                    Text(
                        "Налаштування та детальний моніторинг",
                        style = SunLinkTypography.caption,
                        color = colors.textTertiary
                    )
                }
                StatusPill("${secondary.size + monitoring.size} розділів", colors.primary)
            }
        }

        GroupCard(title = "Вторинні функції", subtitle = "Налаштування та пристрій") {
            secondary.forEachIndexed { index, item ->
                if (index > 0) GroupDivider()
                MoreRow(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }
        }

        GroupCard(title = "Детальний моніторинг", subtitle = "Канали, графіки, діагностика") {
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
        color = colors.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

/** Групова поверхня: DataPanel із заголовком замість окремої картки. */
@Composable
private fun GroupCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) = DataPanel(title = title, subtitle = subtitle, content = content)

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = if (item.compact) PremiumTokens.spaceSm else PremiumTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceMd)
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
                        .background(colors.surfaceElevated, RoundedCornerShape(10.dp)),
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
