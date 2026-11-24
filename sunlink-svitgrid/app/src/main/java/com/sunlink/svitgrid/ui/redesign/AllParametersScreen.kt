package com.sunlink.svitgrid.ui.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.viewmodel.MainViewModel
import java.util.Locale

private enum class ParamGroupKey { BATTERY, PV, GRID, LOAD, ENERGY, TEMPERATURE, SYSTEM }

private data class ParamItem(
    val category: String,
    val apiName: String,
    val label: String,
    val value: MeasurementValueDto?,
    val group: ParamGroupKey
)

private data class ParamGroupData(val key: ParamGroupKey, val items: List<ParamItem>)

private val PARAM_LABELS = mapOf(
    "battery.soc" to "Рівень заряду (SOC)",
    "battery.voltage" to "Напруга батареї",
    "battery.current" to "Струм батареї",
    "battery.power" to "Потужність батареї",
    "battery.temperature" to "Температура батареї",
    "battery.daily_charge_energy" to "Заряд батареї за добу",
    "battery.daily_discharge_energy" to "Розряд батареї за добу",
    "pv.pv1_voltage" to "Напруга PV1",
    "pv.pv1_current" to "Струм PV1",
    "pv.pv1_power" to "Потужність PV1",
    "pv.pv2_voltage" to "Напруга PV2",
    "pv.pv2_current" to "Струм PV2",
    "pv.pv2_power" to "Потужність PV2",
    "pv.pv3_voltage" to "Напруга PV3",
    "pv.pv3_current" to "Струм PV3",
    "pv.pv3_power" to "Потужність PV3",
    "pv.total_power" to "Загальна потужність PV",
    "pv.daily_energy" to "Генерація за добу",
    "pv.total_energy" to "Генерація всього",
    "grid.power" to "Активна потужність мережі",
    "grid.voltage_l1" to "Напруга мережі (L1)",
    "grid.voltage_l2" to "Напруга мережі (L2)",
    "grid.frequency" to "Частота мережі",
    "grid.connected" to "Підключення до мережі",
    "grid.import_energy" to "Імпорт за добу",
    "grid.export_energy" to "Експорт за добу",
    "load.power" to "Потужність навантаження",
    "load.voltage" to "Напруга навантаження",
    "load.current" to "Струм навантаження",
    "load.energy" to "Споживання за добу",
    "inverter.dc_temperature" to "Температура інвертора (DC)",
    "inverter.ac_temperature" to "Температура інвертора (AC)"
)

private val FIELD_LABELS = mapOf(
    "power" to "Потужність",
    "voltage" to "Напруга",
    "current" to "Струм",
    "temperature" to "Температура",
    "dc_temperature" to "Температура (DC)",
    "ac_temperature" to "Температура (AC)",
    "frequency" to "Частота",
    "connected" to "Підключення",
    "status" to "Статус",
    "state" to "Стан",
    "energy" to "Енергія"
)

/**
 * Екран «Усі параметри» (Stitch sunlink_ux_v2_12).
 * Каталог будується виключно з реальних даних `/api/telemetry/full`; відсутні
 * значення лишаються «—», а пошук працює за українською назвою та api_name.
 */
@Composable
fun AllParametersScreen(vm: MainViewModel = viewModel()) {
    val full by vm.fullTelemetry.collectAsState()
    val error by vm.fullTelemetryError.collectAsState()
    val colors = LocalSunLinkColors.current
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadFullTelemetry() }

    val groups = remember(full) { buildGroups(full) }
    val needle = query.trim()
    val filtered = groups
        .map { group -> group.copy(items = group.items.filter { matches(it, needle) }) }
        .filter { it.items.isNotEmpty() }
    val visibleCount = filtered.sumOf { it.items.size }
    val timestamp = formatTelemetryTimestamp(full?.timestamp)
    val subtitle = if (timestamp == "—") "Каталог телеметрії" else "Каталог телеметрії · $timestamp"

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Усі параметри", style = SunLinkTypography.headline, color = colors.textPrimary)
                Text(subtitle, style = SunLinkTypography.caption, color = colors.textTertiary)
            }
            StatusPill(visibleCount.toString(), colors.textSecondary)
        }

        SearchField(query = query, onQueryChange = { query = it })

        if (!error.isNullOrBlank()) {
            Text(error!!, style = SunLinkTypography.caption, color = colors.error)
        }

        when {
            groups.isEmpty() -> EmptyState("Телеметрія недоступна — реальних параметрів немає")
            visibleCount == 0 -> EmptyState("Параметрів не знайдено")
            else -> filtered.forEach { group ->
                val (label, accent) = groupStyle(group.key, colors)
                GroupCaption(label, accent)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    group.items.forEachIndexed { index, item ->
                        if (index > 0) ParameterDivider()
                        ParameterRow(
                            item = item,
                            globalStale = full?.stale == true,
                            offline = !error.isNullOrBlank()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun groupStyle(key: ParamGroupKey, colors: com.sunlink.svitgrid.ui.designsystem.SunLinkColors): Pair<String, Color> =
    when (key) {
        ParamGroupKey.BATTERY -> "Батарея" to colors.battery
        ParamGroupKey.PV -> "Сонце (PV)" to colors.solar
        ParamGroupKey.GRID -> "Мережа" to colors.grid
        ParamGroupKey.LOAD -> "Навантаження" to colors.load
        ParamGroupKey.ENERGY -> "Енергія" to colors.textSecondary
        ParamGroupKey.TEMPERATURE -> "Температура" to colors.load
        ParamGroupKey.SYSTEM -> "Система" to colors.textSecondary
    }

@Composable
private fun GroupCaption(label: String, accent: Color) {
    Text(
        label,
        style = SunLinkTypography.caption,
        color = accent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = LocalSunLinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = SunLinkTypography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isEmpty()) {
                Text("Пошук...", style = SunLinkTypography.body, color = colors.textTertiary)
            }
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Очистити пошук",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ParameterDivider() {
    val colors = LocalSunLinkColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border.copy(alpha = 0.6f))
    )
}

@Composable
private fun ParameterRow(item: ParamItem, globalStale: Boolean, offline: Boolean) {
    val colors = LocalSunLinkColors.current
    val value = item.value
    val number = value?.normalized_value?.toFloat()
    val stale = globalStale || value?.quality == "stale"
    val freshness = freshnessOf(
        stale = stale,
        quality = value?.quality,
        confidence = value?.confidence,
        online = if (offline) false else null
    )
    val secondary = when {
        number == null -> "Недоступно"
        freshness == Freshness.STALE -> "Застаріло"
        else -> telemetryLabel(value)
    }
    val valueColor = when {
        number == null -> colors.textTertiary
        freshness == Freshness.STALE -> colors.warning
        value?.confidence == "model_compatible" -> colors.primary
        else -> colors.textPrimary
    }
    val dotColor = when (freshness) {
        Freshness.FRESH -> colors.success
        Freshness.STALE, Freshness.PROVISIONAL -> colors.warning
        Freshness.UNAVAILABLE -> colors.offline
        Freshness.OFFLINE -> colors.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.label,
                style = SunLinkTypography.body,
                color = if (number == null) colors.textTertiary else colors.textPrimary
            )
            Text(item.apiName, style = SunLinkTypography.mono, color = colors.textTertiary)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (number == null) "—" else formatQuantity(number, value?.unit),
                style = SunLinkTypography.mono,
                color = valueColor
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(6.dp).background(dotColor, RoundedCornerShape(50)))
                Text(secondary, style = SunLinkTypography.caption, color = dotColor)
            }
        }
    }
}

private fun buildGroups(full: TelemetryFullDto?): List<ParamGroupData> {
    val items = full?.categories.orEmpty().flatMap { (category, fields) ->
        fields.orEmpty().map { (field, value) ->
            ParamItem(
                category = category,
                apiName = field,
                label = parameterLabel(category, field),
                value = value,
                group = groupOf(category, field)
            )
        }
    }
    return ParamGroupKey.values().mapNotNull { key ->
        val groupItems = items.filter { it.group == key }.sortedBy { it.apiName }
        if (groupItems.isEmpty()) null else ParamGroupData(key, groupItems)
    }
}

private fun matches(item: ParamItem, needle: String): Boolean =
    needle.isBlank() ||
        item.label.contains(needle, ignoreCase = true) ||
        item.apiName.contains(needle, ignoreCase = true) ||
        item.category.contains(needle, ignoreCase = true)

private fun parameterLabel(category: String, field: String): String =
    PARAM_LABELS["$category.$field"]
        ?: FIELD_LABELS[field]
        ?: field.replace('_', ' ').replaceFirstChar { it.uppercase() }

/** Розподіл реальних полів за групами Stitch без створення вигаданих параметрів. */
private fun groupOf(category: String, field: String): ParamGroupKey {
    val c = category.lowercase()
    val f = field.lowercase()
    return when {
        f.contains("temp") -> ParamGroupKey.TEMPERATURE
        f.contains("energy") || f.startsWith("daily_") -> ParamGroupKey.ENERGY
        c == "battery" -> ParamGroupKey.BATTERY
        c == "pv" || c == "solar" -> ParamGroupKey.PV
        c == "grid" -> ParamGroupKey.GRID
        c == "load" -> ParamGroupKey.LOAD
        c == "temperature" -> ParamGroupKey.TEMPERATURE
        c == "inverter" && (f.contains("status") || f.contains("state") ||
            f.contains("fault") || f.contains("warning")) -> ParamGroupKey.SYSTEM
        else -> ParamGroupKey.SYSTEM
    }
}

private fun formatQuantity(value: Float, unit: String?): String {
    val number = "%.2f".format(Locale.US, value)
        .trimEnd('0').trimEnd('.')
        .let { if (it == "-0") "0" else it }
    val cleanUnit = unit?.trim().orEmpty()
    return if (cleanUnit.isEmpty()) number else "$number $cleanUnit"
}
