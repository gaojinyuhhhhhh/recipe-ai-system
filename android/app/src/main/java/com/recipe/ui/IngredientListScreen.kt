package com.recipe.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

// 视图模式枚举
enum class IngredientViewMode {
    BY_FRESHNESS,  // 按新鲜度分组
    BY_CATEGORY    // 按类别分组
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun IngredientListScreen(
    viewModel: IngredientViewModel = viewModel(),
    onNavigateToCamera: () -> Unit = {},
    onNavigateToRecommend: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val ingredientsByFreshness by viewModel.ingredientsByFreshness.collectAsState()
    val ingredientsByCategory by viewModel.ingredientsByCategory.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var viewMode by remember { mutableStateOf(IngredientViewMode.BY_FRESHNESS) }

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
        viewModel.loadIngredientsByFreshness()
        viewModel.loadIngredientsByCategory()
    }

    // 下拉刷新状态
    val pullRefreshState = rememberPullRefreshState(
        refreshing = loading,
        onRefresh = {
            viewModel.loadIngredients()
            viewModel.loadAlerts()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的食材") },
                actions = {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(Icons.Default.SmartToy, "AI助手")
                    }
                    IconButton(onClick = onNavigateToRecommend) {
                        Icon(Icons.Default.AutoAwesome, "智能推荐")
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 过期提醒区域
                if (alerts.isNotEmpty()) {
                    AlertSection(alerts = alerts)
                }

                // Tab切换
                TabRow(
                    selectedTabIndex = viewMode.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = viewMode == IngredientViewMode.BY_FRESHNESS,
                        onClick = { viewMode = IngredientViewMode.BY_FRESHNESS },
                        text = { Text("按新鲜度") },
                        icon = { Icon(Icons.Default.Timer, null) }
                    )
                    Tab(
                        selected = viewMode == IngredientViewMode.BY_CATEGORY,
                        onClick = { viewMode = IngredientViewMode.BY_CATEGORY },
                        text = { Text("按类别") },
                        icon = { Icon(Icons.Default.Category, null) }
                    )
                }

                // 食材列表
                when {
                    loading && ingredients.isEmpty() -> {
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
                        // 根据视图模式显示不同列表
                        when (viewMode) {
                            IngredientViewMode.BY_FRESHNESS -> {
                                FreshnessGroupedList(
                                    groupedIngredients = ingredientsByFreshness,
                                    onItemClick = { editingIngredient = it },
                                    onConsume = { viewModel.consumeIngredient(it.id!!) },
                                    onDelete = { viewModel.deleteIngredient(it.id!!) }
                                )
                            }
                            IngredientViewMode.BY_CATEGORY -> {
                                CategoryGroupedList(
                                    groupedIngredients = ingredientsByCategory,
                                    onItemClick = { editingIngredient = it },
                                    onConsume = { viewModel.consumeIngredient(it.id!!) },
                                    onDelete = { viewModel.deleteIngredient(it.id!!) }
                                )
                            }
                        }
                    }
                }
            }

            // 下拉刷新指示器
            PullRefreshIndicator(
                refreshing = loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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
    onDelete: () -> Unit,
    highlight: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (highlight) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        } else CardDefaults.cardColors()
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

/**
 * 按新鲜度分组的食材列表
 */
@Composable
fun FreshnessGroupedList(
    groupedIngredients: Map<String, List<Ingredient>>,
    onItemClick: (Ingredient) -> Unit,
    onConsume: (Ingredient) -> Unit,
    onDelete: (Ingredient) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 即将过期 (3天内)
        val expiringSoon = groupedIngredients["expiringSoon"] ?: emptyList()
        if (expiringSoon.isNotEmpty()) {
            item {
                FreshnessSectionHeader(
                    title = "即将过期 (${expiringSoon.size})",
                    color = Color(0xFFE53935),  // 红色
                    icon = Icons.Default.Warning
                )
            }
            items(expiringSoon, key = { "exp_${it.id}" }) { ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    onClick = { onItemClick(ingredient) },
                    onConsume = { onConsume(ingredient) },
                    onDelete = { onDelete(ingredient) },
                    highlight = true
                )
            }
        }

        // 新鲜食材 (4-7天)
        val fresh = groupedIngredients["fresh"] ?: emptyList()
        if (fresh.isNotEmpty()) {
            item {
                FreshnessSectionHeader(
                    title = "新鲜食材 (${fresh.size})",
                    color = Color(0xFFFFA726),  // 橙色
                    icon = Icons.Default.CheckCircle
                )
            }
            items(fresh, key = { "fresh_${it.id}" }) { ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    onClick = { onItemClick(ingredient) },
                    onConsume = { onConsume(ingredient) },
                    onDelete = { onDelete(ingredient) }
                )
            }
        }

        // 长期保存 (7天以上)
        val longTerm = groupedIngredients["longTerm"] ?: emptyList()
        if (longTerm.isNotEmpty()) {
            item {
                FreshnessSectionHeader(
                    title = "长期保存 (${longTerm.size})",
                    color = Color(0xFF66BB6A),  // 绿色
                    icon = Icons.Default.CalendarToday
                )
            }
            items(longTerm, key = { "long_${it.id}" }) { ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    onClick = { onItemClick(ingredient) },
                    onConsume = { onConsume(ingredient) },
                    onDelete = { onDelete(ingredient) }
                )
            }
        }

        // 底部留白
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * 新鲜度分组头部
 */
@Composable
fun FreshnessSectionHeader(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.3f)
        )
    }
}

/**
 * 按类别分组的食材列表
 */
@Composable
fun CategoryGroupedList(
    groupedIngredients: Map<String, List<Ingredient>>,
    onItemClick: (Ingredient) -> Unit,
    onConsume: (Ingredient) -> Unit,
    onDelete: (Ingredient) -> Unit
) {
    // 类别顺序和图标映射（标准9大类别）
    val categoryOrder = listOf(
        "肉类" to Icons.Default.Restaurant,
        "海鲜" to Icons.Default.Water,
        "蔬菜类" to Icons.Default.Spa,
        "水果" to Icons.Default.ShoppingBasket,
        "蛋奶" to Icons.Default.Egg,
        "豆制品" to Icons.Default.Grain,
        "调味类" to Icons.Default.LocalDining,
        "粮油" to Icons.Default.Grass,
        "干货" to Icons.Default.Inventory
    )

    // 获取图标映射
    fun getCategoryIcon(categoryName: String): androidx.compose.ui.graphics.vector.ImageVector {
        return categoryOrder.find { it.first == categoryName }?.second 
            ?: Icons.Default.Folder  // 默认图标
    }

    // 调试：打印所有返回的类别
    LaunchedEffect(groupedIngredients) {
        android.util.Log.d("CategoryGroupedList", "返回的类别: ${groupedIngredients.keys}")
        groupedIngredients.forEach { (k, v) ->
            android.util.Log.d("CategoryGroupedList", "类别 '$k': ${v.size} 个食材")
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 先显示标准9大类别（按预设顺序），包括空的类别
        categoryOrder.forEach { (categoryName, icon) ->
            val ingredients = groupedIngredients[categoryName] ?: emptyList()
            // 始终显示该类别，即使为空
            item {
                CategorySectionHeader(
                    title = "$categoryName (${ingredients.size})",
                    icon = icon
                )
            }
            if (ingredients.isNotEmpty()) {
                items(ingredients, key = { "${categoryName}_${it.id}" }) { ingredient ->
                    IngredientItem(
                        ingredient = ingredient,
                        onClick = { onItemClick(ingredient) },
                        onConsume = { onConsume(ingredient) },
                        onDelete = { onDelete(ingredient) }
                    )
                }
            }
        }

        // 显示其他非标准类别（如数据库中有但不在标准9类中的）
        val standardCategories = categoryOrder.map { it.first } + "未分类"
        groupedIngredients.forEach { (categoryName, ingredients) ->
            if (categoryName !in standardCategories && ingredients.isNotEmpty()) {
                item {
                    CategorySectionHeader(
                        title = "$categoryName (${ingredients.size})",
                        icon = getCategoryIcon(categoryName)
                    )
                }
                items(ingredients, key = { "${categoryName}_${it.id}" }) { ingredient ->
                    IngredientItem(
                        ingredient = ingredient,
                        onClick = { onItemClick(ingredient) },
                        onConsume = { onConsume(ingredient) },
                        onDelete = { onDelete(ingredient) }
                    )
                }
            }
        }

        // 未分类（始终显示）
        val uncategorized = groupedIngredients["未分类"] ?: emptyList()
        item {
            CategorySectionHeader(
                title = "未分类 (${uncategorized.size})",
                icon = Icons.Default.HelpOutline
            )
        }
        if (uncategorized.isNotEmpty()) {
            items(uncategorized, key = { "uncat_${it.id}" }) { ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    onClick = { onItemClick(ingredient) },
                    onConsume = { onConsume(ingredient) },
                    onDelete = { onDelete(ingredient) }
                )
            }
        }

        // 底部留白
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * 类别分组头部
 */
@Composable
fun CategorySectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
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
