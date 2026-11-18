package com.sunlink.svitgrid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto
import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class MainViewModel(private val repository: DataRepository) : ViewModel() {

    private val tag = "SunLinkVM"

    private val _uiState = MutableStateFlow<InverterUiState>(InverterUiState.Loading)
    val uiState: StateFlow<InverterUiState> = _uiState

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _historyState = MutableStateFlow<HistoryUiState>(HistoryUiState.Empty)
    val historyState: StateFlow<HistoryUiState> = _historyState

    /** Інтервал авто-опросу в секундах (за замовчуванням 5). */
    var pollIntervalSeconds: Int = 5
        private set

    fun setPollInterval(seconds: Int) {
        pollIntervalSeconds = seconds.coerceIn(1, 60)
    }

    /** Завантажує дані один раз. Використовується при старті екрана та ручному оновленні. */
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = InverterUiState.Loading
            try {
                val result = repository.loadInverterData()
                if (result.isSuccess) {
                    val (data, status) = result.getOrThrow()
                    if (data.hasMeasurements || status.online) {
                        _uiState.value = InverterUiState.Success(data, status)
                    } else {
                        _uiState.value = InverterUiState.Error(
                            message = "Дані інвертора недоступні",
                            data = data
                        )
                    }
                } else {
                    val e = result.exceptionOrNull()
                    Log.w(tag, "loadData failed", e)
                    _uiState.value = InverterUiState.Error(
                        message = "Помилка завантаження: ${e?.message}",
                        data = null
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "loadData crashed", e)
                _uiState.value = InverterUiState.Error(
                    message = "Помилка: ${e.javaClass.simpleName}: ${e.message}",
                    data = null
                )
            }
        }
    }

    /** Ручне оновлення (кнопка на екрані). */
    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val result = repository.loadInverterData()
                if (result.isSuccess) {
                    val (data, status) = result.getOrThrow()
                    _uiState.value = InverterUiState.Success(data, status)
                } else {
                    val e = result.exceptionOrNull()
                    _uiState.value = InverterUiState.Error(
                        message = "Помилка оновлення: ${e?.message}"
                    )
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Перевірка підключення до сервера (ендпоінт /status). */
    fun checkConnection() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val result = repository.loadStatus()
                if (result.isSuccess) {
                    val status = result.getOrThrow()
                    _uiState.value = if (status.online) {
                        val current = _uiState.value
                        val data = if (current is InverterUiState.Success) current.data else InverterData()
                        InverterUiState.Success(data, status)
                    } else {
                        InverterUiState.Error(message = "Сервер недоступний", data = null)
                    }
                } else {
                    val e = result.exceptionOrNull()
                    _uiState.value = InverterUiState.Error(
                        message = "Помилка підключення: ${e?.message}"
                    )
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Завантаження історії вимірювань для параметра. */
    fun loadHistory(parameter: String, start: String, end: String) {
        viewModelScope.launch {
            _historyState.value = HistoryUiState.Loading
            try {
                val result = repository.loadHistory(parameter, start, end)
                if (result.isSuccess) {
                    val dto = result.getOrThrow()
                    if (dto.measurements.isNotEmpty()) {
                        val points = dto.measurements.mapNotNull { m ->
                            val v = m.normalized_value?.toFloat()
                            HistoryPoint(timestamp = m.timestamp ?: "", value = v, unit = m.unit ?: "")
                        }
                        _historyState.value = HistoryUiState.Success(
                            parameter = dto.parameter ?: parameter,
                            unit = dto.measurements.firstOrNull()?.unit ?: "",
                            points = points
                        )
                    } else {
                        _historyState.value = HistoryUiState.Empty
                    }
                } else {
                    val e = result.exceptionOrNull()
                    _historyState.value = HistoryUiState.Error(
                        message = e?.message ?: "Невідома помилка"
                    )
                }
            } catch (e: Exception) {
                _historyState.value = HistoryUiState.Error(message = e.message ?: "Невідома помилка")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext[Job]?.cancel()
    }
}

/** Стан історії вимірювань. */
sealed class HistoryUiState {
    object Empty : HistoryUiState()
    object Loading : HistoryUiState()
    data class Success(val parameter: String, val unit: String, val points: List<HistoryPoint>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

/** Factory для створення MainViewModel з DataRepository. */
class MainViewModelFactory(private val repository: DataRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
