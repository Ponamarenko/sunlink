package app.sunportal.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.sunportal.data.model.UpdateInfo
import app.sunportal.utils.PermissionUtils
import app.sunportal.utils.UpdateManager
import app.sunportal.api.RetrofitClient
import app.sunportal.api.TestUpdateService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для управления обновлениями
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    application: Application,
    private val updateManager: UpdateManager
) : AndroidViewModel(application) {
    
    // Состояния приложения
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()
    
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()
    
    /**
     * Проверка разрешений
     */
    fun checkPermissions() {
        val context = getApplication<Application>().applicationContext
        _permissionsGranted.value = PermissionUtils.hasAllUpdatePermissions(context)
    }
    
    /**
     * Проверка обновлений
     */
    fun checkForUpdates() {
        if (!_permissionsGranted.value) {
            _error.value = "Необходимо разрешение на скачивание файлов"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Сначала пытаемся получить обновления с тестового сервера
                try {
                    val testUpdateService = RetrofitClient.createRetrofit("https://raw.githubusercontent.com/Ponamarenko/SunPortal/main/").create(TestUpdateService::class.java)
                    val testUpdate = testUpdateService.getTestUpdateInfo()
                    _updateInfo.value = testUpdate
                    _downloadStatus.value = "update_available"
                    return@launch
                } catch (e: Exception) {
                    // Если тестовый сервер не доступен, используем основной
                    android.util.Log.w("UpdateViewModel", "Test update service failed, using main service", e)
                }
                
                val result = updateManager.checkForUpdates()
                result.onSuccess { updateInfo ->
                    _updateInfo.value = updateInfo
                    _downloadStatus.value = "update_available"
                }.onFailure { error ->
                    _error.value = "Ошибка при проверке обновлений: ${error.message}"
                    _downloadStatus.value = "no_update"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка сети: ${e.message}"
                _downloadStatus.value = "check_failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Получение последнего обновления
     */
    fun getLatestUpdate() {
        if (!_permissionsGranted.value) {
            _error.value = "Необходимо разрешение на скачивание файлов"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val result = updateManager.getLatestUpdate()
                result.onSuccess { updateInfo ->
                    _updateInfo.value = updateInfo
                    _downloadStatus.value = "update_available"
                }.onFailure { error ->
                    _error.value = "Ошибка при получении обновлений: ${error.message}"
                    _downloadStatus.value = "no_update"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка сети: ${e.message}"
                _downloadStatus.value = "check_failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Скачивание обновления
     */
    fun downloadUpdate() {
        if (!_permissionsGranted.value) {
            _error.value = "Необходимо разрешение на скачивание файлов"
            return
        }
        
        val currentUpdate = _updateInfo.value
        if (currentUpdate != null) {
            viewModelScope.launch {
                try {
                    val downloadId = updateManager.downloadUpdate(currentUpdate)
                    if (downloadId != -1L) {
                        _downloadStatus.value = "downloading"
                    } else {
                        _error.value = "Ошибка при запуске скачивания"
                        _downloadStatus.value = "download_failed"
                    }
                } catch (e: Exception) {
                    _error.value = "Ошибка при скачивании: ${e.message}"
                    _downloadStatus.value = "download_failed"
                }
            }
        }
    }
    
    /**
     * Сброс состояния
     */
    fun resetState() {
        _updateInfo.value = null
        _isLoading.value = false
        _error.value = null
        _downloadStatus.value = null
    }
    
    /**
     * Проверка статуса скачивания
     */
    fun checkDownloadStatus() {
        viewModelScope.launch {
            try {
                val currentUpdate = _updateInfo.value
                if (currentUpdate != null) {
                    // Здесь можно добавить логику проверки статуса скачивания
                    // через DownloadManager
                }
            } catch (e: Exception) {
                _error.value = "Ошибка при проверке статуса: ${e.message}"
            }
        }
    }
    
    /**
     * Проверка, есть ли доступные обновления
     */
    fun hasUpdateAvailable(): Boolean {
        return _updateInfo.value?.hasUpdate(33) == true
    }
    
    /**
     * Проверка, является ли обновление обязательным
     */
    fun isForceUpdate(): Boolean {
        return _updateInfo.value?.forceUpdate == true
    }
}