package com.sunlink.mobile.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private const val BASE_URL = "http://sunlink.pp.ua:8000/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    // Метод для создания кастомного Retrofit с другим URL
    fun createCustomApiService(baseUrl: String): ApiService {
        val customRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return customRetrofit.create(ApiService::class.java)
    }
    
    // Методы для разных типов подключения
    fun getApiServiceForConnectionType(connectionType: String): ApiService {
        return when (connectionType) {
            "solarman" -> createCustomApiService("https://solarman.pp.ua/api/")
            "esp32" -> createCustomApiService("http://192.168.1.100:8080/")
            "rs485" -> createCustomApiService("http://192.168.1.101:8080/") // RS485 gateway
            "ethernet" -> createCustomApiService("http://192.168.1.102:8080/") // Direct Ethernet
            else -> apiService
        }
    }
}