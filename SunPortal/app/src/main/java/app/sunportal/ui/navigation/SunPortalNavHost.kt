package app.sunportal.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.sunportal.ui.screens.*
import app.sunportal.ui.theme.SunPortalTheme

/**
 * Навигационный хост SunPortal приложения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunPortalNavHost(
    navController: NavHostController = rememberNavController()
) {
    var currentTheme by remember { mutableStateOf("system") }
    var fontSize by remember { mutableStateOf("medium") }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { navController.navigate(it) },
                onThemeChange = { currentTheme = it },
                onFontSizeChange = { fontSize = it }
            )
        }
        
        composable(Screen.Overview.route) {
            OverviewScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Live.route) {
            LiveScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Battery.route) {
            BatteryScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Forecast.route) {
            ForecastScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Charts.route) {
            ChartsScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Tariffs.route) {
            TariffsScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Tou.route) {
            TouScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                currentTheme = currentTheme,
                fontSize = fontSize,
                onThemeChange = { currentTheme = it },
                onFontSizeChange = { fontSize = it },
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.About.route) {
            AboutScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
        
        composable(Screen.Update.route) {
            UpdateScreen(
                onNavigate = { navController.navigate(it) }
            )
        }
    }
}

/**
 * Экраны приложения
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Overview : Screen("overview")
    object Live : Screen("live")
    object Battery : Screen("battery")
    object Forecast : Screen("forecast")
    object Charts : Screen("charts")
    object Tariffs : Screen("tariffs")
    object Tou : Screen("tou")
    object Settings : Screen("settings")
    object About : Screen("about")
    object Update : Screen("update")
}