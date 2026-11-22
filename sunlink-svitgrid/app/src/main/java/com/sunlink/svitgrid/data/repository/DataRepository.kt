package com.sunlink.svitgrid.data.repository

import com.sunlink.svitgrid.data.dto.*
import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.network.ApiService
import com.sunlink.svitgrid.network.NetworkDiagnostics
import com.sunlink.svitgrid.network.RequestType
import com.sunlink.svitgrid.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import com.sunlink.svitgrid.data.model.success
import com.sunlink.svitgrid.data.model.error

/**
 * Репозиторий данных инвертора.
 * Оборачивает Retrofit API, преобразует DTO в доменные модели,
 * обрабатывает ошибки и отсутствующие поля.
 */
open class DataRepository(private val apiService: ApiService? = null) {

    /**
     * Загружает текущие данные инвертора с endpoint /api/live/raw.
     * Возвращает InverterData и InverterStatus.
     */
    open suspend fun loadInverterData(): Result<Pair<InverterData, InverterStatus>> =
        withContext(Dispatchers.IO) {
            val endpoint = RetrofitClient.getBaseUrl() + "api/live/raw"
            NetworkDiagnostics.record(RequestType.REST, endpoint, "REQUEST")
            try {
                val response: Response<LiveRawDto> = apiService!!.getLiveRawDto()
                NetworkDiagnostics.record(
                    RequestType.REST, endpoint, "RESPONSE",
                    httpCode = response.code(), success = response.isSuccessful
                )
                if (response.isSuccessful) {
                    val dto = response.body()
                    if (dto != null) {
                        val data = dtoToInverterData(dto)
                        val status = dtoToStatus(dto)
                        success(Pair(data, status))
                    } else {
                        error(Exception("Пустой ответ сервера /api/live/raw"))
                    }
                } else {
                    error(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                NetworkDiagnostics.record(
                    RequestType.REST, endpoint, "FAILED", errorType = e.javaClass.simpleName
                )
                error(e)
            }
        }

    /**
     * Загружает список инверторов с /api/inverters (этап 12).
     */
    open suspend fun loadInverters(): Result<InvertersDto> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<InvertersDto> = apiService!!.getInvertersDto()
                if (response.isSuccessful) {
                    response.body()?.let { success(it) }
                        ?: error(Exception("Пустой ответ сервера /api/inverters"))
                } else {
                    error(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                error(e)
            }
        }

    /**
     * Загружает статус инвертора с endpoint /status.
     */
    open suspend fun loadStatus(): Result<InverterStatus> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<StatusDto> = apiService!!.getStatusDto()
                if (response.isSuccessful) {
                    val dto = response.body()
                    if (dto != null) {
                        success(
                            InverterStatus(
                                online = dto.connection_status ?: false,
                                lastUpdate = dto.last_update,
                                inverterId = dto.inverter_id ?: "deye_sg02lp1_am3",
                                availableRegisters = dto.available_registers,
                                error = null
                            )
                        )
                    } else {
                        error(Exception("Пустой ответ сервера /status"))
                    }
                } else {
                    error(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                error(e)
            }
        }

    /**
     * Загружает последние измерения с endpoint /api/measurements/latest.
     */
    suspend fun loadLatestMeasurements(): Result<MeasurementsLatestDto> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<MeasurementsLatestDto> = apiService!!.getLatestMeasurementsDto()
                if (response.isSuccessful) {
                    response.body()?.let { success(it) }
                        ?: error(Exception("Пустой ответ сервера /api/measurements/latest"))
                } else {
                    error(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                error(e)
            }
        }

    /**
     * Загружает историю измерений с endpoint /api/measurements/history.
     */
    open suspend fun loadHistory(parameter: String, start: String, end: String): Result<MeasurementsHistoryDto> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<MeasurementsHistoryDto> = apiService!!.getHistoryDto(
                    parameter = parameter, start = start, end = end
                )
                if (response.isSuccessful) {
                    response.body()?.let { success(it) }
                        ?: error(Exception("Пустой ответ сервера /api/measurements/history"))
                } else {
                    error(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                error(e)
            }
        }

    /**
     * Загружает описание регистров с endpoint /registers.
     */
    suspend fun loadRegisters(): Result<RegistersDto> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<RegistersDto> = apiService!!.getRegistersDto()
                if (response.isSuccessful) {
                    response.body()?.let { success(it) }
                        ?: error(Exception("Пустой ответ сервера /registers"))
                } else {
                    error(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                error(e)
            }
        }

    // ------------------------------------------------------------------
    // Comprehensive stage — полная телеметрия, статистика, health/version
    // ------------------------------------------------------------------

    open suspend fun loadFullTelemetry(): Result<TelemetryFullDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService!!.getTelemetryFull()
                if (response.isSuccessful) {
                    response.body()?.let { success(it) }
                        ?: error(Exception("Пустой ответ /api/telemetry/full"))
                } else error(Exception("HTTP ${response.code()}"))
            } catch (e: Exception) {
                error(e)
            }
        }

    open suspend fun loadTelemetryQuality(): Result<TelemetryQualityDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService!!.getTelemetryQuality()
                if (response.isSuccessful) {
                    response.body()?.let { success(it) }
                        ?: error(Exception("Пустой ответ /api/telemetry/quality"))
                } else error(Exception("HTTP ${response.code()}"))
            } catch (e: Exception) {
                error(e)
            }
        }

    open suspend fun loadStats(
        parameter: String,
        start: String,
        end: String
    ): Result<StatsSummaryDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService!!.getStatsSummary(parameter, start, end)
            if (response.isSuccessful) {
                response.body()?.let { success(it) }
                    ?: error(Exception("Пустой ответ /api/stats/summary"))
            } else error(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            error(e)
        }
    }

    open suspend fun loadHealth(): Result<HealthDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService!!.getHealthDto()
            if (response.isSuccessful) {
                response.body()?.let { success(it) }
                    ?: error(Exception("Пустой ответ /api/health"))
            } else error(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            error(e)
        }
    }

    open suspend fun loadBackendVersion(): Result<VersionDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService!!.getVersionDto()
            if (response.isSuccessful) {
                response.body()?.let { success(it) }
                    ?: error(Exception("Пустой ответ /api/version"))
            } else error(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            error(e)
        }
    }

    // ------------------------------------------------------------------
    // Преобразование DTO -> доменные модели
    // ------------------------------------------------------------------

    private fun dtoToInverterData(dto: LiveRawDto): InverterData {
        return InverterData(
            pSun = dto.p_sun?.toFloat(),
            pLoad = dto.p_load?.toFloat(),
            pBatt = dto.p_batt?.toFloat(),
            pGrid = dto.p_grid?.toFloat(),
            batterySoc = dto.batt_soc?.toFloat(),
            batteryVoltage = dto.battery_voltage?.toFloat(),
            batteryCurrent = dto.battery_current?.toFloat(),
            pv1Power = dto.pv1_power?.toFloat(),
            pv2Power = dto.pv2_power?.toFloat(),
            pv3Power = dto.pv3_power?.toFloat(),
            pv1Voltage = dto.pv1_voltage?.toFloat(),
            pv1Current = dto.pv1_current?.toFloat(),
            pv2Voltage = dto.pv2_voltage?.toFloat(),
            pv2Current = dto.pv2_current?.toFloat(),
            pv3Voltage = dto.pv3_voltage?.toFloat(),
            pv3Current = dto.pv3_current?.toFloat(),
            gridVoltageL1 = dto.grid_voltage_l1?.toFloat(),
            dcTemperature = dto.dc_temperature?.toFloat(),
            acTemperature = dto.ac_temperature?.toFloat(),
            timestamp = dto.timestamp,
            connectionStatus = dto.connection_status ?: false,
            // Реальный источник из backend: "modbus_register" или "demo_simulator".
            // Никогда не подменяем DEMO на modbus_register.
            source = dto.source ?: "unknown",
            quality = if (dto.connection_status == true) "valid" else "unavailable",
            // Плоский /api/live/raw не несёт confidence по каждому полю;
            // консервативно не выдаём "corroborated" для DEMO-данных.
            confidence = if (dto.source == "demo_simulator") "unverified" else "corroborated",
            provisionalParameters = dto.provisional_parameters.toSet()
        )
    }

    private fun dtoToStatus(dto: LiveRawDto): InverterStatus {
        return InverterStatus(
            online = dto.connection_status ?: false,
            lastUpdate = dto.timestamp,
            // Реальные значения из ответа backend, без хардкода.
            inverterId = dto.inverter_id ?: "unknown",
            availableRegisters = dto.available_registers ?: 0,
            error = dto.error
        )
    }
}
