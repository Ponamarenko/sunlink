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

    private fun asFloat(v: Any?): Float? = when (v) {
        is Number -> v.toFloat()
        is String -> v.toFloatOrNull()
        is Map<*, *> -> asFloat(v["value"])
        else -> null
    }

    private fun mergeInverterData(maps: List<Map<String, Any>>): Map<String, Any> {
        if (maps.isEmpty()) return emptyMap()
        if (maps.size == 1) return maps[0].toMutableMap().apply { mapCommonFields(this) }
        val out = mutableMapOf<String, Any>()
        // зберігаємо всі ключі першого для сумісності
        out.putAll(maps[0])
        var sumDc = 0f; var hasDc = false
        var sumAc = 0f; var hasAc = false
        var sumToday = 0f; var hasToday = false
        var socSum = 0f; var socN = 0
        var battV: Float? = null
        for (m in maps) {
            asFloat(m["power_dc"])?.let { sumDc += it; hasDc = true }
            asFloat(m["power_ac"])?.let { sumAc += it; hasAc = true }
            asFloat(m["energy_today"])?.let { sumToday += it; hasToday = true }
            asFloat(m["battery_soc"] ?: m["batt_soc"])?.let { if (it > 0f || socN > 0) { socSum += it; socN++ } else { socSum += it; socN++ } }
            if (battV == null) battV = asFloat(m["battery_voltage"])
        }
        if (hasDc) out["p_sun"] = sumDc; out["power_dc_total"] = sumDc
        if (hasAc) out["p_load"] = sumAc; out["power_ac_total"] = sumAc // наближення, якщо немає p_load
        if (hasToday) out["energy_today"] = sumToday
        if (socN > 0) out["batt_soc"] = socSum / socN
        battV?.let { out["battery_voltage"] = it }
        out["inverters_merged"] = maps.size
        // якщо сервер дає точні p_* — не перезаписуємо їх нулями
        mapCommonFields(out)
        return out
    }

    private fun mapCommonFields(m: MutableMap<String, Any>) {
        // Мапінг полів inverter API -> поля Dashboard, тільки якщо ціль відсутня
        fun setIfMissing(key: String, vararg sources: String) {
            if (m[key] != null) return
            for (s in sources) { val v = asFloat(m[s]); if (v != null) { m[key] = v; return } }
        }
        setIfMissing("p_sun", "power_dc", "pv_power")
        setIfMissing("p_load", "load_power", "home_power") // power_ac — не завжди = навантаження
        setIfMissing("batt_soc", "battery_soc", "soc")
        if (m["p_batt"] == null) m["p_batt"] = 0f
        if (m["p_grid"] == null) m["p_grid"] = 0f
    }

    private suspend fun tryLiveByUrl(url: String): Pair<Map<String, Any>?, String?> {
        return try {
            Log.d(tag, "Trying: $url")
            val resp = RetrofitClient.getApiService().getLiveByUrl(url)
            val preview = try { resp.body()?.toString()?.take(300) } catch (_: Exception) { null }
            Log.d(tag, "Response ${resp.code()} $url: $preview")
            if (resp.isSuccessful) {
                val map = elementToMap(resp.body())
                if (map != null && map.isNotEmpty()) map to null else null to "Порожня відповідь: $url"
            } else null to "HTTP ${resp.code()}: $url"
        } catch (e: Exception) {
            Log.w(tag, "Error $url: ${e.javaClass.simpleName}: ${e.message}")
            null to "${e.javaClass.simpleName}: $url"
        }
    }

    private suspend fun tryLiveRelative(
        pathFetcher: suspend () -> retrofit2.Response<JsonElement>,
        name: String
    ): Pair<Map<String, Any>?, String?> {
        val base = RetrofitClient.getBaseUrl()
        return try {
            Log.d(tag, "Trying: $base$name")
            val resp = pathFetcher()
            val preview = try { resp.body()?.toString()?.take(300) } catch (_: Exception) { null }
            Log.d(tag, "Response ${resp.code()} $base$name: $preview")
            if (resp.isSuccessful) {
                val map = elementToMap(resp.body())
                if (map != null && map.isNotEmpty()) map to null else null to "Порожня відповідь: $base$name"
            } else null to "HTTP ${resp.code()}: $base$name"
        } catch (e: Exception) {
            Log.w(tag, "Error $base$name: ${e.javaClass.simpleName}: ${e.message}")
            null to "${e.javaClass.simpleName}: $base$name"
        }
    }

    private suspend fun tryInverterApi(base: String): Pair<Map<String, Any>?, String?> {
        return try {
            Log.d(tag, "Trying inverter API on $base")
            val invResp = RetrofitClient.getApiService().getInverters()
            val body = invResp.body()
            Log.d(tag, "inverters resp ${invResp.code()}: $body")
            val ids = (body?.get("inverters") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            if (ids.isEmpty()) return null to "Немає інверторів: ${base}api/v1/inverters"
            val maps = mutableListOf<Map<String, Any>>()
            for (id in ids) {
                try {
                    val resp = RetrofitClient.getApiService().getInverterData(id)
                    Log.d(tag, "inverter $id ${resp.code()}: ${resp.body()?.keys?.take(12)}")
                    if (resp.isSuccessful) resp.body()?.let { maps += it }
                } catch (e: Exception) {
                    Log.w(tag, "inverter $id error: ${e.message}")
                }
            }
            if (maps.isNotEmpty()) {
                val merged = mergeInverterData(maps)
                Log.d(tag, "Merged ${maps.size} inverters, keys=${merged.keys.take(14)}")
                merged to null
            } else {
                // fallback: system stats
                val stats = RetrofitClient.getApiService().getSystemStats()
                if (stats.isSuccessful && stats.body() != null) {
                    val s = stats.body()!!.toMutableMap()
                    // stats.total_power -> p_sun (наближення)
                    if (s["p_sun"] == null) asFloat(s["total_power"])?.let { s["p_sun"] = it }
                    s to null
                } else null to "Немає даних інверторів: $base"
            }
        } catch (e: Exception) {
            Log.w(tag, "inverter API error: ${e.javaClass.simpleName}: ${e.message}")
            null to "inverter API: ${e.javaClass.simpleName} ($base)"
        }
    }

    private suspend fun tryAllOnBase(base: String, attempts: MutableList<String?>): Map<String, Any>? {
        // переконуємось, що Retrofit налаштований на цей base
        if (RetrofitClient.getBaseUrl() != base) RetrofitClient.setBaseUrl(base)
        for ((name, fetcher) in listOf(
            "api/live/raw" to suspend { RetrofitClient.getApiService().getLiveRaw() },
            "live" to suspend { RetrofitClient.getApiService().getLive() },
        )) {
            val (map, err) = tryLiveRelative(fetcher, name)
            attempts += err
            if (map != null) return map
        }
        val (map, err) = tryInverterApi(base)
        attempts += err
        return map
    }

    fun loadLive() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val configuredBase = RetrofitClient.getBaseUrl()
            try {
                Log.d(tag, "loadLive start, configuredBase=$configuredBase")
                val attempts = mutableListOf<String?>()

                // 1) Налаштований сервер (з Налаштувань)
                tryAllOnBase(configuredBase, attempts)?.let {
                    _data.value = it; _error.value = null; Log.d(tag, "OK configured $configuredBase"); return@launch
                }

                // 2) Fallback: LAN IP сервера (AI-сервер 192.168.25.20:8000), якщо відрізняється
                val lanBase = "http://192.168.25.20:8000/"
                if (configuredBase != lanBase) {
                    Log.d(tag, "Trying LAN fallback $lanBase")
                    tryAllOnBase(lanBase, attempts)?.let {
                        _data.value = it; _error.value = "Дані з LAN ($lanBase). Перевірте URL у Налаштуваннях."
                        Log.d(tag, "OK LAN fallback"); return@launch
                    }
                    // повертаємо Retrofit на налаштований base
                    RetrofitClient.setBaseUrl(configuredBase)
                }

                _data.value = null
                val detail = attempts.filterNotNull().distinct().joinToString("; ").ifBlank { "невідомо" }
                _error.value = "API недоступний ($configuredBase): $detail"
                Log.w(tag, _error.value ?: "")
            } catch (e: Exception) {
                Log.e(tag, "loadLive failed", e)
                _data.value = null
                _error.value = "Помилка підключення: $configuredBase (${e.javaClass.simpleName}: ${e.message})"
                RetrofitClient.setBaseUrl(configuredBase)
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val configuredBase = RetrofitClient.getBaseUrl()
            try {
                Log.d(tag, "checkConnection configuredBase=$configuredBase")
                val msgs = mutableListOf<String>()
                for (base in listOf(configuredBase, "http://192.168.25.20:8000/").distinct()) {
                    if (RetrofitClient.getBaseUrl() != base) RetrofitClient.setBaseUrl(base)
                    for ((name, path) in listOf("health" to "api/v1/health", "inverters" to "api/v1/inverters", "live/raw" to "api/live/raw")) {
                        try {
                            val resp = RetrofitClient.getApiService().getLiveByUrl("$base$path")
                            val preview = resp.body()?.toString()?.take(160) ?: ""
                            Log.d(tag, "check $base$path ${resp.code()}: $preview")
                            if (resp.isSuccessful) {
                                RetrofitClient.setBaseUrl(configuredBase)
                                _error.value = "OK ${resp.code()} $base$path ${preview.take(70)}"
                                return@launch
                            } else msgs += "${resp.code()} $base$path"
                        } catch (e: Exception) {
                            msgs += "${e.javaClass.simpleName} $base$path"
                        }
                    }
                }
                RetrofitClient.setBaseUrl(configuredBase)
                _error.value = "Недоступно: ${msgs.joinToString("; ")}"
            } catch (e: Exception) {
                Log.e(tag, "checkConnection failed", e)
                RetrofitClient.setBaseUrl(configuredBase)
                _error.value = "Помилка: ${e.javaClass.simpleName}: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
