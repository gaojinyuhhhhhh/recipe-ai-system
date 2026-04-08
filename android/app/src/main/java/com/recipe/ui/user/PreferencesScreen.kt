package com.recipe.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // 口味偏好
    val cuisineOptions = listOf("川菜", "粤菜", "湘菜", "鲁菜", "苏菜", "浙菜", "闽菜", "徽菜", "西餐", "日料", "韩餐")
    var selectedCuisines by remember { mutableStateOf(setOf<String>()) }

    // 饮食限制
    val dietOptions = listOf("无限制", "素食", "清真", "无麸质", "低糖", "低脂", "低盐", "高蛋白")
    var selectedDiet by remember { mutableStateOf(setOf<String>()) }

    // 难度偏好
    val difficultyOptions = listOf("简单", "中等", "困难")
    var selectedDifficulty by remember { mutableStateOf("中等") }

    // 家庭人数
    var familySize by remember { mutableStateOf("2") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("偏好设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val prefs = mapOf(
                                        "cuisines" to selectedCuisines.joinToString(","),
                                        "diet" to selectedDiet.joinToString(","),
                                        "difficulty" to selectedDifficulty,
                                        "familySize" to familySize
                                    )
                                    val response = RetrofitClient.api.setPreferences(prefs)
                                    message = if (response.success) "保存成功" else (response.message ?: "保存失败")
                                } catch (e: Exception) {
                                    message = "保存失败: ${e.message}"
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, "保存")
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
            // 提示消息
            message?.let {
                Snackbar(
                    modifier = Modifier.padding(bottom = 16.dp),
                    action = { TextButton(onClick = { message = null }) { Text("关闭") } }
                ) { Text(it) }
            }

            // 口味偏好
            Text("口味偏好", style = MaterialTheme.typography.titleMedium)
            Text("选择你喜欢的菜系", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(cuisineOptions, selectedCuisines) { item ->
                selectedCuisines = if (item in selectedCuisines) selectedCuisines - item else selectedCuisines + item
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 饮食限制
            Text("饮食限制", style = MaterialTheme.typography.titleMedium)
            Text("有特殊饮食需求吗？", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(dietOptions, selectedDiet) { item ->
                selectedDiet = if (item in selectedDiet) selectedDiet - item else selectedDiet + item
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 难度偏好
            Text("难度偏好", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                difficultyOptions.forEach { difficulty ->
                    FilterChip(
                        selected = selectedDifficulty == difficulty,
                        onClick = { selectedDifficulty = difficulty },
                        label = { Text(difficulty) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 家庭人数
            Text("家庭人数", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = familySize,
                onValueChange = { familySize = it.filter { c -> c.isDigit() } },
                label = { Text("人数") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FlowRow(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
