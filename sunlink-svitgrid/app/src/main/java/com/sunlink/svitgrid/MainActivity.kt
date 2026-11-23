package com.sunlink.svitgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sunlink.svitgrid.data.SettingsRepository
import com.sunlink.svitgrid.ui.SvitGridApp
import com.sunlink.svitgrid.ui.theme.LocalThemeController
import com.sunlink.svitgrid.ui.theme.SvitGridTheme
import com.sunlink.svitgrid.ui.theme.ThemeController
import com.sunlink.svitgrid.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Stage 49: контент під статус-баром і системною навігацією.
        enableEdgeToEdge()
        // Stage 50.1: системні панелі завжди темні, іконки світлі — навіть у edge-to-edge.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val settings = SettingsRepository(this)
        setContent {
            // Міграція виконується навіть якщо controllers завжди тёмний:
            // вона очищає застарілі SYSTEM/LIGHT у SharedPreferences.
            settings.migrateLegacyThemeMode()
            val mode = remember { mutableStateOf(ThemeMode.DARK) }
            val controller = ThemeController(
                mode = mode.value,
                setMode = { next ->
                    mode.value = next
                    settings.themeModeName = next.name
                }
            )
            CompositionLocalProvider(LocalThemeController provides controller) {
                SvitGridTheme(themeMode = controller.mode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SvitGridApp()
                    }
                }
            }
        }
    }
}
