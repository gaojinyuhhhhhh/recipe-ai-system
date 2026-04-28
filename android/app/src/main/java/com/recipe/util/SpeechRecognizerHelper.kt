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
 * 三层回退策略（优先国内设备可用性）：
 * 1. 设备端识别 (Android 12+) — 完全本地，不走任何云服务，国内设备最佳选择
 * 2. 离线识别 (EXTRA_PREFER_OFFLINE) — 使用已下载的离线语言包
 * 3. 在线识别 — 连接云服务（国内可能不可用）
 *
 * 注意事项：
 * - 需要 RECORD_AUDIO 权限
 * - 国内设备优先走设备端识别，不依赖Google服务器
 * - TTS 播报期间应暂停监听（避免回声）
 */
class SpeechRecognizerHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechHelper"
        // Android 11+ 新增的错误码
        private const val ERROR_TOO_MANY_REQUESTS = 10
        private const val ERROR_SERVER_DISCONNECTED = 11
        private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        private const val ERROR_LANGUAGE_UNAVAILABLE = 13
        private const val ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS = 15
    }

    /** 当前使用的识别模式 */
    private enum class SttMode {
        ON_DEVICE,   // 设备端识别（Android 12+，纯本地）
        OFFLINE,     // 离线识别（EXTRA_PREFER_OFFLINE）
        ONLINE       // 在线识别（连接云服务）
    }

    private var speechRecognizer: SpeechRecognizer? = null

    /** 当前识别模式 */
    private var currentMode = SttMode.ON_DEVICE

    /** 是否已尝试过设备端识别 */
    private var triedOnDevice = false
    /** 是否已尝试过离线模式 */
    private var triedOffline = false

    /** STT不可用（设备不支持或多次失败） */
    private val _isSttUnavailable = MutableStateFlow(false)
    val isSttUnavailable: StateFlow<Boolean> = _isSttUnavailable

    /** 连续错误计数 */
    private var errorCount = 0

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

    /** 设备是否支持语音识别 */
    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * 检查是否支持设备端语音识别（Android 12+，完全本地运行）
     */
    private fun isOnDeviceAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }

    /**
     * 开始语音识别
     *
     * 三层回退策略：
     * 1. 优先设备端识别（Android 12+，纯本地，不走任何网络）
     * 2. 设备端不可用/失败 → 离线优先模式
     * 3. 离线失败 → 在线模式
     */
    fun startListening() {
        if (!isAvailable) {
            _error.value = "此设备不支持语音识别，请使用文字输入"
            _isSttUnavailable.value = true
            Log.w(TAG, "SpeechRecognizer不可用")
            return
        }

        // 决定使用哪种模式
        val mode = when {
            isOnDeviceAvailable() && !triedOnDevice -> SttMode.ON_DEVICE
            !triedOffline -> SttMode.OFFLINE
            else -> SttMode.ONLINE
        }
        currentMode = mode

        Log.i(TAG, "启动语音识别, mode=$mode")

        // 创建识别器实例
        destroyRecognizer()
        speechRecognizer = when (mode) {
            SttMode.ON_DEVICE -> {
                // Android 12+ 设备端识别器，完全本地运行
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            }
            SttMode.OFFLINE, SttMode.ONLINE -> {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        }
        speechRecognizer?.setRecognitionListener(recognitionListener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 静音检测 — 用户停止说话2秒后自动结束
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            // 离线模式
            if (mode == SttMode.OFFLINE) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        _error.value = null
        _partialText.value = ""
        _recognizedText.value = ""
        _isListening.value = true

        try {
            speechRecognizer?.startListening(intent)
            Log.i(TAG, "语音识别已启动 (mode=$mode)")
        } catch (e: Exception) {
            Log.e(TAG, "启动语音识别失败", e)
            _isListening.value = false
            // 设备端识别启动失败，标记已尝试，下次走离线/在线
            if (mode == SttMode.ON_DEVICE) {
                triedOnDevice = true
                Log.i(TAG, "设备端识别启动失败，自动回退")
                _error.value = "正在尝试其他识别方式..."
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 200)
                return
            }
            _error.value = "启动语音识别失败: ${e.message}"
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

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "准备就绪，请说话...")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "检测到说话")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // 可用于显示音量动画
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "说话结束")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            Log.w(TAG, "语音识别错误: code=$error, mode=$currentMode")

            // 判断是否为网络/服务器错误
            val isNetworkError = error == SpeechRecognizer.ERROR_NETWORK
                    || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                    || error == ERROR_SERVER_DISCONNECTED
                    || error == SpeechRecognizer.ERROR_SERVER

            // 判断是否为语言不支持错误
            val isLangError = error == ERROR_LANGUAGE_NOT_SUPPORTED
                    || error == ERROR_LANGUAGE_UNAVAILABLE

            // 回退策略：按模式逐级降级
            if (isNetworkError || isLangError) {
                errorCount++
                val nextMode = when (currentMode) {
                    SttMode.ON_DEVICE -> {
                        triedOnDevice = true
                        SttMode.OFFLINE
                    }
                    SttMode.OFFLINE -> {
                        triedOffline = true
                        SttMode.ONLINE
                    }
                    SttMode.ONLINE -> null  // 在线也失败了，无路可退
                }

                if (nextMode != null && errorCount <= 3) {
                    Log.i(TAG, "识别失败(code=$error)，回退到 $nextMode (第${errorCount}次)")
                    _error.value = "正在尝试其他识别方式..."
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startListening()  // startListening内部会根据triedXxx标志选择下一个模式
                    }, 300)
                    return
                } else {
                    // 所有模式都失败
                    _error.value = "语音识别不可用，请使用下方文字输入"
                    _isSttUnavailable.value = true
                    errorCount = 0
                    return
                }
            }

            // 非网络/语言错误，正常提示
            errorCount = 0
            val errMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到内容，请再试一次"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到说话"
                SpeechRecognizer.ERROR_AUDIO -> "录音错误"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙，请稍后"
                else -> "识别错误(code=$error)"
            }
            _error.value = errMsg
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            // 识别成功，重置回退状态
            errorCount = 0
            triedOnDevice = false
            triedOffline = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _recognizedText.value = text
                _partialText.value = ""
                Log.i(TAG, "识别结果: $text (mode=$currentMode)")
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
