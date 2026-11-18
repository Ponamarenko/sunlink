package com.sunlink.svitgrid.data.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Тести доменної моделі InverterData.
 */
class InverterDataTest {

    @Test
    fun `hasMeasurements true when any field is non-null`() {
        val data = InverterData(pSun = 1000f)
        assertTrue(data.hasMeasurements)
    }

    @Test
    fun `hasMeasurements false when all fields are null`() {
        val data = InverterData()
        assertFalse(data.hasMeasurements)
    }

    @Test
    fun `batteryDirection charge when pBatt positive`() {
        assertEquals("Заряд", InverterData(pBatt = 10f).batteryDirection)
    }

    @Test
    fun `batteryDirection discharge when pBatt negative`() {
        assertEquals("Розряд", InverterData(pBatt = -10f).batteryDirection)
    }

    @Test
    fun `batteryDirection waiting when pBatt near zero`() {
        assertEquals("Очікування", InverterData(pBatt = 0f).batteryDirection)
        assertEquals("—", InverterData(pBatt = null).batteryDirection)
    }

    @Test
    fun `gridDirection export when pGrid positive`() {
        assertEquals("Експорт", InverterData(pGrid = 100f).gridDirection)
    }

    @Test
    fun `gridDirection import when pGrid negative`() {
        assertEquals("Імпорт", InverterData(pGrid = -100f).gridDirection)
    }

    @Test
    fun `gridDirection balance when pGrid near zero`() {
        assertEquals("Баланс", InverterData(pGrid = 0f).gridDirection)
        assertEquals("—", InverterData(pGrid = null).gridDirection)
    }

    @Test
    fun `toMap contains all fields`() {
        val data = InverterData(
            pSun = 1000f,
            batterySoc = 85f,
            pGrid = 200f,
            timestamp = "2026-11-18T10:00:00Z",
            connectionStatus = true,
            source = "modbus_register",
            quality = "valid",
            confidence = "corroborated"
        )
        val map = data.toMap()
        assertEquals(1000f, map["p_sun"])
        assertEquals(85f, map["batt_soc"])
        assertEquals(200f, map["p_grid"])
        assertEquals(true, map["connection_status"])
        assertEquals("modbus_register", map["source"])
    }
}
