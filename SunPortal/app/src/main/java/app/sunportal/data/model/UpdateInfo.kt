package app.sunportal.data.model

import com.google.gson.annotations.SerializedName

/**
 * Модель информации об обновлении
 */
data class UpdateInfo(
    @SerializedName("version") val version: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("changelog") val changelog: String,
    @SerializedName("release_date") val releaseDate: String,
    @SerializedName("min_sdk") val minSdk: Int,
    @SerializedName("size") val size: Long,
    @SerializedName("force_update") val forceUpdate: Boolean = false
) {
    // Конструктор без параметров для Gson
    constructor() : this(
        version = "1.0.32",
        versionCode = 33,
        downloadUrl = "",
        changelog = "",
        releaseDate = "",
        minSdk = 26,
        size = 0L,
        forceUpdate = false
    )
    
    // Проверка, есть ли доступное обновление
    fun hasUpdate(currentVersionCode: Int): Boolean {
        return versionCode > currentVersionCode
    }
    
    // Форматированный размер файла
    val formattedSize: String get() = when {
        size < 1024 -> "${size} B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
    
    // Цвет для индикатора обновления
    val updateColor: String get() = when {
        forceUpdate -> "#F44336" // Красный для обязательных обновлений
        hasUpdate(33) -> "#FF9800" // Оранжевый для доступных обновлений
        else -> "#4CAF50" // Зеленый для актуальной версии
    }
}