package com.sunlink.svitgrid.data.dto

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты соответствия имен параметров History экрана реальным контрактам backend.
 * Имена register_metadata.py этапа 7/10 — единственный источник истины для
 * endpoint /api/measurements/history.
 */
class HistoryParameterContractTest {

    /** Имена параметров, которые поддерживает backend (domain/register_metadata.py). */
    private val backendParameters = setOf(
        "battery_voltage",
        "battery_soc",
        "pv1_power",
        "pv2_power",
        "pv3_power",
        "battery_current",
        "battery_power",
        "dc_temperature",
        "ac_temperature"
    )

    /**
     * Параметры History экрана должны существовать в backend.
     * Ранее использовались несуществующие batt_soc/p_batt/p_grid — история была пустой.
     */
    @Test
    fun `history screen parameters exist in backend contract`() {
        val historyParameters = listOf(
            "battery_soc",
            "pv1_power",
            "pv2_power",
            "pv3_power",
            "battery_power",
            "battery_voltage",
            "battery_current",
            "dc_temperature"
        )
        historyParameters.forEach { param ->
            assertTrue(
                "History parameter '$param' not found in backend register contract",
                param in backendParameters
            )
        }
    }

    @Test
    fun `obsolete parameter names are not used`() {
        val obsolete = setOf("batt_soc", "p_batt", "p_grid")
        obsolete.forEach { param ->
            assertFalse(
                "Obsolete parameter '$param' must not be used",
                param in backendParameters
            )
        }
    }
}
