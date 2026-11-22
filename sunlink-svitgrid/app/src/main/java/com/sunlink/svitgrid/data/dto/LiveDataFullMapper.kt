package com.sunlink.svitgrid.data.dto

import com.sunlink.svitgrid.data.map.WsToFullFieldMap

/**
 * Объединяет кадр WebSocket `/ws` с категориальной моделью `/api/telemetry/full`.
 *
 * Новые значения приходят только из реального кадра; отсутствующие поля
 * остаются null. Расчётные поля не создаются на основе неполных данных.
 */
object LiveDataFullMapper {
    fun merge(current: TelemetryFullDto?, message: LiveDataMessageDto): TelemetryFullDto {
        val categories = current?.categories?.toMutableMap()?.mapValues { entry ->
            entry.value?.toMutableMap() ?: mutableMapOf()
        }?.toMutableMap() ?: mutableMapOf()

        message.data.orEmpty().forEach { (parameter, measurement) ->
            val target = WsToFullFieldMap[parameter] ?: return@forEach
            val (category, field) = target.split('.', limit = 2)
            val bucket = categories.getOrPut(category) { mutableMapOf() }
            bucket[field] = MeasurementValueDto(
                raw_value = measurement.raw_value,
                normalized_value = measurement.normalized_value,
                unit = measurement.unit,
                confidence = measurement.confidence,
                quality = measurement.quality,
                address = measurement.address,
                timestamp = measurement.timestamp
            )
        }

        return TelemetryFullDto(
            source = message.status?.source ?: current?.source,
            timestamp = message.status?.last_update ?: message.timestamp ?: current?.timestamp,
            connection_status = message.status?.connection_status ?: current?.connection_status,
            stale = current?.stale ?: false,
            inverter_id = current?.inverter_id,
            categories = categories
        )
    }
}
