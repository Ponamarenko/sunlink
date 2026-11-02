package app.sunportal.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.sunportal.R
import app.sunportal.data.model.UpdateInfo
import app.sunportal.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Менеджер обновлений приложения
 */
class UpdateManager(private val context: Context) {
    
    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }
    
    /**
     * Проверка наличия обновлений
     */
    suspend fun checkForUpdates(): Result<UpdateInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = getCurrentVersion()
                val deviceId = getDeviceId()
                
                val updateInfo = RetrofitClient.updateService.checkForUpdates(
                    currentVersion = currentVersion.first,
                    currentCode = currentVersion.second,
                    deviceId = deviceId
                )
                
                Result.success(updateInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Получение информации о последнем обновлении
     */
    suspend fun getLatestUpdate(): Result<UpdateInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val updateInfo = RetrofitClient.updateService.getLatestUpdate()
                Result.success(updateInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Скачивание обновления
     */
    fun downloadUpdate(updateInfo: UpdateInfo): Long {
        try {
            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
                .setTitle("SunPortal ${updateInfo.version}")
                .setDescription("Скачивание обновления...")
                .setMimeType("application/vnd.android.package-archive")
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "sunportal-${updateInfo.version}.apk"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadId = downloadManager.enqueue(request)
            
            // Показать уведомление о скачивании
            showDownloadNotification(downloadId, updateInfo)
            
            return downloadId
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при скачивании: ${e.message}", Toast.LENGTH_SHORT).show()
            return -1
        }
    }
    
    /**
     * Проверка статуса скачивания
     */
    fun getDownloadStatus(downloadId: Long): DownloadManager.Query {
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        return query
    }
    
    /**
     * Показать уведомление о скачивании
     */
    private fun showDownloadNotification(downloadId: Long, updateInfo: UpdateInfo) {
        val notification = NotificationCompat.Builder(context, "updates")
            .setContentTitle("Скачивание обновления")
            .setContentText("SunPortal ${updateInfo.version} (${updateInfo.formattedSize})")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, 0, true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(downloadId.toInt(), notification)
    }
    
    /**
     * Показать уведомление об обновлении
     */
    fun showUpdateNotification(updateInfo: UpdateInfo) {
        val notification = NotificationCompat.Builder(context, "updates")
            .setContentTitle("Доступно обновление SunPortal")
            .setContentText("Версия ${updateInfo.version} доступна для скачивания")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getUpdatePendingIntent(updateInfo))
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1, notification)
    }
    
    /**
     * Показать уведомление об обязательном обновлении
     */
    fun showForceUpdateNotification(updateInfo: UpdateInfo) {
        val notification = NotificationCompat.Builder(context, "updates")
            .setContentTitle("Обязательное обновление SunPortal")
            .setContentText("Версия ${updateInfo.version} обязательна для использования")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(getUpdatePendingIntent(updateInfo))
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(2, notification)
    }
    
    /**
     * Получение PendingIntent для скачивания обновления
     */
    private fun getUpdatePendingIntent(updateInfo: UpdateInfo): PendingIntent {
        val intent = Intent(context, UpdateDownloadService::class.java).apply {
            action = UpdateDownloadService.ACTION_DOWNLOAD_UPDATE
            putExtra(UpdateDownloadService.EXTRA_UPDATE_INFO, updateInfo)
        }
        
        return PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Получение текущей версии приложения
     */
    private fun getCurrentVersion(): Pair<String, Int> {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            
            Pair(packageInfo.versionName, packageInfo.versionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            Pair("1.0.32", 33) // Версия по умолчанию
        }
    }
    
    /**
     * Получение ID устройства
     */
    private fun getDeviceId(): String {
        return try {
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            androidId ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }
    
    /**
     * Логирование скачивания
     */
    suspend fun logDownload(updateInfo: UpdateInfo, success: Boolean) {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val downloadInfo = mapOf(
                    "device_id" to deviceId,
                    "version" to updateInfo.version,
                    "version_code" to updateInfo.versionCode,
                    "success" to success,
                    "timestamp" to System.currentTimeMillis()
                )
                
                RetrofitClient.updateService.logDownload(downloadInfo)
            } catch (e: Exception) {
                // Не критично, просто логируем ошибку
                android.util.Log.e("UpdateManager", "Failed to log download", e)
            }
        }
    }
}