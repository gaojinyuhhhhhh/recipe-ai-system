package com.recipe.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.model.RecipeStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 烹饪模式临时数据持有者
 * 用于在导航到烹饪页面之前传递数据（避免大量数据通过导航参数传递）
 */
object CookingSessionHolder {
    var recipeTitle: String = ""
    var steps: List<RecipeStep> = emptyList()
    var recipeId: Long? = null
    var localRecipeId: Long? = null

    fun set(title: String, steps: List<RecipeStep>, recipeId: Long? = null, localRecipeId: Long? = null) {
        this.recipeTitle = title
        this.steps = steps
        this.recipeId = recipeId
        this.localRecipeId = localRecipeId
    }

    fun clear() {
        recipeTitle = ""
        steps = emptyList()
        recipeId = null
        localRecipeId = null
    }
}

/**
 * 烹饪模式ViewModel
 *
 * 职责范围：
 * 1. 管理烹饪步骤 — 当前步骤索引、步骤切换（上一步/下一步/跳转）
 * 2. 倒计时功能 — 启动/暂停/重置当前步骤计时器
 * 3. 烹饪进度 — 整体进度追踪、完成状态
 *
 * 计时器机制：
 * - 使用协程每秒递减 remainingTime
 * - 倒计时到0不自动跳转，停留在当前步骤等待用户确认
 * - 通过 isStepTimerFinished 状态通知 UI 显示“时间到”提示
 * - 支持暂停/继续
 * - 无 duration 的步骤不计时，手动切换
 */
class CookingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CookingVM"
        private const val TIMER_WARNING_THRESHOLD = 10  // 最后10秒播报提醒
    }

    // ==================== TTS 语音引擎 ====================

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsFailed = false  // TTS引擎是否初始化失败（设备无TTS引擎）
    private val mainHandler = Handler(Looper.getMainLooper())

    // TTS初始化完成前的待播报队列
    private val pendingSpeech = mutableListOf<String>()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled

    // TTS引擎是否可用（设备是否安装了TTS引擎）
    private val _isTtsAvailable = MutableStateFlow(true)
    val isTtsAvailable: StateFlow<Boolean> = _isTtsAvailable

    // 是否已播报过当前步骤的倒计时结束语音
    private var hasSpokenTimerFinish = false
    // 是否已播报过当前步骤的最后10秒提醒
    private var hasSpokenTimerWarning = false

    /**
     * 初始化TTS引擎
     */
    fun initTts() {
        if (tts != null) return
        Log.i(TAG, "开始初始化TTS引擎...")
        // 不指定引擎名，使用系统默认TTS引擎（OPPO/华为等手机没有Google TTS）
        tts = TextToSpeech(getApplication<Application>()) { status ->
            Log.i(TAG, "TTS OnInit回调, status=$status")
            if (status == TextToSpeech.SUCCESS) {
                // 优先尝试简体中文，再尝试中文，最后回退默认
                val locales = listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, Locale.getDefault())
                var langSet = false
                var langResult = -1
                for (locale in locales) {
                    val result = tts?.setLanguage(locale)
                    Log.i(TAG, "设置语言 $locale: result=$result (0=成功, 1=缺少数据, 2=不支持)")
                    if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        langSet = true
                        langResult = result ?: -1
                        Log.i(TAG, "成功设置TTS语言为: $locale")
                        break
                    }
                }
                
                if (!langSet) {
                    Log.w(TAG, "所有中文语言均不可用，回退到默认语言")
                    // 即使没有中文，也尝试用默认语言播报（可能会有英文口音）
                    val defaultResult = tts?.setLanguage(Locale.getDefault())
                    Log.i(TAG, "设置默认语言: result=$defaultResult")
                    if (defaultResult != TextToSpeech.LANG_MISSING_DATA && defaultResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                        langSet = true
                        langResult = defaultResult ?: -1
                    }
                }
                
                if (!langSet) {
                    Log.e(TAG, "TTS引擎无法设置任何语言，语音播报将不可用")
                    ttsFailed = true
                    _isTtsAvailable.value = false
                    _isVoiceEnabled.value = false
                    pendingSpeech.clear()
                    tts?.shutdown()
                    tts = null
                    // 在lambda中不能使用return，需要让后续代码不执行
                    // 通过设置tts=null和ttsFailed=true来阻止后续操作
                } else {
                    // 设置音频属性确保走媒体通道
                    tts?.setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    
                    // 设置音量和语速
                    tts?.setPitch(1.0f)  // 正常音调
                    tts?.setSpeechRate(0.9f)  // 稍慢语速，更清晰

                    ttsReady = true
                    Log.i(TAG, "TTS引擎初始化成功，待播报队列: ${pendingSpeech.size}条")
                    // 延迟500ms后播放缓存，给引擎充分预热时间
                    mainHandler.postDelayed({ flushPendingSpeech() }, 500)
                }
            } else {
                Log.e(TAG, "TTS引擎初始化失败: $status，设备可能未安装TTS引擎")
                ttsFailed = true
                _isTtsAvailable.value = false
                _isVoiceEnabled.value = false
                pendingSpeech.clear()
                // 清理失败的TTS实例
                tts?.shutdown()
                tts = null
            }
        }
    }

    /**
     * 切换语音开关
     */
    fun toggleVoice() {
        _isVoiceEnabled.value = !_isVoiceEnabled.value
    }

    /**
     * 语音播报文本
     */
    private fun speak(text: String) {
        if (!_isVoiceEnabled.value) {
            Log.d(TAG, "语音已关闭，跳过播报: $text")
            return
        }
        if (ttsFailed) {
            Log.w(TAG, "TTS不可用，跳过播报: $text")
            return  // TTS不可用，直接跳过
        }
        if (!ttsReady) {
            // TTS尚未就绪，加入待播报队列
            pendingSpeech.add(text)
            Log.i(TAG, "TTS未就绪，缓存播报: $text")
            return
        }
        
        // 确保TTS引擎正常工作
        val ttsInstance = tts
        if (ttsInstance == null) {
            Log.e(TAG, "TTS实例为空，无法播报: $text")
            return
        }
        
        try {
            val utteranceId = "cooking_${System.currentTimeMillis()}"
            val result = ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            Log.i(TAG, "speak()调用: text=$text, result=$result, utteranceId=$utteranceId")
            
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "TTS播报失败: $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS播报异常: $text", e)
        }
    }

    /**
     * 语音播报（排队模式，不会打断当前播报）
     */
    private fun speakQueue(text: String) {
        if (!_isVoiceEnabled.value) {
            Log.d(TAG, "语音已关闭，跳过排队播报: $text")
            return
        }
        if (ttsFailed) {
            Log.w(TAG, "TTS不可用，跳过排队播报: $text")
            return
        }
        if (!ttsReady) {
            pendingSpeech.add(text)
            Log.i(TAG, "TTS未就绪，缓存排队播报: $text")
            return
        }
        
        val ttsInstance = tts
        if (ttsInstance == null) {
            Log.e(TAG, "TTS实例为空，无法排队播报: $text")
            return
        }
        
        try {
            val utteranceId = "cooking_q_${System.currentTimeMillis()}"
            val result = ttsInstance.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
            Log.i(TAG, "speakQueue()调用: text=$text, result=$result, utteranceId=$utteranceId")
            
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "TTS排队播报失败: $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS排队播报异常: $text", e)
        }
    }

    /**
     * 播放缓存的待播报内容
     */
    private fun flushPendingSpeech() {
        if (pendingSpeech.isEmpty()) {
            Log.d(TAG, "flushPendingSpeech: 无待播报内容")
            return
        }
        
        Log.i(TAG, "flushPendingSpeech: ${pendingSpeech.size}条待播报")
        val toSpeak = pendingSpeech.toList()
        pendingSpeech.clear()
        
        val ttsInstance = tts
        if (ttsInstance == null) {
            Log.e(TAG, "flushPendingSpeech: TTS实例为空")
            return
        }
        
        // 第一条用QUEUE_FLUSH（打断静默），后续用QUEUE_ADD排队
        toSpeak.forEachIndexed { index, text ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            try {
                val utteranceId = "cooking_pending_${System.currentTimeMillis()}_$index"
                val result = ttsInstance.speak(text, queueMode, null, utteranceId)
                Log.i(TAG, "flushPending speak[$index]: result=$result, text=$text, utteranceId=$utteranceId")
                
                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "flushPending speak[$index] 失败: $text")
                }
            } catch (e: Exception) {
                Log.e(TAG, "flushPending speak[$index] 异常: $text", e)
            }
        }
    }

    // ==================== 食谱数据 ====================

    private val _recipeTitle = MutableStateFlow("")
    val recipeTitle: StateFlow<String> = _recipeTitle

    private val _steps = MutableStateFlow<List<RecipeStep>>(emptyList())
    val steps: StateFlow<List<RecipeStep>> = _steps

    // ==================== 步骤控制 ====================

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    val currentStep: RecipeStep?
        get() = _steps.value.getOrNull(_currentStepIndex.value)

    val totalSteps: Int
        get() = _steps.value.size

    // ==================== 计时器 ====================

    private val _remainingTime = MutableStateFlow(0)  // 秒
    val remainingTime: StateFlow<Int> = _remainingTime

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning

    // 当前步骤倒计时是否已结束（等待用户确认）
    private val _isStepTimerFinished = MutableStateFlow(false)
    val isStepTimerFinished: StateFlow<Boolean> = _isStepTimerFinished

    // 当前步骤是否有计时
    val currentStepHasTimer: Boolean
        get() = currentStep?.duration != null && (currentStep?.duration ?: 0) > 0

    // ==================== 进度 ====================

    val progress: Float
        get() = if (totalSteps == 0) 0f else (_currentStepIndex.value + 1).toFloat() / totalSteps

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    // 计时器协程Job
    private var timerJob: Job? = null

    // ==================== 初始化 ====================

    /**
     * 从 CookingSessionHolder 加载烹饪数据
     * 在进入 CookingModeScreen 时调用
     */
    fun loadFromHolder() {
        val holder = CookingSessionHolder
        if (holder.steps.isEmpty()) {
            Log.w(TAG, "CookingSessionHolder 为空，无食谱数据")
            return
        }

        _recipeTitle.value = holder.recipeTitle
        _steps.value = holder.steps
        _currentStepIndex.value = 0
        _isComplete.value = false
        _isTimerRunning.value = false
        hasSpokenTimerFinish = false
        hasSpokenTimerWarning = false

        // 设置第一步的计时器
        val firstStep = holder.steps.firstOrNull()
        if (firstStep?.duration != null && firstStep.duration > 0) {
            _remainingTime.value = firstStep.duration
        } else {
            _remainingTime.value = 0
        }

        // 语音播报第一步内容
        announceStep(firstStep, 0)
    }

    // ==================== 计时器控制 ====================

    /**
     * 启动计时器
     */
    fun startTimer() {
        if (_isTimerRunning.value) return
        if (_remainingTime.value <= 0) return

        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (isActive && _remainingTime.value > 0) {
                delay(1000)
                if (isActive) {
                    val newTime = _remainingTime.value - 1
                    _remainingTime.value = newTime

                    // 最后10秒语音提醒
                    if (newTime == TIMER_WARNING_THRESHOLD && !hasSpokenTimerWarning) {
                        hasSpokenTimerWarning = true
                        speak("还剩10秒")
                    }

                    // 倒计时结束，自动切换下一步
                    if (newTime <= 0) {
                        _isTimerRunning.value = false
                        onTimerFinished()
                        break
                    }
                }
            }
        }
    }

    /**
     * 暂停计时器
     */
    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
    }

    /**
     * 切换计时器状态（启动/暂停）
     */
    fun toggleTimer() {
        if (_isTimerRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    /**
     * 重置当前步骤计时器
     */
    fun resetTimer() {
        pauseTimer()
        currentStep?.duration?.let {
            _remainingTime.value = it
        }
    }

    /**
     * 计时器结束回调 — 不自动跳转，等待用户确认
     */
    private fun onTimerFinished() {
        _isStepTimerFinished.value = true
        if (!hasSpokenTimerFinish) {
            hasSpokenTimerFinish = true
            speak("时间到！")
        }
    }

    // ==================== 步骤切换 ====================

    /**
     * 确认当前步骤完成，进入下一步
     * 当步骤倒计时结束（isStepTimerFinished=true）时由用户点击触发
     */
    fun confirmStepAndNext() {
        _isStepTimerFinished.value = false
        if (_currentStepIndex.value >= totalSteps - 1) {
            _isComplete.value = true
            speak("烹饪完成！太棒了！")
            return
        }
        _currentStepIndex.value += 1
        setupStepTimer()
        announceStep(currentStep, _currentStepIndex.value)
    }

    /**
     * 切换到下一步
     */
    fun nextStep() {
        if (_currentStepIndex.value >= totalSteps - 1) {
            // 已经是最后一步，标记完成
            pauseTimer()
            _isComplete.value = true
            speak("烹饪完成！太棒了！")
            return
        }

        pauseTimer()
        _isStepTimerFinished.value = false
        _currentStepIndex.value += 1
        setupStepTimer()
        announceStep(currentStep, _currentStepIndex.value)
    }

    /**
     * 切换到上一步
     */
    fun previousStep() {
        if (_currentStepIndex.value <= 0) return

        pauseTimer()
        _isStepTimerFinished.value = false
        _currentStepIndex.value -= 1
        setupStepTimer()
        announceStep(currentStep, _currentStepIndex.value)
    }

    /**
     * 跳转到指定步骤
     */
    fun jumpToStep(index: Int) {
        if (index < 0 || index >= totalSteps) return
        if (index == _currentStepIndex.value) return

        pauseTimer()
        _isStepTimerFinished.value = false
        _currentStepIndex.value = index
        setupStepTimer()
        announceStep(currentStep, index)
    }

    /**
     * 设置当前步骤的计时器
     */
    private fun setupStepTimer() {
        val step = currentStep
        if (step?.duration != null && step.duration > 0) {
            _remainingTime.value = step.duration
        } else {
            _remainingTime.value = 0
        }
        // 重置语音播报标记
        hasSpokenTimerFinish = false
        hasSpokenTimerWarning = false
    }

    /**
     * 完成烹饪（手动标记完成）
     */
    fun completeCooking() {
        pauseTimer()
        _isStepTimerFinished.value = false
        _isComplete.value = true
        speak("烹饪完成！太棒了！")
    }

    /**
     * 重新开始烹饪（回到第一步）
     */
    fun restartCooking() {
        pauseTimer()
        _isStepTimerFinished.value = false
        _currentStepIndex.value = 0
        _isComplete.value = false
        setupStepTimer()
        announceStep(currentStep, 0)
    }

    // ==================== 格式化工具 ====================

    /**
     * 语音播报步骤内容
     */
    private fun announceStep(step: RecipeStep?, index: Int) {
        if (step == null) return
        val text = buildString {
            append("第${index + 1}步，")
            append(step.content)
            step.duration?.let { dur ->
                if (dur > 0) {
                    append("，预计${formatDuration(dur)}")
                }
            }
        }
        speak(text)
    }

    /**
     * 格式化剩余时间为 mm:ss
     */
    fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    /**
     * 格式化步骤时长为可读文本
     */
    fun formatDuration(seconds: Int): String {
        return if (seconds >= 60) {
            val mins = seconds / 60
            val secs = seconds % 60
            if (secs > 0) "${mins}分${secs}秒" else "${mins}分钟"
        } else {
            "${seconds}秒"
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        CookingSessionHolder.clear()
    }
}
