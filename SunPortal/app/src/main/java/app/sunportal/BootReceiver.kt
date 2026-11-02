package app.sunportal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

/**
 * Boot receiver для автоматического запуска приложения
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, starting SunPortal...")
            
            // Здесь можно добавить логику для запуска сервиса
            // или других компонентов при загрузке устройства
            
            // Пример: запуск фонового сервиса
            // val serviceIntent = Intent(context, DataCollectionService::class.java)
            // context.startService(serviceIntent)
        }
    }
}