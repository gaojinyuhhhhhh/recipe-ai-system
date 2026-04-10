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

/**
 * 编辑本地食谱页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocalRecipeScreen(
    localRecipeId: Long,
    onNavigateBack: () -> Unit,
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val localRecipe by recipeViewModel.currentLocalRecipe.collectAsState()
    val isLoading by recipeViewModel.isLoading.collectAsState()
    val editSuccess by recipeViewModel.editSuccess.collectAsState()
    val toastMessage by recipeViewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cookingTimeText by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var ingredients by remember { mutableStateOf(listOf(IngredientInput())) }
    var steps by remember { mutableStateOf(listOf(StepInput())) }
    var initialized by remember { mutableStateOf(false) }

    val difficulties = listOf("EASY" to "简单", "MEDIUM" to "中等", "HARD" to "困难")

    LaunchedEffect(localRecipeId) {
        recipeViewModel.loadLocalRecipeDetail(localRecipeId)
    }

    LaunchedEffect(localRecipe) {
        if (localRecipe != null && !initialized) {
            val recipe = localRecipe!!
            title = recipe.title
            description = recipe.description ?: ""
            cookingTimeText = recipe.cookingTime?.toString() ?: ""
            cuisine = recipe.cuisine ?: ""
            selectedDifficulty = recipe.difficulty
            tagsText = recipeViewModel.parseTags(recipe.tags).joinToString(",")

            val parsedIngredients = recipeViewModel.parseIngredients(recipe.ingredients)
            if (parsedIngredients.isNotEmpty()) {
                ingredients = parsedIngredients.map {
                    IngredientInput(name = it.name, quantity = it.quantity?.toString() ?: "", unit = it.unit ?: "")
                }
            }

            val parsedSteps = recipeViewModel.parseSteps(recipe.steps)
            if (parsedSteps.isNotEmpty()) {
                steps = parsedSteps.map {
                    StepInput(content = it.content, duration = it.duration?.toString() ?: "")
                }
            }
            initialized = true
        }
    }

    LaunchedEffect(editSuccess) {
        if (editSuccess) {
            recipeViewModel.resetEditSuccess()
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
                title = { Text("编辑本地食谱") },
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
                            val tagsList = tagsText.split(",", "，", " ").filter { it.isNotBlank() }.map { it.trim() }
                            recipeViewModel.updateLocalRecipe(
                                localId = localRecipeId,
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                ingredientsList = ingredientMaps,
                                stepsList = stepMaps,
                                cookingTime = cookingTimeText.toIntOrNull(),
                                difficulty = selectedDifficulty,
                                cuisine = cuisine.takeIf { it.isNotBlank() },
                                tags = tagsList
                            )
                        },
                        enabled = !isLoading && title.isNotBlank() && initialized
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
        if (!initialized) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("基本信息", style = MaterialTheme.typography.titleMedium) }
                item {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("食谱名称 *") }, singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("食谱描述") }, maxLines = 3
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cookingTimeText,
                            onValueChange = { cookingTimeText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text("烹饪时长(分钟)") }, singleLine = true
                        )
                        OutlinedTextField(
                            value = cuisine, onValueChange = { cuisine = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("菜系") }, placeholder = { Text("如：川菜") }, singleLine = true
                        )
                    }
                }
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
                item {
                    OutlinedTextField(
                        value = tagsText, onValueChange = { tagsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("标签") },
                        placeholder = { Text("用逗号分隔，如：减脂,快手,低糖") }, singleLine = true
                    )
                }

                // 食材清单
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("食材清单 *", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { ingredients = ingredients + IngredientInput() }) {
                            Icon(Icons.Default.Add, "添加食材")
                        }
                    }
                }
                itemsIndexed(ingredients) { index, ing ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = ing.name,
                                onValueChange = { v -> ingredients = ingredients.toMutableList().also { it[index] = it[index].copy(name = v) } },
                                modifier = Modifier.weight(2f), label = { Text("食材") }, singleLine = true
                            )
                            OutlinedTextField(
                                value = ing.quantity,
                                onValueChange = { v -> ingredients = ingredients.toMutableList().also { it[index] = it[index].copy(quantity = v) } },
                                modifier = Modifier.weight(1f), label = { Text("用量") }, singleLine = true
                            )
                            OutlinedTextField(
                                value = ing.unit,
                                onValueChange = { v -> ingredients = ingredients.toMutableList().also { it[index] = it[index].copy(unit = v) } },
                                modifier = Modifier.weight(1f), label = { Text("单位") }, singleLine = true
                            )
                            if (ingredients.size > 1) {
                                IconButton(onClick = { ingredients = ingredients.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Close, "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // 烹饪步骤
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("烹饪步骤 *", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { steps = steps + StepInput() }) {
                            Icon(Icons.Default.Add, "添加步骤")
                        }
                    }
                }
                itemsIndexed(steps) { index, step ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("步骤 ${index + 1}", style = MaterialTheme.typography.labelMedium)
                                if (steps.size > 1) {
                                    IconButton(onClick = { steps = steps.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = step.content,
                                onValueChange = { v -> steps = steps.toMutableList().also { it[index] = it[index].copy(content = v) } },
                                modifier = Modifier.fillMaxWidth(), label = { Text("操作描述") }, maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = step.duration,
                                onValueChange = { v -> steps = steps.toMutableList().also { it[index] = it[index].copy(duration = v.filter { c -> c.isDigit() }) } },
                                modifier = Modifier.fillMaxWidth(), label = { Text("预计时长(秒)") }, singleLine = true
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
