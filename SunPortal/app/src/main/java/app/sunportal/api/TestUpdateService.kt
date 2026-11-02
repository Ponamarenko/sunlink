package app.sunportal.api

import app.sunportal.data.model.UpdateInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Тестовый API сервис для демонстрации обновлений
 */
interface TestUpdateService {
    
    @GET("https://api.github.com/repos/Ponamarenko/SunPortal/releases/latest")
    suspend fun getLatestRelease(
        @Query("current_version") currentVersion: String? = null
    ): Response<Map<String, Any>>
    
    @GET("https://api.github.com/repos/Ponamarenko/SunPortal/releases")
    suspend fun getAllReleases(): Response<List<Map<String, Any>>>
    
    // Метод для получения тестовых данных обновлений
    @GET("https://raw.githubusercontent.com/Ponamarenko/SunPortal/main/test-update.json")
    suspend fun getTestUpdateInfo(): UpdateInfo
}