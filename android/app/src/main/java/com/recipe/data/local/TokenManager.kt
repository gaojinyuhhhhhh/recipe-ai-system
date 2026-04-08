package com.recipe.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Token本地存储管理
 * 使用SharedPreferences存储JWT Token和用户基本信息
 */
object TokenManager {

    private const val PREF_NAME = "recipe_ai_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"

    private lateinit var prefs: SharedPreferences

    /**
     * 初始化，在Application或MainActivity中调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存登录信息
     */
    fun saveLogin(token: String, userId: Long, username: String, nickname: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NICKNAME, nickname ?: username)
            .apply()
    }

    /**
     * 获取Token
     */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /**
     * 获取用户ID
     */
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)

    /**
     * 获取用户名
     */
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    /**
     * 获取昵称
     */
    fun getNickname(): String? = prefs.getString(KEY_NICKNAME, null)

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean = getToken() != null

    /**
     * 退出登录，清除所有信息
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
