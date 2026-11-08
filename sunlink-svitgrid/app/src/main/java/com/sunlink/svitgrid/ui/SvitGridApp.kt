package com.sunlink.svitgrid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.sunlink.svitgrid.data.SettingsRepository
import com.sunlink.svitgrid.network.RetrofitClient
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.viewmodel.MainViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvitGridApp() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val repo = SettingsRepository(context)
        RetrofitClient.setBaseUrl(repo.getBaseUrl())
    }
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
                    "dashboard" to ("Головна" to Icons.Default.Home),
                    "live" to ("Live моніторинг" to Icons.Default.Info),
                    "charts" to ("Графіки" to Icons.Default.List),
                    "battery" to ("Батарея" to Icons.Default.Favorite),
                    "forecast" to ("Прогноз" to Icons.Default.DateRange),
                    "settings" to ("Налаштування" to Icons.Default.Settings),
                    "about" to ("Про додаток" to Icons.Default.Info)
                ).forEach { (route, pair) ->
                    val (label, icon) = pair
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null) },
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
                composable("dashboard") { com.sunlink.svitgrid.ui.dashboard.DashboardScreen() }
                composable("live") { LiveScreen() }
                composable("charts") { ChartsScreen() }
                composable("battery") { BatteryScreen() }
                composable("forecast") { com.sunlink.svitgrid.ui.forecast.ForecastScreen() }
                composable("settings") { SettingsScreen() }
                composable("about") { AboutScreen() }
            }
        }
    }
}

// Старий DashboardScreen видалено, використовується новий з пакета dashboard

@Composable
fun LiveScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.loadLive() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Моніторинг в реальному часі", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val d = data
        if (d != null) {
            val items = listOf(
                "Потужність AC" to "${d["power_ac"]} Вт",
                "Потужність DC" to "${d["power_dc"]} Вт",
                "Напруга AC" to "${d["voltage_ac"]} В",
                "Напруга DC" to "${d["voltage_dc"]} В",
                "Струм AC" to "${d["current_ac"]} А",
                "Струм DC" to "${d["current_dc"]} А",
                "Частота" to "${d["frequency"]} Гц",
                "Температура" to "${d["temperature"]} °C",
                "Ефективність" to "${d["efficiency"]} %",
                "Енергія сьогодні" to "${d["energy_today"]} кВт·год",
                "Енергія всього" to "${d["energy_total"]} кВт·год",
                "Стан" to "${d["state"]}"
            )
            items.forEachIndexed { index, (label, value) ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 80L); visible = true }
                AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Text("Немає даних", style = MaterialTheme.typography.bodyLarge)
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
fun BatteryScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.loadLive() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Батарея", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val d = data
        if (d != null) {
            val items = listOf(
                "Рівень заряду (SOC)" to "${d["battery_soc"]} %",
                "Напруга батареї" to "${d["battery_voltage"]} В",
                "Напруга мережі" to "${d["grid_voltage"]} В",
                "Температура" to "${d["temperature"]} °C",
                "Енергія сьогодні" to "${d["energy_today"]} кВт·год",
                "Енергія всього" to "${d["energy_total"]} кВт·год"
            )
            items.forEach { (label, value) ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Text("Немає даних", style = MaterialTheme.typography.bodyLarge)
        }
    }
}



@Composable
fun SettingsScreen(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    var serverAddress by remember { mutableStateOf(repo.serverAddress) }
    var serverPort by remember { mutableStateOf(repo.serverPort) }
    var username by remember { mutableStateOf(repo.username) }
    var password by remember { mutableStateOf(repo.password) }
    var savedMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Налаштування", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Підключення до сервера", style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    label = { Text("Адреса сервера") },
                    placeholder = { Text("sunline.pp.ua") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { serverPort = it },
                    label = { Text("Порт") },
                    placeholder = { Text("8000") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Логін") },
                    placeholder = { Text("admin") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        repo.serverAddress = serverAddress
                        repo.serverPort = serverPort
                        repo.username = username
                        repo.password = password
                        RetrofitClient.setBaseUrl(repo.getBaseUrl())
                        savedMessage = "Налаштування збережено: $serverAddress:$serverPort"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зберегти")
                }
                
                if (savedMessage.isNotEmpty()) {
                    Text(savedMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                val connStatus by vm.error.collectAsState()
                val loading by vm.loading.collectAsState()
                Button(
                    onClick = { vm.checkConnection() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (loading) "Перевірка..." else "Тест підключення") }
                connStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Інформація", style = MaterialTheme.typography.titleMedium)
                Text("Сервер за замовчуванням: sunline.pp.ua:8000", style = MaterialTheme.typography.bodySmall)
                Text("Авторизація буде реалізована на сервері", style = MaterialTheme.typography.bodySmall)
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
                Text("Версія: 2.7.2", style = MaterialTheme.typography.bodyLarge)
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
                                val client = okhttp3.OkHttpClient()
                                val request = okhttp3.Request.Builder()
                                    .url("https://api.github.com/repos/Ponamarenko/sunlink/releases/latest")
                                    .header("User-Agent", "SunLink-App")
                                    .header("Accept", "application/vnd.github+json")
                                    .build()
                                val response = client.newCall(request).execute()
                                val body = response.body?.string() ?: ""
                                updateStatus = "HTTP ${response.code} len=${body.length}\n${body.take(200)}"
                            } catch (e: Exception) {
                                updateStatus = "Помилка: ${e.message}"
                            }
                            isChecking = false
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
