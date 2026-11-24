package com.sunlink.svitgrid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
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
import com.sunlink.svitgrid.ui.redesign.PremiumTokens
import com.sunlink.svitgrid.ui.theme.LocalThemeController
import com.sunlink.svitgrid.ui.theme.ThemeMode
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterUiState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.sunlink.svitgrid.ui.inverters.InverterDetailsScreen
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
    val colors = LocalSunLinkColors.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Єдина основна навігація — п'ять вкладок. Додаткові розділи живуть тільки в More.
    val tabs = listOf(
        NavTarget("home", R.string.nav_home, Icons.Default.Home),
        NavTarget("energy", R.string.nav_energy, Icons.Default.Bolt),
        NavTarget("battery", R.string.tab_battery, Icons.Default.BatteryChargingFull),
        NavTarget("solar", R.string.tab_solar, Icons.Default.SolarPower),
        NavTarget("more", R.string.nav_more, Icons.Default.MoreVert)
    )

    fun goTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun go(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    Scaffold(
        containerColor = colors.background,
        // Статус-бар обробляє Scaffold; нижню панель обробляє PremiumBottomNavigation
        // через navigationBarsPadding(), щоб не подвоювати системний inset.
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        bottomBar = {
            com.sunlink.svitgrid.ui.redesign.PremiumBottomNavigation(
                items = tabs.map {
                    com.sunlink.svitgrid.ui.redesign.PremiumNavItem(
                        route = it.route,
                        label = stringResource(it.labelRes),
                        icon = it.icon
                    )
                },
                currentRoute = currentRoute,
                onSelect = { route -> goTab(route) }
            )
        }
    ) { innerPadding ->
        val viewModel: MainViewModel = viewModel(
            factory = MainViewModelFactory(repository = repository)
        )
        DisposableEffect(viewModel) {
            viewModel.startLiveUpdates()
            onDispose { viewModel.stopLiveUpdates() }
        }
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(androidx.compose.animation.core.tween(220)) },
            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) }
        ) {
            composable("home") {
                com.sunlink.svitgrid.ui.redesign.HomeScreen(vm = viewModel, onNavigate = { go(it) })
            }
            composable("energy") { com.sunlink.svitgrid.ui.redesign.EnergyScreen(vm = viewModel, onNavigate = { go(it) }) }
            composable("battery") { com.sunlink.svitgrid.ui.redesign.BatteryScreen(vm = viewModel) }
            composable("solar") { com.sunlink.svitgrid.ui.redesign.SolarScreen(vm = viewModel) }
            composable("more") {
                com.sunlink.svitgrid.ui.redesign.MoreScreen(
                    currentRoute = currentRoute ?: "home",
                    onNavigate = { go(it) }
                )
            }
            composable("grid") { com.sunlink.svitgrid.ui.redesign.GridScreen(vm = viewModel, onNavigate = { go(it) }) }
            composable("load") { com.sunlink.svitgrid.ui.redesign.LoadScreen(vm = viewModel) }
            composable("analytics") { com.sunlink.svitgrid.ui.redesign.AnalyticsScreen(vm = viewModel) }
            composable("diagnostics") { com.sunlink.svitgrid.ui.redesign.DiagnosticsScreen(vm = viewModel) }

            // Корисні додаткові розділи — доступні лише з More.
            composable("inverter-details") { com.sunlink.svitgrid.ui.redesign.InverterDetailsScreen(vm = viewModel, onNavigate = { go(it) }) }
            composable("all-parameters") { com.sunlink.svitgrid.ui.redesign.AllParametersScreen(vm = viewModel) }
            // Debug — тільки для debug-збірки; у release deep link безпечно веде в Settings.
            composable("debug") {
                if (BuildConfig.DEBUG) DebugScreen()
                else LegacyRedirect(navController, "debug", "settings")
            }
            composable("settings") { SettingsScreen(vm = viewModel) }

            // Безпечні аліаси старих маршрутів: deep links не ламаються,
            // користувач потрапляє на актуальний екран.
            composable("dashboard") { LegacyRedirect(navController, "dashboard", "home") }
            composable("pv") { LegacyRedirect(navController, "pv", "solar") }
            composable("energy-flow") { LegacyRedirect(navController, "energy-flow", "energy") }
            composable("telemetry") { LegacyRedirect(navController, "telemetry", "all-parameters") }
            composable("statistics") { LegacyRedirect(navController, "statistics", "analytics") }
            composable("history") { LegacyRedirect(navController, "history", "analytics") }
            composable("inverter-status") { LegacyRedirect(navController, "inverter-status", "inverter-details") }
            composable("live") { LegacyRedirect(navController, "live", "home") }
            // Прогноз у Stage49 був фіктивним (emoji + "15-22°C"): прибраний з навігації.
            composable("forecast") { LegacyRedirect(navController, "forecast", "home") }
            // Про додаток об'єднано з налаштуваннями (версія/збірка вже там).
            composable("about") { LegacyRedirect(navController, "about", "settings") }
        }
    }
}

@Composable
private fun LegacyRedirect(navController: NavHostController, from: String, to: String) {
    LaunchedEffect(from) {
        navController.navigate(to) {
            popUpTo(from) { inclusive = true }
            launchSingleTop = true
        }
    }
}
// Старий DashboardScreen видалено, використовується новий з пакета redesign


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val colors = LocalSunLinkColors.current
    val uiState by vm.uiState.collectAsState()
    val loading by vm.refreshing.collectAsState()
    val success = uiState as? InverterUiState.Success
    var pollSeconds by remember { mutableStateOf(repo.pollIntervalSeconds) }
    var alerts by remember { mutableStateOf(repo.alertsEnabled) }
    var lowSoc by remember { mutableStateOf(repo.lowSocWarningEnabled) }
    var socThreshold by remember { mutableStateOf(repo.lowSocThreshold) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.setPollInterval(pollSeconds) }

    com.sunlink.svitgrid.ui.redesign.SunLinkScreen(
        title = "Налаштування",
        subtitle = "Параметри застосунку та звʼязку",
        refreshing = loading,
        onRefresh = { vm.checkConnection() }
    ) {
        com.sunlink.svitgrid.ui.redesign.PremiumCard(accent = colors.primary) {
            com.sunlink.svitgrid.ui.redesign.PremiumSectionHeader("Вигляд і мова")
            Column(verticalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Тема інтерфейсу", style = SunLinkTypography.body, color = colors.textSecondary)
                    com.sunlink.svitgrid.ui.redesign.StatusPill("Темна (Hybrid Premium) · Активна", colors.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Мова", style = SunLinkTypography.body, color = colors.textSecondary)
                    Text("Українська ✓", style = SunLinkTypography.label, color = colors.textPrimary)
                }
            }
        }

        com.sunlink.svitgrid.ui.redesign.PremiumCard(accent = colors.grid) {
            com.sunlink.svitgrid.ui.redesign.PremiumSectionHeader("Підключення та сервер", "Лише перегляд")
            Column(verticalArrangement = Arrangement.spacedBy(PremiumTokens.spaceXs)) {
                settingRow("Транспорт", "WebSocket")
                settingRow("Вузол сервера", repo.serverAddress.ifBlank { "Локальний шлюз" })
                settingRow("Адреса", repo.getBaseUrl())
                settingRow(
                    "Стан з'єднання",
                    if (success?.status?.online == true) "Підключено" else "Недоступно"
                )
            }
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(PremiumTokens.spaceSm))
                Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                    OutlinedButton(onClick = {
                        if (repo.useDebugLanEndpoint()) {
                            RetrofitClient.setBaseUrl(repo.getBaseUrl())
                            vm.checkConnection()
                        }
                    }) { Text("Debug LAN") }
                    OutlinedButton(onClick = {
                        repo.usePublicEndpoint()
                        RetrofitClient.setBaseUrl(repo.getBaseUrl())
                        vm.checkConnection()
                    }) { Text("Публічний") }
                }
            }
        }

        com.sunlink.svitgrid.ui.redesign.PremiumCard(accent = colors.battery) {
            com.sunlink.svitgrid.ui.redesign.PremiumSectionHeader("Оновлення та сповіщення")
            Text("Інтервал опитування", style = SunLinkTypography.label, color = colors.textSecondary)
            Spacer(Modifier.height(PremiumTokens.spaceXs))
            Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                listOf(5, 15, 30).forEach { seconds ->
                    FilterChip(
                        selected = pollSeconds == seconds,
                        onClick = {
                            pollSeconds = seconds
                            repo.pollIntervalSeconds = seconds
                            vm.setPollInterval(seconds)
                        },
                        label = { Text("$seconds с") }
                    )
                }
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Сповіщення про аварії", style = SunLinkTypography.body, color = colors.textSecondary)
                Switch(checked = alerts, onCheckedChange = { alerts = it; repo.alertsEnabled = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Низький заряд батареї", style = SunLinkTypography.body, color = colors.textSecondary)
                Switch(checked = lowSoc, onCheckedChange = { lowSoc = it; repo.lowSocWarningEnabled = it })
            }
            Text("Поріг попередження: $socThreshold% SOC", style = SunLinkTypography.caption, color = colors.textTertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(PremiumTokens.spaceSm)) {
                listOf(10, 20, 30).forEach { threshold ->
                    FilterChip(
                        selected = socThreshold == threshold,
                        onClick = { socThreshold = threshold; repo.lowSocThreshold = threshold },
                        label = { Text("$threshold%") }
                    )
                }
            }
        }

        com.sunlink.svitgrid.ui.redesign.PremiumCard(accent = colors.solar) {
            com.sunlink.svitgrid.ui.redesign.PremiumSectionHeader("Про застосунок")
            Column(verticalArrangement = Arrangement.spacedBy(PremiumTokens.spaceXs)) {
                settingRow("Версія застосунку", BuildConfig.VERSION_NAME)
                settingRow("Збірка", "${BuildConfig.BUILD_TIME} · ${BuildConfig.GIT_HASH}")
                settingRow("Ліцензії", "MIT · Ponamarenko")
            }
            Spacer(Modifier.height(PremiumTokens.spaceSm))
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
                Text(if (checkingUpdate) stringResource(R.string.checking) else stringResource(R.string.check_updates))
            }
            if (downloading) {
                LinearProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.fillMaxWidth())
                downloadError?.let { Text(it, color = colors.error, style = SunLinkTypography.caption) }
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { if (!downloading) showUpdateDialog = false },
            title = { Text("Доступне оновлення ${info.version}") },
            text = { Text(info.changelog.ifBlank { "Немає опису змін." }) },
            confirmButton = {
                TextButton(enabled = !downloading, onClick = {
                    downloading = true
                    downloadError = null
                    scope.launch {
                        val res = UpdateManager.downloadAndInstall(context, info.downloadUrl) { downloadProgress = it }
                        downloading = false
                        res.onFailure { e -> downloadError = "Помилка завантаження: ${e.message}" }
                        showUpdateDialog = false
                    }
                }) { Text("Завантажити та встановити") }
            },
            dismissButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("Скасувати") } }
        )
    }
}

@Composable
private fun settingRow(label: String, value: String) {
    val colors = LocalSunLinkColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SunLinkTypography.caption, color = colors.textTertiary)
        Text(value, style = SunLinkTypography.mono, color = colors.textPrimary, modifier = Modifier.padding(start = 12.dp))
    }
}
