package com.recipe.ui.recipe

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.recipe.data.model.RecipeStep
import com.recipe.util.SpeechRecognizerHelper
import com.recipe.viewmodel.ChatMessage
import com.recipe.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

/**
 * 烹饪模式语音助手浮层
 *
 * 功能：
 * - 按住/点击麦克风按钮进行语音输入
 * - 自动将当前烹饪步骤注入AI上下文
 * - AI回复自动语音播报
 * - 支持查看对话历史
 * - 轻量浮层设计，不遮挡烹饪界面
 *
 * 语音输入三层兜底：
 * - SpeechRecognizer API → 最佳体验
 * - Intent系统弹窗 → 兼容性好
 * - 键盘文字输入 → 最终兜底（可用键盘语音按钮）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingVoiceChatPanel(
    chatViewModel: ChatViewModel,
    recipeTitle: String,
    steps: List<RecipeStep>,
    currentStepIndex: Int,
    remainingTime: Int,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val error by chatViewModel.error.collectAsState()
    val isSpeaking by chatViewModel.isSpeaking.collectAsState()
    val isTtsEnabled by chatViewModel.isTtsEnabled.collectAsState()

    // 语音识别
    val speechHelper = remember { SpeechRecognizerHelper(context) }
    val isListening by speechHelper.isListening.collectAsState()
    val recognizedText by speechHelper.recognizedText.collectAsState()
    val partialText by speechHelper.partialText.collectAsState()
    val sttError by speechHelper.error.collectAsState()
    val voiceMode by speechHelper.voiceInputMode.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 文字输入
    var textInput by remember { mutableStateOf("") }
    // 是否显示文字输入框
    // KEYBOARD_ONLY 模式默认显示文字输入，其他模式默认显示麦克风
    var showTextInput by remember(voiceMode) {
        mutableStateOf(voiceMode == SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY)
    }

    // 运行时录音权限
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        // 权限获取后，直接尝试启动语音识别
        // Intent模式下用户需再次点击麦克风按钮触发
        if (granted && voiceMode == SpeechRecognizerHelper.VoiceInputMode.SPEECH_RECOGNIZER) {
            speechHelper.startListening()
        }
    }

    // Intent方式语音识别（系统弹窗）
    val intentSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            speechHelper.handleIntentResult(result.data)
        }
    }

    // 进入时设置烹饪上下文
    LaunchedEffect(isVisible) {
        if (isVisible) {
            chatViewModel.enterCookingMode(recipeTitle, steps, currentStepIndex)
        }
    }

    // 同步烹饪状态
    LaunchedEffect(currentStepIndex, remainingTime) {
        if (isVisible) {
            chatViewModel.updateCookingState(currentStepIndex, remainingTime)
        }
    }

    // 识别到文本后自动发送
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank()) {
            chatViewModel.sendMessage(recognizedText)
            speechHelper.clearResult()
        }
    }

    // 新消息自动滚动
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // TTS播报时暂停监听（防回声）
    LaunchedEffect(isSpeaking) {
        if (isSpeaking && isListening) {
            speechHelper.cancel()
        }
    }

    // voiceMode降级时自动切换到文字输入
    LaunchedEffect(voiceMode) {
        if (voiceMode == SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY) {
            showTextInput = true
        }
    }

    // 离开时释放资源
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.destroy()
            chatViewModel.exitCookingMode()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "语音助手",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "问我任何关于当前烹饪的问题",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // TTS开关
                    IconButton(
                        onClick = { chatViewModel.toggleTts() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (isTtsEnabled) "关闭语音回复" else "开启语音回复",
                            tint = if (isTtsEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // 关闭按钮
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 消息区域
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (messages.isEmpty() && !isLoading) {
                        item {
                            // 空状态提示
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    if (voiceMode == SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY)
                                        Icons.Default.Keyboard else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    when (voiceMode) {
                                        SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY ->
                                            "输入问题与AI对话"
                                        SpeechRecognizerHelper.VoiceInputMode.INTENT ->
                                            "点击麦克风按钮开始语音输入"
                                        SpeechRecognizerHelper.VoiceInputMode.SPEECH_RECOGNIZER ->
                                            "点击下方麦克风按钮说话"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    when (voiceMode) {
                                        SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY ->
                                            "输入问题，如：\"现在该放盐了吗？\"\n提示：点击键盘上的语音按钮也可以语音输入"
                                        else ->
                                            "试试说：\"现在该放盐了吗？\""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    items(messages) { message ->
                        VoiceChatBubble(message = message)
                    }

                    // 加载中
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "食小天正在思考...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 底部操作区域
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 识别中文本显示
                        if (isListening || partialText.isNotBlank()) {
                            Text(
                                text = if (partialText.isNotBlank()) partialText else "正在听...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // 错误提示
                        if (sttError != null || error != null) {
                            Text(
                                text = sttError ?: error ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // TTS播报中提示
                        if (isSpeaking) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "正在播报回复...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { chatViewModel.stopSpeaking() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("停止", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // 输入区域
                        if (showTextInput) {
                            // 文字输入模式
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text(
                                            if (voiceMode == SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY)
                                                "输入问题（可用键盘语音按钮）"
                                            else "输入问题...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    maxLines = 2,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    shape = RoundedCornerShape(20.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (textInput.isNotBlank() && !isLoading) {
                                            chatViewModel.sendMessage(textInput.trim())
                                            textInput = ""
                                        }
                                    })
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledIconButton(
                                    onClick = {
                                        if (textInput.isNotBlank() && !isLoading) {
                                            chatViewModel.sendMessage(textInput.trim())
                                            textInput = ""
                                        }
                                    },
                                    enabled = textInput.isNotBlank() && !isLoading,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "发送", modifier = Modifier.size(18.dp))
                                }
                                // 可切换回语音模式（仅语音可用时）
                                if (voiceMode != SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { showTextInput = false },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = "切换语音",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // 麦克风按钮 + 切换文字输入按钮
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MicrophoneButton(
                                    isListening = isListening,
                                    isLoading = isLoading,
                                    isSpeaking = isSpeaking,
                                    onClick = {
                                        if (isListening) {
                                            speechHelper.stopListening()
                                        } else if (isSpeaking) {
                                            chatViewModel.stopSpeaking()
                                        } else {
                                            if (!hasAudioPermission) {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@MicrophoneButton
                                            }
                                            when (voiceMode) {
                                                SpeechRecognizerHelper.VoiceInputMode.SPEECH_RECOGNIZER -> {
                                                    speechHelper.startListening()
                                                }
                                                SpeechRecognizerHelper.VoiceInputMode.INTENT -> {
                                                    try {
                                                        intentSpeechLauncher.launch(speechHelper.createRecognizeSpeechIntent())
                                                    } catch (e: Exception) {
                                                        // Intent启动失败，切到文字输入
                                                        showTextInput = true
                                                    }
                                                }
                                                SpeechRecognizerHelper.VoiceInputMode.KEYBOARD_ONLY -> {
                                                    showTextInput = true
                                                }
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(24.dp))
                                // 切换文字输入按钮
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { showTextInput = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Keyboard,
                                            contentDescription = "文字输入",
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "打字",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 麦克风按钮 — 带动画脉冲效果
 */
@Composable
fun MicrophoneButton(
    isListening: Boolean,
    isLoading: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val buttonColor = when {
        isListening -> MaterialTheme.colorScheme.error
        isLoading -> MaterialTheme.colorScheme.outline
        isSpeaking -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val icon = when {
        isListening -> Icons.Default.MicOff
        isSpeaking -> Icons.Default.Stop
        else -> Icons.Default.Mic
    }

    val label = when {
        isListening -> "点击停止"
        isLoading -> "等待回复..."
        isSpeaking -> "停止播报"
        else -> "点击说话"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier
                .size(56.dp)
                .then(
                    if (isListening) Modifier.scale(pulseScale) else Modifier
                ),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = buttonColor
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 语音聊天消息气泡 — 紧凑版
 */
@Composable
fun VoiceChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 12.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
