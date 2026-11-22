package com.sunlink.svitgrid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunlink.svitgrid.data.dto.HealthDto
import com.sunlink.svitgrid.data.dto.InvertersDto
import com.sunlink.svitgrid.data.dto.StatsSummaryDto
import com.sunlink.svitgrid.data.dto.TelemetryFullDto
import com.sunlink.svitgrid.data.dto.VersionDto
import com.sunlink.svitgrid.data.dto.LiveDataMessageDto
import com.sunlink.svitgrid.data.dto.MeasurementsHistoryDto
import com.sunlink.svitgrid.data.dto.WebSocketLiveParser
import com.sunlink.svitgrid.data.dto.WebSocketTelemetryMapper
import com.sunlink.svitgrid.data.model.*
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.network.RetrofitClient
import com.sunlink.svitgrid.network.TelemetryWebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.net.URI
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

    private val _connectionDiagnostic = MutableStateFlow<ConnectionDiagnostic?>(null)
    val connectionDiagnostic: StateFlow<ConnectionDiagnostic?> = _connectionDiagnostic

    /** Comprehensive stage: категориальная модель, статистика и диагностика backend. */
    private val _fullTelemetry = MutableStateFlow<TelemetryFullDto?>(null)
    val fullTelemetry: StateFlow<TelemetryFullDto?> = _fullTelemetry
    private val _fullTelemetryError = MutableStateFlow<String?>(null)
    val fullTelemetryError: StateFlow<String?> = _fullTelemetryError

    private val _stats = MutableStateFlow<StatsSummaryDto?>(null)
    val stats: StateFlow<StatsSummaryDto?> = _stats

    private val _backendHealth = MutableStateFlow<HealthDto?>(null)
    val backendHealth: StateFlow<HealthDto?> = _backendHealth

    private val _backendVersion = MutableStateFlow<VersionDto?>(null)
    val backendVersion: StateFlow<VersionDto?> = _backendVersion

    /** Stage 29: транспорт WebSocket (OPEN — только после реального handshake). */
    private val _liveTransport = MutableStateFlow(TelemetryWebSocketClient.WsTransportState.CLOSED)
    val liveTransport: StateFlow<TelemetryWebSocketClient.WsTransportState> = _liveTransport

    private var wsClient: TelemetryWebSocketClient? = null

    /** Последние успешные данные — сохраняются при потере связи (offline/stale). */
    private var lastGood: Pair<InverterData, InverterStatus>? = null

    /** Інтервал авто-опросу в секундах (за замовчуванням 5). */
    var pollIntervalSeconds: Int = 5
        private set

    fun setPollInterval(seconds: Int) {
        pollIntervalSeconds = seconds.coerceIn(1, 60)
    }

    // ------------------------------------------------------------------
    // Stage 29: WebSocket live-телеметрія (один клієнт, REST — reserve)
    // ------------------------------------------------------------------

    /** Запускає єдиний WebSocket-клієнт. Повторний виклик нічого не змінює. */
    fun startLiveUpdates() {
        if (wsClient != null) return
        wsClient = TelemetryWebSocketClient(
            scope = viewModelScope,
            urlProvider = { RetrofitClient.getWebSocketUrl() },
            onMessage = { onLiveDataMessage(it) },
            onState = { handleTransportState(it) }
        ).also { it.start() }
    }

    /** Зупиняє клієнт і знімає підписку на зміну адреси. */
    fun stopLiveUpdates() {
        wsClient?.stop()
        wsClient = null
        _liveTransport.value = TelemetryWebSocketClient.WsTransportState.CLOSED
    }

    /**
     * Обробка реального кадру `live_data`.
     * ONLINE встановлюється тільки після успішного обміну: статус сервера
     * connection_status=true і хоча б одне валідне вимірювання.
     */
    fun onLiveDataMessage(message: LiveDataMessageDto) {
        if (!WebSocketLiveParser.isLiveData(message)) return
        val data = WebSocketTelemetryMapper.toInverterData(message)
        val status = WebSocketTelemetryMapper.toInverterStatus(message)
        if (!status.online || !data.hasMeasurements) {
            // Реальний кадр, але backend не має підтверджених даних — не видаємо ONLINE.
            applyError("Backend не має підтверджених даних")
            return
        }
        if (data.timestamp.isNullOrBlank()) {
            // Немає часу — не можемо стверджувати свіжість даних.
            applyError("WebSocket кадр без часу оновлення")
            return
        }
        applySuccess(data, status)
    }

    private fun handleTransportState(state: TelemetryWebSocketClient.WsTransportState) {
        _liveTransport.value = state
        if (state != TelemetryWebSocketClient.WsTransportState.RECONNECTING) return
        // REST як резервний механізм: один запит, без створення другого poller.
        viewModelScope.launch {
            val result = try {
                repository.loadInverterData()
            } catch (e: Exception) {
                Log.w(tag, "REST fallback failed", e)
                null
            }
            if (result is Result.Success) {
                val (data, status) = result.data
                if (data.hasMeasurements || status.online) applySuccess(data, status)
                else applyError("Дані інвертора недоступні")
            } else {
                applyError("Втрату з'єднання з WebSocket підтверджено; REST недоступний")
            }
        }
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
            val started = System.currentTimeMillis()
            val url = RetrofitClient.getBaseUrl()
            try {
                val result = repository.loadStatus()
                val elapsed = System.currentTimeMillis() - started
                if (result is Result.Success) {
                    val status = result.data
                    _connectionDiagnostic.value = connectionDiagnostic(
                        url, "OK",
                        "З'єднання встановлено", elapsed, status.lastUpdate
                    )
                    if (status.online) {
                        val data = lastGood?.first ?: InverterData()
                        applySuccess(data, status)
                    } else {
                        applyError("Сервер недоступний")
                    }
                } else {
                    val e = (result as? Result.Error)?.exception
                    val kind = classifyConnectionError(e)
                    _connectionDiagnostic.value = connectionDiagnostic(
                        url, kind.first, kind.second, elapsed, null
                    )
                    applyError("Помилка підключення: ${kind.second}")
                }
            } catch (e: Exception) {
                val kind = classifyConnectionError(e)
                _connectionDiagnostic.value = connectionDiagnostic(
                    RetrofitClient.getBaseUrl(), kind.first, kind.second,
                    System.currentTimeMillis() - started, null
                )
                applyError("Помилка підключення: ${kind.second}")
            } finally {
                _refreshing.value = false
            }
        }
    }

    private fun connectionDiagnostic(
        url: String,
        state: String,
        message: String,
        elapsedMs: Long,
        lastSuccess: String?
    ): ConnectionDiagnostic = try {
        val uri = URI.create(url)
        ConnectionDiagnostic(
            url = url,
            host = uri.host ?: "—",
            port = if (uri.port > 0) uri.port else null,
            https = uri.scheme.equals("https", ignoreCase = true),
            state = state,
            message = message,
            responseTimeMs = elapsedMs,
            lastSuccessUtc = lastSuccess,
            checkedAtUtc = Instant.now().toString()
        )
    } catch (e: Exception) {
        ConnectionDiagnostic(
            url = url, host = "—", port = null, https = false,
            state = "INVALID_URL", message = "Некоректна адреса backend",
            responseTimeMs = elapsedMs, checkedAtUtc = Instant.now().toString()
        )
    }

    /** Класифікація помилки без виведення стек-трейсів і секретів. */
    private fun classifyConnectionError(e: Exception?): Pair<String, String> {
        val text = e?.javaClass?.name.orEmpty() + " " + e?.message.orEmpty()
        return when {
            text.contains("UnknownHost", true) || text.contains("resolve host", true) ->
                "DNS_FAILURE" to "Не вдалося визначити IP-адресу сервера (DNS)"
            text.contains("SSLHandshake", true) || text.contains("certificate", true) ||
                text.contains("CertPath", true) || text.contains("TrustAnchor", true) ->
                "TLS_ERROR" to "Помилка TLS: сертифікат не підтверджено або ім'я хоста не збігається"
            text.contains("timeout", true) || text.contains("Timeout", true) ->
                "TIMEOUT" to "Сервер не відповів у часі очікування"
            text.contains("ConnectException", true) || text.contains("Failed to connect", true) ->
                "CONNECTION_REFUSED" to "Підключення відхилено (порт закритий або недоступний)"
            text.contains("HTTP ", true) ->
                "HTTP_ERROR" to "Сервер відповів помилкою HTTP"
            else -> "OFFLINE" to "Backend недоступний"
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

    /** Прямая установка уже загруженной модели (используется тестами и WS-адаптерами). */
    fun onFullTelemetryLoaded(dto: TelemetryFullDto) {
        _fullTelemetry.value = dto
    }

    /** Загружает полную категориальную модель телеметрии (без физических запросов). */
    fun loadFullTelemetry() {
        viewModelScope.launch {
            val result = try {
                repository.loadFullTelemetry()
            } catch (e: Exception) {
                Log.w(tag, "loadFullTelemetry failed", e)
                null
            }
            if (result is Result.Success) {
                _fullTelemetry.value = result.data
                _fullTelemetryError.value = null
            } else {
                _fullTelemetryError.value = (result as? Result.Error)?.exception?.message
            }
        }
    }

    /** Статистика по параметру за период. */
    fun loadStats(parameter: String, start: String, end: String) {
        viewModelScope.launch {
            val result = try {
                repository.loadStats(parameter, start, end)
            } catch (e: Exception) {
                Log.w(tag, "loadStats failed", e)
                null
            }
            if (result is Result.Success) _stats.value = result.data
        }
    }

    /** Health/version backend — для экрана диагностики. */
    fun loadBackendInfo() {
        viewModelScope.launch {
            val health = try { repository.loadHealth() } catch (e: Exception) { null }
            if (health is Result.Success) _backendHealth.value = health.data
            val version = try { repository.loadBackendVersion() } catch (e: Exception) { null }
            if (version is Result.Success) _backendVersion.value = version.data
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
        stopLiveUpdates()
        super.onCleared()
        viewModelScope.coroutineContext[Job]?.cancel()
    }
}

/** Діагностика останньої перевірки з'єднання (Stage 25B). */
data class ConnectionDiagnostic(
    val url: String,
    val host: String,
    val port: Int?,
    val https: Boolean,
    val state: String,
    val message: String,
    val responseTimeMs: Long? = null,
    val lastSuccessUtc: String? = null,
    val checkedAtUtc: String = ""
)

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
