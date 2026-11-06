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
import app.sunportal.data.model.UpdateInfo
import app.sunportal.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Тестовый сервис для демонстрации обновлений
 */
@AndroidEntryPoint
class TestUpdateService : Service() {
    
    @Inject
    lateinit var updateManager: UpdateManager
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    companion object {
        const val ACTION_TEST_UPDATE = "app.sunportal.TEST_UPDATE"
        
        fun startTestUpdateService(context: Context) {
            val intent = Intent(context, TestUpdateService::class.java).apply {
                action = ACTION_TEST_UPDATE
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
            ACTION_TEST_UPDATE -> {
                startForeground(1, createNotification())
                simulateUpdateCheck()
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
    
    private fun simulateUpdateCheck() {
        serviceScope.launch {
            try {
                // Имитация задержки
                delay(2000)
                
                // Создаем тестовое обновление
                val testUpdate = UpdateInfo(
                    version = "1.0.33",
                    versionCode = 34,
                    downloadUrl = "https://github.com/Ponamarenko/SunPortal/releases/download/v1.0.33/sunportal-v1.0.33.apk",
                    changelog = "🎉 Новые возможности:\n• Добавлена функция автообновлений\n• Улучшен интерфейс управления батареей\n• Исправлены ошибки в графике производства\n• Оптимизировано потребление батареи\n• Добавлены новые настройки уведомлений\n\n🔧 Технические улучшения:\n• Обновлена библиотека Compose до версии 1.5.8\n• Исправлена утечка памяти в фоновом сервисе\n• Оптимизирована работа с сетью\n• Улучшена обработка ошибок\n\n🐛 Исправления:\n• Исправлена ошибка при загрузке больших графиков\n• Исправлена проблема с темной темой\n• Исправлены утечки памяти в ViewModel",
                    releaseDate = "2024-11-02T12:00:00Z",
                    minSdk = 26,
                    size = 6500000L,
                    forceUpdate = false
                )
                
                // Показываем уведомление об обновлении
                updateManager.showUpdateNotification(testUpdate)
                
                // Записываем лог
                updateManager.logDownload(testUpdate, true)
                
            } catch (e: Exception) {
                android.util.Log.e("TestUpdateService", "Error in test update", e)
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "test_update")
            .setContentTitle("Тест обновления")
            .setContentText("Проверка тестового обновления...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "test_update",
                "Test Update Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }
}