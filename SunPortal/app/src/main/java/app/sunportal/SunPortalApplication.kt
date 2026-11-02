package app.sunportal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Класс приложения SunPortal
 * 
 * @author Ponamarenko
 * @version 1.0.32
 */
@HiltAndroidApp
class SunPortalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация приложения
    }
}