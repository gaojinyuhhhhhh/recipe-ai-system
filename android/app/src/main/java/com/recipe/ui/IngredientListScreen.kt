package com.recipe.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.recipe.R
import com.recipe.data.model.ExpiryAlert
import com.recipe.data.model.Ingredient
import com.recipe.ui.ingredient.AddIngredientDialog
import com.recipe.ui.ingredient.EditIngredientDialog
import com.recipe.viewmodel.IngredientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientListScreen(
    viewModel: IngredientViewModel = viewModel(),
    onNavigateToCamera: () -> Unit = {}
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingIngredient by remember { mutableStateOf<Ingredient?>(null) }

    val context = LocalContext.current

    // Toast 提示
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadIngredients()
        viewModel.loadAlerts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的食材") },
                actions = {
                    IconButton(onClick = onNavigateToCamera) {
                        Icon(Icons.Default.CameraAlt, "拍照识别")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "添加食材")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 过期提醒区域
            if (alerts.isNotEmpty()) {
                AlertSection(alerts = alerts)
            }

            // 食材列表
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ingredients.isEmpty() -> {
                    // 空状态
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Kitchen, null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "冰箱还是空的",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "点击 + 手动添加，或拍照识别食材",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("手动添加")
                                }
                                OutlinedButton(onClick = onNavigateToCamera) {
                                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("拍照识别")
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ingredients, key = { it.id ?: 0 }) { ingredient ->
                            IngredientItem(
                                ingredient = ingredient,
                                onClick = { editingIngredient = ingredient },
                                onConsume = { viewModel.consumeIngredient(ingredient.id!!) },
                                onDelete = { viewModel.deleteIngredient(ingredient.id!!) }
                            )
                        }
                        // 底部留白，避免被FAB遮挡
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // 添加对话框
    if (showAddDialog) {
        AddIngredientDialog(
            onDismiss = { showAddDialog = false },
            onAddSingle = { name, category, qty, unit, expiry, storage ->
                viewModel.addIngredient(name, category, qty, unit, expiry, storage)
            },
            onAddBatch = { text -> viewModel.batchAddIngredients(text) }
        )
    }

    // 编辑对话框
    editingIngredient?.let { ingredient ->
        EditIngredientDialog(
            ingredient = ingredient,
            onDismiss = { editingIngredient = null },
            onSave = { viewModel.updateIngredient(it) },
            onDelete = { viewModel.deleteIngredient(it) }
        )
    }
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    onClick: () -> Unit,
    onConsume: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 食材图片
            AsyncImage(
                model = ingredient.imageUrl ?: R.drawable.ic_ingredient_placeholder,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 食材信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (ingredient.isConsumed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "已消耗",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 分类+数量
                val infoText = buildString {
                    ingredient.category?.let { append(it) }
                    ingredient.quantity?.let { q ->
                        if (isNotEmpty()) append(" · ")
                        append(q.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() })
                        ingredient.unit?.let { append(it) }
                    }
                    ingredient.storageMethod?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                }
                if (infoText.isNotBlank()) {
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 过期状态
                ingredient.getRemainingDays()?.let { days ->
                    Text(
                        text = when {
                            days < 0 -> "已过期${-days}天"
                            days == 0 -> "今天到期！"
                            days == 1 -> "明天到期"
                            days <= 7 -> "还剩${days}天"
                            else -> "剩余${days}天"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ingredient.getPriorityColor()
                    )
                }
            }

            // 操作按钮
            if (!ingredient.isConsumed) {
                IconButton(onClick = onConsume) {
                    Icon(
                        Icons.Default.CheckCircle, "消耗",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AlertSection(alerts: List<ExpiryAlert>) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "过期提醒 (${alerts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    alerts.forEach { alert ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                text = "• ${alert.message}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        alert.quickSolution?.let {
                            Text(
                                text = "  💡 $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
