package com.recipe.ui.user

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.data.model.PreferenceAnalysis
import com.recipe.data.model.PreferenceItem
import com.recipe.data.model.UserAiProfile
import com.recipe.viewmodel.AiProfileViewModel

/**
 * AI画像页面 - 展示AI学习到的用户偏好
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProfileScreen(
    onNavigateBack: () -> Unit,
    onApplyPreferences: () -> Unit,
    viewModel: AiProfileViewModel = viewModel()
) {
    val aiProfile by viewModel.aiProfile.collectAsState()
    val analysis by viewModel.preferenceAnalysis.collectAsState()
    val suggestedPrefs by viewModel.suggestedPreferences.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI画像") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && aiProfile == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // AI画像概览卡片
                    AiProfileOverviewCard(
                        profile = aiProfile,
                        analysis = analysis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 偏好分析详情
                    analysis?.let { analysisData ->
                        PreferenceAnalysisSection(analysis = analysisData)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI推荐偏好
                    suggestedPrefs?.let { suggested ->
                        SuggestedPreferencesCard(
                            suggested = suggested,
                            onApply = {
                                viewModel.applySuggestedPreferences {
                                    onApplyPreferences()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 错误提示
            error?.let { errorMsg ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("提示") },
                    text = { Text(errorMsg) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("确定")
                        }
                    }
                )
            }

            // 成功提示
            successMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearSuccessMessage()
                }
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(msg, color = Color.White)
                }
            }
        }
    }
}

/**
 * AI画像概览卡片
 */
@Composable
private fun AiProfileOverviewCard(
    profile: UserAiProfile?,
    analysis: PreferenceAnalysis?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI画像",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "基于您的行为自动学习",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 交互统计
            val interactions = profile?.totalInteractions ?: analysis?.totalInteractions ?: 0
            val confidence = analysis?.confidenceScore ?: 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = interactions.toString(),
                    label = "交互次数",
                    icon = Icons.Default.TouchApp
                )
                StatItem(
                    value = "${(confidence * 100).toInt()}%",
                    label = "置信度",
                    icon = Icons.Default.Star
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 学习进度
            LinearProgressIndicator(
                progress = confidence.coerceIn(0.0, 1.0).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    confidence >= 0.85 -> "AI已充分了解您的偏好"
                    confidence >= 0.60 -> "AI正在学习中，继续互动可提高准确性"
                    confidence >= 0.40 -> "数据积累中，多使用收藏和评论功能"
                    else -> "刚开始学习，多互动让AI更懂你"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 偏好分析详情
 */
@Composable
private fun PreferenceAnalysisSection(analysis: PreferenceAnalysis) {
    Column {
        Text(
            text = "偏好分析",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 最喜欢的菜系
        if (analysis.topCuisines.isNotEmpty()) {
            PreferenceCategoryCard(
                title = "喜欢的菜系",
                icon = Icons.Default.Restaurant,
                items = analysis.topCuisines
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 喜欢的口味
        if (analysis.topTastes.isNotEmpty()) {
            PreferenceCategoryCard(
                title = "喜欢的口味",
                icon = Icons.Default.Favorite,
                items = analysis.topTastes
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 营养目标
        if (analysis.topNutrition.isNotEmpty()) {
            PreferenceCategoryCard(
                title = "营养目标",
                icon = Icons.Default.HealthAndSafety,
                items = analysis.topNutrition
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 推荐设置
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "推荐设置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RecommendationItem(
                        label = "难度",
                        value = when (analysis.recommendedDifficulty) {
                            "EASY" -> "简单"
                            "MEDIUM" -> "中等"
                            "HARD" -> "困难"
                            else -> analysis.recommendedDifficulty
                        }
                    )
                    RecommendationItem(
                        label = "烹饪时长",
                        value = "${analysis.recommendedCookingTime}分钟"
                    )
                }
            }
        }
    }
}

/**
 * 偏好分类卡片
 */
@Composable
private fun PreferenceCategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<PreferenceItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 使用 Row 替代 FlowRow 避免实验性 API
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    PreferenceChip(
                        name = item.name,
                        score = item.score
                    )
                }
            }
        }
    }
}

/**
 * 偏好标签
 */
@Composable
private fun PreferenceChip(
    name: String,
    score: Int
) {
    val intensity = (score.coerceAtMost(50) / 50f).coerceIn(0.3f, 1f)
    val color = MaterialTheme.colorScheme.primary.copy(alpha = intensity)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${score}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 推荐项
 */
@Composable
private fun RecommendationItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * AI推荐偏好卡片
 */
@Composable
private fun SuggestedPreferencesCard(
    suggested: com.recipe.data.model.SuggestedPreferencesResponse,
    onApply: () -> Unit
) {
    val preferences = suggested.preferences

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI推荐偏好",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = suggested.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (preferences != null) {
                Spacer(modifier = Modifier.height(12.dp))

                // 展示推荐偏好摘要
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (preferences.cuisines.isNotEmpty()) {
                        Text(
                            text = "菜系：${preferences.cuisines.joinToString("、")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (preferences.tastes.isNotEmpty()) {
                        Text(
                            text = "口味：${preferences.tastes.joinToString("、")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (preferences.nutritionGoals.isNotEmpty()) {
                        Text(
                            text = "营养：${preferences.nutritionGoals.joinToString("、")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("应用推荐偏好")
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("数据不足，继续互动以获取推荐")
                }
            }
        }
    }
}

// 置信度图标使用 Star
