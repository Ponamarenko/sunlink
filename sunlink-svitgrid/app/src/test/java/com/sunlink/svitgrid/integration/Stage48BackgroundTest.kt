package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.TelemetryQualityDto
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage48BackgroundTest {
    @Test
    fun `quality dto parses background polling diagnostics`() {
        val dto = Gson().fromJson(
            """{"known_parameters":80,"available_parameters":23,"polling_running":true,
                "cycles":42,"failed_cycles":2,"last_cycle_ms":310.5,"missed_cycles":1,
                "extended_polling":{"enabled":true,"jobs":23}}""",
            TelemetryQualityDto::class.java
        )
        assertTrue(dto.polling_running!!)
        assertEquals(42, dto.cycles)
        assertEquals(2, dto.failed_cycles)
        assertEquals(310.5, dto.last_cycle_ms!!, 0.0)
        assertEquals(1, dto.missed_cycles)
        assertTrue(dto.extended_polling!!.enabled!!)
    }

    @Test
    fun `diagnostics shows polling state`() {
        val src = File("src/main/java/com/sunlink/svitgrid/ui/diagnostics/DiagnosticsScreen.kt").readText()
        assertTrue(src.contains("polling_running"))
        assertTrue(src.contains("failed_cycles"))
    }

    @Test
    fun `telemetry catalog endpoint exists in backend`() {
        val src = File("../sunlink-svitgrid/server/deye_gateway.py").readText()
        assertTrue(src.contains("/api/telemetry/catalog"))
    }
}
