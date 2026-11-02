package app.sunportal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sunportal.R
import app.sunportal.ui.theme.ColorPalette
import app.sunportal.ui.theme.CardGradientStart
import app.sunportal.ui.theme.CardGradientEnd
import app.sunportal.ui.theme.Shapes
import app.sunportal.ui.theme Typography
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

/**
 * Главный экран SunPortal приложения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onFontSizeChange: (String) -> Unit
) {
    var selectedTheme by remember { mutableStateOf("system") }
    var selectedFontSize by remember { mutableStateOf("medium") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "SunPortal",
            style = Typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Карточки навигации
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                NavigationCard(
                    title = "🏠 Обзор",
                    description = "Основные показатели системы",
                    onClick = { onNavigate("overview") }
                )
            }

            item {
                NavigationCard(
                    title = "⚡ Live",
                    description = "Мониторинг в реальном времени",
                    onClick = { onNavigate("live") }
                )
            }

            item {
                NavigationCard(
                    title = "🔋 Батарея",
                    description = "Управление и статус батареи",
                    onClick = { onNavigate("battery") }
                )
            }

            item {
                NavigationCard(
                    title = "📊 Прогноз",
                    description = "Прогноз производства энергии",
                    onClick = { onNavigate("forecast") }
                )
            }

            item {
                NavigationCard(
                    title = "📈 Графики",
                    description = "Статистика и графики",
                    onClick = { onNavigate("charts") }
                )
            }

            item {
                NavigationCard(
                    title = "💰 Тарифы",
                    description = "Настройки тарифов",
                    onClick = { onNavigate("tariffs") }
                )
            }

            item {
                NavigationCard(
                    title = "⏰ TOU",
                    description = "Time of Use управление",
                    onClick = { onNavigate("tou") }
                )
            }

            item {
                NavigationCard(
                    title = "⚙️ Настройки",
                    description = "Внешний вид и настройки",
                    onClick = { onNavigate("settings") }
                )
            }

            item {
                NavigationCard(
                    title = "ℹ️ О приложении",
                    description = "Версия и информация",
                    onClick = { onNavigate("about") }
                )
            }
        }
    }
}

/**
 * Навигационная карточка
 */
@Composable
fun NavigationCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}