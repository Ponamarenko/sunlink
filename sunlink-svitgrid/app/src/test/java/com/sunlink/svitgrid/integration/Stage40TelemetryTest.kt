package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.LiveDataFullMapper
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.WebSocketLiveParser
import com.sunlink.svitgrid.viewmodel.MainViewModel
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage40TelemetryTest {
    private val fullJson = """
        {"source":"deye_modbus","timestamp":"2026-11-22T10:00:00Z","connection_status":true,
         "categories":{
           "battery":{"soc":{"normalized_value":82.0,"unit":"%"},"voltage":null,"status":null,
             "total_charge_energy":null,"total_discharge_energy":null},
           "pv":{"pv1_voltage":null,"pv3_power":null,"status":null},
           "grid":{"current_l1":null,"import_power":null,"connected":null},
           "load":{"frequency":null,"power_factor":null},
           "inverter":{"operating_state":null,"fault_code":null,"efficiency":null},
           "energy":{"total_grid_import":null}
         }}
    """.trimIndent()

    @Test
    fun `full telemetry dto exposes all extended category fields`() {
        val dto = Gson().fromJson(fullJson, TelemetryFullDto::class.java)
        assertEquals(82.0, dto.categories!!["battery"]!!["soc"]!!.normalized_value)
        assertTrue(dto.categories!!["inverter"]!!.containsKey("operating_state"))
        assertTrue(dto.categories!!["grid"]!!.containsKey("import_power"))
        assertTrue(dto.categories!!["battery"]!!.containsKey("total_charge_energy"))
    }

    @Test
    fun `websocket merge maps new parameter names without inventing values`() {
        val frame = """
            {"type":"live_data","data":{
              "battery_soc":{"raw_value":82,"normalized_value":82.0,"unit":"%",
                "confidence":"corroborated","quality":"valid","address":184,"timestamp":"t"},
              "grid_power":{"raw_value":100,"normalized_value":1000.0,"unit":"W",
                "confidence":"provisional","quality":"valid","timestamp":"t"},
              "unknown_parameter":{"raw_value":1,"normalized_value":1.0,"unit":"X","timestamp":"t"}},
             "status":{"connection_status":true,"last_update":"t","source":"deye_modbus"}}
        """.trimIndent()
        val current = Gson().fromJson(fullJson, TelemetryFullDto::class.java)
        val merged = LiveDataFullMapper.merge(current, WebSocketLiveParser.parse(frame)!!)
        assertEquals(82.0, merged.categories!!["battery"]!!["soc"]!!.normalized_value)
        assertEquals(1000.0, merged.categories!!["grid"]!!["power"]!!.normalized_value)
        assertNull(merged.categories!!["grid"]!!["current_l1"])
        assertFalse(merged.categories!!.values.any { it.containsKey("unknown_parameter") })
    }

    @Test
    fun `inverter status screen route exists and is unique`() {
        val app = File("src/main/java/com/sunlink/svitgrid/ui/SvitGridApp.kt").readText()
        val routes = Regex("""composable\("([^"]+)"\)""").findAll(app).map { it.groupValues[1] }.toList()
        assertEquals(routes.size, routes.toSet().size)
        assertTrue(routes.contains("inverter-status"))
    }

    @Test
    fun `ukrainian and english resources stay in sync`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        listOf("screen_inverter", "inverter_operating_state", "battery_total_charge",
            "grid_import_power", "load_power_factor").forEach { assertTrue(keys(uk).contains(it)) }
    }
}
