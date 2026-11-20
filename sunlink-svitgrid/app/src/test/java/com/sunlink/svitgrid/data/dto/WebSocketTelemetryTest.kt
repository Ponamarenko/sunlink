package com.sunlink.svitgrid.data.dto

import org.junit.Assert.*
import org.junit.Test

/**
 * Stage 29 — парсинг и маппинг РЕАЛЬНОГО кадра `/ws`.
 * JSON ниже — формат ответа deye_gateway.py (проверен на физическом reads).
 */
class WebSocketTelemetryTest {

    private val realFrame = """
        {
          "type": "live_data",
          "data": {
            "battery_soc": {
              "raw_value": 94,
              "normalized_value": 94.0,
              "unit": "%",
              "confidence": "corroborated",
              "quality": "valid",
              "address": 184,
              "timestamp": "2026-11-20T08:09:59.614700+00:00"
            }
          },
          "status": {
            "connection_status": true,
            "last_update": "2026-11-20T08:09:59.614700+00:00",
            "inverter_id": "deye_sg02lp1_am3",
            "available_registers": 9,
            "registers": [90, 91, 183, 184, 186, 187, 188, 190, 191],
            "source": "deye_modbus"
          },
          "timestamp": "2026-11-20T08:12:24.691288+00:00"
        }
    """.trimIndent()

    @Test
    fun `parses real live_data frame`() {
        val msg = WebSocketLiveParser.parse(realFrame)
        assertNotNull(msg)
        assertTrue(WebSocketLiveParser.isLiveData(msg))
        assertEquals("deye_modbus", msg!!.status!!.source)
        assertEquals(94, msg.data!!["battery_soc"]!!.raw_value)
        assertEquals(184, msg.data!!["battery_soc"]!!.address)
    }

    @Test
    fun `maps only confirmed values and keeps missing fields null`() {
        val msg = WebSocketLiveParser.parse(realFrame)!!
        val data = WebSocketTelemetryMapper.toInverterData(msg)
        val status = WebSocketTelemetryMapper.toInverterStatus(msg)

        assertEquals(94.0f, data.batterySoc)
        assertTrue(data.hasMeasurements)
        assertTrue(status.online)
        assertEquals("deye_modbus", data.source)
        assertEquals("valid", data.quality)
        assertEquals("deye_sg02lp1_am3", data.inverterId)
        assertTrue(data.provisionalParameters.isEmpty())

        // Неприсланые параметры остаются null — не нулями.
        assertNull(data.pSun)
        assertNull(data.pLoad)
        assertNull(data.pGrid)
        assertNull(data.pBatt)
        assertNull(data.pv1Power)
        assertNull(data.batteryVoltage)
        assertNull(data.dcTemperature)
    }

    @Test
    fun `rejects non valid quality and empty data`() {
        val stale = WebSocketLiveParser.parse(
            realFrame.replace("\"quality\": \"valid\"", "\"quality\": \"stale\"")
        )!!
        assertNull(WebSocketTelemetryMapper.toInverterData(stale).batterySoc)

        val empty = WebSocketLiveParser.parse(
            realFrame.replace("\"battery_soc\": {", "\"unused\": {")
        )!!
        assertFalse(WebSocketTelemetryMapper.toInverterData(empty).hasMeasurements)
    }

    @Test
    fun `demo source is never silently promoted to real modbus`() {
        val demo = WebSocketLiveParser.parse(
            realFrame.replace("\"source\": \"deye_modbus\"", "\"source\": \"demo_simulator\"")
        )!!
        val data = WebSocketTelemetryMapper.toInverterData(demo)
        assertEquals("demo_simulator", data.source)
        assertEquals("unverified", data.confidence)
    }

    @Test
    fun `malformed frame is ignored`() {
        assertNull(WebSocketLiveParser.parse("not a json"))
        assertFalse(WebSocketLiveParser.isLiveData(WebSocketLiveParser.parse("{\"type\":\"ping\"}")))
    }

    @Test
    fun `provisional measurement is marked, not hidden as confirmed`() {
        val provisional = WebSocketLiveParser.parse(
            realFrame.replace("\"confidence\": \"corroborated\"", "\"confidence\": \"provisional\"")
        )!!
        val data = WebSocketTelemetryMapper.toInverterData(provisional)
        assertTrue(data.provisionalParameters.contains("battery_soc"))
    }
}
