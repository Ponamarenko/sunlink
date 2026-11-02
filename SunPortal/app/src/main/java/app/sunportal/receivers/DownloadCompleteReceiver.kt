package app.sunportal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import app.sunportal.R
import app.sunportal.services.UpdateDownloadService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver для обработки завершения скачивания
 */
@AndroidEntryPoint
class DownloadCompleteReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var notificationManager: NotificationManager
    
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            
            if (downloadId != -1L) {
                val query = DownloadManager.Query()
                query.setFilterById(downloadId)
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL && uri != null) {
                        // Скачивание успешно завершено
                        showDownloadCompleteNotification(context, Uri.parse(uri))
                        Toast.makeText(context, "Обновление успешно скачано", Toast.LENGTH_SHORT).show()
                    } else {
                        // Ошибка скачивания
                        showDownloadFailedNotification(context)
                        Toast.makeText(context, "Ошибка при скачивании обновления", Toast.LENGTH_SHORT).show()
                    }
                }
                
                cursor.close()
            }
        }
    }
    
    private fun showDownloadCompleteNotification(context: Context, apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, "download")
            .setContentTitle("Обновление скачано")
            .setContentText("Нажмите для установки SunPortal")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1, notification)
    }
    
    private fun showDownloadFailedNotification(context: Context) {
        val retryIntent = Intent(context, UpdateDownloadService::class.java).apply {
            action = UpdateDownloadService.ACTION_DOWNLOAD_UPDATE
        }
        
        val pendingIntent = PendingIntent.getService(
            context,
            0,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, "download")
            .setContentTitle("Ошибка скачивания")
            .setContentText("Не удалось скачать обновление")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(2, notification)
    }
}