package com.sunlink.svitgrid.data.model

/**
 * Состояние UI ViewModel. Замкнутый тип — только одно состояние одновременно.
 */
sealed class InverterUiState {
    object Loading : InverterUiState()
    data class Success(
        val data: InverterData,
        val status: InverterStatus,
        /** Дані отримані, але timestamp старший за STALE_THRESHOLD_MS. */
        val stale: Boolean = false
    ) : InverterUiState()
    data class Error(
        val message: String,
        /** Останні успішні дані — не підміняємо їх нулями при offline. */
        val data: InverterData? = null,
        val status: InverterStatus? = null,
        val stale: Boolean = false
    ) : InverterUiState()
}

/**
 * Состояние подробного просмотра инвертора.
 */
sealed class InverterDetailsUiState {
    object Loading : InverterDetailsUiState()
    data class Success(
        val inverterId: String,
        val model: String,
        val status: InverterStatus,
        val pv: List<PvChannel>,
        val battery: BatteryDetail,
        val grid: GridDetail,
        val load: LoadDetail,
        val temperatures: List<TemperatureDetail>,
        val allMeasurements: Map<String, Any>
    ) : InverterDetailsUiState()
    data class Error(val message: String) : InverterDetailsUiState()
}

data class PvChannel(
    val name: String,
    val power: Float?,
    val voltage: Float?,
    val current: Float?,
    val unitPower: String = "Вт",
    val unitVoltage: String = "В",
    val unitCurrent: String = "А"
)

data class BatteryDetail(
    val soc: Float?,
    val voltage: Float?,
    val current: Float?,
    val power: Float?,
    val temperature: Float?,
    val direction: String
)

data class GridDetail(
    val power: Float?,
    val voltage: Float?,
    val direction: String
)

data class LoadDetail(
    val power: Float?,
    val unit: String = "Вт"
)

data class TemperatureDetail(
    val name: String,
    val value: Float?,
    val unit: String = "°C"
)
