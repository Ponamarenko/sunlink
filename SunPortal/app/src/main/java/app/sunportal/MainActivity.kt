package app.sunportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.sunportal.ui.SunPortalApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Главная активность SunPortal приложения
 * 
 * @author Ponamarenko
 * @version 1.0.32
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SunPortalApp()
        }
    }
}