package app.sunportal.ui.screens

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import app.sunportal.ui.theme.Shapes
import app.sunportal.ui.theme Typography

/**
 * Фрагмент настроек внешнего вида
 */
class AppearanceFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppearanceScreen()
            }
        }
    }
}

/**
 * Экран настроек внешнего вида
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen() {
    var selectedTheme by remember { mutableStateOf("system") }
    var selectedFontSize by remember { mutableStateOf("medium") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Внешний вид",
            style = Typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Карточка темы
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
                    text = "🎨 Тема",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { selectedTheme = "light" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Светлая")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedTheme = "dark" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Тёмная")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedTheme = "system" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Система")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Карточка размера шрифта
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
                    text = "📏 Размер шрифта",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { selectedFontSize = "small" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Маленький")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedFontSize = "medium" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Средний")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedFontSize = "large" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Большой")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка применения
        Button(
            onClick = { /* TODO: Применить настройки */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Применить настройки")
        }
    }
}