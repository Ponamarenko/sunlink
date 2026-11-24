package com.sunlink.svitgrid.ui.redesign

import com.sunlink.svitgrid.data.dto.MeasurementValueDto
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.util.Locale

class Stage53UxTelemetryPolishTest {

    private val redesign = File("src/main/java/com/sunlink/svitgrid/ui/redesign")
    private fun src(name: String) = File("src/main/java/com/sunlink/svitgrid/ui/redesign/$name").readText()

    @Test
    fun `transient REST failure is not shown as global telemetry loss when WS data is fresh`() {
        val dns = "Unable to resolve host \"sunlink.pp.ua\": No address associated with hostname"
        assertEquals("REST тимчасово недоступний · дані з WebSocket", telemetryErrorNote(dns, true))
        assertEquals(dns, telemetryErrorNote(dns, false))
        assertNull(telemetryErrorNote(null, true))
        assertNull(telemetryErrorNote("", false))

        // Помилка залишається видимою в Diagnostics у технічному вигляді.
        val diagnostics = src("DiagnosticsScreen.kt")
        assertTrue(diagnostics.contains("restError"))
        assertTrue(diagnostics.contains("Остання помилка REST"))
        assertTrue(diagnostics.contains("telemetryProvenance("))
    }

    @Test
    fun `battery has no separate network client or endpoint`() {
        val battery = src("BatteryScreen.kt")
        assertTrue(battery.contains("vm.loadFullTelemetry()"))
        assertTrue(battery.contains("vm.loadSeriesFor("))
        assertFalse(battery.contains("OkHttp"))
        assertFalse(battery.contains("Retrofit"))
        assertFalse(battery.contains("WebSocket"))
        assertTrue(battery.contains("telemetryErrorNote(error"))
    }

    @Test
    fun `provenance is user friendly outside diagnostics`() {
        assertEquals("Підтверджено вимірюванням", telemetryLabel(MeasurementValueDto(confidence = "physically_verified")))
        assertEquals("Сумісне з моделлю", telemetryLabel(MeasurementValueDto(confidence = "model_compatible")))
        assertEquals("Перевірено", telemetryLabel(MeasurementValueDto(confidence = "verified")))
        assertEquals("Підтверджено", telemetryLabel(MeasurementValueDto(confidence = "corroborated")))
        assertEquals("Розраховано", telemetryLabel(MeasurementValueDto(confidence = "calculated")))
        assertEquals("Попередньо", telemetryLabel(MeasurementValueDto(confidence = "provisional")))
        assertEquals("Не перевірено", telemetryLabel(MeasurementValueDto(confidence = "unverified")))
        assertEquals("Немає даних", telemetryLabel(null))

        assertEquals("physically_verified", telemetryProvenance(MeasurementValueDto(confidence = "physically_verified")))
        assertEquals("model_compatible", telemetryProvenance(MeasurementValueDto(confidence = "model_compatible")))

        val sources = redesign.listFiles { f -> f.extension == "kt" }!!.map { it.readText() }.joinToString("\n")
        assertFalse(sources.contains(" physically_verified\" to"))
        assertFalse(sources.contains("model_compatible\" to"))
    }

    @Test
    fun `freshness labels are Ukrainian everywhere`() {
        assertEquals("Дані актуальні", freshnessLabel(Freshness.FRESH))
        assertEquals("Дані застаріли", freshnessLabel(Freshness.STALE))
        assertEquals("Попередні дані", freshnessLabel(Freshness.PROVISIONAL))
        assertEquals("Немає даних", freshnessLabel(Freshness.UNAVAILABLE))
        assertEquals("Немає з'єднання", freshnessLabel(Freshness.OFFLINE))
        val ds = src("DesignSystem.kt")
        assertTrue(ds.contains("freshnessLabel(Freshness.FRESH)"))
        assertFalse(ds.contains("\"Fresh\" to"))
    }

    @Test
    fun `timestamps are local and human readable`() {
        assertEquals("24 Nov, 05:37:48", formatTelemetryTimestamp("2026-11-24T05:37:48.541356+00:00", ZoneId.of("UTC"), Locale.US))
        assertEquals("—", formatTelemetryTimestamp(null))
        assertEquals("—", formatTelemetryTimestamp(" "))
        assertEquals("2026-11-24 05:37:48", formatTelemetryTimestamp("2026-11-24T05:37:48+bad"))
        assertFalse(src("BatteryScreen.kt").contains("Text(full?.timestamp ?: \"—\""))
        assertTrue(src("DesignSystem.kt").contains("formatTelemetryTimestamp(timestamp)"))
    }

    @Test
    fun `incomplete balance is explicit and never filled with zeros`() {
        assertTrue(isBalanceComplete(listOf(148f, 52f, 800f, 525f)))
        assertFalse(isBalanceComplete(listOf(148f, null, null, 525f)))
        val home = src("HomeScreen.kt")
        assertTrue(home.contains("isBalanceComplete("))
        assertTrue(home.contains("balance_partial"))
        assertFalse(home.contains("available_parameters"))
        assertFalse(home.contains("28/28"))
        assertTrue(src("DiagnosticsScreen.kt").contains("quality_known"))
    }

    @Test
    fun `flow directions stay unconfirmed when data is missing`() {
        assertEquals(FlowDirection.UNKNOWN, pvFlowDirection(null, true))
        assertEquals(FlowDirection.UNKNOWN, gridFlowDirection(null, true))
        assertEquals(FlowDirection.UNKNOWN, loadFlowDirection(null, true))
        assertEquals(FlowDirection.UNKNOWN, batteryFlowDirection())
        val home = src("HomeScreen.kt")
        assertTrue(home.contains("batteryFlowDirection()"))
        assertTrue(home.contains("gridFlowDirection("))
    }

    @Test
    fun `version is bumped to stage 53`() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 77"))
        assertTrue(gradle.contains("versionName = \"2.53.1\""))
    }
}
