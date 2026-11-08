package com.sunlink.svitgrid.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.viewmodel.MainViewModel
import androidx.compose.ui.graphics.Color

/**
 * DashboardScreen в стилі SvitGrid
 * Створено з нуля, адаптовано під API k4a.pp.ua:8000
 */
@Composable
fun DashboardScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    
    LaunchedEffect(Unit) { vm.loadLive() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Шапка
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOME",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { /* fullscreen */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Повний екран")
                }
                IconButton(onClick = { /* notifications */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Сповіщення")
                }
            }
        }
        
        // Погода (з /weather)
        Text(
            text = "Погода: 14° Мряка",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }
        
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            return
        }
        
        val d = data
        if (d != null) {
            // PV-панели (3 панелі)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("PV$i", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${d["voltage_dc"]} В · ${d["current_dc"]} А", style = MaterialTheme.typography.bodySmall)
                            Text("${((d["power_dc"] as? Number)?.toFloat() ?: 0f) / 1000f} кВт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Схема потоків енергії
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Схема потоків", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    // PV → Інвертор
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "PV", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Потік", tint = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.Info, contentDescription = "Інвертор", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Інвертор → Батарея/Мережа
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Favorite, contentDescription = "Батарея", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
                            Text("${d["battery_soc"]}%", style = MaterialTheme.typography.bodySmall)
                            Text("Заряджається · ${d["power_ac"]} Вт", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Потік", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = "Мережа", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            Text("${d["grid_voltage"]} В", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            // Додаткова інформація
            val stats = listOf(
                "Ефективність" to "${d["efficiency"]} %",
                "Енергія сьогодні" to "${d["energy_today"]} кВт·год",
                "Енергія всього" to "${d["energy_total"]} кВт·год",
                "Частота" to "${d["frequency"]} Гц"
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats.forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Text("Немає даних", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
