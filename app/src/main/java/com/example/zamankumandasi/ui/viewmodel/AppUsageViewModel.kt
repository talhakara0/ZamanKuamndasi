package com.example.zamankumandasi.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamankumandasi.data.model.AppInfo
import com.example.zamankumandasi.data.model.AppUsage
import com.example.zamankumandasi.data.repository.AppUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUsageViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    private val _installedApps = MutableLiveData<List<AppInfo>>()
    val installedApps: LiveData<List<AppInfo>> = _installedApps

    private val _appUsageList = MutableLiveData<List<AppUsage>>()
    val appUsageList: LiveData<List<AppUsage>> = _appUsageList

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadInstalledApps() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val apps = appUsageRepository.getInstalledApps()
                _installedApps.value = apps
            } catch (e: Exception) {
                _error.value = "Uygulamalar yüklenirken hata: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadAppUsageByUser(userId: String) {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val usageList = appUsageRepository.getAppUsageByUser(userId)
                _appUsageList.value = usageList
            } catch (e: Exception) {
                _error.value = "Kullanım verileri yüklenirken hata: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun setDailyLimit(userId: String, packageName: String, appName: String, limitInMinutes: Int) {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                appUsageRepository.setDailyLimit(userId, packageName, appName, limitInMinutes)
                // Listeyi yenile
                loadAppUsageByUser(userId)
            } catch (e: Exception) {
                _error.value = "Süre sınırı ayarlanırken hata: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun resetDailyUsage(userId: String) {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                appUsageRepository.resetDailyUsage(userId)
                // Listeyi yenile
                loadAppUsageByUser(userId)
            } catch (e: Exception) {
                _error.value = "Günlük kullanım sıfırlanırken hata: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateUsedTime(userId: String, packageName: String, usedTime: Long) {
        viewModelScope.launch {
            try {
                appUsageRepository.updateUsedTime(userId, packageName, usedTime)
            } catch (e: Exception) {
                _error.value = "Kullanım süresi güncellenirken hata: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
