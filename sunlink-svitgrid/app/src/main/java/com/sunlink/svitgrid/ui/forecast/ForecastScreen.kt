package com.sunlink.svitgrid.ui.forecast

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

/**
 * ForecastScreen в стилі SvitGrid
 * Відображає прогноз погоди та енергетичні дані
 */
@Composable
fun ForecastScreen(vm: MainViewModel = viewModel()) {
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
                text = "FORECAST",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            IconButton(onClick = { /* refresh */ }) {
                Icon(Icons.Default.Refresh, contentDescription = "Оновити")
            }
        }
        
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
            // Погода на сьогодні
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Погода сьогодні", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☀️", style = MaterialTheme.typography.titleLarge)
                            Text("${d["temperature"]?.toString() ?: "0"}°C", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💧", style = MaterialTheme.typography.titleLarge)
                            Text("${d["humidity"]?.toString() ?: "0"}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💨", style = MaterialTheme.typography.titleLarge)
                            Text("${d["wind_speed"]?.toString() ?: "0"} м/с", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Прогноз на 3 дні
            Text(
                text = "Прогноз на 3 дні",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("День $i", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("☁️", style = MaterialTheme.typography.headlineSmall)
                            Text("15-22°C", style = MaterialTheme.typography.bodySmall)
                            Text("Вітер 3-5 м/с", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            // Енергетичний прогноз
            Text(
                text = "Енергетичний прогноз",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Прогенеровано сьогодні", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡", style = MaterialTheme.typography.titleLarge)
                            Text("${d["power_dc"]?.toString() ?: "0"} Вт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("DC потужність", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        
                        Text("→", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡", style = MaterialTheme.typography.titleLarge)
                            Text("${d["power_ac"]?.toString() ?: "0"} Вт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("AC потужність", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    
                    Text(
                        text = "Ефективність: ${d["efficiency"]?.toString() ?: "0"}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Інформація про батарею
            if (d["battery_soc"] != null && (d["battery_soc"] as? Number)?.toFloat() ?: 0f > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Стан батареї", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔋", style = MaterialTheme.typography.titleLarge)
                                Text("${d["battery_soc"]}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔌", style = MaterialTheme.typography.titleLarge)
                                Text("${d["battery_voltage"]} В", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Text("Немає даних для прогнозу", style = MaterialTheme.typography.bodyLarge)
        }
    }
}