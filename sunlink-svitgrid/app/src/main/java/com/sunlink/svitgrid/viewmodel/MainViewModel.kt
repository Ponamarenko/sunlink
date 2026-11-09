package com.sunlink.svitgrid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sunlink.svitgrid.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val tag = "SunLinkAPI"
    private val gson = Gson()

    private val _data = MutableStateFlow<Map<String, Any>?>(null)
    val data: StateFlow<Map<String, Any>?> = _data

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private fun elementToMap(el: JsonElement?): Map<String, Any>? {
        if (el == null || el.isJsonNull) return null
        return try {
            val obj = when {
                el.isJsonObject -> el.asJsonObject
                el.isJsonArray -> {
                    val arr = el.asJsonArray
                    if (arr.size() == 0) return null
                    arr.last().asJsonObject // останній = найсвіжіший запис
                }
                else -> return null
            }
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson<Map<String, Any>>(obj, type)
        } catch (e: Exception) {
            Log.w(tag, "Parse error: ${e.message}")
            null
        }
    }

    private suspend fun tryLiveRelative(
        pathFetcher: suspend () -> retrofit2.Response<JsonElement>,
        name: String
    ): Pair<Map<String, Any>?, String?> {
        val base = RetrofitClient.getBaseUrl()
        val url = "$base$name"
        return try {
            Log.d(tag, "Trying: $url")
            val resp = pathFetcher()
            val preview = try { resp.body()?.toString()?.take(250) } catch (_: Exception) { null }
            Log.d(tag, "Response ${resp.code()} $url: $preview")
            if (resp.isSuccessful) {
                val map = elementToMap(resp.body())
                if (map != null && map.isNotEmpty()) map to null else null to "Порожня відповідь: $url"
            } else null to "HTTP ${resp.code()}: $url"
        } catch (e: Exception) {
            Log.w(tag, "Error $url: ${e.javaClass.simpleName}: ${e.message}")
            null to "${e.javaClass.simpleName}: $url (${e.message})"
        }
    }

    fun loadLive() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val base = RetrofitClient.getBaseUrl()
            try {
                Log.d(tag, "loadLive start, baseUrl=$base")

                // Тільки URL з налаштувань (без жорстких локальних fallback)
                val attempts = mutableListOf<String?>()
                for ((name, fetcher) in listOf(
                    "api/live/raw" to suspend { RetrofitClient.getApiService().getLiveRaw() },
                    "live" to suspend { RetrofitClient.getApiService().getLive() },
                )) {
                    val (map, err) = tryLiveRelative(fetcher, name)
                    attempts += err
                    if (map != null) {
                        Log.d(tag, "OK $base$name keys=${map.keys.take(10)}")
                        _data.value = map
                        _error.value = null
                        return@launch
                    }
                }

                // Fallback: inverter/system API на тому ж сервері
                try {
                    val invResp = RetrofitClient.getApiService().getInverters()
                    val body = invResp.body()
                    val ids = (body?.get("inverters") as? List<*>)?.map { it.toString() } ?: emptyList()
                    val firstId = ids.firstOrNull()
                    if (firstId != null) {
                        val resp = RetrofitClient.getApiService().getInverterData(firstId)
                        if (resp.isSuccessful && resp.body() != null) {
                            _data.value = resp.body()
                            _error.value = null
                            return@launch
                        }
                        attempts += "HTTP ${resp.code()}: ${base}api/v1/inverters/$firstId/data"
                    } else {
                        val stats = RetrofitClient.getApiService().getSystemStats()
                        if (stats.isSuccessful && stats.body() != null) {
                            _data.value = stats.body()
                            _error.value = null
                            return@launch
                        }
                        attempts += "Немає інверторів/system stats: $base"
                    }
                } catch (e: Exception) {
                    attempts += "inverter API: ${e.javaClass.simpleName} ($base)"
                }

                _data.value = null
                val detail = attempts.filterNotNull().distinct().joinToString("; ").ifBlank { "невідомо" }
                _error.value = "API недоступний ($base): $detail"
                Log.w(tag, _error.value ?: "")
            } catch (e: Exception) {
                Log.e(tag, "loadLive failed", e)
                _data.value = null
                _error.value = "Помилка підключення: $base (${e.javaClass.simpleName}: ${e.message})"
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val base = RetrofitClient.getBaseUrl()
            try {
                Log.d(tag, "checkConnection baseUrl=$base")
                val msgs = mutableListOf<String>()
                // Спроба live endpoint-ів (ті самі, що використовує додаток)
                for ((name, fetcher) in listOf(
                    "api/live/raw" to suspend { RetrofitClient.getApiService().getLiveRaw() },
                    "live" to suspend { RetrofitClient.getApiService().getLive() },
                )) {
                    try {
                        val resp = fetcher()
                        val preview = resp.body()?.toString()?.take(180) ?: ""
                        Log.d(tag, "check $name ${resp.code()}: $preview")
                        if (resp.isSuccessful) {
                            _error.value = "OK ${resp.code()} $base$name ${preview.take(80)}"
                            return@launch
                        } else msgs += "${resp.code()} $base$name"
                    } catch (e: Exception) {
                        msgs += "${e.javaClass.simpleName} $base$name"
                    }
                }
                // Додатково: health
                try {
                    val resp = RetrofitClient.getApiService().getHealth()
                    if (resp.isSuccessful) {
                        _error.value = "OK ${resp.code()} ${base}api/v1/health"
                        return@launch
                    } else msgs += "${resp.code()} ${base}api/v1/health"
                } catch (e: Exception) {
                    msgs += "${e.javaClass.simpleName} ${base}api/v1/health"
                }
                _error.value = "Недоступно ($base): ${msgs.joinToString("; ")}"
            } catch (e: Exception) {
                Log.e(tag, "checkConnection failed", e)
                _error.value = "Помилка: $base (${e.javaClass.simpleName}: ${e.message})"
            } finally {
                _loading.value = false
            }
        }
    }
}
