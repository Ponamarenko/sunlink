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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sunlink.svitgrid.BuildConfig
import com.sunlink.svitgrid.R
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
import com.sunlink.svitgrid.ui.designsystem.LocalSunLinkColors
import com.sunlink.svitgrid.ui.designsystem.SunLinkTypography
import com.sunlink.svitgrid.ui.theme.LocalThemeController
import com.sunlink.svitgrid.ui.theme.ThemeMode
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterUiState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.sunlink.svitgrid.ui.dashboard.DashboardScreen
import com.sunlink.svitgrid.ui.history.HistoryScreen
import com.sunlink.svitgrid.ui.inverters.InverterDetailsScreen
import com.sunlink.svitgrid.ui.diagnostics.DiagnosticsScreen
import com.sunlink.svitgrid.ui.telemetry.TelemetryScreen
import com.sunlink.svitgrid.ui.statistics.StatisticsScreen
import com.sunlink.svitgrid.ui.telemetry.BatteryDetailScreen
import com.sunlink.svitgrid.ui.telemetry.PvDetailScreen
import com.sunlink.svitgrid.ui.telemetry.GridDetailScreen
import com.sunlink.svitgrid.ui.telemetry.LoadDetailScreen
import com.sunlink.svitgrid.ui.telemetry.EnergyFlowScreen
import com.sunlink.svitgrid.ui.telemetry.InverterDetailScreen
import com.sunlink.svitgrid.ui.telemetry.AllParametersScreen

private data class NavTarget(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvitGridApp(repository: DataRepository = DataRepository(RetrofitClient.getApiService())) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val repo = SettingsRepository(context)
        RetrofitClient.setBaseUrl(repo.getBaseUrl())
    }
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = LocalSunLinkColors.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val tabs = listOf(
        NavTarget("home", R.string.nav_home, Icons.Default.Home),
        NavTarget("energy", R.string.nav_energy, Icons.Default.Bolt),
        NavTarget("battery", R.string.tab_battery, Icons.Default.BatteryChargingFull),
        NavTarget("solar", R.string.tab_solar, Icons.Default.SolarPower),
        NavTarget("more", R.string.nav_more, Icons.Default.MoreVert)
    )

    val sections = listOf(
        stringResource(R.string.drawer_monitoring) to listOf(
            NavTarget("home", R.string.nav_home, Icons.Default.Home),
            NavTarget("energy", R.string.nav_energy, Icons.Default.Bolt),
            NavTarget("battery", R.string.tab_battery, Icons.Default.BatteryChargingFull),
            NavTarget("solar", R.string.tab_solar, Icons.Default.SolarPower),
            NavTarget("grid", R.string.screen_grid, Icons.Default.GridOn),
            NavTarget("load", R.string.screen_load, Icons.Default.Power)
        ),
        stringResource(R.string.drawer_analytics) to listOf(
            NavTarget("analytics", R.string.statistics, Icons.Default.Analytics),
            NavTarget("history", R.string.nav_history, Icons.Default.History),
            NavTarget("statistics", R.string.nav_statistics, Icons.Default.ShowChart)
        ),
        stringResource(R.string.drawer_system) to listOf(
            NavTarget("diagnostics", R.string.nav_diagnostics, Icons.Default.Build),
            NavTarget("settings", R.string.nav_settings, Icons.Default.Settings),
            NavTarget("about", R.string.nav_about, Icons.Default.Info)
        ),
        stringResource(R.string.drawer_other) to listOf(
            NavTarget("inverter-details", R.string.nav_inverter, Icons.Default.Memory),
            NavTarget("energy-flow", R.string.screen_energy_flow, Icons.Default.Bolt),
            NavTarget("telemetry", R.string.nav_telemetry, Icons.Default.DataObject),
            NavTarget("inverter-status", R.string.screen_inverter, Icons.Default.Memory),
            NavTarget("all-parameters", R.string.all_parameters, Icons.Default.List),
            NavTarget("live", R.string.live_monitoring, Icons.Default.Stream),
            NavTarget("forecast", R.string.nav_forecast, Icons.Default.DateRange),
            NavTarget("debug", R.string.nav_debug, Icons.Default.BugReport)
        )
    )

    fun goTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    fun go(route: String) {
        navController.navigate(route) { launchSingleTop = true }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "SunLink",
                    style = SunLinkTypography.display,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Text(
                    text = "Svitgrid · Deye SUN-12K",
                    style = SunLinkTypography.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Divider(color = colors.border, modifier = Modifier.padding(vertical = 12.dp))
                sections.forEach { (title, items) ->
                    Text(
                        text = title,
                        style = SunLinkTypography.caption,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    items.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentRoute == item.route,
                            onClick = { go(item.route) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = colors.primarySoft,
                                selectedTextColor = colors.textPrimary,
                                selectedIconColor = colors.primary,
                                unselectedTextColor = colors.textSecondary,
                                unselectedIconColor = colors.textTertiary
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("SunLink", style = SunLinkTypography.title, color = colors.textPrimary)
                            tabs.find { it.route == currentRoute }?.let {
                                Text(stringResource(it.labelRes), style = SunLinkTypography.caption, color = colors.textTertiary)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.nav_menu),
                                tint = colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.textPrimary,
                        navigationIconContentColor = colors.textPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = colors.surface, contentColor = colors.textSecondary) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { goTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                unselectedIconColor = colors.textTertiary,
                                unselectedTextColor = colors.textTertiary,
                                indicatorColor = colors.primarySoft
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository = repository)
            )
            // Один WebSocket-клієнт на весь час роботи екрана.
            DisposableEffect(viewModel) {
                viewModel.startLiveUpdates()
                onDispose { viewModel.stopLiveUpdates() }
            }
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    com.sunlink.svitgrid.ui.redesign.HomeScreen(vm = viewModel)
                }
                composable("energy") {
                    com.sunlink.svitgrid.ui.redesign.EnergyScreen(vm = viewModel)
                }
                composable("battery") { com.sunlink.svitgrid.ui.redesign.BatteryScreen(vm = viewModel) }
                composable("solar") { com.sunlink.svitgrid.ui.redesign.SolarScreen(vm = viewModel) }
                composable("more") {
                    com.sunlink.svitgrid.ui.redesign.MoreScreen(
                        currentRoute = currentRoute ?: "home",
                        onNavigate = { go(it) }
                    )
                }
                composable("grid") { com.sunlink.svitgrid.ui.redesign.GridScreen(vm = viewModel) }
                composable("load") { com.sunlink.svitgrid.ui.redesign.LoadScreen(vm = viewModel) }
                composable("analytics") { com.sunlink.svitgrid.ui.redesign.AnalyticsScreen(vm = viewModel) }
                composable("diagnostics") {
                    com.sunlink.svitgrid.ui.redesign.DiagnosticsScreen(vm = viewModel)
                }
                composable("dashboard") { DashboardScreen(vm = viewModel, onNavigate = { go(it) }) }
                composable("pv") { PvDetailScreen(vm = viewModel) }
                composable("energy-flow") { EnergyFlowScreen(vm = viewModel) }
                composable("inverter-status") { InverterDetailScreen(vm = viewModel) }
                composable("all-parameters") { AllParametersScreen(vm = viewModel) }
                composable("telemetry") { TelemetryScreen(vm = viewModel) }
                composable("statistics") { StatisticsScreen(vm = viewModel) }
                composable("history") { HistoryScreen(vm = viewModel) }
                composable("inverter-details") { InverterDetailsScreen(vm = viewModel) }
                composable("live") { LiveScreen(vm = viewModel) }
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
        Text(stringResource(R.string.live_monitoring), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
            Text(stringResource(R.string.dash_no_data), style = MaterialTheme.typography.bodyLarge)
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
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        // Stage 31: System / Light / Dark — застосовується без перезапуску.
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themeController = LocalThemeController.current
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        }
                        FilterChip(
                            selected = themeController.mode == mode,
                            onClick = { themeController.setMode(mode) },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    repo.useDebugLanEndpoint()
                    RetrofitClient.setBaseUrl(repo.getBaseUrl())
                    vm.checkConnection()
                }) { Text("Debug LAN :18800") }
                Button(onClick = {
                    repo.usePublicEndpoint()
                    RetrofitClient.setBaseUrl(repo.getBaseUrl())
                    vm.checkConnection()
                }) { Text("Публічний :9443") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_server), style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it; settingsError = null },
                    label = { Text(stringResource(R.string.settings_address)) },
                    placeholder = { Text(com.sunlink.svitgrid.data.BackendDefaults.HOST) },
                    supportingText = { Text("Публічний: https://sunlink.pp.ua:9443/ • Debug LAN: http://192.168.25.20:18800/") },
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
                    Text(stringResource(R.string.settings_use_https))
                }

                Button(
                    onClick = {
                        val endpoint = SettingsRepository.parseEndpoint(serverAddress)
                        if (endpoint == null) {
                            settingsError = context.getString(R.string.settings_invalid_address)
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
                    Text(stringResource(R.string.settings_save_apply))
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
                ) { Text(if (connectionLoading) stringResource(R.string.checking) else "Перевірити з'єднання") }
                connError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                val diag by vm.connectionDiagnostic.collectAsState()
                diag?.let { d ->
                    Divider()
                    Text(stringResource(R.string.settings_connection_diagnostics), style = MaterialTheme.typography.titleSmall)
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
                Text(stringResource(R.string.settings_updates), style = MaterialTheme.typography.titleMedium)
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
                                Toast.makeText(context, context.getString(R.string.update_check_failed), Toast.LENGTH_LONG).show()
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
                    Text(if (checkingUpdate) stringResource(R.string.checking) else stringResource(R.string.check_updates))
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
                Text(stringResource(R.string.settings_info), style = MaterialTheme.typography.titleMedium)
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
    val context = LocalContext.current
    var updateStatus by remember { mutableStateOf("Натисніть кнопку для перевірки") }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.nav_about), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
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
                Text(stringResource(R.string.settings_description), style = MaterialTheme.typography.titleMedium)
                Text("SunLink — сучасний додаток для моніторингу сонячних систем. Підтримує різні типи підключення: RS485, ESP32, Solarman WiFi, Ethernet.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_updates), style = MaterialTheme.typography.titleMedium)
                Text(updateStatus, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        isChecking = true
                        updateStatus = context.getString(R.string.checking)
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
                    Text(if (isChecking) stringResource(R.string.checking) else stringResource(R.string.check_updates))
                }
            }
        }
    }
}
