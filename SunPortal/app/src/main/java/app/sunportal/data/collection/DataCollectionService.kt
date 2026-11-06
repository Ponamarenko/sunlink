package app.sunportal.data.collection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import app.sunportal.R
import app.sunportal.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Сервис для сбора данных в фоновом режиме
 */
@AndroidEntryPoint
class DataCollectionService : Service() {
    
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private val channelId = "sunportal_service"
    private val notificationId = 1
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("DataCollectionService", "Service started")
        
        // Запускаем сбор данных в фоновом режиме
        serviceScope.launch {
            while (isActive) {
                try {
                    // Загружаем данные инвертора
                    
                    // Загружаем погодные данные
                    
                    // Ждем 30 секунд перед следующим обновлением
                    delay(30000)
                } catch (e: Exception) {
                    Log.e("DataCollectionService", "Error collecting data", e)
                    delay(60000) // Жимаем дольше при ошибке
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("DataCollectionService", "Service destroyed")
        serviceJob.cancel()
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "SunPortal Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    
    private fun createNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SunPortal")
            .setContentText("Сбор данных в фоновом режиме")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}