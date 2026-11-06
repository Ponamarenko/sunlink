package com.sunlink.svitgrid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
                    "dashboard" to "Головна",
                    "live" to "Live моніторинг",
                    "charts" to "Графіки",
                    "battery" to "Батарея",
                    "forecast" to "Прогноз",
                    "settings" to "Налаштування",
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
                composable("settings") { SettingsScreen() }
                composable("about") { AboutScreen() }
            }
        }
    }
}

@Composable
fun DashboardScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.loadLive() }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Головна панель", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        data?.forEach { (k, v) ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(k, style = MaterialTheme.typography.titleMedium)
                        Text("$v", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveScreen(vm: MainViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.loadLive() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Моніторинг в реальному часі", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (data != null) {
                    data!!.forEach { (k, v) -> Text("$k: $v", style = MaterialTheme.typography.bodyLarge) }
                } else {
                    Text("Немає даних", style = MaterialTheme.typography.bodyLarge)
                }
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
