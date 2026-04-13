package com.recipe.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.data.model.UserPreferences
import com.recipe.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateBack: () -> Unit = {},
    userViewModel: UserViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val userInfo by userViewModel.userInfo.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val toastMessage by userViewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 从用户信息解析偏好
    var preferences by remember { mutableStateOf(UserPreferences()) }
    // 是否处于编辑模式
    var isEditing by remember { mutableStateOf(false) }
    // 是否已经设置过偏好
    var hasPreferences by remember { mutableStateOf(false) }

    // 加载用户偏好
    LaunchedEffect(Unit) {
        userViewModel.loadUserInfo()
    }

    // 解析用户偏好 —— 必须等数据加载完成后再判断
    LaunchedEffect(userInfo) {
        val info = userInfo ?: return@LaunchedEffect // 数据未加载时不做任何判断

        val prefsJson = info.preferences
        if (!prefsJson.isNullOrBlank() && prefsJson != "{}") {
            preferences = parsePreferences(prefsJson)
            hasPreferences = true
            // 有偏好数据时显示摘要（保存后自动切回查看模式）
            isEditing = false
        } else {
            // 没有设置过偏好，自动进入编辑模式
            hasPreferences = false
            isEditing = true
        }
    }

    // 显示提示
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            userViewModel.clearToast()
            // 保存成功后切换到查看模式
            if (it.contains("成功") || it.contains("已保存")) {
                isEditing = false
                hasPreferences = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("偏好设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                userViewModel.savePreferences(preferences)
                            },
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, "保存")
                            }
                        }
                    } else if (hasPreferences) {
                        // 查看模式下显示编辑按钮
                        TextButton(onClick = { isEditing = true }) {
                            Text("编辑")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 提示说明
            InfoCard(hasPreferences)
            Spacer(modifier = Modifier.height(16.dp))

            // 根据模式显示不同内容
            if (!isEditing && hasPreferences) {
                // 查看模式：显示偏好摘要
                PreferencesSummary(
                    preferences = preferences,
                    onEditClick = { isEditing = true }
                )
            } else {
                // 编辑模式：显示设置表单

            // 1. 口味偏好
            PreferenceSection(
                icon = Icons.Default.Restaurant,
                title = "口味偏好",
                subtitle = "选择你喜欢的菜系和口味"
            ) {
                // 菜系选择
                Text("喜欢的菜系", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                MultiSelectChips(
                    options = UserPreferences.CUISINE_OPTIONS,
                    selected = preferences.cuisines.toSet(),
                    onToggle = { item ->
                        preferences = preferences.copy(
                            cuisines = if (item in preferences.cuisines) {
                                preferences.cuisines - item
                            } else {
                                preferences.cuisines + item
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 口味选择
                Text("口味偏好", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                MultiSelectChips(
                    options = UserPreferences.TASTE_OPTIONS,
                    selected = preferences.tastes.toSet(),
                    onToggle = { item ->
                        preferences = preferences.copy(
                            tastes = if (item in preferences.tastes) {
                                preferences.tastes - item
                            } else {
                                preferences.tastes + item
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 烹饪场景
            PreferenceSection(
                icon = Icons.Default.Timer,
                title = "烹饪场景",
                subtitle = "设置你的烹饪习惯"
            ) {
                // 难度选择
                Text("难度偏好", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SingleSelectChips(
                    options = UserPreferences.DIFFICULTY_OPTIONS.map { it.second },
                    selected = when(preferences.difficulty) {
                        "EASY" -> "简单"
                        "MEDIUM" -> "中等"
                        "HARD" -> "困难"
                        else -> "中等"
                    },
                    onSelect = { selected ->
                        preferences = preferences.copy(
                            difficulty = when(selected) {
                                "简单" -> "EASY"
                                "困难" -> "HARD"
                                else -> "MEDIUM"
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 烹饪时长
                Text("最大烹饪时长", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SingleSelectChips(
                    options = UserPreferences.TIME_OPTIONS.map { "${it}分钟" },
                    selected = "${preferences.maxCookingTime}分钟",
                    onSelect = { selected ->
                        val time = selected.replace("分钟", "").toIntOrNull() ?: 60
                        preferences = preferences.copy(maxCookingTime = time)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 烹饪场景
                Text("常用场景", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SingleSelectChips(
                    options = UserPreferences.COOKING_SCENE_OPTIONS,
                    selected = preferences.cookingScene,
                    onSelect = { selected ->
                        preferences = preferences.copy(cookingScene = selected)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 营养目标
            PreferenceSection(
                icon = Icons.Default.Favorite,
                title = "营养目标",
                subtitle = "你的健康饮食目标"
            ) {
                MultiSelectChips(
                    options = UserPreferences.NUTRITION_GOAL_OPTIONS,
                    selected = preferences.nutritionGoals.toSet(),
                    onToggle = { item ->
                        preferences = preferences.copy(
                            nutritionGoals = if (item in preferences.nutritionGoals) {
                                preferences.nutritionGoals - item
                            } else {
                                preferences.nutritionGoals + item
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. 饮食限制
            PreferenceSection(
                icon = Icons.Default.Kitchen,
                title = "饮食限制",
                subtitle = "忌口食材和饮食禁忌"
            ) {
                // 饮食禁忌（支持预设选项 + 自定义输入）
                Text("饮食禁忌", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                CustomInputChips(
                    options = UserPreferences.DIET_OPTIONS,
                    selected = preferences.diet.toSet(),
                    onToggle = { item ->
                        preferences = preferences.copy(
                            diet = if (item in preferences.diet) {
                                preferences.diet - item
                            } else {
                                preferences.diet + item
                            }
                        )
                    },
                    onAdd = { item ->
                        if (item !in preferences.diet) {
                            preferences = preferences.copy(diet = preferences.diet + item)
                        }
                    },
                    onRemove = { item ->
                        preferences = preferences.copy(diet = preferences.diet - item)
                    },
                    placeholder = "例如：清真、生酮、低碳水..."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 忌口食材（支持预设选项 + 自定义输入）
                Text("忌口食材", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                CustomInputChips(
                    options = UserPreferences.DISLIKE_OPTIONS,
                    selected = preferences.dislikedIngredients.toSet(),
                    onToggle = { item ->
                        preferences = preferences.copy(
                            dislikedIngredients = if (item in preferences.dislikedIngredients) {
                                preferences.dislikedIngredients - item
                            } else {
                                preferences.dislikedIngredients + item
                            }
                        )
                    },
                    onAdd = { item ->
                        if (item !in preferences.dislikedIngredients) {
                            preferences = preferences.copy(dislikedIngredients = preferences.dislikedIngredients + item)
                        }
                    },
                    onRemove = { item ->
                        preferences = preferences.copy(dislikedIngredients = preferences.dislikedIngredients - item)
                    },
                    placeholder = "例如：香菜、榴莲、松花蛋..."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. 烹饪设备
            PreferenceSection(
                icon = Icons.Default.Kitchen,
                title = "烹饪设备",
                subtitle = "你拥有的厨房设备"
            ) {
                MultiSelectChips(
                    options = UserPreferences.EQUIPMENT_OPTIONS,
                    selected = preferences.cookingEquipment.toSet(),
                    onToggle = { item ->
                        preferences = preferences.copy(
                            cookingEquipment = if (item in preferences.cookingEquipment) {
                                preferences.cookingEquipment - item
                            } else {
                                preferences.cookingEquipment + item
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. 家庭信息
            PreferenceSection(
                icon = Icons.Default.Restaurant,
                title = "家庭信息",
                subtitle = "用于推荐合适的份量"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = preferences.familySize.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let {
                                preferences = preferences.copy(familySize = it.coerceIn(1, 20))
                            }
                        },
                        label = { Text("家庭人数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = preferences.cookingFrequency.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let {
                                preferences = preferences.copy(cookingFrequency = it.coerceIn(1, 21))
                            }
                        },
                        label = { Text("每周烹饪(次)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮（仅在编辑模式显示）
            if (isEditing) {
                Button(
                    onClick = {
                        userViewModel.savePreferences(preferences)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存偏好设置")
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PreferencesSummary(
    preferences: UserPreferences,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "我的偏好档案",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 口味偏好
            if (preferences.cuisines.isNotEmpty() || preferences.tastes.isNotEmpty()) {
                SummaryItem(
                    icon = Icons.Default.Restaurant,
                    title = "口味偏好",
                    content = buildString {
                        if (preferences.cuisines.isNotEmpty()) {
                            append("菜系：${preferences.cuisines.joinToString("、")}")
                        }
                        if (preferences.tastes.isNotEmpty()) {
                            if (isNotEmpty()) append("\n")
                            append("口味：${preferences.tastes.joinToString("、")}")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 烹饪场景
            SummaryItem(
                icon = Icons.Default.Timer,
                title = "烹饪习惯",
                content = buildString {
                    append("难度：${when(preferences.difficulty) {
                        "EASY" -> "简单"
                        "HARD" -> "困难"
                        else -> "中等"
                    }}")
                    append(" | 时长：${preferences.maxCookingTime}分钟内")
                    preferences.cookingScene?.let {
                        append("\n场景：$it")
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 营养目标
            if (preferences.nutritionGoals.isNotEmpty()) {
                SummaryItem(
                    icon = Icons.Default.Favorite,
                    title = "营养目标",
                    content = preferences.nutritionGoals.joinToString("、")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 饮食限制
            if (preferences.diet.isNotEmpty() || preferences.dislikedIngredients.isNotEmpty()) {
                SummaryItem(
                    icon = Icons.Default.Kitchen,
                    title = "饮食限制",
                    content = buildString {
                        if (preferences.diet.isNotEmpty()) {
                            append("禁忌：${preferences.diet.joinToString("、")}")
                        }
                        if (preferences.dislikedIngredients.isNotEmpty()) {
                            if (isNotEmpty()) append("\n")
                            append("忌口：${preferences.dislikedIngredients.joinToString("、")}")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 烹饪设备
            if (preferences.cookingEquipment.isNotEmpty()) {
                SummaryItem(
                    icon = Icons.Default.Kitchen,
                    title = "烹饪设备",
                    content = preferences.cookingEquipment.joinToString("、")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 家庭信息
            SummaryItem(
                icon = Icons.Default.Restaurant,
                title = "家庭信息",
                content = "${preferences.familySize}人家庭 | 每周烹饪${preferences.cookingFrequency}次"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 重新设置按钮
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("修改偏好设置")
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: ImageVector,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoCard(hasPreferences: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (hasPreferences) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasPreferences) Icons.Default.Check else Icons.Default.Favorite,
                contentDescription = null,
                tint = if (hasPreferences) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    if (hasPreferences) "已设置个性化偏好" else "个性化推荐",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (hasPreferences) 
                        "AI推荐食谱和食小天会根据你的偏好进行个性化推荐"
                    else 
                        "设置偏好后，AI推荐食谱和食小天会优先推荐符合你口味的食谱",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { item ->
            FilterChip(
                selected = item in selected,
                onClick = { onToggle(item) },
                label = { Text(item) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SingleSelectChips(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(item) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CustomInputChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    placeholder: String = "输入自定义选项..."
) {
    var inputText by remember { mutableStateOf("") }
    val customItems = selected.filter { it !in options }

    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 预设选项
        options.forEach { item ->
            FilterChip(
                selected = item in selected,
                onClick = { onToggle(item) },
                label = { Text(item) }
            )
        }
        // 用户自定义条目（可删除）
        customItems.forEach { item ->
            InputChip(
                selected = true,
                onClick = { onRemove(item) },
                label = { Text(item) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    if (inputText.isNotBlank()) {
                        onAdd(inputText.trim())
                        inputText = ""
                    }
                }
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = {
                if (inputText.isNotBlank()) {
                    onAdd(inputText.trim())
                    inputText = ""
                }
            },
            enabled = inputText.isNotBlank()
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("添加")
        }
    }
}

/**
 * 解析后端返回的偏好JSON
 */
private fun parsePreferences(json: String): UserPreferences {
    return try {
        val gson = com.google.gson.Gson()
        val map = gson.fromJson(json, Map::class.java)
        UserPreferences(
            cuisines = (map["cuisines"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            tastes = (map["tastes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            diet = (map["diet"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            difficulty = map["difficulty"] as? String ?: "MEDIUM",
            maxCookingTime = (map["maxCookingTime"] as? Number)?.toInt() ?: 60,
            cookingScene = map["cookingScene"] as? String,
            nutritionGoals = (map["nutritionGoals"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            cookingEquipment = (map["cookingEquipment"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            dislikedIngredients = (map["dislikedIngredients"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            familySize = (map["familySize"] as? Number)?.toInt() ?: 2,
            cookingFrequency = (map["cookingFrequency"] as? Number)?.toInt() ?: 7
        )
    } catch (e: Exception) {
        UserPreferences()
    }
}
