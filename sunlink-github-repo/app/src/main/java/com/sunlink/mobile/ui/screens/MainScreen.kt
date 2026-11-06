package com.sunlink.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunlink.mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SunLink") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        if (currentRoute != "dashboard") {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Live") },
                    label = { Text("Live") },
                    selected = currentRoute == "live",
                    onClick = {
                        if (currentRoute != "live") {
                            navController.navigate("live")
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Charts") },
                    label = { Text("Charts") },
                    selected = currentRoute == "charts",
                    onClick = {
                        if (currentRoute != "charts") {
                            navController.navigate("charts")
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery") },
                    label = { Text("Battery") },
                    selected = currentRoute == "battery",
                    onClick = {
                        if (currentRoute != "battery") {
                            navController.navigate("battery")
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Forecast") },
                    label = { Text("Forecast") },
                    selected = currentRoute == "forecast",
                    onClick = {
                        if (currentRoute != "forecast") {
                            navController.navigate("forecast")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen()
            }
            composable("live") {
                LiveScreen()
            }
            composable("charts") {
                ChartsScreen()
            }
            composable("battery") {
                BatteryScreen()
            }
            composable("forecast") {
                ForecastScreen()
            }
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
            color = MaterialTheme.colorScheme.primary
        )
        
        // Power Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Power", style = MaterialTheme.typography.titleMedium)
                Text("2,500 W", style = MaterialTheme.typography.headlineLarge)
            }
        }
        
        // Voltage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Voltage", style = MaterialTheme.typography.titleMedium)
                Text("230 V", style = MaterialTheme.typography.headlineLarge)
            }
        }
        
        // Battery Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Battery", style = MaterialTheme.typography.titleMedium)
                Text("75%", style = MaterialTheme.typography.headlineLarge)
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
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Current Status", style = MaterialTheme.typography.titleMedium)
                Text("Online", style = MaterialTheme.typography.bodyLarge)
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
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Power Chart", style = MaterialTheme.typography.titleMedium)
                Text("Last 24 hours", style = MaterialTheme.typography.bodyLarge)
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
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Battery Level", style = MaterialTheme.typography.titleMedium)
                Text("75%", style = MaterialTheme.typography.headlineLarge)
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
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Weather", style = MaterialTheme.typography.titleMedium)
                Text("Sunny, 25°C", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}