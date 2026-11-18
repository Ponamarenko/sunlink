package com.sunlink.svitgrid.data.repository

import com.sunlink.svitgrid.data.dto.*
import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Репозиторий данных инвертора.
 * Оборачивает Retrofit API, преобразует DTO в доменные модели,
 * обрабатывает ошибки и отсутствующие поля.
 */
class DataRepository(private val apiService: ApiService? = null) {

    /**
     * Загружает текущие данные инвертора с endpoint /api/live/raw.
     * Возвращает InverterData и InverterStatus.
     */
    suspend fun loadInverterData(): Result<Pair<InverterData, InverterStatus>> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<LiveRawDto> = apiService!!.getLiveRawDto()
                if (response.isSuccessful) {
                    val dto = response.body()
                    if (dto != null) {
                        val data = dtoToInverterData(dto)
                        val status = dtoToStatus(dto)
                        Result.success(Pair(data, status))
                    } else {
                        Result.failure(Exception("Пустой ответ сервера /api/live/raw"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Загружает статус инвертора с endpoint /status.
     */
    suspend fun loadStatus(): Result<InverterStatus> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<StatusDto> = apiService!!.getStatusDto()
                if (response.isSuccessful) {
                    val dto = response.body()
                    if (dto != null) {
                        Result.success(
                            InverterStatus(
                                online = dto.connection_status ?: false,
                                lastUpdate = dto.last_update,
                                inverterId = dto.inverter_id ?: "deye_sg02lp1_am3",
                                availableRegisters = dto.available_registers,
                                error = null
                            )
                        )
                    } else {
                        Result.failure(Exception("Пустой ответ сервера /status"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
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
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Пустой ответ сервера /api/measurements/latest"))
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Загружает историю измерений с endpoint /api/measurements/history.
     */
    suspend fun loadHistory(parameter: String, start: String, end: String): Result<MeasurementsHistoryDto> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<MeasurementsHistoryDto> = apiService!!.getHistoryDto(
                    parameter = parameter, start = start, end = end
                )
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Пустой ответ сервера /api/measurements/history"))
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
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
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Пустой ответ сервера /registers"))
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
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
            source = "modbus_register",
            quality = if (dto.connection_status == true) "valid" else "unavailable",
            confidence = "corroborated"
        )
    }

    private fun dtoToStatus(dto: LiveRawDto): InverterStatus {
        return InverterStatus(
            online = dto.connection_status ?: false,
            lastUpdate = dto.timestamp,
            inverterId = "deye_sg02lp1_am3",
            availableRegisters = 9,
            error = dto.error
        )
    }
}
