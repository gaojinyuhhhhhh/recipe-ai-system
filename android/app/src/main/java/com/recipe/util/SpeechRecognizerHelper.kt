package com.recipe.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 语音识别辅助类
 *
 * 封装 Android SpeechRecognizer API，提供简洁的 StateFlow 状态驱动接口。
 * 主要用于烹饪模式语音聊天 — 用户说话后自动识别为文本发送给AI。
 *
 * 可用性检测策略：
 * 初始化时一次性检测设备能力，决定使用哪种模式：
 * 1. SpeechRecognizer API（最佳体验，无弹窗，实时部分识别）
 * 2. Intent 方式（系统弹窗，兼容性好）
 * 3. 键盘语音输入（兜底方案，引导用户使用输入法自带的语音按钮）
 *
 * 注意事项：
 * - 需要 RECORD_AUDIO 权限
 * - 国内设备无GMS时，SpeechRecognizer API 通常不可用
 * - TTS 播报期间应暂停监听（避免回声）
 */
class SpeechRecognizerHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechHelper"
        // Android 11+ 新增的错误码
        private const val ERROR_SERVER_DISCONNECTED = 11
        private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        private const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }

    /** 语音输入的可用模式 */
    enum class VoiceInputMode {
        SPEECH_RECOGNIZER,  // SpeechRecognizer API（最佳，无弹窗）
        INTENT,             // 系统语音输入弹窗
        KEYBOARD_ONLY       // 仅键盘输入（无语音能力）
    }

    private var speechRecognizer: SpeechRecognizer? = null

    // ==================== 公开状态 ====================

    /** 当前设备支持的语音输入模式 */
    private val _voiceInputMode = MutableStateFlow(VoiceInputMode.KEYBOARD_ONLY)
    val voiceInputMode: StateFlow<VoiceInputMode> = _voiceInputMode

    /** 是否正在监听中 */
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    /** 识别到的文本（每次识别完成后更新） */
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    /** 实时部分识别结果（说话过程中） */
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText

    /** 错误信息 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** STT完全不可用（SpeechRecognizer和Intent都不行） */
    val isSttUnavailable: Boolean
        get() = _voiceInputMode.value == VoiceInputMode.KEYBOARD_ONLY

    /** 是否应该使用Intent方式 */
    val shouldUseIntent: Boolean
        get() = _voiceInputMode.value == VoiceInputMode.INTENT

    // ==================== 初始化检测 ====================

    init {
        detectVoiceInputMode()
    }

    /**
     * 一次性检测设备语音输入能力
     * 避免用户点击后才逐级失败等待
     */
    private fun detectVoiceInputMode() {
        // 1. 检查 SpeechRecognizer API
        val hasRecognitionService = SpeechRecognizer.isRecognitionAvailable(context)
        val hasOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && try {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } catch (_: Exception) { false }

        if (hasRecognitionService || hasOnDevice) {
            _voiceInputMode.value = VoiceInputMode.SPEECH_RECOGNIZER
            Log.i(TAG, "检测结果: SPEECH_RECOGNIZER (service=$hasRecognitionService, onDevice=$hasOnDevice)")
            return
        }

        // 2. 检查 Intent 方式
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        if (activities.isNotEmpty()) {
            _voiceInputMode.value = VoiceInputMode.INTENT
            Log.i(TAG, "检测结果: INTENT (${activities.size}个App支持)")
            return
        }

        // 3. 都不可用
        _voiceInputMode.value = VoiceInputMode.KEYBOARD_ONLY
        Log.i(TAG, "检测结果: KEYBOARD_ONLY (设备无语音识别能力)")
    }

    // ==================== SpeechRecognizer 模式 ====================

    /**
     * 开始语音识别（SpeechRecognizer API模式）
     */
    fun startListening() {
        if (_voiceInputMode.value != VoiceInputMode.SPEECH_RECOGNIZER) {
            Log.w(TAG, "当前模式不支持SpeechRecognizer: ${_voiceInputMode.value}")
            return
        }

        destroyRecognizer()

        speechRecognizer = try {
            val hasOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && try {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            } catch (_: Exception) { false }

            if (hasOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.i(TAG, "使用设备端识别器")
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                Log.i(TAG, "使用标准识别器")
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建SpeechRecognizer失败，降级到Intent模式", e)
            _voiceInputMode.value = VoiceInputMode.INTENT
            _error.value = "语音识别服务异常，已切换到系统语音输入"
            return
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        _error.value = null
        _partialText.value = ""
        _recognizedText.value = ""
        _isListening.value = true

        try {
            speechRecognizer?.startListening(intent)
            Log.i(TAG, "语音识别已启动")
        } catch (e: Exception) {
            Log.e(TAG, "启动语音识别失败，降级到Intent模式", e)
            _isListening.value = false
            // SpeechRecognizer实际不可用，降级
            _voiceInputMode.value = VoiceInputMode.INTENT
            _error.value = "语音识别服务异常，已切换到系统语音输入"
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "停止语音识别异常", e)
        }
        _isListening.value = false
    }

    /**
     * 取消语音识别
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "取消语音识别异常", e)
        }
        _isListening.value = false
        _partialText.value = ""
    }

    // ==================== Intent 模式 ====================

    /**
     * 创建用于 ActivityResultLauncher 的语音识别 Intent
     */
    fun createRecognizeSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    /**
     * 处理 Intent 方式返回的识别结果
     */
    fun handleIntentResult(data: Intent?) {
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = results?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _recognizedText.value = text
            Log.i(TAG, "Intent识别结果: $text")
        } else {
            _error.value = "未识别到内容，请再试一次"
        }
    }

    // ==================== 公共方法 ====================

    /**
     * 清除识别结果（发送后调用）
     */
    fun clearResult() {
        _recognizedText.value = ""
        _partialText.value = ""
    }

    /**
     * 释放资源（离开页面时调用）
     */
    fun destroy() {
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "销毁SpeechRecognizer异常", e)
        }
        speechRecognizer = null
    }

    // ==================== 识别回调 ====================

    /** SpeechRecognizer连续错误计数 */
    private var consecutiveErrors = 0

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "准备就绪，请说话...")
            consecutiveErrors = 0  // 能走到这里说明服务可用
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "检测到说话")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "说话结束")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            Log.w(TAG, "语音识别错误: code=$error")

            // 严重错误 — SpeechRecognizer根本不可用，降级到Intent
            val isFatalError = error == SpeechRecognizer.ERROR_CLIENT
                    || error == SpeechRecognizer.ERROR_NETWORK
                    || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                    || error == ERROR_SERVER_DISCONNECTED
                    || error == SpeechRecognizer.ERROR_SERVER
                    || error == ERROR_LANGUAGE_NOT_SUPPORTED
                    || error == ERROR_LANGUAGE_UNAVAILABLE

            if (isFatalError) {
                consecutiveErrors++
                if (consecutiveErrors >= 2) {
                    // 连续2次致命错误，SpeechRecognizer确实不可用
                    Log.w(TAG, "SpeechRecognizer连续${consecutiveErrors}次致命错误，降级")
                    val intentAvailable = try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
                    } catch (_: Exception) { false }

                    if (intentAvailable) {
                        _voiceInputMode.value = VoiceInputMode.INTENT
                        _error.value = "已切换到系统语音输入模式，请再次点击麦克风"
                    } else {
                        _voiceInputMode.value = VoiceInputMode.KEYBOARD_ONLY
                        _error.value = "语音识别不可用，请使用键盘输入（可点击键盘上的语音按钮）"
                    }
                    return
                }
                // 第一次致命错误，提示用户重试
                _error.value = "语音识别连接失败，请再试一次"
                return
            }

            // 非致命错误，正常提示
            consecutiveErrors = 0
            val errMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到内容，请再试一次"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到说话，请再试一次"
                SpeechRecognizer.ERROR_AUDIO -> "录音错误，请检查麦克风权限"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限，请在设置中授予"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙，请稍后重试"
                else -> "识别错误(code=$error)"
            }
            _error.value = errMsg
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            consecutiveErrors = 0
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _recognizedText.value = text
                _partialText.value = ""
                Log.i(TAG, "识别结果: $text")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _partialText.value = text
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
