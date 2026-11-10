package com.sunlink.svitgrid.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunlink.svitgrid.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val BgDark = Color(0xFF0A0D12)
private val BgCard = Color(0xFF131A24)
private val Mono = FontFamily.Monospace

private fun fmt(v: Any?): String = when (v) {
    null -> "—"
    is Number -> v.toString()
    is Boolean -> v.toString()
    else -> v.toString()
}

@Composable
fun DebugScreen() {
    var invList by remember { mutableStateOf<List<String>>(emptyList()) }
    var invData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    var stats by remember { mutableStateOf<Map<String, Any>?>(null) }
    var health by remember { mutableStateOf<Map<String, Any>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                loading = invData.isEmpty()
                val api = RetrofitClient.getApiService()
                try {
                    val h = api.getHealth()
                    if (h.isSuccessful) health = h.body() as? Map<String, Any>
                } catch (_: Exception) {}
                try {
                    val st = api.getSystemStats()
                    if (st.isSuccessful) stats = st.body()
                } catch (_: Exception) {}
                try {
                    val invResp = api.getInverters()
                    val body = invResp.body()
                    val ids = (body?.get("inverters") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    invList = ids
                    val map = mutableMapOf<String, Map<String, Any>>()
                    for (id in ids) {
                        try {
                            val d = api.getInverterData(id)
                            if (d.isSuccessful) {
                                val m = d.body()
                                if (m != null) map[id] = m
                            }
                        } catch (_: Exception) {}
                    }
                    invData = map
                    error = null
                } catch (e: Exception) {
                    error = e.message
                }
                lastUpdate = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(12.dp)
    ) {
        Text("Відладка / Сирі дані", color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Оновлення кожні 5с • Останнє: $lastUpdate • База: ${RetrofitClient.getBaseUrl()}", color = Color.Gray, fontFamily = Mono, fontSize = 9.sp)
        error?.let { Text("Помилка: $it", color = Color(0xFFE57373), fontFamily = Mono, fontSize = 10.sp) }
        Spacer(Modifier.height(8.dp))
        if (loading && invData.isEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Color.White) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("КЛЮЧОВІ ЗНАЧЕННЯ (зведено)", color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold)
                        val p1 = invData["INV001"]
                        val p2 = invData["INV002"]
                        val pv1w = (p1?.get("p_string1") as? Number)?.toDouble() ?: ((p1?.get("pv1_power") as? Number)?.toDouble())
                        val pv2w = (p1?.get("p_string2") as? Number)?.toDouble() ?: ((p2?.get("p_string1") as? Number)?.toDouble())
                        val pv3w = (p2?.get("p_string2") as? Number)?.toDouble() ?: (p2?.get("p_string3") as? Number)?.toDouble()
                        val gridW = (p1?.get("p_grid") as? Number)?.toDouble() ?: (p2?.get("p_grid") as? Number)?.toDouble()
                        val battW = (p1?.get("p_batt") as? Number)?.toDouble() ?: (p2?.get("p_batt") as? Number)?.toDouble()
                        val loadW = (p1?.get("p_load") as? Number)?.toDouble() ?: (p2?.get("p_load") as? Number)?.toDouble() ?: ((p1?.get("power_ac") as? Number)?.toDouble()?.plus((p2?.get("power_ac") as? Number)?.toDouble() ?: 0.0))
                        val battSoc = (p1?.get("battery_soc") as? Number)?.toDouble() ?: (p2?.get("battery_soc") as? Number)?.toDouble()
                        val battV = (p1?.get("battery_voltage") as? Number)?.toDouble() ?: (p2?.get("battery_voltage") as? Number)?.toDouble()
                        Text("PV1 = ${if (pv1w != null) "${pv1w.toInt()} W" else "—"}", color = Color.White, fontFamily = Mono)
                        Text("PV2 = ${if (pv2w != null) "${pv2w.toInt()} W" else "—"}", color = Color.White, fontFamily = Mono)
                        Text("PV3 = ${if (pv3w != null) "${pv3w.toInt()} W" else "—"}", color = Color.White, fontFamily = Mono)
                        Text("Сеть = ${if (gridW != null) "${gridW.toInt()} W" else "—"}", color = Color.White, fontFamily = Mono)
                        val battStr = if (battW != null || battSoc != null) {
                            "${if (battW != null) "${battW.toInt()} W" else "—"}${if (battSoc != null) " (${battSoc.toInt()}%" else ""}${if (battV != null) ", ${String.format("%.1f", battV)}V)" else if (battSoc != null) ")" else ""}"
                        } else "—"
                        Text("Батарея = $battStr", color = Color.White, fontFamily = Mono)
                        Text("Дом = ${if (loadW != null) if (loadW >= 1000) "${String.format("%.1f", loadW/1000.0)} kW" else "${loadW.toInt()} W" else "—"}", color = Color.White, fontFamily = Mono)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("health", color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold)
                        val h = health
                        if (h == null) Text("health = ОТСУТСТВУЄ", color = Color(0xFFE57373), fontFamily = Mono, fontSize = 11.sp)
                        else h.toSortedMap().forEach { (k,v) -> Text("$k = ${fmt(v)}", color = Color.LightGray, fontFamily = Mono, fontSize = 11.sp) }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("system/stats", color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold)
                        val s = stats
                        if (s == null) Text("system/stats = ОТСУТСТВУЄ", color = Color(0xFFE57373), fontFamily = Mono, fontSize = 11.sp)
                        else s.toSortedMap().forEach { (k,v) -> Text("$k = ${fmt(v)}", color = Color.LightGray, fontFamily = Mono, fontSize = 11.sp) }
                    }
                }
            }
            invList.forEach { id ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(id, color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold)
                            val m = invData[id]
                            if (m == null) Text("$id/data = ОТСУТСТВУЄ", color = Color(0xFFE57373), fontFamily = Mono, fontSize = 11.sp)
                            else {
                                val keys = listOf("p_string1","p_string2","p_string3","pv1_voltage","pv1_current","pv2_voltage","pv2_current","pv3_voltage","pv3_current","p_sun","p_load","p_batt","p_grid","batt_soc","battery_soc","battery_voltage","battery_current","battery_temp","grid_voltage","grid_voltage_l1")
                                keys.forEach { k -> Text("$k = ${if (m.containsKey(k)) fmt(m[k]) else "ОТСУТСТВУЄ"}", color = if (m.containsKey(k)) Color.LightGray else Color(0xFFE57373), fontFamily = Mono, fontSize = 11.sp) }
                                Spacer(Modifier.height(4.dp))
                                Text("ВСІ ПОЛЯ:", color = Color.White, fontFamily = Mono)
                                m.toSortedMap().forEach { (k,v) -> Text("$k = ${fmt(v)}", color = Color(0xFFB0BEC5), fontFamily = Mono, fontSize = 10.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
