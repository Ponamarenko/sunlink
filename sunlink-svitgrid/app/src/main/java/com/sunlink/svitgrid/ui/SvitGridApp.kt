package com.sunlink.svitgrid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvitGridApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "SunLink",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                Spacer(Modifier.height(8.dp))
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                listOf(
                    "dashboard" to "Головна",
                    "live" to "Live моніторинг",
                    "charts" to "Графіки",
                    "battery" to "Батарея",
                    "forecast" to "Прогноз",
                    "about" to "Про додаток"
                ).forEach { (route, label) ->
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) { popUpTo("dashboard") { saveState = true } }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SunLink") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") { DashboardScreen() }
                composable("live") { LiveScreen() }
                composable("charts") { ChartsScreen() }
                composable("battery") { BatteryScreen() }
                composable("forecast") { ForecastScreen() }
                composable("about") { AboutScreen() }
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Головна панель", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Потужність", style = MaterialTheme.typography.titleMedium)
                Text("2,500 Вт", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Напруга", style = MaterialTheme.typography.titleMedium)
                Text("230 В", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Батарея", style = MaterialTheme.typography.titleMedium)
                Text("75%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LiveScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Моніторинг в реальному часі", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Поточна потужність: 2,500 Вт", style = MaterialTheme.typography.bodyLarge)
                Text("Напруга: 230 В", style = MaterialTheme.typography.bodyLarge)
                Text("Струм: 10.9 А", style = MaterialTheme.typography.bodyLarge)
                Text("Температура: 35°C", style = MaterialTheme.typography.bodyLarge)
                Text("Статус: Онлайн", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ChartsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Графіки та аналітика", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Графік потужності", style = MaterialTheme.typography.titleMedium)
                Text("За останні 24 години", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Графік ефективності", style = MaterialTheme.typography.titleMedium)
                Text("Ефективність системи з часом", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun BatteryScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Управління батареєю", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Рівень заряду", style = MaterialTheme.typography.titleMedium)
                Text("75%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Температура: 25°C", style = MaterialTheme.typography.bodyLarge)
                Text("Напруга: 52.5 В", style = MaterialTheme.typography.bodyLarge)
                Text("Здоров'я: 95%", style = MaterialTheme.typography.bodyLarge)
                Text("Цикли: 250", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ForecastScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Погода та прогноз", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Погода: Сонячно, 25°C", style = MaterialTheme.typography.bodyLarge)
                Text("Вологість: 60%", style = MaterialTheme.typography.bodyLarge)
                Text("Вітер: 5 м/с", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Прогноз енергії", style = MaterialTheme.typography.titleMedium)
                Text("Завтра: 15.2 кВт·год", style = MaterialTheme.typography.bodyLarge)
                Text("Тиждень: 98.5 кВт·год", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun AboutScreen() {
    var updateStatus by remember { mutableStateOf("Натисніть кнопку для перевірки") }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Про додаток", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SunLink", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Версія: 1.6.0", style = MaterialTheme.typography.bodyLarge)
                Text("Розробник: Ponamarenko", style = MaterialTheme.typography.bodyLarge)
                Text("Ліцензія: MIT", style = MaterialTheme.typography.bodyLarge)
                Text("© 2026 Ponamarenko", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Опис", style = MaterialTheme.typography.titleMedium)
                Text("SunLink — сучасний додаток для моніторингу сонячних систем. Підтримує різні типи підключення: RS485, ESP32, Solarman WiFi, Ethernet.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Оновлення", style = MaterialTheme.typography.titleMedium)
                Text(updateStatus, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        isChecking = true
                        updateStatus = "Перевірка..."
                        scope.launch {
                            try {
                                val client = OkHttpClient()
                                val request = Request.Builder()
                                    .url("https://api.github.com/repos/Ponamarenko/sunlink/releases/latest")
                                    .build()
                                val response = client.newCall(request).execute()
                                if (response.isSuccessful) {
                                    val json = JSONObject(response.body?.string() ?: "{}")
                                    val tagName = json.optString("tag_name", "невідомо")
                                    val name = json.optString("name", "невідомо")
                                    updateStatus = "Остання версія: $tagName\n$name"
                                } else {
                                    updateStatus = "Помилка перевірки: ${response.code}"
                                }
                            } catch (e: Exception) {
                                updateStatus = "Помилка: ${e.message}"
                            } finally {
                                isChecking = false
                            }
                        }
                    },
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isChecking) "Перевірка..." else "Перевірити оновлення")
                }
            }
        }
    }
}
