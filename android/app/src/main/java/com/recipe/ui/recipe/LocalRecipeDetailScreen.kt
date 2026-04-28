package com.recipe.ui.recipe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.viewmodel.RecipeViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 本地食谱详情页
 * 支持查看详情、编辑、删除、上传到社区
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRecipeDetailScreen(
    localRecipeId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateToCooking: () -> Unit = {},
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val localRecipe by recipeViewModel.currentLocalRecipe.collectAsState()
    val isLoading by recipeViewModel.isLoading.collectAsState()
    val toastMessage by recipeViewModel.toastMessage.collectAsState()
    val comments by recipeViewModel.recipeComments.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showUnpublishDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(localRecipeId) {
        recipeViewModel.loadLocalRecipeDetail(localRecipeId)
    }

    // 已发布到社区时，自动加载社区评论
    LaunchedEffect(localRecipe) {
        val recipe = localRecipe ?: return@LaunchedEffect
        if (recipe.syncStatus == "UPLOADED" && recipe.serverId != null) {
            recipeViewModel.loadRecipeComments(recipe.serverId)
        }
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
                title = { Text(localRecipe?.title ?: "食谱详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
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
                                    onNavigateToEdit(localRecipeId)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            // 已发布的食谱显示下架选项
                            if (localRecipe?.syncStatus == "UPLOADED") {
                                DropdownMenuItem(
                                    text = { Text("从社区下架", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        showUnpublishDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                            // 未发布或已下架的食谱显示删除选项
                            if (localRecipe?.syncStatus != "UPLOADED") {
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
            localRecipe?.let { recipe ->
                // 只有原创的本地食谱（非下载的）且未上传过的才显示上传按钮
                if (recipe.syncStatus == "LOCAL") {
                    ExtendedFloatingActionButton(
                        onClick = { showUploadDialog = true },
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        text = { Text("发布到社区") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        if (localRecipe == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val recipe = localRecipe!!
            val ingredients = recipeViewModel.parseIngredients(recipe.ingredients)
            val steps = recipeViewModel.parseSteps(recipe.steps)
            val tags = recipeViewModel.parseTags(recipe.tags)
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

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
                            Text(desc, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 状态标签
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 同步状态
                            val (statusText, statusColor, statusIcon) = when (recipe.syncStatus) {
                                "UPLOADED" -> Triple("已发布到社区", Color(0xFF4CAF50), Icons.Default.CloudDone)
                                "DOWNLOADED" -> Triple("从社区下载", Color(0xFF2196F3), Icons.Default.CloudDownload)
                                else -> Triple("仅本地保存", Color(0xFFFF9800), Icons.Default.PhoneAndroid)
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(statusText) },
                                leadingIcon = {
                                    Icon(statusIcon, null, modifier = Modifier.size(16.dp), tint = statusColor)
                                }
                            )

                            // 难度
                            val diffColor = when (recipe.difficulty) {
                                "EASY" -> Color(0xFF4CAF50); "MEDIUM" -> Color(0xFFFF9800)
                                "HARD" -> Color(0xFFF44336); else -> Color.Gray
                            }
                            val diffDisplay = when (recipe.difficulty) {
                                "EASY" -> "简单"; "MEDIUM" -> "中等"; "HARD" -> "困难"; else -> recipe.difficulty
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(diffDisplay) },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp), tint = diffColor)
                                }
                            )

                            recipe.cookingTime?.let { time ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${time}分钟") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }

                        // 来源
                        recipe.originalAuthor?.let { author ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("原作者: $author", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // 标签
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(tags) { tag ->
                                    SuggestionChip(onClick = {}, label = { Text(tag) })
                                }
                            }
                        }

                        // 时间信息
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "创建: ${dateFormat.format(Date(recipe.createdAt))}  更新: ${dateFormat.format(Date(recipe.updatedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 食材清单
                item {
                    Text("食材清单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (ingredients.isEmpty()) {
                    item { Text("暂无食材信息", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(ingredients) { ing ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                Text("•  ", color = MaterialTheme.colorScheme.primary)
                                Text(ing.getDisplayText(), style = MaterialTheme.typography.bodyMedium)
                            }
                            // 显示备注（如果有）
                            ing.notes?.takeIf { it.isNotBlank() }?.let { note ->
                                Text(note, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 烹饪步骤
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("烹饪步骤", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        // 开始烹饪按钮
                        if (steps.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    com.recipe.viewmodel.CookingSessionHolder.set(
                                        title = recipe.title,
                                        steps = steps,
                                        localRecipeId = localRecipeId
                                    )
                                    onNavigateToCooking()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("开始烹饪", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (steps.isEmpty()) {
                    item { Text("暂无步骤信息", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    itemsIndexed(steps) { index, step ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
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
                                    Text(step.content, style = MaterialTheme.typography.bodyMedium)
                                    // 显示时长和温度（如果有）
                                    val stepInfo = buildList {
                                        step.duration?.let { dur ->
                                            add(if (dur >= 60) "约${dur / 60}分钟" else "约${dur}秒")
                                        }
                                        step.temperature?.takeIf { it.isNotBlank() }?.let { temp ->
                                            add(temp)
                                        }
                                    }
                                    if (stepInfo.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            stepInfo.joinToString(" | "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // 显示提示（如果有）
                                    step.tips?.takeIf { it.isNotBlank() }?.let { tips ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "💡 $tips",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 社区评论区（仅已发布到社区的食谱显示）
                if (recipe.syncStatus == "UPLOADED") {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ChatBubbleOutline, null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "社区评论 (${comments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (comments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubbleOutline, null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("暂无评论", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "快去食谱社区看看吧",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(comments) { comment ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AccountCircle, null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                comment.username ?: "匿名用户",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        comment.rating?.let { rating ->
                                            Row {
                                                repeat(5) { i ->
                                                    Icon(
                                                        if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                        null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = if (i < rating) Color(0xFFFFC107) else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                                    comment.createdAt?.let { time ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            time.replace("T", " ").take(16),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
            text = { Text("删除后无法恢复，确定要删除这个本地食谱吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        recipeViewModel.deleteLocalRecipe(localRecipeId)
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

    // 上传确认对话框
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            icon = { Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("发布到食谱社区") },
            text = {
                Column {
                    Text("发布后，所有用户都可以看到、收藏和评论你的食谱。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("本地食谱仍会保留。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUploadDialog = false
                        recipeViewModel.uploadToCommuity(localRecipeId)
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("确认发布")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) { Text("取消") }
            }
        )
    }

    // 下架确认对话框
    if (showUnpublishDialog) {
        AlertDialog(
            onDismissRequest = { showUnpublishDialog = false },
            icon = { Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("从社区下架") },
            text = {
                Column {
                    Text("下架后，其他用户将无法在社区中看到此食谱。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("下架后你可以：", style = MaterialTheme.typography.bodySmall)
                    Text("• 在本地继续查看和编辑", style = MaterialTheme.typography.bodySmall)
                    Text("• 删除该食谱", style = MaterialTheme.typography.bodySmall)
                    Text("• 重新发布到社区", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("确定要下架吗？", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpublishDialog = false
                        localRecipe?.serverId?.let { serverId ->
                            recipeViewModel.unpublishRecipe(serverId, localRecipeId)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("确认下架")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpublishDialog = false }) { Text("取消") }
            }
        )
    }
}
