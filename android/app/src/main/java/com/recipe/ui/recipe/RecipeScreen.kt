package com.recipe.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.data.local.LocalRecipeEntity
import com.recipe.data.model.Recipe
import com.recipe.ui.components.EmptyRecipesState
import com.recipe.ui.components.RecipeCard
import com.recipe.viewmodel.RecipeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun RecipeScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToLocalDetail: (Long) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("我的食谱", "食谱社区")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "食谱",
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "创建食谱")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab栏
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.PhoneAndroid else Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(title)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> LocalRecipesTab(
                    recipeViewModel = recipeViewModel,
                    onNavigateToLocalDetail = onNavigateToLocalDetail,
                    onNavigateToCreate = onNavigateToCreate
                )
                1 -> CommunityRecipesTab(
                    recipeViewModel = recipeViewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
        }
    }
}

// ==================== 本地食谱Tab ====================

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun LocalRecipesTab(
    recipeViewModel: RecipeViewModel,
    onNavigateToLocalDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val localRecipes by recipeViewModel.localRecipes.collectAsState()
    val isRefreshing by recipeViewModel.isRefreshing.collectAsState()
    
    // 下拉刷新状态
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { recipeViewModel.refreshLocalRecipes() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (localRecipes.isEmpty()) {
            EmptyRecipesState(onCreateClick = onNavigateToCreate)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(localRecipes) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = { onNavigateToLocalDetail(recipe.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        
        // 下拉刷新指示器
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// ==================== 社区食谱Tab ====================

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CommunityRecipesTab(
    recipeViewModel: RecipeViewModel,
    onNavigateToDetail: (Long) -> Unit
) {
    val hotRecipes by recipeViewModel.hotRecipes.collectAsState()
    val searchResults by recipeViewModel.searchResults.collectAsState()
    val myRecipes by recipeViewModel.myRecipes.collectAsState()
    val isLoading by recipeViewModel.isLoading.collectAsState()
    val isSearching by recipeViewModel.isSearching.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var showMyRecipes by remember { mutableStateOf(false) }
    val difficulties = listOf("EASY" to "简单", "MEDIUM" to "中等", "HARD" to "困难")

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            when {
                showMyRecipes -> recipeViewModel.loadMyRecipes()
                isSearching -> recipeViewModel.searchRecipes(searchText, difficulty = selectedDifficulty)
                else -> recipeViewModel.loadHotRecipes()
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 搜索栏
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索社区食谱...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(onClick = {
                            searchText = ""
                            recipeViewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                enabled = !showMyRecipes
            )

            // 难度筛选 + 我发布的
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = !showMyRecipes && selectedDifficulty == null,
                        onClick = {
                            showMyRecipes = false
                            selectedDifficulty = null
                            recipeViewModel.clearSearch()
                            searchText = ""
                        },
                        label = { Text("全部") }
                    )
                }
                items(difficulties) { (value, label) ->
                    FilterChip(
                        selected = !showMyRecipes && selectedDifficulty == value,
                        onClick = {
                            if (!showMyRecipes) {
                                selectedDifficulty = if (selectedDifficulty == value) null else value
                                recipeViewModel.searchRecipes(
                                    searchText,
                                    difficulty = if (selectedDifficulty == value) value else null
                                )
                            }
                        },
                        label = { Text(label) }
                    )
                }
                item {
                    FilterChip(
                        selected = showMyRecipes,
                        onClick = {
                            showMyRecipes = !showMyRecipes
                            if (showMyRecipes) {
                                selectedDifficulty = null
                                searchText = ""
                                recipeViewModel.clearSearch()
                                recipeViewModel.loadMyRecipes()
                            }
                        },
                        label = { Text("我发布的") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            // 搜索按钮（「我发布的」模式下不显示）
            if (searchText.isNotBlank() && !showMyRecipes) {
                Button(
                    onClick = { recipeViewModel.searchRecipes(searchText, difficulty = selectedDifficulty) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("搜索")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 内容区域
            if (isLoading && hotRecipes.isEmpty() && searchResults.isEmpty() && myRecipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (showMyRecipes) {
                // 我发布的食谱
                if (myRecipes.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CloudUpload, null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "你还没有发布过食谱",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "在「我的食谱」中发布本地食谱到社区吧",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(myRecipes) { recipe ->
                            RecipeCard(
                                recipe = recipe, 
                                onClick = { recipe.id?.let { onNavigateToDetail(it) } },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            } else if (isSearching) {
                if (searchResults.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("没有找到相关食谱", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(searchResults) { recipe ->
                            RecipeCard(
                                recipe = recipe, 
                                onClick = { recipe.id?.let { onNavigateToDetail(it) } },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            } else {
                if (hotRecipes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Public, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("社区暂无食谱", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("把你的本地食谱分享到社区吧", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { recipeViewModel.loadHotRecipes() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("刷新")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(hotRecipes) { recipe ->
                            RecipeCard(
                                recipe = recipe, 
                                onClick = { recipe.id?.let { onNavigateToDetail(it) } },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// ==================== 社区食谱卡片 ====================

@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    viewModel: RecipeViewModel = viewModel()
) {
    val tags = viewModel.parseTags(recipe.tags)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // AI生成标识
                if (recipe.isAiGenerated) {
                    Surface(color = Color(0xFF9C27B0).copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                                tint = Color(0xFF9C27B0), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("AI生成", color = Color(0xFF9C27B0), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                recipe.getAiRatingDisplay()?.let { rating ->
                    val ratingColor = when (recipe.aiRating) {
                        "S" -> Color(0xFFFF6B00); "A" -> Color(0xFF4CAF50)
                        "B" -> Color(0xFF2196F3); else -> Color.Gray
                    }
                    Surface(color = ratingColor.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
                        Text(rating, color = ratingColor, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            recipe.description?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(tags.take(4)) { tag ->
                        SuggestionChip(onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val diffColor = when (recipe.difficulty) {
                        "EASY" -> Color(0xFF4CAF50); "MEDIUM" -> Color(0xFFFF9800)
                        "HARD" -> Color(0xFFF44336); else -> Color.Gray
                    }
                    Surface(color = diffColor.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
                        Text(recipe.getDifficultyDisplay(), color = diffColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    recipe.cookingTime?.let { time ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("${time}分钟", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    recipe.cuisine?.let { c ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(c, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // 显示作者信息
                    recipe.authorName?.let { author ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(author, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${recipe.viewCount}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(14.dp),
                        tint = Color(0xFFE91E63))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${recipe.favoriteCount}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ChatBubble, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${recipe.commentCount}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
