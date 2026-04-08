package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.recipe.data.local.TokenManager
import com.recipe.data.model.ApiResponse
import com.recipe.data.remote.LoginRequest
import com.recipe.data.remote.RegisterRequest
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AuthViewModel : ViewModel() {
    private val api = RetrofitClient.api

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState

    private val _isLoggedIn = MutableStateFlow(TokenManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    /**
     * 登录
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = AuthState.Error("请输入用户名和密码")
            return
        }
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            try {
                val response = api.login(LoginRequest(username, password))
                Log.d("AuthVM", "Login response: success=${response.success}, data=${response.data}")
                if (response.success && response.data != null) {
                    val data = response.data
                    val token = data["token"]?.toString() ?: ""
                    val id = (data["id"] as? Number)?.toLong() ?: 0L
                    val uname = data["username"]?.toString() ?: username
                    val nick = data["nickname"]?.toString()
                    Log.d("AuthVM", "Saving token: ${token.take(30)}... id=$id")
                    TokenManager.saveLogin(token, id, uname, nick)
                    Log.d("AuthVM", "Saved token verify: ${TokenManager.getToken()?.take(30)}...")
                    _isLoggedIn.value = true
                    _loginState.value = AuthState.Success("登录成功")
                } else {
                    _loginState.value = AuthState.Error(response.message ?: "登录失败")
                }
            } catch (e: HttpException) {
                val msg = parseErrorMessage(e) ?: "登录失败"
                _loginState.value = AuthState.Error(msg)
            } catch (e: Exception) {
                _loginState.value = AuthState.Error("网络错误: ${e.message}")
            }
        }
    }

    /**
     * 注册
     */
    fun register(username: String, password: String, confirmPassword: String, phone: String?) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = AuthState.Error("请输入用户名和密码")
            return
        }
        if (password != confirmPassword) {
            _loginState.value = AuthState.Error("两次密码不一致")
            return
        }
        if (password.length < 6) {
            _loginState.value = AuthState.Error("密码至少6位")
            return
        }
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            try {
                val response = api.register(
                    RegisterRequest(username, password, phone?.takeIf { it.isNotBlank() })
                )
                Log.d("AuthVM", "Register response: success=${response.success}, data=${response.data}")
                if (response.success && response.data != null) {
                    val data = response.data
                    val token = data["token"]?.toString() ?: ""
                    val id = (data["id"] as? Number)?.toLong() ?: 0L
                    val uname = data["username"]?.toString() ?: username
                    val nick = data["nickname"]?.toString()
                    Log.d("AuthVM", "Saving token: ${token.take(30)}... id=$id")
                    TokenManager.saveLogin(token, id, uname, nick)
                    Log.d("AuthVM", "Saved token verify: ${TokenManager.getToken()?.take(30)}...")
                    _isLoggedIn.value = true
                    _loginState.value = AuthState.Success("注册成功")
                } else {
                    _loginState.value = AuthState.Error(response.message ?: "注册失败")
                }
            } catch (e: HttpException) {
                val msg = parseErrorMessage(e) ?: "注册失败"
                _loginState.value = AuthState.Error(msg)
            } catch (e: Exception) {
                _loginState.value = AuthState.Error("网络错误: ${e.message}")
            }
        }
    }

    /**
     * 从 HttpException 中提取后端返回的错误消息
     */
    private fun parseErrorMessage(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string()
            if (body != null) {
                val errorResponse = Gson().fromJson(body, ApiResponse::class.java)
                errorResponse.message
            } else null
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        TokenManager.logout()
        _isLoggedIn.value = false
        _loginState.value = AuthState.Idle
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _loginState.value = AuthState.Idle
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
