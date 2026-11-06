package app.sunportal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sunportal.ui.theme.Shapes
import app.sunportal.ui.theme.Typography

/**
 * Экран настроек приложения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    fontSize: String,
    onThemeChange: (String) -> Unit,
    onFontSizeChange: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Настройки",
            style = Typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Внешний вид
                AppearanceSettingsCard(
                    currentTheme = currentTheme,
                    fontSize = fontSize,
                    onThemeChange = onThemeChange,
                    onFontSizeChange = onFontSizeChange
                )
            }

            item {
                // Система
                SystemSettingsCard()
            }

            item {
                // Уведомления
                NotificationsSettingsCard()
            }

            item {
                // Сервер
                ServerSettingsCard()
            }

            item {
                // О приложении
                AboutSettingsCard()
            }
        }
    }
}

/**
 * Карточка настроек внешнего вида
 */
@Composable
fun AppearanceSettingsCard(
    currentTheme: String,
    fontSize: String,
    onThemeChange: (String) -> Unit,
    onFontSizeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🎨 Внешний вид",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Тема
            Text(
                text = "Тема",
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onThemeChange("light") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Светлая")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onThemeChange("dark") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Тёмная")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onThemeChange("system") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Система")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Размер шрифта
            Text(
                text = "Размер шрифта",
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onFontSizeChange("small") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Маленький")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onFontSizeChange("medium") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Средний")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onFontSizeChange("large") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Большой")
                }
            }
        }
    }
}

/**
 * Карточка настроек системы
 */
@Composable
fun SystemSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔧 Система",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Автозапуск",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = true, onCheckedChange = { /* TODO */ })
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Фоновый режим",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = true, onCheckedChange = { /* TODO */ })
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Кэширование",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = true, onCheckedChange = { /* TODO */ })
            }
        }
    }
}

/**
 * Карточка настроек уведомлений
 */
@Composable
fun NotificationsSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔔 Уведомления",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Блекауты",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = true, onCheckedChange = { /* TODO */ })
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Заряд батареи",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = true, onCheckedChange = { /* TODO */ })
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ошибки системы",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = true, onCheckedChange = { /* TODO */ })
            }
        }
    }
}

/**
 * Карточка настроек сервера
 */
@Composable
fun ServerSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🌐 Сервер",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Адрес сервера",
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = "192.168.1.100:8800",
                onValueChange = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Таймаут",
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = "30",
                onValueChange = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Карточка информации о приложении
 */
@Composable
fun AboutSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ℹ️ О приложении",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "SunPortal v1.0.32",
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "© 2024 Ponamarenko",
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Android 15",
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}