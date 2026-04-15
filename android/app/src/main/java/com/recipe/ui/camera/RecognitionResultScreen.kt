package com.recipe.ui.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.data.model.RecognizedIngredient
import com.recipe.viewmodel.IngredientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionResultScreen(
    recognizedItems: List<RecognizedIngredient>,
    onNavigateBack: () -> Unit = {},
    onAddAll: (List<RecognizedIngredient>) -> Unit = {},
    viewModel: IngredientViewModel = viewModel()
) {
    // 使用可变状态保存编辑后的列表
    var editableItems by remember { mutableStateOf(recognizedItems.map { it.copy() }) }
    var selectedItems by remember { mutableStateOf<Set<Int>>(recognizedItems.indices.toSet()) }
    val isLoading by viewModel.loading.collectAsState()
    
    // 编辑对话框状态
    var editingItem by remember { mutableStateOf<RecognizedIngredient?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("识别结果") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            selectedItems = if (selectedItems.size == editableItems.size) {
                                emptySet()
                            } else {
                                editableItems.indices.toSet()
                            }
                        }
                    ) {
                        Text(if (selectedItems.size == editableItems.size) "取消全选" else "全选")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "已选择 ${selectedItems.size} 项",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { 
                            val selected = editableItems.filterIndexed { index, _ -> 
                                selectedItems.contains(index) 
                            }
                            onAddAll(selected)
                        },
                        enabled = selectedItems.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加到食材库")
                    }
                }
            }
        }
    ) { padding ->
        if (editableItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "未识别到食材",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "请重新拍照，确保食材清晰可见",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "AI识别完成",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "共识别出 ${editableItems.size} 种食材，点击卡片可编辑",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                itemsIndexed(editableItems) { index, item ->
                    val isSelected = selectedItems.contains(index)

                    RecognizedItemCard(
                        item = item,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedItems = if (isSelected) {
                                selectedItems - index
                            } else {
                                selectedItems + index
                            }
                        },
                        onEdit = {
                            editingItem = item
                            editingIndex = index
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        
        // 编辑对话框
        editingItem?.let { item ->
            EditIngredientDialog(
                item = item,
                onDismiss = { 
                    editingItem = null
                    editingIndex = -1
                },
                onConfirm = { updatedItem ->
                    // 更新列表中的对应项
                    editableItems = editableItems.toMutableList().apply {
                        set(editingIndex, updatedItem)
                    }
                    editingItem = null
                    editingIndex = -1
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecognizedItemCard(
    item: RecognizedIngredient,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = item.getFreshnessColor().copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = item.getFreshnessDisplay(),
                            color = item.getFreshnessColor(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Scale,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.estimatedWeight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "保质期至: ${item.getExpiryDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.AcUnit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.getStorageMethodDisplay(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 编辑按钮
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditIngredientDialog(
    item: RecognizedIngredient,
    onDismiss: () -> Unit,
    onConfirm: (RecognizedIngredient) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var category by remember { mutableStateOf(item.category) }
    var estimatedWeight by remember { mutableStateOf(item.estimatedWeight) }
    var shelfLife by remember { mutableStateOf(item.shelfLife.toString()) }
    var storageMethod by remember { mutableStateOf(item.storageMethod) }
    
    // 下拉菜单展开状态
    var categoryExpanded by remember { mutableStateOf(false) }
    var storageExpanded by remember { mutableStateOf(false) }
    
    // 选项列表
    val categories = listOf("蔬菜", "水果", "肉类", "海鲜", "蛋奶", "调味品", "主食", "饮品", "其他")
    val storageMethods = listOf(
        "ROOM_TEMP" to "常温",
        "REFRIGERATE" to "冷藏",
        "FREEZE" to "冷冻",
        "DRY_COOL" to "干燥阴凉"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑食材信息") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 食材名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("食材名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 类别下拉
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类别") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // 预估重量
                OutlinedTextField(
                    value = estimatedWeight,
                    onValueChange = { estimatedWeight = it },
                    label = { Text("预估重量 (如: 500g, 3个)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 保质期天数
                OutlinedTextField(
                    value = shelfLife,
                    onValueChange = { 
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            shelfLife = it
                        }
                    },
                    label = { Text("保质期 (天)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 保存方式下拉
                ExposedDropdownMenuBox(
                    expanded = storageExpanded,
                    onExpandedChange = { storageExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = storageMethods.find { it.first == storageMethod }?.second ?: "冷藏",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("保存方式") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storageExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = storageExpanded,
                        onDismissRequest = { storageExpanded = false }
                    ) {
                        storageMethods.forEach { (value, display) ->
                            DropdownMenuItem(
                                text = { Text(display) },
                                onClick = {
                                    storageMethod = value
                                    storageExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedItem = item.copy(
                        name = name.trim(),
                        category = category,
                        estimatedWeight = estimatedWeight.trim(),
                        shelfLife = shelfLife.toIntOrNull() ?: item.shelfLife,
                        storageMethod = storageMethod
                    )
                    onConfirm(updatedItem)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
