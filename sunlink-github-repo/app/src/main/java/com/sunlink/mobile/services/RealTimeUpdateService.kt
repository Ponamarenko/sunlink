package com.sunlink.mobile.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sunlink.mobile.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class RealTimeUpdateService : Service() {

    @Inject
    lateinit var viewModel: com.sunlink.mobile.viewmodel.MainViewModel

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "realtime_updates"
        const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (!isRunning) {
            isRunning = true
            startDataUpdates()
        }
        
        return START_STICKY
    }

    private fun startDataUpdates() {
        coroutineScope.launch {
            while (isRunning) {
                viewModel.refreshData()
                delay(5000) // Обновление каждые 5 секунд
            }
        }
    }

    private fun createNotification(): android.app.Notification {
        val channelId = NOTIFICATION_CHANNEL_ID
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Real-time Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SunLink")
            .setContentText("Real-time monitoring active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        coroutineScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}