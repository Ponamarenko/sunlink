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
import androidx.compose.ui.unit.dp
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.sunlink.svitgrid.BuildConfig
import com.sunlink.svitgrid.data.SettingsRepository
import com.sunlink.svitgrid.network.RetrofitClient
import com.sunlink.svitgrid.ui.debug.DebugScreen
import com.sunlink.svitgrid.update.UpdateInfo
import com.sunlink.svitgrid.update.UpdateManager
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.viewmodel.MainViewModel
import com.sunlink.svitgrid.viewmodel.MainViewModelFactory
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterUiState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.sunlink.svitgrid.ui.dashboard.DashboardScreen
import com.sunlink.svitgrid.ui.history.HistoryScreen
import com.sunlink.svitgrid.ui.inverters.InverterDetailsScreen
import com.sunlink.svitgrid.ui.diagnostics.DiagnosticsScreen

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
                    "history" to ("Історія" to Icons.Default.List),
                    "inverter-details" to ("Інвертор" to Icons.Default.Info),
                    "diagnostics" to ("Діагностика" to Icons.Default.Build),
                    "live" to ("Live моніторинг" to Icons.Default.Info),
                    "battery" to ("Батарея" to Icons.Default.Favorite),
                    "forecast" to ("Прогноз" to Icons.Default.DateRange),
                    "debug" to ("Відладка / Сирі дані" to Icons.Default.Info),
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
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(
                    repository = DataRepository(RetrofitClient.getApiService())
                )
            )
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") { DashboardScreen(vm = viewModel) }
                composable("history") { HistoryScreen(vm = viewModel) }
                composable("inverter-details") { InverterDetailsScreen(vm = viewModel) }
                composable("diagnostics") { DiagnosticsScreen(vm = viewModel) }
                composable("live") { LiveScreen(vm = viewModel) }
                composable("battery") { BatteryScreen(vm = viewModel) }
                composable("forecast") { com.sunlink.svitgrid.ui.forecast.ForecastScreen(vm = viewModel) }
                composable("debug") { DebugScreen() }
                composable("settings") { SettingsScreen(vm = viewModel) }
                composable("about") { AboutScreen() }
            }
        }
    }
}

// Старий DashboardScreen видалено, використовується новий з пакета dashboard

@Composable
fun LiveScreen(vm: MainViewModel = viewModel(factory = MainViewModelFactory(repository = DataRepository(RetrofitClient.getApiService())))) {
    val uiState by vm.uiState.collectAsState()
    val loading by vm.refreshing.collectAsState()

    val uiStateValue = uiState
    val error = when (uiStateValue) {
        is InverterUiState.Error -> uiStateValue.message
        else -> null
    }
    val data = when (uiStateValue) {
        is InverterUiState.Success -> uiStateValue.data.toMap()
        else -> emptyMap<String, Any?>()
    }
    LaunchedEffect(Unit) { vm.loadData() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Моніторинг в реальному часі", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val d = data
        if (d.isNotEmpty()) {
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
fun BatteryScreen(vm: MainViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val loading by vm.refreshing.collectAsState()

    val uiStateValue = uiState
    val error = when (uiStateValue) {
        is InverterUiState.Error -> uiStateValue.message
        else -> null
    }
    val d = when (uiStateValue) {
        is InverterUiState.Success -> uiStateValue.data
        else -> InverterData()
    }
    LaunchedEffect(Unit) { vm.loadData() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Батарея", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (d.hasMeasurements) {
            val items = listOf(
                "Рівень заряду (SOC)" to "${d.batterySoc?.let { it.toInt() } ?: "—"} %",
                "Напруга батареї" to "${d.batteryVoltage?.let { String.format("%.1f", it) } ?: "—"} В",
                "Напруга мережі" to "${d.gridVoltageL1?.let { String.format("%.1f", it) } ?: "—"} В",
                "Температура" to "${d.acTemperature?.let { String.format("%.1f", it) } ?: "—"} °C",
                "Енергія сьогодні" to "— кВт·год",
                "Енергія всього" to "— кВт·год"
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
    val scope = rememberCoroutineScope()
    var serverAddress by remember { mutableStateOf(repo.serverAddress) }
    var serverPort by remember { mutableStateOf(repo.serverPort) }
    var useHttps by remember { mutableStateOf(repo.useHttps) }
    var savedMessage by remember { mutableStateOf("") }
    var settingsError by remember { mutableStateOf<String?>(null) }

    var checkingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val uiState by vm.uiState.collectAsState()
    val connectionLoading by vm.refreshing.collectAsState()

    val uiStateValue = uiState
    val connError = when (uiStateValue) {
        is InverterUiState.Error -> uiStateValue.message
        else -> null
    }

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
                    onValueChange = { serverAddress = it; settingsError = null },
                    label = { Text("Адреса сервера") },
                    placeholder = { Text(com.sunlink.svitgrid.data.BackendDefaults.HOST) },
                    supportingText = { Text("Публічний backend: https://sunlink.pp.ua:8443/") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = settingsError != null
                )

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { serverPort = it; settingsError = null },
                    label = { Text("Порт") },
                    placeholder = { Text(com.sunlink.svitgrid.data.BackendDefaults.PORT.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = settingsError != null
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useHttps, onCheckedChange = { useHttps = it })
                    Text("Використовувати HTTPS")
                }

                Button(
                    onClick = {
                        val endpoint = SettingsRepository.parseEndpoint(serverAddress)
                        if (endpoint == null) {
                            settingsError = "Некоректна адреса сервера"
                            return@Button
                        }
                        val finalPort = endpoint.port?.toString() ?: serverPort
                        val explicitScheme = serverAddress.contains("://")
                        val finalHttps = if (explicitScheme) SettingsRepository.isHttpsInput(serverAddress) else useHttps
                        val error = SettingsRepository.validate(endpoint.host, finalPort, finalHttps)
                        if (error != null) {
                            settingsError = error
                            return@Button
                        }
                        repo.useHttps = finalHttps
                        repo.serverAddress = endpoint.host
                        repo.serverPort = finalPort
                        val baseUrl = repo.getBaseUrl()
                        if (!RetrofitClient.setBaseUrl(baseUrl)) {
                            settingsError = "Не вдалося застосувати адресу: $baseUrl"
                            return@Button
                        }
                        serverAddress = endpoint.host
                        serverPort = finalPort
                        useHttps = repo.useHttps
                        settingsError = null
                        savedMessage = "Backend: $baseUrl (Перевірте підключення)"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зберегти та застосувати")
                }

                settingsError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (savedMessage.isNotEmpty()) {
                    Text(savedMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Поточний backend: ${RetrofitClient.getBaseUrl()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { vm.checkConnection() },
                    enabled = !connectionLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (connectionLoading) "Перевірка..." else "Перевірити з'єднання") }
                connError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                val diag by vm.connectionDiagnostic.collectAsState()
                diag?.let { d ->
                    Divider()
                    Text("Діагностика з'єднання", style = MaterialTheme.typography.titleSmall)
                    Text("URL: ${d.url}", style = MaterialTheme.typography.bodySmall)
                    Text("Хост: ${d.host} • порт: ${d.port ?: "—"} • HTTPS: ${if (d.https) "так" else "ні"}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Стан: ${d.state} • час відповіді: ${d.responseTimeMs ?: "—"} мс",
                        style = MaterialTheme.typography.bodySmall)
                    Text(d.message, style = MaterialTheme.typography.bodySmall,
                        color = if (d.state == "OK") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error)
                    d.lastSuccessUtc?.let {
                        Text("Останній успішний запит: $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Оновлення", style = MaterialTheme.typography.titleMedium)
                Text("Поточна версія: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        checkingUpdate = true
                        scope.launch {
                            val result = UpdateManager.checkForUpdates()
                            checkingUpdate = false
                            result.onSuccess { info ->
                                if (info != null) {
                                    updateInfo = info
                                    showUpdateDialog = true
                                } else {
                                    Toast.makeText(context, "У вас остання версія", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure {
                                Toast.makeText(context, "Не вдалося перевірити оновлення", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !checkingUpdate && !downloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (checkingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (checkingUpdate) "Перевірка..." else "Перевірити оновлення")
                }
                if (downloading) {
                    LinearProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("Завантаження: $downloadProgress%", style = MaterialTheme.typography.bodySmall)
                }
                downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Інформація", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Сервер за замовчуванням: ${com.sunlink.svitgrid.data.BackendDefaults.HOST}:${com.sunlink.svitgrid.data.BackendDefaults.PORT} (deye_gateway.py)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Авторизація буде реалізована на сервері", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { if (!downloading) showUpdateDialog = false },
            title = { Text("Доступне оновлення ${info.version}") },
            text = {
                Column {
                    Text(info.changelog.ifBlank { "Немає опису змін." }, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !downloading,
                    onClick = {
                        downloading = true
                        downloadProgress = 0
                        downloadError = null
                        scope.launch {
                            val res = UpdateManager.downloadAndInstall(context, info.downloadUrl) { pct ->
                                downloadProgress = pct
                            }
                            downloading = false
                            res.onFailure { e ->
                                downloadError = "Помилка завантаження: ${e.message}"
                            }
                            showUpdateDialog = false
                        }
                    }
                ) { Text("Завантажити та встановити") }
            },
            dismissButton = {
                TextButton(enabled = !downloading, onClick = { showUpdateDialog = false }) { Text("Скасувати") }
            }
        )
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SunLink", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Версія: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyLarge)
                Text("Дата збірки: ${BuildConfig.BUILD_TIME}", style = MaterialTheme.typography.bodyMedium)
                Text("Git-коміт: ${BuildConfig.GIT_HASH}", style = MaterialTheme.typography.bodyMedium)
                Text("Розробник: Ponamarenko", style = MaterialTheme.typography.bodyMedium)
                Text("Ліцензія: MIT", style = MaterialTheme.typography.bodySmall)
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
