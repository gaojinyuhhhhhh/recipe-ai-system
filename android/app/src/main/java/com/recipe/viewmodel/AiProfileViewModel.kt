package com.recipe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.model.PreferenceAnalysis
import com.recipe.data.model.SuggestedPreferencesResponse
import com.recipe.data.model.UserAiProfile
import com.recipe.data.model.UserPreferences
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI画像ViewModel
 */
class AiProfileViewModel : ViewModel() {

    private val apiService = RetrofitClient.api

    // AI画像数据
    private val _aiProfile = MutableStateFlow<UserAiProfile?>(null)
    val aiProfile: StateFlow<UserAiProfile?> = _aiProfile

    // 偏好分析
    private val _preferenceAnalysis = MutableStateFlow<PreferenceAnalysis?>(null)
    val preferenceAnalysis: StateFlow<PreferenceAnalysis?> = _preferenceAnalysis

    // 推荐偏好
    private val _suggestedPreferences = MutableStateFlow<SuggestedPreferencesResponse?>(null)
    val suggestedPreferences: StateFlow<SuggestedPreferencesResponse?> = _suggestedPreferences

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 操作成功提示
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    /**
     * 加载AI画像
     */
    fun loadAiProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.getAiProfile()
                if (response.success) {
                    _aiProfile.value = response.data
                } else {
                    _error.value = response.message ?: "获取AI画像失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载偏好分析
     */
    fun loadPreferenceAnalysis() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.getPreferenceAnalysis()
                if (response.success) {
                    _preferenceAnalysis.value = response.data
                } else {
                    _error.value = response.message ?: "获取偏好分析失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载AI推荐的偏好设置
     */
    fun loadSuggestedPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.getSuggestedPreferences()
                if (response.success) {
                    _suggestedPreferences.value = response.data
                } else {
                    _error.value = response.message ?: "获取推荐偏好失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 应用AI推荐的偏好设置
     */
    fun applySuggestedPreferences(onSuccess: (UserPreferences) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.applySuggestedPreferences()
                if (response.success) {
                    _successMessage.value = "偏好设置已应用"
                    _suggestedPreferences.value?.preferences?.let { onSuccess(it) }
                } else {
                    _error.value = response.message ?: "应用失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 清除成功提示
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * 加载所有数据
     */
    fun loadAll() {
        loadAiProfile()
        loadPreferenceAnalysis()
        loadSuggestedPreferences()
    }
}
