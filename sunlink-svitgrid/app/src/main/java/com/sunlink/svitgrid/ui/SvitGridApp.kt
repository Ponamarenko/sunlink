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

@Composable
fun SvitGridApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = { navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Live") },
                    label = { Text("Live") },
                    selected = currentRoute == "live",
                    onClick = { navController.navigate("live") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Charts") },
                    label = { Text("Charts") },
                    selected = currentRoute == "charts",
                    onClick = { navController.navigate("charts") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Battery") },
                    label = { Text("Battery") },
                    selected = currentRoute == "battery",
                    onClick = { navController.navigate("battery") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Forecast") },
                    label = { Text("Forecast") },
                    selected = currentRoute == "forecast",
                    onClick = { navController.navigate("forecast") }
                )
            }
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
        }
    }
}

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SunLink Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Power", style = MaterialTheme.typography.titleMedium)
                Text("2,500 W", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Voltage", style = MaterialTheme.typography.titleMedium)
                Text("230 V", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Battery", style = MaterialTheme.typography.titleMedium)
                Text("75%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LiveScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Real-time Monitoring",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current Power: 2,500 W", style = MaterialTheme.typography.bodyLarge)
                Text("Voltage: 230 V", style = MaterialTheme.typography.bodyLarge)
                Text("Current: 10.9 A", style = MaterialTheme.typography.bodyLarge)
                Text("Temperature: 35°C", style = MaterialTheme.typography.bodyLarge)
                Text("Status: Online", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ChartsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analytics & Charts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Power Chart", style = MaterialTheme.typography.titleMedium)
                Text("Last 24 hours", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Efficiency Chart", style = MaterialTheme.typography.titleMedium)
                Text("System efficiency over time", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun BatteryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Battery Management",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Battery Level", style = MaterialTheme.typography.titleMedium)
                Text("75%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Temperature: 25°C", style = MaterialTheme.typography.bodyLarge)
                Text("Voltage: 52.5 V", style = MaterialTheme.typography.bodyLarge)
                Text("Health: 95%", style = MaterialTheme.typography.bodyLarge)
                Text("Cycles: 250", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ForecastScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Weather & Forecast",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weather: Sunny, 25°C", style = MaterialTheme.typography.bodyLarge)
                Text("Humidity: 60%", style = MaterialTheme.typography.bodyLarge)
                Text("Wind: 5 m/s", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Energy Forecast", style = MaterialTheme.typography.titleMedium)
                Text("Tomorrow: 15.2 kWh", style = MaterialTheme.typography.bodyLarge)
                Text("Week: 98.5 kWh", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
