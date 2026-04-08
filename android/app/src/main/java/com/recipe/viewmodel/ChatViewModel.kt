package com.recipe.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.local.ChatHistoryManager
import com.recipe.data.local.ChatSession
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 聊天消息
 */
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val suggestions: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val api = RetrofitClient.api
    private val historyManager = ChatHistoryManager(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 历史会话列表
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions

    // 当前会话ID（null表示新对话）
    private var currentSessionId: String? = null

    // 对话上下文（保留最近10轮）
    private val contextHistory = mutableListOf<String>()

    init {
        loadSessions()
    }

    fun clearError() { _error.value = null }

    /**
     * 加载历史会话列表
     */
    fun loadSessions() {
        _sessions.value = historyManager.getSessions()
    }

    /**
     * 开始新对话
     */
    fun startNewChat() {
        // 先保存当前对话
        saveCurrentSession()
        currentSessionId = null
        _messages.value = emptyList()
        contextHistory.clear()
    }

    /**
     * 加载一个历史会话
     */
    fun loadSession(sessionId: String) {
        // 先保存当前对话
        saveCurrentSession()

        val session = historyManager.getSession(sessionId) ?: return
        currentSessionId = session.id
        _messages.value = session.messages
        contextHistory.clear()
        contextHistory.addAll(session.contextHistory)
    }

    /**
     * 删除一个历史会话
     */
    fun deleteSession(sessionId: String) {
        historyManager.deleteSession(sessionId)
        loadSessions()
        // 如果删的是当前会话，清空
        if (sessionId == currentSessionId) {
            currentSessionId = null
            _messages.value = emptyList()
            contextHistory.clear()
        }
    }

    /**
     * 发送消息
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // 添加用户消息
        val userMsg = ChatMessage(content = message, isUser = true)
        _messages.value = _messages.value + userMsg

        // 如果是新对话，创建sessionId
        if (currentSessionId == null) {
            currentSessionId = System.currentTimeMillis().toString()
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val request = mutableMapOf<String, Any?>(
                    "message" to message
                )
                if (contextHistory.isNotEmpty()) {
                    request["context"] = contextHistory.toList()
                }

                val response = api.aiChat(request)
                if (response.success) {
                    val data = response.data
                    val reply = parseReply(data)
                    val suggestions = parseSuggestions(data)

                    val aiMsg = ChatMessage(
                        content = reply,
                        isUser = false,
                        suggestions = suggestions
                    )
                    _messages.value = _messages.value + aiMsg

                    // 更新上下文
                    contextHistory.add("用户: $message")
                    contextHistory.add("助手: $reply")
                    while (contextHistory.size > 20) {
                        contextHistory.removeAt(0)
                    }

                    // 自动保存
                    saveCurrentSession()
                } else {
                    _error.value = response.message ?: "对话失败"
                }
            } catch (e: Exception) {
                _error.value = "发送失败: ${e.message}"
                Log.e("ChatVM", "发送失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清空当前对话
     */
    fun clearChat() {
        currentSessionId?.let { historyManager.deleteSession(it) }
        currentSessionId = null
        _messages.value = emptyList()
        contextHistory.clear()
        loadSessions()
    }

    /**
     * 保存当前会话到本地
     */
    private fun saveCurrentSession() {
        val msgs = _messages.value
        val sessionId = currentSessionId
        if (msgs.isEmpty() || sessionId == null) return

        val firstUserMsg = msgs.firstOrNull { it.isUser }?.content ?: "新对话"
        val title = if (firstUserMsg.length > 20) firstUserMsg.take(20) + "..." else firstUserMsg
        val lastMsg = msgs.lastOrNull()?.content ?: ""
        val lastSummary = if (lastMsg.length > 30) lastMsg.take(30) + "..." else lastMsg

        val session = ChatSession(
            id = sessionId,
            title = title,
            lastMessage = lastSummary,
            timestamp = System.currentTimeMillis(),
            messages = msgs,
            contextHistory = contextHistory.toList()
        )
        historyManager.saveSession(session)
        loadSessions()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseReply(data: Any?): String {
        return when (data) {
            is Map<*, *> -> (data["reply"] as? String) ?: "抱歉，我没有理解你的问题"
            else -> "抱歉，我没有理解你的问题"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSuggestions(data: Any?): List<String> {
        return try {
            when (data) {
                is Map<*, *> -> (data["suggestions"] as? List<String>) ?: emptyList()
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
