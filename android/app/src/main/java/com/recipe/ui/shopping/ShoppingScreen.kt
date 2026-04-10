package com.recipe.ui.shopping

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.data.model.ShoppingItem
import com.recipe.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ShoppingScreen(
    shoppingViewModel: ShoppingViewModel = viewModel()
) {
    val pendingItems by shoppingViewModel.pendingItems.collectAsState()
    val completedItems by shoppingViewModel.completedItems.collectAsState()
    val isLoading by shoppingViewModel.isLoading.collectAsState()
    val toastMessage by shoppingViewModel.toastMessage.collectAsState()
    val selectedIds by shoppingViewModel.selectedIds.collectAsState()
    val currentProcessingItem by shoppingViewModel.currentProcessingItem.collectAsState()
    val inferredInfo by shoppingViewModel.inferredInfo.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showCompletedTab by remember { mutableStateOf(false) }

    // Toast
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            shoppingViewModel.clearToast()
        }
    }

    // 加载已完成列表
    LaunchedEffect(showCompletedTab) {
        if (showCompletedTab) {
            shoppingViewModel.loadCompletedItems()
        }
    }

    // 下拉刷新状态
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            if (showCompletedTab) {
                shoppingViewModel.loadCompletedItems()
            } else {
                shoppingViewModel.loadPendingItems()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("购物清单") },
                actions = {
                    if (!showCompletedTab && selectedIds.isNotEmpty()) {
                        // 批量完成按钮
                        IconButton(onClick = { shoppingViewModel.completeSelected() }) {
                            Icon(Icons.Default.CheckCircle, "批量完成")
                        }
                    }
                    if (showCompletedTab && selectedIds.isNotEmpty()) {
                        // 批量同步到食材库
                        IconButton(onClick = { shoppingViewModel.syncSelectedToIngredients() }) {
                            Icon(Icons.Default.Sync, "同步到食材库")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showCompletedTab) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, "添加采购项")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab切换
                TabRow(
                    selectedTabIndex = if (showCompletedTab) 1 else 0
                ) {
                    Tab(
                        selected = !showCompletedTab,
                        onClick = {
                            showCompletedTab = false
                            shoppingViewModel.clearSelection()
                        },
                        text = {
                            Text("待购买 (${pendingItems.size})")
                        }
                    )
                    Tab(
                        selected = showCompletedTab,
                        onClick = {
                            showCompletedTab = true
                            shoppingViewModel.clearSelection()
                        },
                        text = {
                            Text("已完成 (${completedItems.size})")
                        }
                    )
                }

                // 批量操作栏
                val currentItems = if (showCompletedTab) completedItems else pendingItems
                if (currentItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIds.size == currentItems.size && currentItems.isNotEmpty(),
                                onCheckedChange = { shoppingViewModel.toggleSelectAll() }
                            )
                            Text(
                                text = if (selectedIds.isEmpty()) "全选" else "已选${selectedIds.size}项",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (showCompletedTab && selectedIds.isNotEmpty()) {
                            TextButton(onClick = { shoppingViewModel.syncSelectedToIngredients() }) {
                                Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("同步到食材库")
                            }
                        }
                        if (!showCompletedTab && selectedIds.isNotEmpty()) {
                            TextButton(onClick = { shoppingViewModel.completeSelected() }) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("标记完成")
                            }
                        }
                    }
                }

                // 列表内容
                if (isLoading && currentItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (currentItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (showCompletedTab) "暂无已完成的采购项" else "购物清单为空",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!showCompletedTab) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击右下角 + 添加采购项",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    // 按类别分组显示（使用标准9大类别）
                    val grouped = currentItems.groupBy { it.getCategoryDisplay() }
                    val categoryOrder = listOf("肉类", "海鲜", "蔬菜类", "水果", "蛋奶", "豆制品", "调味类", "粮油", "干货", "未分类")
                    val sortedGroups = grouped.entries.sortedBy {
                        val idx = categoryOrder.indexOf(it.key)
                        if (idx >= 0) idx else 99
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sortedGroups.forEach { (category, items) ->
                            item {
                                CategoryHeader(category = category, count = items.size)
                            }
                            items(items, key = { it.id ?: 0L }) { item ->
                                ShoppingItemCard(
                                    item = item,
                                    isSelected = selectedIds.contains(item.id),
                                    isCompletedTab = showCompletedTab,
                                    onToggleSelect = { item.id?.let { shoppingViewModel.toggleSelection(it) } },
                                    onComplete = { shoppingViewModel.startProcessingItem(item) },
                                    onDelete = { item.id?.let { shoppingViewModel.deleteItem(it) } }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // 下拉刷新指示器
            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // 添加对话框
    if (showAddDialog) {
        AddShoppingItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, quantity, unit, category ->
                shoppingViewModel.addItem(name, quantity, unit, category)
                showAddDialog = false
            }
        )
    }

    // AI推断确认对话框
    if (currentProcessingItem != null && inferredInfo != null) {
        IngredientConfirmDialog(
            item = currentProcessingItem!!,
            inferredInfo = inferredInfo!!,
            onDismiss = { shoppingViewModel.cancelProcessing() },
            onConfirm = { info ->
                shoppingViewModel.confirmAndAddToIngredients(info)
            }
        )
    }
}

@Composable
fun CategoryHeader(category: String, count: Int) {
    // 标准9大类别颜色和图标
    val color = when (category) {
        "肉类" -> Color(0xFFE53935)
        "海鲜" -> Color(0xFF1E88E5)
        "蔬菜类" -> Color(0xFF43A047)
        "水果" -> Color(0xFFFB8C00)
        "蛋奶" -> Color(0xFFFDD835)
        "豆制品" -> Color(0xFF8E24AA)
        "调味类" -> Color(0xFF6D4C41)
        "粮油" -> Color(0xFFFFA726)
        "干货" -> Color(0xFF78909C)
        else -> Color(0xFF607D8B)
    }
    val icon = when (category) {
        "肉类" -> Icons.Default.Restaurant
        "海鲜" -> Icons.Default.Water
        "蔬菜类" -> Icons.Default.Spa
        "水果" -> Icons.Default.ShoppingBasket
        "蛋奶" -> Icons.Default.Egg
        "豆制品" -> Icons.Default.Grain
        "调味类" -> Icons.Default.LocalDining
        "粮油" -> Icons.Default.Grass
        "干货" -> Icons.Default.Inventory
        else -> Icons.Default.Category
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    isSelected: Boolean,
    isCompletedTab: Boolean,
    onToggleSelect: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "bgColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleSelect() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择框 / 完成勾选
            if (!isCompletedTab) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCompletedTab) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompletedTab) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.getQuantityDisplay().isNotEmpty()) {
                        Text(
                            text = item.getQuantityDisplay(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (item.aiAdvice != null) {
                        Text(
                            text = item.aiAdvice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 操作按钮
            if (!isCompletedTab) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Done,
                        "完成",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${item.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AddShoppingItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: Double?, unit: String?, category: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // 标准9大类别，与食材库分类展示保持一致
    val categories = listOf("肉类", "海鲜", "蔬菜类", "水果", "蛋奶", "豆制品", "调味类", "粮油", "干货")
    val units = listOf("g", "kg", "ml", "L", "个", "根", "把", "包", "瓶", "盒")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加采购项") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("食材名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 数量和单位
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("数量") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    // 单位选择
                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("单位") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 类别选择 - 分两行显示
                Text("类别", style = MaterialTheme.typography.bodyMedium)
                val firstRow = categories.take(5)
                val secondRow = categories.drop(5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        firstRow.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                                label = { Text(cat, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        secondRow.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                                label = { Text(cat, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }

                Text(
                    "提示：不填数量时，系统会根据AI推荐采购量",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            quantity.toDoubleOrNull(),
                            unit.ifBlank { null },
                            selectedCategory
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
