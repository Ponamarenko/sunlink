package com.sunlink.svitgrid.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.viewmodel.MainViewModel

/**
 * DashboardScreen в стилі SvitGrid
 * Створено з нуля, точно повторює візуал оригіналу
 */
@Composable
fun DashboardScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    
    // Тестові дані (якщо API не працює)
    val testData = remember {
        mapOf(
            "power_dc" to mapOf("value" to 4680.0),
            "power_ac" to mapOf("value" to 4708.0),
            "battery_soc" to mapOf("value" to 65.0),
            "battery_voltage" to mapOf("value" to 48.2),
            "pv1_voltage" to mapOf("value" to 296.0),
            "pv1_current" to mapOf("value" to 2.1),
            "pv2_voltage" to mapOf("value" to 295.0),
            "pv2_current" to mapOf("value" to 2.0),
            "pv3_voltage" to mapOf("value" to 294.0),
            "pv3_current" to mapOf("value" to 1.9),
            "p_sun" to 4680.0,
            "p_load" to 4708.0,
            "p_batt" to 0.0,
            "p_grid" to 28.0
        )
    }
    
    val d = data ?: testData
    
    LaunchedEffect(Unit) { vm.loadLive() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12))
    ) {
        // Шапка
        HeaderSection()
        
        // Плашка блекаута
        BlackoutSection()
        
        // Таймлайн отключений
        TimelineSection()
        
        // PV-панелі
        PVSection(d)
        
        // Схема потоків
        FlowSchemeSection(d)
        
        // Нижня навігація
        BottomNavigationSection()
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HOME",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            fontSize = 20.sp
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "14° Мряка",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Щойно",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔔", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text("⛶", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text("☰", style = MaterialTheme.typography.bodyLarge, color = Color.White)
        }
    }
}

@Composable
fun BlackoutSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFCDDC39) // Жовтий
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Без світла 16 хв · на акумуляторі",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TimelineSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0..23) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i % 3 == 0) Color(0xFF66BB6A) else Color(0xFFE74C3C)
                    )
            )
        }
    }
}

@Composable
fun PVSection(data: Map<String, Any>) {
    val p_sun = (data["p_sun"] as? Number)?.toFloat() ?: 0f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 1..3) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Brush.horizontalGradient(
                        listOf(Color(0xFF131A24), Color(0xFF0A0D12))
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Загальна потужність сонця
                    Text(
                        text = "☀ ${(p_sun/1000f).toString().format(1f)} кВт",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    // PNG іконка PV-панелі (заміна на простий коло)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Напруга і струм
                    Text(
                        text = "296 В · 2.1 А",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.Gray
                    )
                    
                    // PV ідентифікатор і потужність
                    Text(
                        text = "PV1 · 0.6",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun FlowSchemeSection(data: Map<String, Any>) {
    val batterySOC = (data["battery_soc"] as? Map<String, Any>)?.get("value") as? Number?.toFloat() ?: 0f
    val batteryPower = (data["p_batt"] as? Number)?.toFloat() ?: 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Bezier лінії
            val path = Path().apply {
                moveTo(50f, 50f) // PV1
                cubicTo(100f, 50f, 150f, 100f, canvasWidth/2f, 100f) // до інвертора
                moveTo(canvasWidth/2f, 100f) // від інвертора
                cubicTo(canvasWidth/2f, 150f, 100f, 200f, 50f, canvasHeight-50f) // до батареї
                moveTo(canvasWidth/2f, 100f) // від інвертора
                cubicTo(canvasWidth/2f, 150f, canvasWidth-100f, 200f, canvasWidth-50f, canvasHeight-50f) // до мережі
                moveTo(canvasWidth/2f, 100f) // від інвертора
                cubicTo(canvasWidth/2f, canvasHeight-100f, canvasWidth/2f, canvasHeight-50f, canvasWidth/2f, canvasHeight-50f) // до дому
            }
            
            drawPath(path = path, color = Color(0xFF2196F3), style = Stroke(width = 3f))
            
            // Статична точка на лінії
            drawCircle(
                color = Color(0xFFFF9800),
                radius = 8f,
                center = Offset(100f, 75f)
            )
        }
        
        // Узли поверх Canvas
        // Інвертор (центр)
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF9800), Color(0xFFF57C00)
                    )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .align(Alignment.Center)
                .clickable { /* bottom sheet */ }
        )
        
        // Батарея (ліворуч)
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4CAF50), Color(0xFF388E3C)
                    )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .align(Alignment.CenterStart)
                .offset(x = (-50).dp, y = 50.dp)
                .clickable { /* bottom sheet */ }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🔋", fontSize = 24.sp)
                Text(
                    text = "${batterySOC.toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
                Text(
                    text = if (batteryPower > 0) "Заряджається · ${batteryPower.toInt()} Вт" 
                           else "Розряджається",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // Мережа (праворуч)
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2196F3), Color(0xFF1976D2)
                    )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .align(Alignment.CenterEnd)
                .offset(x = 50.dp, y = 50.dp)
                .clickable { /* bottom sheet */ }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🌐", fontSize = 24.sp)
                Text(
                    text = "Мережа",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
            }
        }
        
        // Дім (знизу)
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2)
                    )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .align(Alignment.BottomCenter)
                .clickable { /* bottom sheet */ }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🏠", fontSize = 24.sp)
                Text(
                    text = "Дім",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BottomNavigationSection() {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color(0xFF1A1A1A)
    ) {
        NavigationBarItem(
            icon = { Text("🏠") },
            selected = true,
            onClick = { /* навігація */ }
        )
        NavigationBarItem(
            icon = { Text("📊") },
            selected = false,
            onClick = { /* навігація */ }
        )
        NavigationBarItem(
            icon = { Text("📅") },
            selected = false,
            onClick = { /* навігація */ }
        )
        NavigationBarItem(
            icon = { Text("☀️") },
            selected = false,
            onClick = { /* навігація */ }
        )
        NavigationBarItem(
            icon = { Text("⚙️") },
            selected = false,
            onClick = { /* навігація */ }
        )
    }
}