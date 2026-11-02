package app.sunportal.api

import retrofit2.http.GET
import retrofit2.http.Query
import app.sunportal.data.model.UpdateInfo
import retrofit2.http.POST
import retrofit2.http.Body

/**
 * API сервис для проверки обновлений
 */
interface UpdateService {
    
    @GET("api/update/check")
    suspend fun checkForUpdates(
        @Query("current_version") currentVersion: String,
        @Query("current_code") currentCode: Int,
        @Query("device_id") deviceId: String,
        @Query("platform") platform: String = "android"
    ): UpdateInfo
    
    @GET("api/update/latest")
    suspend fun getLatestUpdate(): UpdateInfo
    
    @POST("api/update/download")
    suspend fun logDownload(
        @Body downloadInfo: Map<String, Any>
    ): Map<String, Any>
    
    // Тестовый эндпоинт для демонстрации
    @GET("api/update/test")
    suspend fun getTestUpdate(): UpdateInfo
}