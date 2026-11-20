package com.sunlink.svitgrid.network

import com.sunlink.svitgrid.BuildConfig
import com.sunlink.svitgrid.data.BackendDefaults
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var baseUrl: String = BackendDefaults.BASE_URL
    private var okHttpClient: OkHttpClient? = null
    private var retrofit: Retrofit? = null
    private var realService: ApiService? = null

    /** Stage 29: подписчики на смену адреса (WebSocket должен переподключиться). */
    private val baseUrlListeners = mutableListOf<(String) -> Unit>()

    fun addBaseUrlListener(listener: (String) -> Unit) {
        synchronized(baseUrlListeners) { baseUrlListeners.add(listener) }
    }

    fun removeBaseUrlListener(listener: (String) -> Unit) {
        synchronized(baseUrlListeners) { baseUrlListeners.remove(listener) }
    }

    /** Единственный экземпляр, который держат Repository/ViewModel. */
    private val bus: ApiService = DelegatingApiService { currentService() }

    private fun currentService(): ApiService {
        if (realService == null) {
            val logging = HttpLoggingInterceptor().apply {
                // Тело запросов логируем только в debug; release — минимум.
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            }
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient!!)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            realService = retrofit!!.create(ApiService::class.java)
        }
        return realService!!
    }

    fun getApiService(): ApiService = bus

    fun getBaseUrl(): String = baseUrl

    /**
     * Stage 29: адрес WebSocket того же backend. Используется тот же хост/порт
     * и та же политика TLS (http→ws, https→wss). Никаких демо-адресов.
     */
    fun getWebSocketUrl(): String {
        val parsed = baseUrl.toHttpUrlOrNull()
            ?: return "ws://127.0.0.1:8800/ws"
        val scheme = when (parsed.scheme) {
            "https" -> "wss"
            else -> "ws"
        }
        val port = if (parsed.port > 0) ":${parsed.port}" else ""
        return "$scheme://${parsed.host}$port/ws"
    }

    /**
     * Меняет адрес backend. Возвращает false для некорректного URL —
     * предыдущая конфигурация остаётся рабочей.
     */
    fun setBaseUrl(url: String): Boolean {
        val normalized = normalizeBaseUrl(url) ?: return false
        if (normalized == baseUrl && realService != null) return true

        // Отменяем запросы к старому серверу, чтобы они не изменили UI.
        okHttpClient?.dispatcher?.cancelAll()
        baseUrl = normalized
        realService = null
        retrofit = null
        okHttpClient = null
        synchronized(baseUrlListeners) {
            baseUrlListeners.toList().forEach { runCatching { it(normalized) } }
        }
        return true
    }

    private fun normalizeBaseUrl(url: String): String? {
        val candidate = if (url.endsWith("/")) url else "$url/"
        return try {
            val parsed = candidate.toHttpUrlOrNull() ?: return null
            if (parsed.scheme !in setOf("http", "https")) return null
            if (parsed.host.isNullOrEmpty()) return null
            parsed.toString()
        } catch (e: Exception) {
            null
        }
    }
}
