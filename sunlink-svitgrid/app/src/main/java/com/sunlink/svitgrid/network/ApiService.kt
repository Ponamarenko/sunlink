package com.sunlink.svitgrid.network

import com.google.gson.JsonElement
import com.sunlink.svitgrid.data.dto.InvertersDto
import com.sunlink.svitgrid.data.dto.HealthDto
import com.sunlink.svitgrid.data.dto.LiveRawDto
import com.sunlink.svitgrid.data.dto.MeasurementDto
import com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto
import com.sunlink.svitgrid.data.dto.MeasurementsLatestDto
import com.sunlink.svitgrid.data.dto.RegistersDto
import com.sunlink.svitgrid.data.dto.StatsSummaryDto
import com.sunlink.svitgrid.data.dto.StatusDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.TelemetryQualityDto
import com.sunlink.svitgrid.data.dto.VersionDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @GET("api/live/raw")
    suspend fun getLiveRaw(): Response<JsonElement>

    @GET("live")
    suspend fun getLive(): Response<JsonElement>

    @GET
    suspend fun getLiveByUrl(@Url url: String): Response<JsonElement>

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

    @GET("weather")
    suspend fun getWeather(): Response<Map<String, Any>>

    @GET("forecast")
    suspend fun getForecast(): Response<Map<String, Any>>

    // Этап 10 — production endpoints deye_gateway.py
    @GET("api/live/raw")
    suspend fun getLiveRawDto(): Response<LiveRawDto>

    @GET("status")
    suspend fun getStatusDto(): Response<StatusDto>

    @GET("api/inverters")
    suspend fun getInvertersDto(): Response<InvertersDto>

    @GET("registers")
    suspend fun getRegistersDto(): Response<RegistersDto>

    @GET("api/measurements/latest")
    suspend fun getLatestMeasurementsDto(): Response<MeasurementsLatestDto>

    @GET("api/measurements/latest/{parameter}")
    suspend fun getLatestMeasurementDto(@Path("parameter") parameter: String): Response<MeasurementDto>

    @GET("api/measurements/history")
    suspend fun getHistoryDto(
        @Query("parameter") parameter: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<MeasurementsHistoryDto>

    // Comprehensive stage — категориальная модель и статистика.
    @GET("api/telemetry/full")
    suspend fun getTelemetryFull(): Response<TelemetryFullDto>

    @GET("api/telemetry/quality")
    suspend fun getTelemetryQuality(): Response<TelemetryQualityDto>

    @GET("api/stats/summary")
    suspend fun getStatsSummary(
        @Query("parameter") parameter: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<StatsSummaryDto>

    @GET("api/health")
    suspend fun getHealthDto(): Response<HealthDto>

    @GET("api/version")
    suspend fun getVersionDto(): Response<VersionDto>
}
