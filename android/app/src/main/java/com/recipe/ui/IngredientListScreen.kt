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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.recipe.R
import com.recipe.data.model.ExpiryAlert
import com.recipe.data.model.Ingredient
import com.recipe.ui.components.EmptyIngredientsState
import com.recipe.ui.components.IngredientCard
import com.recipe.ui.components.SecondaryButton
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
    val error by viewModel.error.collectAsState()

    // Toast 提示
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 初始加载（确保页面首次显示时一定会加载数据）
    LaunchedEffect(Unit) {
        viewModel.loadIngredients()
        viewModel.loadAlerts()
    }

    // 使用 lifecycleCurrentState 作为 key，确保每次页面可见时重新加载数据
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 监听页面可见状态，每次从其他页面返回时刷新数据
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadIngredients()
                viewModel.loadAlerts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 下拉刷新状态（刷新所有数据源）
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
                title = { 
                    Text(
                        "我的食材",
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
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
                    !loading && error != null && ingredients.isEmpty() -> {
                        // 加载出错状态
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = error ?: "加载失败",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = {
                                    viewModel.clearError()
                                    viewModel.loadIngredients()
                                    viewModel.loadAlerts()
                                }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                    ingredients.isEmpty() -> {
                        // 空状态
                        EmptyIngredientsState(
                            onAddClick = { showAddDialog = true }
                        )
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



/**
 * 按新鲜度分组的食材列表（支持展开/折叠）
 */
@Composable
fun FreshnessGroupedList(
    groupedIngredients: Map<String, List<Ingredient>>,
    onItemClick: (Ingredient) -> Unit,
    onConsume: (Ingredient) -> Unit,
    onDelete: (Ingredient) -> Unit
) {
    // 新鲜度分组顺序和配置
    val freshnessGroups = listOf(
        Triple("expiringSoon", "即将过期", Color(0xFFE53935)),  // 红色
        Triple("fresh", "新鲜食材", Color(0xFFFFA726)),         // 橙色
        Triple("longTerm", "长期保存", Color(0xFF66BB6A))       // 绿色
    )
    
    val groupIcons = mapOf(
        "expiringSoon" to Icons.Default.Warning,
        "fresh" to Icons.Default.CheckCircle,
        "longTerm" to Icons.Default.CalendarToday
    )
    
    // 记录每个分组的展开状态
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    // 默认展开所有有食材的分组
    LaunchedEffect(groupedIngredients) {
        freshnessGroups.forEach { (key, _, _) ->
            val ingredients = groupedIngredients[key] ?: emptyList()
            if (expandedGroups[key] == null) {
                expandedGroups[key] = ingredients.isNotEmpty()
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        freshnessGroups.forEach { (key, title, color) ->
            val ingredients = groupedIngredients[key] ?: emptyList()
            val isExpanded = expandedGroups[key] ?: true
            
            // 分组头部（可点击展开/折叠）
            item(key = "header_$key") {
                FreshnessSectionHeader(
                    title = title,
                    count = ingredients.size,
                    color = color,
                    icon = groupIcons[key] ?: Icons.Default.Folder,
                    isExpanded = isExpanded,
                    onToggle = { expandedGroups[key] = !isExpanded }
                )
            }
            
            // 展开的食材列表
            if (isExpanded && ingredients.isNotEmpty()) {
                items(ingredients, key = { "${key}_${it.id}" }) { ingredient ->
                    IngredientCard(
                        ingredient = ingredient,
                        onClick = { onItemClick(ingredient) },
                        onConsume = { onConsume(ingredient) },
                        onDelete = { onDelete(ingredient) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 底部留白
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * 新鲜度分组头部（可点击展开/折叠）
 */
@Composable
fun FreshnessSectionHeader(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0) 
                color.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            // 展开/折叠图标
            if (count > 0) {
                Icon(
                    imageVector = if (isExpanded) 
                        Icons.Default.ExpandLess 
                    else 
                        Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
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
    // 类别顺序和 Emoji 映射（标准10大类别，与 IngredientCategoryIcon 共用同一套 Emoji）
    val categoryOrder = listOf(
        "肉类", "海鲜", "蔬菜类", "水果", "蛋奶",
        "豆制品", "调味类", "粮油", "干货", "饮品"
    )

    // 获取类别 Emoji（复用 RecipeCards.kt 中的统一映射）
    fun getCategoryEmoji(categoryName: String): String {
        return com.recipe.ui.components.getCategoryEmojiAndColor(categoryName).first
    }

    // 记录每个类别的展开状态
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    // 默认展开有食材的类别
    LaunchedEffect(groupedIngredients) {
        categoryOrder.forEach { categoryName ->
            val ingredients = groupedIngredients[categoryName] ?: emptyList()
            if (expandedCategories[categoryName] == null) {
                expandedCategories[categoryName] = ingredients.isNotEmpty()
            }
        }
        // 未分类默认展开
        if (expandedCategories["未分类"] == null) {
            expandedCategories["未分类"] = (groupedIngredients["未分类"] ?: emptyList()).isNotEmpty()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 先显示标准10大类别（按预设顺序），包括空的类别
        categoryOrder.forEach { categoryName ->
            val ingredients = groupedIngredients[categoryName] ?: emptyList()
            val emoji = getCategoryEmoji(categoryName)
            val isExpanded = expandedCategories[categoryName] ?: true
            
            // 类别头部（可点击展开/折叠）
            item(key = "header_$categoryName") {
                CategorySectionHeader(
                    title = categoryName,
                    count = ingredients.size,
                    emoji = emoji,
                    isExpanded = isExpanded,
                    onToggle = { expandedCategories[categoryName] = !isExpanded }
                )
            }
            
            // 展开的食材列表
            if (isExpanded && ingredients.isNotEmpty()) {
                items(ingredients, key = { "${categoryName}_${it.id}" }) { ingredient ->
                    IngredientCard(
                        ingredient = ingredient,
                        onClick = { onItemClick(ingredient) },
                        onConsume = { onConsume(ingredient) },
                        onDelete = { onDelete(ingredient) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 显示其他非标准类别（如数据库中有但不在标准10类中的）
        val standardCategories = categoryOrder + "未分类"
        groupedIngredients.forEach { (categoryName, ingredients) ->
            if (categoryName !in standardCategories && ingredients.isNotEmpty()) {
                val isExpanded = expandedCategories[categoryName] ?: true
                
                item(key = "header_$categoryName") {
                    CategorySectionHeader(
                        title = categoryName,
                        count = ingredients.size,
                        emoji = getCategoryEmoji(categoryName),
                        isExpanded = isExpanded,
                        onToggle = { expandedCategories[categoryName] = !isExpanded }
                    )
                }
                
                if (isExpanded) {
                    items(ingredients, key = { "${categoryName}_${it.id}" }) { ingredient ->
                        IngredientCard(
                            ingredient = ingredient,
                            onClick = { onItemClick(ingredient) },
                            onConsume = { onConsume(ingredient) },
                            onDelete = { onDelete(ingredient) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 未分类
        val uncategorized = groupedIngredients["未分类"] ?: emptyList()
        val isUncategorizedExpanded = expandedCategories["未分类"] ?: true
        
        item(key = "header_uncategorized") {
            CategorySectionHeader(
                title = "未分类",
                count = uncategorized.size,
                emoji = "❓",
                isExpanded = isUncategorizedExpanded,
                onToggle = { expandedCategories["未分类"] = !isUncategorizedExpanded }
            )
        }
        
        if (isUncategorizedExpanded && uncategorized.isNotEmpty()) {
            items(uncategorized, key = { "uncat_${it.id}" }) { ingredient ->
                IngredientCard(
                    ingredient = ingredient,
                    onClick = { onItemClick(ingredient) },
                    onConsume = { onConsume(ingredient) },
                    onDelete = { onDelete(ingredient) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // 底部留白
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * 类别分组头部（可点击展开/折叠）
 */
@Composable
fun CategorySectionHeader(
    title: String,
    count: Int,
    emoji: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    // 获取该类别对应的主题色
    val (_, themeColor) = com.recipe.ui.components.getCategoryEmojiAndColor(title)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0) 
                themeColor.copy(alpha = 0.08f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji 图标
            Text(
                text = emoji,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (count > 0) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (count > 0) 
                    themeColor
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            // 展开/折叠图标
            if (count > 0) {
                Icon(
                    imageVector = if (isExpanded) 
                        Icons.Default.ExpandLess 
                    else 
                        Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
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
