package com.recipe.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.local.ChatHistoryManager
import com.recipe.data.local.ChatSession
import com.recipe.data.model.RecipeStep
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 聊天消息
 */
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val suggestions: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI对话助手ViewModel
 *
 * 职责范围：
 * 1. 与AI助手对话 — 发送消息、接收回复、展示建议问题
 * 2. 会话管理 — 新建/加载/删除/清空历史会话
 * 3. 上下文维护 — 保留最近10轮对话作为AI上下文，实现连续对话
 * 4. 烹饪模式上下文感知 — 在烹饪时自动将当前步骤注入AI上下文
 * 5. TTS语音播报AI回复 — 烹饪模式下自动朗读回复
 *
 * 存储机制：
 * - 会话历史通过 [ChatHistoryManager] 存储在本地 SharedPreferences
 * - 每次AI回复后自动保存当前会话
 * - 会话标题取第一条用户消息的前20个字符
 */
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

    // 当前会话ID（null表示新对话，使用时间戳作为ID）
    private var currentSessionId: String? = null

    // 对话上下文（保留最近10轮 = 20条消息，供 AI 实现连续对话理解）
    private val contextHistory = mutableListOf<String>()

    // ==================== 烹饪模式上下文 ====================

    /** 是否处于烹饪模式聊天（决定是否注入烹饪上下文+自动语音播报回复） */
    private val _isCookingMode = MutableStateFlow(false)
    val isCookingMode: StateFlow<Boolean> = _isCookingMode

    /** 烹饪模式的食谱标题 */
    private var cookingRecipeTitle: String = ""
    /** 烹饪模式的所有步骤 */
    private var cookingSteps: List<RecipeStep> = emptyList()
    /** 烹饪模式的当前步骤索引（实时从CookingViewModel同步） */
    private var cookingCurrentStepIndex: Int = 0
    /** 烹饪模式的当前剩余时间 */
    private var cookingRemainingTime: Int? = null

    // ==================== TTS（AI回复语音播报） ====================

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled

    // TTS正在播报中
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        loadSessions()
    }

    fun clearError() { _error.value = null }

    // ==================== 烹饪模式管理 ====================

    /**
     * 进入烹饪模式聊天
     * 在CookingModeScreen中打开语音助手时调用
     */
    fun enterCookingMode(recipeTitle: String, steps: List<RecipeStep>, currentStepIndex: Int) {
        _isCookingMode.value = true
        cookingRecipeTitle = recipeTitle
        cookingSteps = steps
        cookingCurrentStepIndex = currentStepIndex
        cookingRemainingTime = null
        // 烹饪模式使用独立会话
        startNewChat()
        // 初始化TTS
        initTts()
    }

    /**
     * 退出烹饪模式聊天
     */
    fun exitCookingMode() {
        _isCookingMode.value = false
        cookingRecipeTitle = ""
        cookingSteps = emptyList()
        stopSpeaking()
    }

    /**
     * 更新烹饪模式实时状态（由CookingViewModel同步调用）
     */
    fun updateCookingState(currentStepIndex: Int, remainingTime: Int?) {
        cookingCurrentStepIndex = currentStepIndex
        cookingRemainingTime = remainingTime
    }

    // ==================== TTS 语音播报 ====================

    /**
     * 初始化TTS引擎（用于播报AI回复）
     */
    fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(getApplication<Application>()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locales = listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, Locale.getDefault())
                for (locale in locales) {
                    val result = tts?.setLanguage(locale)
                    if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        break
                    }
                }
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
                ttsReady = true
                Log.i("ChatVM", "TTS初始化成功")
            } else {
                Log.w("ChatVM", "TTS初始化失败: $status")
                ttsReady = false
            }
        }
    }

    /**
     * 播报AI回复文本
     */
    private fun speakReply(text: String) {
        if (!_isTtsEnabled.value || !ttsReady || !_isCookingMode.value) return
        val ttsInstance = tts ?: return
        // 截取合适长度播报（避免过长）
        val toSpeak = if (text.length > 200) text.take(200) + "..." else text
        val utteranceId = "chat_reply_${System.currentTimeMillis()}"
        ttsInstance.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * 停止当前播报
     */
    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    /**
     * 切换TTS开关
     */
    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
        if (!_isTtsEnabled.value) stopSpeaking()
    }

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
     * 发送消息给AI助手
     * 流程：添加用户消息到列表 → 带上上下文调用后端 → 解析回复+建议问题 → 更新上下文 → 自动保存
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

        // 播报前停止当前播报
        stopSpeaking()

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

                // 烹饪模式下注入实时上下文
                if (_isCookingMode.value && cookingSteps.isNotEmpty()) {
                    val currentStep = cookingSteps.getOrNull(cookingCurrentStepIndex)
                    request["cookingContext"] = mapOf(
                        "recipeTitle" to cookingRecipeTitle,
                        "currentStep" to (cookingCurrentStepIndex + 1),
                        "totalSteps" to cookingSteps.size,
                        "currentStepContent" to (currentStep?.content ?: ""),
                        "remainingTime" to cookingRemainingTime,
                        "allSteps" to cookingSteps.map { it.content }
                    )
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

                    // 烹饪模式下自动语音播报AI回复
                    if (_isCookingMode.value) {
                        speakReply(reply)
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

    /** 解析AI回复内容，容错处理为默认提示 */
    @Suppress("UNCHECKED_CAST")
    private fun parseReply(data: Any?): String {
        return when (data) {
            is Map<*, *> -> (data["reply"] as? String) ?: "抱歉，我没有理解你的问题"
            else -> "抱歉，我没有理解你的问题"
        }
    }

    /** 解析AI返回的建议跟进问题列表 */
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

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
