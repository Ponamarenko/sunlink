package com.sunlink.svitgrid.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Response

interface ApiService {
    @GET("api/v1/inverters")
    suspend fun getInverters(): Response<Map<String, Any>>

    @GET("api/v1/inverters/{id}/data")
    suspend fun getInverterData(@Path("id") id: String): Response<Map<String, Any>>

    @GET("api/v1/inverters/{id}/status")
    suspend fun getInverterStatus(@Path("id") id: String): Response<Map<String, Any>>

    @GET("api/v1/inverters/{id}/telemetry")
    suspend fun getTelemetry(@Path("id") id: String): Response<Map<String, Any>>

    @GET("api/v1/system/stats")
    suspend fun getSystemStats(): Response<Map<String, Any>>

    @GET("api/v1/health")
    suspend fun getHealth(): Response<Map<String, Any>>
}
