package com.sunlink.svitgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        val settings = SettingsRepository(this)
        setContent {
            val mode = remember {
                mutableStateOf(
                    runCatching { ThemeMode.valueOf(settings.themeModeName) }
                        .getOrDefault(ThemeMode.SYSTEM)
                )
            }
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
