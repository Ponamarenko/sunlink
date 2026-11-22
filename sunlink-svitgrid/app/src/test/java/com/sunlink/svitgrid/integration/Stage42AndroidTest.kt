package com.sunlink.svitgrid.integration

import com.google.gson.Gson
import com.sunlink.svitgrid.data.dto.TelemetryQualityDto
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Stage42AndroidTest {
    @Test
    fun `quality dto parses extended polling status`() {
        val dto = Gson().fromJson(
            """{"known_parameters":80,"available_parameters":1,"active_registers":[184],
                "extended_polling":{"enabled":false,"jobs":0,"dry_run":false}}""",
            TelemetryQualityDto::class.java
        )
        assertEquals(80, dto.known_parameters)
        assertEquals(listOf(184), dto.active_registers)
        assertFalse(dto.extended_polling!!.enabled!!)
        assertEquals(0, dto.extended_polling!!.jobs)
    }

    @Test
    fun `localization parity includes extended polling key`() {
        val uk = File("src/main/res/values/strings.xml").readText()
        val en = File("src/main/res/values-en/strings.xml").readText()
        fun keys(s: String) = Regex("""<string name="([^"]+)"""").findAll(s).map { it.groupValues[1] }.toSet()
        assertEquals(keys(uk), keys(en))
        assertTrue(keys(uk).contains("extended_polling_status"))
    }

    @Test
    fun `android production sources contain no modbus client`() {
        val root = File("src/main/java/com/sunlink/svitgrid")
        val violations = root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            listOf("ModbusTcpClient", "write_register", "FC06").filter { file.readText().contains(it) }
                .map { "${file.name}:$it" }
        }.toList()
        assertTrue("violations: $violations", violations.isEmpty())
    }
}
