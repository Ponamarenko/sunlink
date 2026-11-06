package com.sunlink.svitgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunlink.svitgrid.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _data = MutableStateFlow<Map<String, Any>?>(null)
    val data: StateFlow<Map<String, Any>?> = _data

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadLive() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
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
                val resp = RetrofitClient.getApiService().getHealth()
                if (resp.isSuccessful) {
                    _error.value = "Підключення OK (${resp.code()})"
                } else {
                    _error.value = "Помилка: ${resp.code()} ${resp.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.javaClass.simpleName}: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
