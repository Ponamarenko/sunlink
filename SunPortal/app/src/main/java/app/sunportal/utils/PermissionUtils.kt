package app.sunportal.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Утилита для проверки разрешений
 */
object PermissionUtils {
    
    /**
     * Проверка разрешения на доступ к интернету
     */
    fun hasInternetPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Проверка разрешения на доступ к сети
     */
    fun hasNetworkStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Проверка разрешения на запись во внешнее хранилище
     */
    fun hasWriteStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Проверка разрешения на чтение из внешнего хранилища
     */
    fun hasReadStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Проверка разрешения на показ уведомлений
     */
    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Для старых версий Android разрешение не требуется
        }
    }
    
    /**
     * Проверка всех необходимых разрешений для обновлений
     */
    fun hasAllUpdatePermissions(context: Context): Boolean {
        return hasInternetPermission(context) &&
                hasNetworkStatePermission(context) &&
                hasWriteStoragePermission(context) &&
                hasReadStoragePermission(context) &&
                hasPostNotificationsPermission(context)
    }
    
    /**
     * Проверка, нужно ли запрашивать разрешения
     */
    fun shouldRequestUpdatePermissions(context: Context): Boolean {
        return !hasAllUpdatePermissions(context)
    }
}