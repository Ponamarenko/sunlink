package com.sunlink.svitgrid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunlink.svitgrid.data.dto.InvertersDto
import com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto
import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.data.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.Instant

class MainViewModel(private val repository: DataRepository) : ViewModel() {

    private val tag = "SunLinkVM"

    /** Данные старше этого возраста считаются застаревшими (stale). */
    companion object {
        const val STALE_THRESHOLD_MS = 60_000L
    }

    private val _uiState = MutableStateFlow<InverterUiState>(InverterUiState.Loading)
    val uiState: StateFlow<InverterUiState> = _uiState

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _historyState = MutableStateFlow<HistoryUiState>(HistoryUiState.Empty)
    val historyState: StateFlow<HistoryUiState> = _historyState

    private val _inverters = MutableStateFlow<InvertersDto?>(null)
    val inverters: StateFlow<InvertersDto?> = _inverters

    /** Последние успешные данные — сохраняются при потере связи (offline/stale). */
    private var lastGood: Pair<InverterData, InverterStatus>? = null

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
                if (result is Result.Success) {
                    val (data, status) = result.data
                    if (data.hasMeasurements || status.online) {
                        applySuccess(data, status)
                    } else {
                        // Успешный ответ, но без измерений — это ошибка, а не нули.
                        applyError("Дані інвертора недоступні")
                    }
                } else {
                    val e = (result as? Result.Error)?.exception
                    Log.w(tag, "loadData failed", e)
                    applyError("Помилка завантаження: ${e?.message}")
                }
            } catch (e: Exception) {
                Log.e(tag, "loadData crashed", e)
                applyError("Помилка: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    /** Ручне оновлення (кнопка на екрані). Та сама логіка, що й loadData. */
    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val result = repository.loadInverterData()
                if (result is Result.Success) {
                    val (data, status) = result.data
                    if (data.hasMeasurements || status.online) {
                        applySuccess(data, status)
                    } else {
                        applyError("Дані інвертора недоступні")
                    }
                } else {
                    val e = (result as? Result.Error)?.exception
                    applyError("Помилка оновлення: ${e?.message}")
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
                if (result is Result.Success) {
                    val status = result.data
                    if (status.online) {
                        val data = lastGood?.first ?: InverterData()
                        applySuccess(data, status)
                    } else {
                        applyError("Сервер недоступний")
                    }
                } else {
                    val e = (result as? Result.Error)?.exception
                    applyError("Помилка підключення: ${e?.message}")
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Отримання списку інверторів (ендпоінт /api/inverters, етап 12). */
    fun loadInverters() {
        viewModelScope.launch {
            try {
                val result = repository.loadInverters()
                if (result is Result.Success) {
                    _inverters.value = result.data
                } else {
                    Log.w(tag, "loadInverters failed")
                }
            } catch (e: Exception) {
                Log.w(tag, "loadInverters crashed", e)
            }
        }
    }

    /** Завантаження історії вимірювань для параметра. */
    fun loadHistory(parameter: String, start: String, end: String) {
        viewModelScope.launch {
            _historyState.value = HistoryUiState.Loading
            try {
                val result = repository.loadHistory(parameter, start, end)
                if (result is Result.Success) {
                    val dto = result.data
                    if (dto.measurements.isNotEmpty()) {
                        val points = dto.measurements.mapNotNull { m ->
                            val v = m.normalized_value?.toFloat()
                            HistoryPoint(timestamp = m.timestamp ?: "", value = v, unit = m.unit ?: "")
                        }
                        _historyState.value = HistoryUiState.Success(
                            parameter = dto.parameter ?: parameter,
                            unit = dto.measurements.firstOrNull()?.unit ?: "",
                            points = points,
                            staleCount = points.count { it.value == null }
                        )
                    } else {
                        _historyState.value = HistoryUiState.Empty
                    }
                } else {
                    val e = (result as? Result.Error)?.exception
                    _historyState.value = HistoryUiState.Error(
                        message = e?.message ?: "Невідома помилка"
                    )
                }
            } catch (e: Exception) {
                _historyState.value = HistoryUiState.Error(message = e.message ?: "Невідома помилка")
            }
        }
    }

    private fun applySuccess(data: InverterData, status: InverterStatus) {
        lastGood = Pair(data, status)
        _uiState.value = InverterUiState.Success(
            data = data,
            status = status,
            stale = isStale(data.timestamp)
        )
    }

    /**
     * Помилка/offline: зберігаємо останні добрі дані, щоб UI показав їх
     * як застарілі, а не підміняв нулями.
     */
    private fun applyError(message: String) {
        val previous = lastGood
        _uiState.value = InverterUiState.Error(
            message = message,
            data = previous?.first,
            status = previous?.second,
            stale = previous != null
        )
    }

    private fun isStale(timestamp: String?): Boolean {
        if (timestamp.isNullOrBlank()) return false
        return try {
            val normalized = if (timestamp.endsWith("Z")) timestamp else timestamp.replace("+00:00", "Z")
            val instant = Instant.parse(normalized)
            System.currentTimeMillis() - instant.toEpochMilli() > STALE_THRESHOLD_MS
        } catch (e: Exception) {
            // Неสามารถ розпарсити — не стверджуємо, що дані застарілі.
            false
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
    data class Success(
        val parameter: String,
        val unit: String,
        val points: List<HistoryPoint>,
        val staleCount: Int = 0
    ) : HistoryUiState()
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
