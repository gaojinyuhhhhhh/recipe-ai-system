package com.recipe.ui.shopping

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.data.model.ShoppingItem
import com.recipe.ui.components.CategoryHeader
import com.recipe.ui.components.EmptyShoppingState
import com.recipe.ui.components.ExpandableCategoryHeader
import com.recipe.ui.components.ShoppingItemCard
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
    var showClearDialog by remember { mutableStateOf(false) }
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
                    if (showCompletedTab) {
                        if (selectedIds.isNotEmpty()) {
                            // 批量同步到食材库
                            IconButton(onClick = { shoppingViewModel.syncSelectedToIngredients() }) {
                                Icon(Icons.Default.Sync, "同步到食材库")
                            }
                        }
                        // 清空已完成按钮
                        if (completedItems.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.DeleteSweep, "清空已完成")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showCompletedTab) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                // 提醒横幅：有待入库的已完成食材
                if (completedItems.isNotEmpty() && !showCompletedTab) {
                    CompletedItemsReminderBanner(
                        count = completedItems.size,
                        onClick = { showCompletedTab = true }
                    )
                }
                
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
                    EmptyShoppingState(
                        isCompletedTab = showCompletedTab,
                        onAddClick = { showAddDialog = true }
                    )
                } else {
                    // 已完成列表按时间倒序排列，待购买按类别分组
                    val sortedItems = if (showCompletedTab) {
                        // 已完成：按 completedAt 时间倒序
                        currentItems.sortedByDescending { it.completedAt ?: it.createdAt ?: "" }
                    } else {
                        // 待购买：保持原顺序
                        currentItems
                    }
                    
                    // 按类别分组显示（使用标准10大类别）
                    val grouped = sortedItems.groupBy { it.getCategoryDisplay() }
                    val categoryOrder = listOf("肉类", "海鲜", "蔬菜类", "水果", "蛋奶", "豆制品", "调味类", "粮油", "干货", "饮品", "未分类")
                    
                    // 按顺序排序，确保有食材的类别排在前面
                    val sortedGroups = categoryOrder.mapNotNull { category ->
                        grouped[category]?.let { category to it }
                    }.toMutableList()
                    
                    // 添加不在标准类别中的其他类别
                    grouped.forEach { (category, items) ->
                        if (category !in categoryOrder) {
                            sortedGroups.add(category to items)
                        }
                    }

                    // 记录展开状态
                    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
                    // 默认展开所有类别
                    LaunchedEffect(sortedGroups) {
                        sortedGroups.forEach { (category, _) ->
                            if (expandedCategories[category] == null) {
                                expandedCategories[category] = true
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        sortedGroups.forEach { (category, items) ->
                            val isExpanded = expandedCategories[category] ?: true
                            
                            // 类别头部（可点击展开/折叠）
                            item(key = "header_$category") {
                                ExpandableCategoryHeader(
                                    category = category, 
                                    count = items.size,
                                    isExpanded = isExpanded,
                                    onToggle = { expandedCategories[category] = !isExpanded },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            
                            // 展开的购物项列表
                            if (isExpanded) {
                                items(items, key = { "${category}_${it.id}" }) { item ->
                                    ShoppingItemCard(
                                        item = item,
                                        isSelected = selectedIds.contains(item.id),
                                        isCompletedTab = showCompletedTab,
                                        onToggleSelect = { item.id?.let { shoppingViewModel.toggleSelection(it) } },
                                        onComplete = { 
                                            if (showCompletedTab) {
                                                // 已完成列表：点击入库，调用AI推断
                                                shoppingViewModel.startProcessingItem(item)
                                            } else {
                                                // 待购买列表：标记完成
                                                item.id?.let { shoppingViewModel.completeSingleItem(it) }
                                            }
                                        },
                                        onDelete = { item.id?.let { shoppingViewModel.deleteItem(it) } },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
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

    // 清空已完成确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空已完成") },
            text = { Text("确定要清空所有已完成的采购项吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        shoppingViewModel.clearAllCompleted()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}



/**
 * 已完成食材提醒横幅
 */
@Composable
fun CompletedItemsReminderBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "有 $count 个已购买食材待入库",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "去处理 →",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
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
    val categories = listOf("肉类", "海鲜", "蔬菜类", "水果", "蛋奶", "豆制品", "调味类", "粮油", "干货", "饮品")
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
