package com.recipe.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.recipe.viewmodel.ChatMessage

/**
 * 对话会话
 */
data class ChatSession(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,           // 取第一条用户消息作为标题
    val lastMessage: String,     // 最后一条消息摘要
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage>,
    val contextHistory: List<String> = emptyList()
)

/**
 * 聊天历史管理器 - 基于SharedPreferences持久化
 * 最多保留10条对话历史
 */
class ChatHistoryManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxSessions = 10

    /**
     * 获取所有会话（按时间倒序）
     */
    fun getSessions(): List<ChatSession> {
        val json = prefs.getString("sessions", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChatSession>>() {}.type
            val sessions: List<ChatSession> = gson.fromJson(json, type)
            sessions.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存/更新一个会话
     */
    fun saveSession(session: ChatSession) {
        val sessions = getSessions().toMutableList()
        val existingIndex = sessions.indexOfFirst { it.id == session.id }
        if (existingIndex >= 0) {
            sessions[existingIndex] = session
        } else {
            sessions.add(0, session)
        }
        // 只保留最近N条
        val trimmed = sessions.sortedByDescending { it.timestamp }.take(maxSessions)
        val json = gson.toJson(trimmed)
        prefs.edit().putString("sessions", json).apply()
    }

    /**
     * 删除一个会话
     */
    fun deleteSession(sessionId: String) {
        val sessions = getSessions().filter { it.id != sessionId }
        val json = gson.toJson(sessions)
        prefs.edit().putString("sessions", json).apply()
    }

    /**
     * 加载指定会话
     */
    fun getSession(sessionId: String): ChatSession? {
        return getSessions().find { it.id == sessionId }
    }
}
