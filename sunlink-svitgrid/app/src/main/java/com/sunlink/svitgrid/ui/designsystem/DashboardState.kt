package com.sunlink.svitgrid.ui.designsystem

import androidx.annotation.StringRes
import com.sunlink.svitgrid.R
import com.sunlink.svitgrid.data.model.InverterData
import com.sunlink.svitgrid.data.model.InverterUiState
import com.sunlink.svitgrid.network.TelemetryWebSocketClient

/** Явные состояния экрана. ONLINE встановлюється тільки після реального обміну. */
enum class DashboardState(@StringRes val labelRes: Int) {
    LOADING(R.string.state_loading),
    CONNECTING(R.string.state_connecting),
    ONLINE(R.string.state_online),
    OFFLINE(R.string.state_offline),
    STALE(R.string.state_stale),
    ERROR(R.string.state_error),
    UNKNOWN(R.string.state_unknown);

    val isActiveFlowAllowed: Boolean
        get() = this == ONLINE
}

/**
 * Чистое преобразование состояния домена в состояние UI.
 * Вынесено из Composable, чтобы тестировать без Android UI.
 */
object DashboardStateMapper {
    fun map(
        uiState: InverterUiState,
        refreshing: Boolean,
        transport: TelemetryWebSocketClient.WsTransportState
    ): DashboardState = when {
        uiState is InverterUiState.Success && uiState.stale -> DashboardState.STALE
        uiState is InverterUiState.Success && uiState.status.online -> DashboardState.ONLINE
        uiState is InverterUiState.Error && uiState.data != null -> DashboardState.STALE
        uiState is InverterUiState.Error -> DashboardState.ERROR
        transport == TelemetryWebSocketClient.WsTransportState.CONNECTING ||
            transport == TelemetryWebSocketClient.WsTransportState.RECONNECTING -> DashboardState.CONNECTING
        refreshing && uiState !is InverterUiState.Success -> DashboardState.LOADING
        uiState is InverterUiState.Loading -> DashboardState.LOADING
        else -> DashboardState.OFFLINE
    }

    fun inverterState(
        data: InverterData,
        state: DashboardState
    ): InverterPresentation = when {
        !data.hasMeasurements && state == DashboardState.ERROR -> InverterPresentation.NO_DATA
        state == DashboardState.ONLINE -> InverterPresentation.ONLINE
        data.timestamp == null -> InverterPresentation.WAITING
        else -> InverterPresentation.OFFLINE
    }
}

enum class InverterPresentation(@StringRes val labelRes: Int) {
    ONLINE(R.string.dash_inverter_online),
    OFFLINE(R.string.dash_inverter_offline),
    WAITING(R.string.dash_inverter_waiting),
    NO_DATA(R.string.dash_inverter_no_data)
}

/**
 * Поток энергии. Направление рисуется только если backend подтвердил семантику знака.
 * При неизвестном значении/power==null поток нейтральный и не анимируется.
 */
data class EnergyFlowConnection(
    val key: String,
    val power: Float?,
    val directionConfirmed: Boolean,
    val color: androidx.compose.ui.graphics.Color,
    val reverse: Boolean = false
) {
    val isActive: Boolean
        get() = power != null && directionConfirmed && kotlin.math.abs(power) > 0.1f
}
