package com.sunlink.svitgrid.data.dto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterStatus

/**
 * Stage 29 — кадр `live_data` от backend WebSocket `/ws`.
 *
 * Формат фактически совпадает с ответом `deye_gateway.py`:
 * {
 *   "type": "live_data",
 *   "data": {"battery_soc": {"raw_value": 94, "normalized_value": 94.0,
 *            "unit": "%", "confidence": "corroborated", "quality": "valid",
 *            "address": 184, "timestamp": "..."}},
 *   "status": {"connection_status": true, "last_update": "...",
 *              "inverter_id": "...", "available_registers": 9, "source": "deye_modbus"},
 *   "timestamp": "..."
 * }
 *
 * Никаких локальных/симуляционных значений: DTO только описывает то,
 * что реально прислал сервер.
 */
data class LiveDataMessageDto(
    val type: String? = null,
    val data: Map<String, WsMeasurementDto>? = null,
    val status: WsStatusDto? = null,
    val timestamp: String? = null
)

data class WsMeasurementDto(
    val raw_value: Int? = null,
    val normalized_value: Double? = null,
    val unit: String? = null,
    val confidence: String? = null,
    val quality: String? = null,
    val address: Int? = null,
    val timestamp: String? = null
)

data class WsStatusDto(
    val connection_status: Boolean? = null,
    val last_update: String? = null,
    val inverter_id: String? = null,
    val available_registers: Int? = null,
    val source: String? = null
)

object WebSocketLiveParser {
    private val gson = Gson()
    private val messageType = object : TypeToken<LiveDataMessageDto>() {}.type

    fun parse(text: String): LiveDataMessageDto? = try {
        gson.fromJson<LiveDataMessageDto>(text, messageType)
    } catch (e: Exception) {
        null
    }

    fun isLiveData(message: LiveDataMessageDto?): Boolean =
        message?.type == "live_data"
}

/**
 * Преобразование WebSocket-кадра в доменные модели.
 *
 * Ключевое правило: значение появляется только если сервер реально прислал
 * measurement с quality=valid и normalized_value != null. Отсутствующие
 * параметры остаются null — UI обязан показывать «Немає даних», а не ноль.
 */
object WebSocketTelemetryMapper {

    fun toInverterData(message: LiveDataMessageDto): InverterData {
        val values = message.data.orEmpty()
        val provisional = values.filter { (_, m) ->
            m.confidence.equals("provisional", ignoreCase = true)
        }.keys.toSet()

        fun value(name: String): Float? {
            val m = values[name] ?: return null
            if (!m.quality.equals("valid", ignoreCase = true)) return null
            return m.normalized_value?.toFloat()
        }

        val connected = message.status?.connection_status == true
        return InverterData(
            inverterId = message.status?.inverter_id ?: "deye_sg02lp1_am3",
            pSun = value("p_sun"),
            pLoad = value("p_load"),
            pBatt = value("p_batt"),
            pGrid = value("p_grid"),
            batterySoc = value("battery_soc"),
            batteryVoltage = value("battery_voltage"),
            batteryCurrent = value("battery_current"),
            pv1Power = value("pv1_power") ?: value("p_string1"),
            pv2Power = value("pv2_power") ?: value("p_string2"),
            pv3Power = value("pv3_power") ?: value("p_string3"),
            pv1Voltage = value("pv1_voltage"),
            pv1Current = value("pv1_current"),
            pv2Voltage = value("pv2_voltage"),
            pv2Current = value("pv2_current"),
            pv3Voltage = value("pv3_voltage"),
            pv3Current = value("pv3_current"),
            gridVoltageL1 = value("grid_voltage_l1") ?: value("grid_voltage"),
            dcTemperature = value("dc_temperature"),
            acTemperature = value("ac_temperature"),
            timestamp = message.status?.last_update ?: message.timestamp,
            connectionStatus = connected,
            // Пропускаем источник как есть: DEMO не должен превращаться в реальный Modbus.
            source = message.status?.source ?: "unknown",
            quality = if (connected) "valid" else "unavailable",
            confidence = if (message.status?.source == "demo_simulator") "unverified" else "corroborated",
            provisionalParameters = provisional
        )
    }

    fun toInverterStatus(message: LiveDataMessageDto): InverterStatus = InverterStatus(
        online = message.status?.connection_status == true,
        lastUpdate = message.status?.last_update ?: message.timestamp,
        inverterId = message.status?.inverter_id ?: "deye_sg02lp1_am3",
        availableRegisters = message.status?.available_registers ?: 0,
        error = null
    )
}
