package com.recipe.ui.recipe

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.viewmodel.ChatViewModel
import com.recipe.viewmodel.CookingViewModel
import com.recipe.data.model.RecipeStep

/**
 * 烹饪模式主界面
 *
 * 功能：
 * - 大号倒计时显示当前步骤剩余时间
 * - 当前步骤内容卡片（高亮显示）
 * - 步骤进度条
 * - 播放/暂停/重置计时器控制
 * - 上一步/下一步切换按钮
 * - 可展开的步骤总览列表
 * - 烹饪完成庆祝界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    onNavigateBack: () -> Unit,
    cookingViewModel: CookingViewModel = viewModel()
) {
    val recipeTitle by cookingViewModel.recipeTitle.collectAsState()
    val steps by cookingViewModel.steps.collectAsState()
    val currentStepIndex by cookingViewModel.currentStepIndex.collectAsState()
    val remainingTime by cookingViewModel.remainingTime.collectAsState()
    val isTimerRunning by cookingViewModel.isTimerRunning.collectAsState()
    val isStepTimerFinished by cookingViewModel.isStepTimerFinished.collectAsState()
    val isComplete by cookingViewModel.isComplete.collectAsState()
    val isVoiceEnabled by cookingViewModel.isVoiceEnabled.collectAsState()
    val isTtsAvailable by cookingViewModel.isTtsAvailable.collectAsState()

    val context = LocalContext.current

    var showStepList by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showVoiceChat by remember { mutableStateOf(false) }

    // 语音助手的ChatViewModel
    val chatViewModel: ChatViewModel = viewModel()

    // Android 13+ 运行时请求通知权限
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* 无论授权与否都不阻塞流程 */ }

    // 进入时先初始化TTS，然后延迟加载数据以确保TTS准备就绪
    LaunchedEffect(Unit) {
        // 请求通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        cookingViewModel.initTts()
        // 给TTS引擎一些时间初始化，然后再加载数据
        kotlinx.coroutines.delay(200)
        cookingViewModel.loadFromHolder()
    }

    // 步骤倒计时结束时，播放提示音+震动
    LaunchedEffect(isStepTimerFinished) {
        if (isStepTimerFinished) {
            playTimerAlert(context)
        }
    }

    // 当前步骤
    val currentStep = steps.getOrNull(currentStepIndex)
    val hasTimer = currentStep?.duration != null && currentStep.duration > 0
    val totalSteps = steps.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "烹饪模式",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            recipeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isTimerRunning || currentStepIndex > 0) {
                            showExitDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    // 语音开关按钮（仅TTS可用时显示）
                    if (isTtsAvailable) {
                        IconButton(onClick = { cookingViewModel.toggleVoice() }) {
                            Icon(
                                if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (isVoiceEnabled) "关闭语音" else "开启语音",
                                tint = if (isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showStepList = !showStepList }) {
                        Icon(
                            if (showStepList) Icons.Default.List else Icons.Default.ViewList,
                            contentDescription = "步骤列表"
                        )
                    }
                    // 语音助手按钮
                    IconButton(onClick = { showVoiceChat = !showVoiceChat }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "语音助手",
                            tint = if (showVoiceChat) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (isComplete) {
            // ========== 烹饪完成界面 ==========
            CookingCompleteView(
                recipeTitle = recipeTitle,
                onRestart = { cookingViewModel.restartCooking() },
                onExit = { onNavigateBack() },
                modifier = Modifier.padding(padding)
            )
        } else if (steps.isEmpty()) {
            // ========== 无步骤数据 ==========
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("未找到步骤数据", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateBack) { Text("返回") }
                }
            }
        } else {
            // ========== 烹饪进行中界面 ==========
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // TTS不可用提示条
                if (!isTtsAvailable) {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "语音播报不可用",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "倒计时仍通过铃声和震动提醒。如需语音播报，请安装TTS引擎并下载中文语音数据包。",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                // 进度条
                LinearProgressIndicator(
                    progress = cookingViewModel.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )

                // 步骤进度文字
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "步骤 ${currentStepIndex + 1} / $totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${(cookingViewModel.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showStepList) {
                    // ========== 步骤总览列表模式 ==========
                    StepListView(
                        steps = steps,
                        currentIndex = currentStepIndex,
                        onStepClick = { index -> cookingViewModel.jumpToStep(index) },
                        formatDuration = { cookingViewModel.formatDuration(it) }
                    )
                } else {
                    // ========== 当前步骤大卡片模式 ==========
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 倒计时显示
                        if (hasTimer) {
                            TimerDisplay(
                                remainingTime = remainingTime,
                                totalTime = currentStep!!.duration!!,
                                isRunning = isTimerRunning,
                                isStepFinished = isStepTimerFinished,
                                formatTime = { cookingViewModel.formatTime(it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 步骤时间到 - 醒目提示卡片
                            if (isStepTimerFinished) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "时间到！",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "此步骤已完成，准备好后进入下一步",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        if (currentStepIndex < totalSteps - 1) {
                                            Button(
                                                onClick = { cookingViewModel.confirmStepAndNext() },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("进入下一步")
                                            }
                                        } else {
                                            Button(
                                                onClick = { cookingViewModel.completeCooking() },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF4CAF50)
                                                )
                                            ) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("完成烹饪")
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 计时器控制按钮（时间未结束时显示）
                            if (!isStepTimerFinished) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 重置按钮
                                    IconButton(
                                        onClick = { cookingViewModel.resetTimer() },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Replay,
                                            contentDescription = "重置",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))

                                    // 播放/暂停按钮（大号）
                                    FilledIconButton(
                                        onClick = { cookingViewModel.toggleTimer() },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isTimerRunning) "暂停" else "开始",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))

                                    // 跳过按钮（下一步）
                                    IconButton(
                                        onClick = { cookingViewModel.nextStep() },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = "跳过",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            // 无计时的步骤 - 显示手动切换提示
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.TouchApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "此步骤无需计时",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "完成后点击下方按钮继续",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 当前步骤内容卡片
                        CurrentStepCard(
                            step = currentStep!!,
                            stepIndex = currentStepIndex,
                            formatDuration = { cookingViewModel.formatDuration(it) }
                        )

                        // 上一步/下一步提示
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (currentStepIndex > 0) {
                                TextButton(onClick = { cookingViewModel.previousStep() }) {
                                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("上一步")
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            if (currentStepIndex < totalSteps - 1) {
                                FilledTonalButton(onClick = { cookingViewModel.nextStep() }) {
                                    Text("下一步")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Button(onClick = {
                                    cookingViewModel.completeCooking()
                                }) {
                                    Text("完成烹饪")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 退出确认对话框
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("退出烹饪模式？") },
            text = { Text("当前烹饪进度将不会保存，确定退出吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        cookingViewModel.pauseTimer()
                        showExitDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("继续烹饪") }
            }
        )
    }

    // 语音助手浮层
    Box(modifier = Modifier.fillMaxSize()) {
        CookingVoiceChatPanel(
            chatViewModel = chatViewModel,
            recipeTitle = recipeTitle,
            steps = steps,
            currentStepIndex = currentStepIndex,
            remainingTime = remainingTime,
            isVisible = showVoiceChat,
            onDismiss = { showVoiceChat = false }
        )
    }
}

// ==================== 子组件 ====================

/**
 * 倒计时显示组件 - 大号圆环+时间
 */
@Composable
fun TimerDisplay(
    remainingTime: Int,
    totalTime: Int,
    isRunning: Boolean,
    isStepFinished: Boolean = false,
    formatTime: (Int) -> String
) {
    val animatedProgress = animateFloatAsState(
        targetValue = if (totalTime > 0) remainingTime.toFloat() / totalTime.toFloat() else 0f,
        label = "timerProgress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // 背景圆环
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.size(200.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                strokeWidth = 8.dp
            )
            // 进度圆环
            CircularProgressIndicator(
                progress = animatedProgress.value,
                modifier = Modifier.size(200.dp),
                color = when {
                    remainingTime <= 10 && remainingTime > 0 -> Color(0xFFF44336) // 红色警告
                    remainingTime <= 30 && remainingTime > 10 -> Color(0xFFFF9800) // 橙色提醒
                    else -> MaterialTheme.colorScheme.primary
                },
                strokeWidth = 8.dp
            )
            // 中间时间文字
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(remainingTime),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (remainingTime >= 3600) 36.sp else 48.sp
                    ),
                    color = when {
                        isStepFinished -> Color(0xFF4CAF50)
                        remainingTime <= 10 && remainingTime > 0 -> Color(0xFFF44336)
                        remainingTime <= 30 && remainingTime > 10 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = when {
                        isStepFinished -> "时间到！"
                        isRunning -> "计时中"
                        remainingTime == 0 && totalTime > 0 -> "已完成"
                        else -> "已暂停"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 当前步骤内容卡片
 */
@Composable
fun CurrentStepCard(
    step: RecipeStep,
    stepIndex: Int,
    formatDuration: (Int) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 步骤编号
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "第 ${stepIndex + 1} 步",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 步骤时长标签
                step.duration?.let { dur ->
                    if (dur > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    formatDuration(dur),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 步骤内容
            Text(
                step.content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
            )

            // 温度提示
            step.temperature?.takeIf { it.isNotBlank() }?.let { temp ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Thermostat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "温度: $temp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF44336)
                    )
                }
            }

            // 小贴士
            step.tips?.takeIf { it.isNotBlank() }?.let { tips ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFC107)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tips,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * 步骤总览列表
 */
@Composable
fun StepListView(
    steps: List<RecipeStep>,
    currentIndex: Int,
    onStepClick: (Int) -> Unit,
    formatDuration: (Int) -> String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(steps) { index, step ->
            val isCurrent = index == currentIndex
            val isPast = index < currentIndex

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStepClick(index) },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isCurrent) CardDefaults.outlinedCardBorder() else null
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 步骤序号
                    Surface(
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isPast -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                            if (isPast) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 步骤内容
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            step.content,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 2
                        )
                        step.duration?.let { dur ->
                            if (dur > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        formatDuration(dur),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 跳转按钮
                    if (!isCurrent) {
                        FilledTonalButton(
                            onClick = { onStepClick(index) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("前往", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "当前",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 烹饪完成庆祝界面
 */
@Composable
fun CookingCompleteView(
    recipeTitle: String,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 完成图标
        Surface(
            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "烹饪完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            recipeTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "太棒了！享受你的美食吧",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 按钮组
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Replay, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("重新开始")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("完成")
        }
    }
}

/**
 * 播放步骤计时器结束提示音+震动
 */
private fun playTimerAlert(context: Context) {
    // 播放系统通知提示音
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .build()
        ringtone.play()
    } catch (e: Exception) {
        // 静默失败，某些设备可能没有通知音
    }

    // 震动提醒
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 震动模式: 等待0ms, 震300ms, 等100ms, 震300ms
                it.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 300, 100, 300),
                        intArrayOf(0, 128, 0, 128),
                        -1  // 不重复
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 300, 100, 300), -1)
            }
        }
    } catch (e: Exception) {
        // 静默失败
    }
}
