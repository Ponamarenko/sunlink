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

    private suspend fun tryLiveUrl(url: String): Map<String, Any>? {
        return try {
            Log.d(tag, "Trying URL: $url")
            val resp = RetrofitClient.getApiService().getLiveByUrl(url)
            val bodyStr = try { resp.body()?.toString()?.take(300) } catch (_: Exception) { null }
            Log.d(tag, "Response ${resp.code()} for $url: $bodyStr")
            if (resp.isSuccessful) elementToMap(resp.body()) else null
        } catch (e: Exception) {
            Log.w(tag, "Error for $url: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private suspend fun tryLiveRelative(pathFetcher: suspend () -> retrofit2.Response<JsonElement>, name: String): Map<String, Any>? {
        return try {
            Log.d(tag, "Trying relative: $name")
            val resp = pathFetcher()
            val bodyStr = try { resp.body()?.toString()?.take(300) } catch (_: Exception) { null }
            Log.d(tag, "Response ${resp.code()} for $name: $bodyStr")
            if (resp.isSuccessful) elementToMap(resp.body()) else null
        } catch (e: Exception) {
            Log.w(tag, "Error for $name: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    fun loadLive() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                Log.d(tag, "loadLive start")

                // 1) Абсолютні fallback URL (ha4a local + public)
                val absoluteUrls = listOf(
                    "http://192.168.25.244:8080/api/live/raw",
                    "http://ha4a.pp.ua:8080/api/live/raw",
                )
                for (url in absoluteUrls) {
                    val map = tryLiveUrl(url)
                    if (map != null && map.isNotEmpty()) {
                        Log.d(tag, "OK from $url, keys=${map.keys.take(8)}")
                        _data.value = map
                        _error.value = null
                        return@launch
                    }
                }

                // 2) Відносні endpoint-и на налаштованому сервері
                for ((name, fetcher) in listOf(
                    "api/live/raw" to suspend { RetrofitClient.getApiService().getLiveRaw() },
                    "live" to suspend { RetrofitClient.getApiService().getLive() },
                )) {
                    val map = tryLiveRelative(fetcher, name)
                    if (map != null && map.isNotEmpty()) {
                        Log.d(tag, "OK from $name, keys=${map.keys.take(8)}")
                        _data.value = map
                        _error.value = null
                        return@launch
                    }
                }

                // 3) Fallback: inverter API
                val invResp = RetrofitClient.getApiService().getInverters()
                val body = invResp.body()
                val ids = (body?.get("inverters") as? List<*>)?.map { it.toString() } ?: emptyList()
                val firstId = ids.firstOrNull()
                if (firstId != null) {
                    val resp = RetrofitClient.getApiService().getInverterData(firstId)
                    if (resp.isSuccessful) _data.value = resp.body()
                    else _error.value = "Помилка: ${resp.code()}"
                } else {
                    val stats = RetrofitClient.getApiService().getSystemStats()
                    if (stats.isSuccessful) _data.value = stats.body()
                    else _error.value = "Немає інверторів"
                }
            } catch (e: Exception) {
                Log.e(tag, "loadLive failed", e)
                _error.value = "Помилка підключення: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val url = "http://192.168.25.244:8080/api/live/raw"
                Log.d(tag, "checkConnection $url")
                val resp = RetrofitClient.getApiService().getLiveByUrl(url)
                val preview = resp.body()?.toString()?.take(220) ?: ""
                _error.value = if (resp.isSuccessful) "API OK ${resp.code()}: $preview" else "Помилка: ${resp.code()}"
                Log.d(tag, "checkConnection result: ${_error.value}")
            } catch (e: Exception) {
                Log.e(tag, "checkConnection failed", e)
                _error.value = "Помилка: ${e.javaClass.simpleName}: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
