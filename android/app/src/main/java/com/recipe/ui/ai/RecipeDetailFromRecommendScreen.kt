package com.recipe.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.viewmodel.GeneratedRecipeDetail
import com.recipe.viewmodel.RecipeDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailFromRecommendScreen(
    recipeName: String,
    mainIngredients: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToMyRecipes: () -> Unit = {},
    onNavigateToLocalRecipes: () -> Unit = {},
) {
    val context = LocalContext.current.applicationContext as android.app.Application
    val viewModel: RecipeDetailViewModel = viewModel(
        factory = RecipeDetailViewModel.createFactory(context)
    )
    val recipe by viewModel.recipe.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val isImported by viewModel.isImported.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // 进入页面时自动生成完整食谱
    LaunchedEffect(recipeName) {
        if (recipe == null) {
            viewModel.generateFullRecipe(recipeName, mainIngredients)
        }
    }

    // 监听toast消息
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            viewModel.clearToast()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(recipeName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 右上角不放置操作按钮，避免与底部按钮重复
                    // 所有操作（发布到社区、导入本地）统一在底部按钮区域
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    LoadingContent()
                }
                error != null -> {
                    ErrorContent(
                        error = error!!,
                        onRetry = { viewModel.generateFullRecipe(recipeName, mainIngredients) }
                    )
                }
                recipe != null -> {
                    RecipeDetailContent(
                        recipe = recipe!!,
                        isSaved = isSaved,
                        isImported = isImported,
                        onSave = { viewModel.saveRecipe() },
                        onImport = { viewModel.importToLocalRecipes() },
                        onNavigateToMyRecipes = onNavigateToMyRecipes,
                        onNavigateToLocalRecipes = onNavigateToLocalRecipes
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "AI 正在生成完整食谱...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("重试")
            }
        }
    }
}

@Composable
private fun RecipeDetailContent(
    recipe: GeneratedRecipeDetail,
    isSaved: Boolean,
    isImported: Boolean,
    onSave: () -> Unit,
    onImport: () -> Unit,
    onNavigateToMyRecipes: () -> Unit,
    onNavigateToLocalRecipes: () -> Unit
) {
    var showSaveSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题和描述
        Text(
            text = recipe.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (recipe.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 基本信息卡片
        InfoCard(recipe = recipe)

        Spacer(modifier = Modifier.height(16.dp))

        // 食材清单
        IngredientsSection(ingredients = recipe.ingredients)

        Spacer(modifier = Modifier.height(16.dp))

        // 烹饪步骤
        StepsSection(steps = recipe.steps)

        Spacer(modifier = Modifier.height(16.dp))

        // 烹饪技巧
        if (recipe.tips.isNotBlank()) {
            TipsSection(tips = recipe.tips)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 营养信息
        if (recipe.nutrition.isNotBlank()) {
            NutritionSection(nutrition = recipe.nutrition)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 保存和导入按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 保存到云端按钮
            if (!isSaved) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaved  // 保存中/保存后禁用
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发布到社区")
                }
            } else {
                OutlinedButton(
                    onClick = onNavigateToMyRecipes,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("已发布 - 查看")
                }
            }

            // 导入到本地按钮
            if (!isImported) {
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导入本地")
                }
            } else {
                OutlinedButton(
                    onClick = onNavigateToLocalRecipes,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("已导入 - 查看")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 保存成功提示 - 使用 LaunchedEffect 监听 isSaved 状态变化
    LaunchedEffect(isSaved) {
        if (isSaved) {
            showSaveSuccess = true
            kotlinx.coroutines.delay(2000)
            showSaveSuccess = false
        }
    }
    
    if (showSaveSuccess) {
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("食谱已发布到社区！")
        }
    }
}

@Composable
private fun InfoCard(recipe: GeneratedRecipeDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoItem(
                icon = Icons.Default.Schedule,
                label = "烹饪时间",
                value = "${recipe.cookingTime}分钟"
            )
            InfoItem(
                icon = Icons.Default.SignalCellularAlt,
                label = "难度",
                value = recipe.getDifficultyDisplay()
            )
            InfoItem(
                icon = Icons.Default.People,
                label = "份量",
                value = "${recipe.servings}人份"
            )
        }
    }
}

@Composable
private fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IngredientsSection(ingredients: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "食材清单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            ingredients.forEach { ingredient ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ingredient,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StepsSection(steps: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "烹饪步骤",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TipsSection(tips: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "烹饪技巧",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            MarkdownContent(
                markdown = tips,
                textColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun NutritionSection(nutrition: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "营养信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            MarkdownContent(
                markdown = nutrition,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
