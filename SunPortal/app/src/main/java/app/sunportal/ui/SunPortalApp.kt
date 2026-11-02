package app.sunportal.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sunportal.ui.navigation.SunPortalNavHost
import app.sunportal.ui.theme.SunPortalTheme

/**
 * Основной компонент SunPortal приложения
 */
@Composable
fun SunPortalApp() {
    SunPortalTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SunPortalNavHost()
        }
    }
}