package com.sunlink.svitgrid.data.dto

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

/**
 * Тести парсингу DTO відповідей API.
 * Використовує реальні формати JSON з deye_gateway.py (етап 10).
 */
class ApiResponseDtoTest {

    private val gson = Gson()

    @Test
    fun `live raw dto parse complete response`() {
        val json = """{
            "p_sun": 1500.5,
            "p_load": 1200.0,
            "p_batt": -100.0,
            "p_grid": 300.0,
            "batt_soc": 86.0,
            "battery_voltage": 52.5,
            "battery_current": -2.0,
            "dc_temperature": 45.0,
            "ac_temperature": 40.0,
            "pv1_power": 800.0,
            "pv2_power": 700.0,
            "pv3_power": 0.0,
            "p_string1": 800.0,
            "p_string2": 700.0,
            "p_string3": 0.0,
            "grid_voltage_l1": 230.0,
            "timestamp": "2026-11-18T10:00:00Z",
            "connection_status": true,
            "error": null
        }"""
        val dto = gson.fromJson(json, LiveRawDto::class.java)
        assertNotNull(dto)
        assertEquals(1500.5, dto.p_sun!!, 0.001)
        assertEquals(86.0, dto.batt_soc!!, 0.001)
        assertEquals(52.5, dto.battery_voltage!!, 0.001)
        assertEquals(true, dto.connection_status!!)
        assertNull(dto.error)
    }

    @Test
    fun `live raw dto parse partial response`() {
        // Сервер може повернути лише частину полів
        val json = """{
            "p_sun": 1200.0,
            "batt_soc": 88.0,
            "connection_status": true
        }"""
        val dto = gson.fromJson(json, LiveRawDto::class.java)
        assertNotNull(dto)
        assertEquals(1200.0, dto.p_sun!!, 0.001)
        assertEquals(88.0, dto.batt_soc!!, 0.001)
        assertNull(dto.p_load)  // відсутнє поле = null
        assertNull(dto.error)
    }

    @Test
    fun `live raw dto error response`() {
        val json = """{
            "p_sun": 0.0,
            "p_load": 0.0,
            "p_batt": 0.0,
            "p_grid": 0.0,
            "batt_soc": 0.0,
            "connection_status": false,
            "error": "Modbus unavailable"
        }"""
        val dto = gson.fromJson(json, LiveRawDto::class.java)
        assertNotNull(dto)
        assertEquals(false, dto.connection_status)
        assertEquals("Modbus unavailable", dto.error)
    }

    @Test
    fun `measurements latest dto parse`() {
        val json = """{
            "inverter_id": "deye_sg02lp1_am3",
            "count": 2,
            "measurements": [
                {
                    "inverter_id": "deye_sg02lp1_am3",
                    "register_address": 184,
                    "parameter_name": "battery_soc",
                    "raw_value": 86,
                    "normalized_value": 86.0,
                    "unit": "%",
                    "timestamp": "2026-11-18T10:00:00Z",
                    "quality": "valid",
                    "confidence": "corroborated",
                    "source": "modbus_register",
                    "description": "Battery SOC"
                },
                {
                    "inverter_id": "deye_sg02lp1_am3",
                    "register_address": 183,
                    "parameter_name": "battery_voltage",
                    "raw_value": 5250,
                    "normalized_value": 52.5,
                    "unit": "V",
                    "timestamp": "2026-11-18T10:00:00Z",
                    "quality": "valid",
                    "confidence": "corroborated",
                    "source": "modbus_register",
                    "description": "Battery Voltage"
                }
            ]
        }"""
        val dto = gson.fromJson(json, MeasurementsLatestDto::class.java)
        assertNotNull(dto)
        assertEquals("deye_sg02lp1_am3", dto.inverter_id)
        assertEquals(2, dto.count)
        assertEquals(2, dto.measurements.size)
        assertEquals("battery_soc", dto.measurements[0].parameter_name)
        assertEquals(86.0, dto.measurements[0].normalized_value!!, 0.001)
        assertEquals("valid", dto.measurements[0].quality)
        assertEquals("corroborated", dto.measurements[0].confidence)
    }

    @Test
    fun `measurement dto parse`() {
        val json = """{
            "inverter_id": "deye_sg02lp1_am3",
            "register_address": 186,
            "parameter_name": "pv1_power",
            "raw_value": 1500,
            "normalized_value": 1500.0,
            "unit": "W",
            "timestamp": "2026-11-18T10:00:00Z",
            "quality": "valid",
            "confidence": "provisional",
            "source": "modbus_register",
            "description": "PV1 Power"
        }"""
        val dto = gson.fromJson(json, MeasurementDto::class.java)
        assertNotNull(dto)
        assertEquals("pv1_power", dto.parameter_name)
        assertEquals(1500, dto.raw_value)
        assertEquals(1500.0, dto.normalized_value!!, 0.001)
        assertEquals("W", dto.unit)
        assertEquals("provisional", dto.confidence)
    }

    @Test
    fun `status dto parse`() {
        val json = """{
            "connection_status": true,
            "last_update": "2026-11-18T10:00:00Z",
            "inverter_id": "deye_sg02lp1_am3",
            "available_registers": 9,
            "registers": [90, 91, 183, 184, 186, 187, 188, 190, 191]
        }"""
        val dto = gson.fromJson(json, StatusDto::class.java)
        assertNotNull(dto)
        assertEquals(true, dto.connection_status)
        assertEquals("deye_sg02lp1_am3", dto.inverter_id)
        assertEquals(9, dto.available_registers)
        assertEquals(9, dto.registers.size)
    }

    @Test
    fun `registers dto parse`() {
        val json = """{
            "registers": [
                {
                    "address": 183,
                    "name": "battery_voltage",
                    "data_type": "U_WORD",
                    "scale": 0.01,
                    "unit": "V",
                    "confidence": "corroborated",
                    "provisional_reason": "",
                    "description": "Battery Voltage"
                },
                {
                    "address": 190,
                    "name": "battery_power",
                    "data_type": "S_WORD",
                    "scale": 1.0,
                    "unit": "W",
                    "confidence": "provisional",
                    "provisional_reason": "Sign semantics not confirmed",
                    "description": "Battery Power"
                }
            ],
            "count": 2,
            "source": "domain.register_metadata (stage 7)"
        }"""
        val dto = gson.fromJson(json, RegistersDto::class.java)
        assertNotNull(dto)
        assertEquals(2, dto.count)
        assertEquals(2, dto.registers.size)
        assertEquals(183, dto.registers[0].address)
        assertEquals("battery_voltage", dto.registers[0].name)
        assertEquals("corroborated", dto.registers[0].confidence)
        assertEquals(190, dto.registers[1].address)
        assertEquals("provisional", dto.registers[1].confidence)
    }

    @Test
    fun `measurements history dto parse`() {
        val json = """{
            "inverter_id": "deye_sg02lp1_am3",
            "parameter": "batt_soc",
            "start": "2026-11-01T00:00:00Z",
            "end": "2026-11-18T23:59:59Z",
            "count": 3,
            "measurements": [
                {"parameter_name": "batt_soc", "normalized_value": 80.0, "unit": "%", "timestamp": "2026-11-18T08:00:00Z"},
                {"parameter_name": "batt_soc", "normalized_value": 83.0, "unit": "%", "timestamp": "2026-11-18T10:00:00Z"},
                {"parameter_name": "batt_soc", "normalized_value": 86.0, "unit": "%", "timestamp": "2026-11-18T12:00:00Z"}
            ]
        }"""
        val dto = gson.fromJson(json, MeasurementsHistoryDto::class.java)
        assertNotNull(dto)
        assertEquals("batt_soc", dto.parameter)
        assertEquals(3, dto.count)
        assertEquals(3, dto.measurements.size)
        assertEquals(80.0, dto.measurements[0].normalized_value!!, 0.001)
        assertEquals(86.0, dto.measurements[2].normalized_value!!, 0.001)
    }
}
