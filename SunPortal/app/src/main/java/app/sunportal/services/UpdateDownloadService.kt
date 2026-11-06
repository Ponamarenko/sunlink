package app.sunportal.services

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.net.Uri
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
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
 * Сервис для скачивания обновлений
 */
@AndroidEntryPoint
class UpdateDownloadService : Service() {
    
    @Inject
    lateinit var updateManager: UpdateManager
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private var downloadId: Long = -1
    private var updateInfo: UpdateInfo? = null
    
    companion object {
        const val ACTION_DOWNLOAD_UPDATE = "app.sunportal.DOWNLOAD_UPDATE"
        const val EXTRA_UPDATE_INFO = "update_info"
        
        fun startDownloadService(context: Context, updateInfo: UpdateInfo) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD_UPDATE
                putExtra(EXTRA_UPDATE_INFO, updateInfo)
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(downloadCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD_UPDATE -> {
                updateInfo = intent.getSerializableExtra(EXTRA_UPDATE_INFO) as? UpdateInfo
                updateInfo?.let {
                    downloadId = updateManager.downloadUpdate(it)
                    startForeground(downloadId.toInt(), createNotification())
                }
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompleteReceiver)
        serviceJob.cancel()
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId != null && downloadId == this@UpdateDownloadService.downloadId) {
                serviceScope.launch {
                    val query = updateManager.getDownloadStatus(downloadId)
                    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL && uri != null) {
                            // Скачивание успешно завершено
                            showDownloadCompleteNotification(uri)
                            updateManager.logDownload(updateInfo!!, true)
                        } else {
                            // Ошибка скачивания
                            showDownloadFailedNotification()
                            updateManager.logDownload(updateInfo!!, false)
                        }
                    }
                    
                    stopSelf()
                }
            }
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "download")
            .setContentTitle("Скачивание обновления")
            .setContentText("SunPortal ${updateInfo?.version}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, 0, true)
            .build()
    }
    
    private fun showDownloadCompleteNotification(apkUri: String) {
        val notification = NotificationCompat.Builder(this, "download")
            .setContentTitle("Обновление скачано")
            .setContentText("Нажмите для установки SunPortal ${updateInfo?.version}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getInstallPendingIntent(apkUri))
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(downloadId.toInt(), notification)
    }
    
    private fun showDownloadFailedNotification() {
        val notification = NotificationCompat.Builder(this, "download")
            .setContentTitle("Ошибка скачивания")
            .setContentText("Не удалось скачать обновление")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getRetryPendingIntent())
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(downloadId.toInt(), notification)
    }
    
    private fun getInstallPendingIntent(apkUri: String): android.app.PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(apkUri), "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        return android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun getRetryPendingIntent(): android.app.PendingIntent {
        val intent = Intent(this, UpdateDownloadService::class.java).apply {
            action = ACTION_DOWNLOAD_UPDATE
            putExtra(EXTRA_UPDATE_INFO, updateInfo)
        }
        
        return android.app.PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "download",
                "Download Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }
}