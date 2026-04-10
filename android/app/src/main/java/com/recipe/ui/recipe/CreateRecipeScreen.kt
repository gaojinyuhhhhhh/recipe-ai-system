package com.recipe.ui.recipe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    onNavigateBack: () -> Unit,
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val isLoading by recipeViewModel.isLoading.collectAsState()
    val createSuccess by recipeViewModel.createSuccess.collectAsState()
    val toastMessage by recipeViewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cookingTimeText by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }

    // 食材列表
    var ingredients by remember { mutableStateOf(listOf(IngredientInput())) }
    // 步骤列表
    var steps by remember { mutableStateOf(listOf(StepInput())) }

    // AI辅助生成状态
    var isAiGenerating by remember { mutableStateOf(false) }
    var showAiConfirmDialog by remember { mutableStateOf(false) }
    var aiGeneratedRecipe by remember { mutableStateOf<AiGeneratedRecipe?>(null) }

    val difficulties = listOf("EASY" to "简单", "MEDIUM" to "中等", "HARD" to "困难")

    LaunchedEffect(createSuccess) {
        if (createSuccess) {
            recipeViewModel.resetCreateSuccess()
            onNavigateBack()
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
                title = { Text("创建本地食谱") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                    actions = {
                    TextButton(
                        onClick = {
                            val ingredientMaps = ingredients
                                .filter { it.name.isNotBlank() }
                                .map { mapOf<String, Any?>("name" to it.name, "quantity" to it.quantity, "unit" to it.unit) }
                            val stepMaps = steps
                                .filter { it.content.isNotBlank() }
                                .mapIndexed { idx, s -> mapOf<String, Any?>("step" to idx + 1, "content" to s.content, "duration" to s.duration.toIntOrNull()) }
                            val tags = tagsText.split(",", "，", " ").filter { it.isNotBlank() }.map { it.trim() }
                            recipeViewModel.createLocalRecipe(
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                ingredientsList = ingredientMaps,
                                stepsList = stepMaps,
                                cookingTime = cookingTimeText.toIntOrNull(),
                                difficulty = selectedDifficulty,
                                cuisine = cuisine.takeIf { it.isNotBlank() },
                                tags = tags
                            )
                        },
                        enabled = !isLoading && title.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("保存", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 基本信息
            item {
                Text("基本信息", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("食谱名称 *") },
                        singleLine = true
                    )
                    // AI补充按钮
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("请先输入食谱名称")
                                }
                                return@Button
                            }
                            isAiGenerating = true
                            coroutineScope.launch {
                                try {
                                    val result = recipeViewModel.assistCreateRecipe(title)
                                    result?.let { recipe ->
                                        aiGeneratedRecipe = recipe
                                        showAiConfirmDialog = true
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("AI生成失败: ${e.message}")
                                } finally {
                                    isAiGenerating = false
                                }
                            }
                        },
                        enabled = !isAiGenerating && title.isNotBlank(),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (isAiGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI补充")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("食谱描述") },
                    maxLines = 3
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cookingTimeText,
                        onValueChange = { cookingTimeText = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("烹饪时长(分钟)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cuisine,
                        onValueChange = { cuisine = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("菜系") },
                        placeholder = { Text("如：川菜") },
                        singleLine = true
                    )
                }
            }

            // 难度选择
            item {
                Text("难度", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    difficulties.forEach { (value, label) ->
                        FilterChip(
                            selected = selectedDifficulty == value,
                            onClick = { selectedDifficulty = value },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // 标签
            item {
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标签") },
                    placeholder = { Text("用逗号分隔，如：减脂,快手,低糖") },
                    singleLine = true
                )
            }

            // 食材清单
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("食材清单 *", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        ingredients = ingredients + IngredientInput()
                    }) {
                        Icon(Icons.Default.Add, "添加食材")
                    }
                }
            }
            itemsIndexed(ingredients) { index, ing ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = ing.name,
                            onValueChange = { v ->
                                ingredients = ingredients.toMutableList().also { it[index] = it[index].copy(name = v) }
                            },
                            modifier = Modifier.weight(2f),
                            label = { Text("食材") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ing.quantity,
                            onValueChange = { v ->
                                ingredients = ingredients.toMutableList().also { it[index] = it[index].copy(quantity = v) }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("用量") },
                            singleLine = true
                        )
                        // 单位下拉选择
                        val units = listOf("g", "kg", "ml", "L", "个", "根", "把", "包", "瓶", "盒", "勺", "杯")
                        var unitExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = ing.unit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("单位") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                modifier = Modifier.menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                units.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            ingredients = ingredients.toMutableList().also { it[index] = it[index].copy(unit = u) }
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        if (ingredients.size > 1) {
                            IconButton(
                                onClick = {
                                    ingredients = ingredients.toMutableList().also { it.removeAt(index) }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, "删除", tint = MaterialTheme.colorScheme.error)
                            }
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
                    Text("烹饪步骤 *", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        steps = steps + StepInput()
                    }) {
                        Icon(Icons.Default.Add, "添加步骤")
                    }
                }
            }
            itemsIndexed(steps) { index, step ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("步骤 ${index + 1}", style = MaterialTheme.typography.labelMedium)
                            if (steps.size > 1) {
                                IconButton(
                                    onClick = { steps = steps.toMutableList().also { it.removeAt(index) } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = step.content,
                            onValueChange = { v ->
                                steps = steps.toMutableList().also { it[index] = it[index].copy(content = v) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("操作描述") },
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = step.duration,
                            onValueChange = { v ->
                                steps = steps.toMutableList().also { it[index] = it[index].copy(duration = v.filter { c -> c.isDigit() }) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("预计时长(秒)") },
                            singleLine = true
                        )
                    }
                }
            }

            // 底部间距
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // AI生成结果确认对话框
    if (showAiConfirmDialog && aiGeneratedRecipe != null) {
        val recipe = aiGeneratedRecipe!!
        AlertDialog(
            onDismissRequest = { showAiConfirmDialog = false },
            icon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("AI已生成食谱") },
            text = {
                Column {
                    Text("AI已为「$title」生成完整食谱信息，是否应用到当前表单？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "包含：${recipe.ingredients.size}种食材、${recipe.steps.size}个步骤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "预计烹饪时间：${recipe.cookingTime}分钟 | 难度：${recipe.difficultyDisplay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 应用AI生成的数据到表单
                        description = recipe.description
                        cookingTimeText = recipe.cookingTime.toString()
                        cuisine = recipe.cuisine
                        selectedDifficulty = recipe.difficulty
                        tagsText = recipe.tags.joinToString(", ")
                        ingredients = recipe.ingredients.map { 
                            IngredientInput(
                                name = it.name,
                                quantity = it.quantity?.toString() ?: "",
                                unit = it.unit ?: ""
                            )
                        }.ifEmpty { listOf(IngredientInput()) }
                        steps = recipe.steps.map {
                            StepInput(
                                content = it.content,
                                duration = it.duration?.toString() ?: ""
                            )
                        }.ifEmpty { listOf(StepInput()) }
                        showAiConfirmDialog = false
                        aiGeneratedRecipe = null
                    }
                ) {
                    Text("应用并修改")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAiConfirmDialog = false
                    aiGeneratedRecipe = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

data class IngredientInput(
    val name: String = "",
    val quantity: String = "",
    val unit: String = ""
)

data class StepInput(
    val content: String = "",
    val duration: String = ""
)

/**
 * AI生成的食谱数据
 */
data class AiGeneratedRecipe(
    val title: String,
    val description: String,
    val ingredients: List<AiIngredient>,
    val steps: List<AiStep>,
    val cookingTime: Int,
    val difficulty: String,
    val difficultyDisplay: String,
    val cuisine: String,
    val tags: List<String>
)

data class AiIngredient(
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val notes: String?
)

data class AiStep(
    val step: Int,
    val content: String,
    val duration: Int?,
    val temperature: String?,
    val tips: String?
)
