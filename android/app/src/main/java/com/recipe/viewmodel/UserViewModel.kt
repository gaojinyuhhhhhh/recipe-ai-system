package com.recipe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.recipe.data.model.UserPreferences
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 用户信息和偏好设置 ViewModel
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val api = RetrofitClient.api
    private val gson = Gson()

    // 用户信息
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 提示消息
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    /**
     * 加载用户信息
     */
    fun loadUserInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getUserInfo()
                if (response.success && response.data != null) {
                    val remote = response.data
                    android.util.Log.d("UserViewModel", "Loaded preferences: ${remote.preferences}")
                    _userInfo.value = UserInfo(
                        id = remote.id,
                        username = remote.username,
                        nickname = remote.nickname,
                        phone = remote.phone,
                        avatar = remote.avatar,
                        preferences = remote.preferences,
                        familySize = remote.familySize,
                        cookingFrequency = remote.cookingFrequency
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "Load user info error", e)
                _toastMessage.value = "加载用户信息失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 保存偏好设置
     */
    fun savePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 将偏好对象转为JSON字符串
                val prefsJson = gson.toJson(preferences)
                android.util.Log.d("UserViewModel", "Saving preferences: $prefsJson")
                // 发送给后端：{"preferences": "json字符串"}
                val response = api.setPreferences(mapOf("preferences" to prefsJson))
                if (response.success) {
                    _toastMessage.value = "偏好设置已保存"
                    // 刷新用户信息
                    loadUserInfo()
                } else {
                    _toastMessage.value = response.message ?: "保存失败"
                }
            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "Save preferences error", e)
                _toastMessage.value = "保存失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

/**
 * 用户信息数据类
 */
data class UserInfo(
    val id: Long,
    val username: String,
    val nickname: String?,
    val phone: String?,
    val avatar: String?,
    val preferences: String?,  // JSON格式
    val familySize: Int,
    val cookingFrequency: Int
)
