package com.sunlink.svitgrid.data.dto

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Этап 12 — контрактные тесты Android ↔ Backend.
 *
 * Тесты читают РЕАЛЬНЫЕ JSON-ответы, записанные с изолированного DEMO backend
 * (demo_backend.py, порт 8801) в app/src/test/resources/demo/.
 * Никаких сетевых запросов и выдуманных данных — только фактический контракт.
 */
class DemoBackendContractTest {

    private val gson = Gson()

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("demo/$name")
            ?.bufferedReader()?.use { reader: java.io.BufferedReader -> reader.readText() }
            ?: error("Fixture demo/$name not found")

    @Test
    fun `live_raw fixture parses into LiveRawDto with real backend fields`() {
        val dto = gson.fromJson(fixture("live_raw.json"), LiveRawDto::class.java)

        assertEquals("demo_simulator", dto.source)
        assertEquals("deye_sg02lp1_am3", dto.inverter_id)
        assertEquals(9, dto.available_registers)
        assertEquals(true, dto.connection_status)

        // Реальные DEMO-значения из 9 регистров этапа 7
        assertEquals(85.0, dto.batt_soc!!, 0.001)
        assertEquals(53.22, dto.battery_voltage!!, 0.001)
        assertEquals(3.61, dto.battery_current!!, 0.001)
        assertEquals(211.0, dto.pv1_power!!, 0.001)
        assertEquals(131.0, dto.pv2_power!!, 0.001)
        assertEquals(107.0, dto.pv3_power!!, 0.001)
        assertEquals(449.0, dto.p_sun!!, 0.001) // сумма PV
        assertEquals(1915.0, dto.p_batt!!, 0.001)

        // Отсутствующие в register_map параметры приходят как null, НЕ 0.0
        assertNull(dto.p_grid)
        assertNull(dto.p_load)

        // timestamp в ISO-8601 UTC с +00:00 — парсится как строка
        assertTrue(dto.timestamp!!.contains("+00:00") || dto.timestamp!!.endsWith("Z"))
    }

    @Test
    fun `status fixture has source and real register list`() {
        val dto = gson.fromJson(fixture("status.json"), StatusDto::class.java)
        assertEquals("demo_simulator", dto.source)
        assertEquals("deye_sg02lp1_am3", dto.inverter_id)
        assertEquals(9, dto.available_registers)
        assertEquals(
            listOf(90, 91, 183, 184, 186, 187, 188, 190, 191),
            dto.registers
        )
        assertEquals(true, dto.connection_status)
    }

    @Test
    fun `inverters fixture provides real single inverter`() {
        val dto = gson.fromJson(fixture("inverters.json"), InvertersDto::class.java)
        assertEquals(1, dto.count)
        assertEquals(1, dto.inverters.size)
        assertEquals("deye_sg02lp1_am3", dto.inverters[0].inverter_id)
        assertEquals(true, dto.inverters[0].online)
        assertEquals(9, dto.inverters[0].available_registers)
        assertEquals("demo_simulator", dto.inverters[0].source)
    }

    @Test
    fun `latest measurements fixture has 9 measurements with provenance`() {
        val dto = gson.fromJson(fixture("measurements_latest.json"), MeasurementsLatestDto::class.java)
        assertEquals(9, dto.count)
        assertEquals(9, dto.measurements.size)
        assertEquals("deye_sg02lp1_am3", dto.inverter_id)

        val byName = dto.measurements.associateBy { it.parameter_name }
        assertTrue(byName.keys.containsAll(
            listOf("battery_soc", "battery_voltage", "battery_power",
                "pv1_power", "pv2_power", "pv3_power",
                "battery_current", "dc_temperature", "ac_temperature")
        ))

        // Каждое измерение несёт источник, качество и достоверность
        dto.measurements.forEach { m ->
            assertEquals("demo_simulator", m.source)
            assertEquals("valid", m.quality)
            assertNotNull(m.normalized_value)
            assertNotNull(m.timestamp)
            assertNotNull(m.unit)
        }

        // Provisional-регистры не выдаются за подтверждённые
        assertEquals("provisional", byName["battery_power"]!!.confidence)
        assertEquals("provisional", byName["dc_temperature"]!!.confidence)
        assertEquals("corroborated", byName["battery_soc"]!!.confidence)
    }

    @Test
    fun `single latest measurement fixture parses`() {
        val m = gson.fromJson(fixture("measurement_latest_battery_soc.json"), MeasurementDto::class.java)
        assertEquals("battery_soc", m.parameter_name)
        assertEquals(184, m.register_address)
        assertEquals(85.0, m.normalized_value!!, 0.001)
        assertEquals("%", m.unit)
        assertEquals("demo_simulator", m.source)
        assertEquals("corroborated", m.confidence)
    }

    @Test
    fun `history fixture parses into sorted non-empty points`() {
        val dto = gson.fromJson(fixture("history_battery_soc.json"), MeasurementsHistoryDto::class.java)
        assertEquals("battery_soc", dto.parameter)
        assertTrue(dto.count > 0)
        assertEquals(dto.count, dto.measurements.size)

        val timestamps = dto.measurements.map { it.timestamp!! }
        assertEquals(timestamps, timestamps.sorted()) //Backend сортирует по времени

        dto.measurements.forEach { m ->
            assertEquals("battery_soc", m.parameter_name)
            assertEquals("%", m.unit)
            assertNotNull(m.normalized_value)
            assertEquals("demo_simulator", m.source)
        }
    }

    @Test
    fun `empty history fixture parses honestly`() {
        val dto = gson.fromJson(fixture("history_empty.json"), MeasurementsHistoryDto::class.java)
        assertEquals(0, dto.count)
        assertTrue(dto.measurements.isEmpty())
    }
}
