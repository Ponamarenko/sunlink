package app.sunportal.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.sunportal.R
import app.sunportal.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Сервис для фоновой проверки обновлений
 */
@AndroidEntryPoint
class UpdateCheckService : Service() {
    
    @Inject
    lateinit var updateManager: UpdateManager
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    companion object {
        const val ACTION_CHECK_UPDATES = "app.sunportal.CHECK_UPDATES"
        
        fun startCheckService(context: Context) {
            val intent = Intent(context, UpdateCheckService::class.java).apply {
                action = ACTION_CHECK_UPDATES
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CHECK_UPDATES -> {
                startForeground(1, createNotification())
                checkForUpdates()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    private fun checkForUpdates() {
        serviceScope.launch {
            try {
                val result = updateManager.checkForUpdates()
                result.onSuccess { updateInfo ->
                    if (updateInfo.hasUpdate(33)) { // 33 - текущий versionCode
                        // Показать уведомление об обновлении
                        if (updateInfo.forceUpdate) {
                            updateManager.showForceUpdateNotification(updateInfo)
                        } else {
                            updateManager.showUpdateNotification(updateInfo)
                        }
                    }
                }.onFailure { error ->
                    // Не показывать уведомление об ошибке, это фоновая проверка
                    android.util.Log.e("UpdateCheckService", "Update check failed", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("UpdateCheckService", "Update check error", e)
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "update_check")
            .setContentTitle("Проверка обновлений")
            .setContentText("SunPortal проверяет наличие новых версий...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "update_check",
                "Update Check Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }
}