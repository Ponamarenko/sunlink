package com.sunlink.svitgrid.network

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response

interface ApiService {
    @GET("api/live/raw")
    suspend fun getInverterData(): Response<Map<String, Any>>

    @GET("weather_read.php")
    suspend fun getWeatherData(): Response<Map<String, Any>>

    @GET("forecast_read.php")
    suspend fun getForecastData(): Response<Map<String, Any>>

    @GET("tou_read.php")
    suspend fun getTouData(): Response<Map<String, Any>>
}
