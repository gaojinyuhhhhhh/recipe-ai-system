package com.recipe.ui.recipe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.data.local.TokenManager
import com.recipe.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val detail by recipeViewModel.recipeDetail.collectAsState()
    val isLoading by recipeViewModel.isLoading.collectAsState()
    val toastMessage by recipeViewModel.toastMessage.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var commentRating by remember { mutableStateOf(5) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val currentUserId = TokenManager.getUserId()
    val isOwner = detail?.recipe?.userId == currentUserId

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(recipeId) {
        recipeViewModel.loadRecipeDetail(recipeId)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            recipeViewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(detail?.recipe?.title ?: "食谱详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 收藏按钮
                    detail?.let { d ->
                        IconButton(onClick = { recipeViewModel.toggleFavorite(recipeId) }) {
                            Icon(
                                if (d.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (d.isFavorited) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // 作者操作菜单
                    if (isOwner) {
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("编辑食谱") },
                                    onClick = {
                                        showMoreMenu = false
                                        onNavigateToEdit(recipeId)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除食谱", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (detail != null) {
                FloatingActionButton(
                    onClick = { showCommentDialog = true }
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = "评论")
                }
            }
        }
    ) { padding ->
        if (isLoading && detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("加载失败", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val recipe = detail!!.recipe
            val ingredients = recipeViewModel.parseIngredients(recipe.ingredients)
            val steps = recipeViewModel.parseSteps(recipe.steps)
            val tags = recipeViewModel.parseTags(recipe.tags)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题与基本信息
                item {
                    Column {
                        Text(
                            text = recipe.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        recipe.description?.let { desc ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 信息标签行
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 难度
                            val diffColor = when (recipe.difficulty) {
                                "EASY" -> Color(0xFF4CAF50)
                                "MEDIUM" -> Color(0xFFFF9800)
                                "HARD" -> Color(0xFFF44336)
                                else -> Color.Gray
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(recipe.getDifficultyDisplay()) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Speed,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = diffColor
                                    )
                                }
                            )

                            recipe.cookingTime?.let { time ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${time}分钟") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }

                            recipe.cuisine?.let { cuisine ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(cuisine) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Restaurant,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                        // AI评级
                        recipe.getAiRatingDisplay()?.let { rating ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val ratingColor = when (recipe.aiRating) {
                                "S" -> Color(0xFFFF6B00)
                                "A" -> Color(0xFF4CAF50)
                                "B" -> Color(0xFF2196F3)
                                else -> Color.Gray
                            }
                            Surface(
                                color = ratingColor.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ratingColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "AI评级: $rating",
                                        color = ratingColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 标签
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(tags) { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }

                        // 统计数据
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(Icons.Default.Visibility, "${recipe.viewCount}", "浏览")
                            StatItem(Icons.Default.Favorite, "${recipe.favoriteCount}", "收藏")
                            StatItem(Icons.Default.ChatBubble, "${recipe.commentCount}", "评论")
                        }
                    }
                }

                // 可制作状态
                item {
                    val canMake = detail!!.canMake
                    val missing = detail!!.missingIngredients

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (canMake)
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else
                                Color(0xFFFF9800).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (canMake) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (canMake) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    if (canMake) "食材齐全，可以制作！" else "缺少 ${missing.size} 种食材",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (missing.isNotEmpty()) {
                                    Text(
                                        "缺少: ${missing.joinToString("、")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 食材清单
                item {
                    Text(
                        "食材清单",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (ingredients.isEmpty()) {
                    item {
                        Text("暂无食材信息", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(ingredients) { ing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                Text("•  ", color = MaterialTheme.colorScheme.primary)
                                Text(ing.name, style = MaterialTheme.typography.bodyMedium)
                            }
                            val quantityText = buildString {
                                ing.quantity?.let { append(it) }
                                ing.unit?.let { append(it) }
                            }
                            if (quantityText.isNotBlank()) {
                                Text(
                                    quantityText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 烹饪步骤
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "烹饪步骤",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (steps.isEmpty()) {
                    item {
                        Text("暂无步骤信息", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    itemsIndexed(steps) { index, step ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // 步骤序号
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = step.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    step.duration?.let { dur ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "约 ${if (dur >= 60) "${dur / 60}分钟" else "${dur}秒"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // AI建议
                recipe.aiSuggestion?.takeIf { it.isNotBlank() }?.let { suggestion ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "AI优化建议",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // 底部间距
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，确定要删除这个食谱吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        recipeViewModel.deleteRecipe(recipeId)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    // 评论对话框
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("发表评论") },
            text = {
                Column {
                    // 评分
                    Text("评分", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { commentRating = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (star <= commentRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (star <= commentRating) Color(0xFFFFC107) else Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 评论内容
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("写下你的评价...") },
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        recipeViewModel.addComment(recipeId, commentText, commentRating)
                        commentText = ""
                        commentRating = 5
                        showCommentDialog = false
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Text("发表")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
