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
import com.sunlink.svitgrid.data.dto.VersionDto
import retrofit2.Response

/**
 * Стабильная шина ApiService: делегирует вызовы текущему сервису Retrofit.
 *
 * Repository и ViewModel создаются один раз и хранят ссылку на этот объект.
 * При смене адреса backend в Настройках RetrofitClient пересоздаёт внутренний
 * сервис, а шина остаётся той же — поэтому все последующие запросы идут
 * на новый адрес без перезапуска приложения.
 */
class DelegatingApiService(private val provider: () -> ApiService) : ApiService {

    private val api: ApiService get() = provider()

    override suspend fun getLiveRaw(): Response<JsonElement> = api.getLiveRaw()
    override suspend fun getLive(): Response<JsonElement> = api.getLive()
    override suspend fun getLiveByUrl(url: String): Response<JsonElement> = api.getLiveByUrl(url)
    override suspend fun getInverters(): Response<Map<String, Any>> = api.getInverters()
    override suspend fun getInverterData(id: String): Response<Map<String, Any>> = api.getInverterData(id)
    override suspend fun getInverterStatus(id: String): Response<Map<String, Any>> = api.getInverterStatus(id)
    override suspend fun getTelemetry(id: String): Response<Map<String, Any>> = api.getTelemetry(id)
    override suspend fun getSystemStats(): Response<Map<String, Any>> = api.getSystemStats()
    override suspend fun getHealth(): Response<Map<String, Any>> = api.getHealth()
    override suspend fun getWeather(): Response<Map<String, Any>> = api.getWeather()
    override suspend fun getForecast(): Response<Map<String, Any>> = api.getForecast()
    override suspend fun getLiveRawDto(): Response<LiveRawDto> = api.getLiveRawDto()
    override suspend fun getStatusDto(): Response<StatusDto> = api.getStatusDto()
    override suspend fun getInvertersDto(): Response<InvertersDto> = api.getInvertersDto()
    override suspend fun getRegistersDto(): Response<RegistersDto> = api.getRegistersDto()
    override suspend fun getLatestMeasurementsDto(): Response<MeasurementsLatestDto> = api.getLatestMeasurementsDto()
    override suspend fun getLatestMeasurementDto(parameter: String): Response<MeasurementDto> =
        api.getLatestMeasurementDto(parameter)
    override suspend fun getHistoryDto(
        parameter: String,
        start: String,
        end: String
    ): Response<MeasurementsHistoryDto> = api.getHistoryDto(parameter, start, end)
    override suspend fun getTelemetryFull(): Response<TelemetryFullDto> = api.getTelemetryFull()
    override suspend fun getStatsSummary(
        parameter: String,
        start: String,
        end: String
    ): Response<StatsSummaryDto> = api.getStatsSummary(parameter, start, end)
    override suspend fun getHealthDto(): Response<HealthDto> = api.getHealthDto()
    override suspend fun getVersionDto(): Response<VersionDto> = api.getVersionDto()
}
