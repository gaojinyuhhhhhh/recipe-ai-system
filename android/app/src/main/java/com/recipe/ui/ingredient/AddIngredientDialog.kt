package com.recipe.ui.ingredient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * 添加食材对话框
 * 支持两种模式：单个详细添加 / 批量文本输入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onAddSingle: (name: String, category: String?, quantity: Double?, unit: String?, expiryDate: String?, storageMethod: String?) -> Unit,
    onAddBatch: (text: String) -> Unit
) {
    var isBatchMode by remember { mutableStateOf(false) }

    // 单个模式字段
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var storageMethod by remember { mutableStateOf("") }

    // 过期日期相关
    var expiryMode by remember { mutableStateOf("days") } // "days" or "date"
    var shelfLifeDays by remember { mutableStateOf("") }
    val today = remember { LocalDate.now() }
    var selectedYear by remember { mutableIntStateOf(today.year) }
    var selectedMonth by remember { mutableIntStateOf(today.monthValue) }
    var selectedDay by remember { mutableIntStateOf(today.dayOfMonth) }

    // 批量模式字段
    var batchText by remember { mutableStateOf("") }

    // 标准10大类别，与食材库分类展示保持一致
    val categoryOptions = listOf("肉类", "海鲜", "蔬菜类", "水果", "蛋奶", "豆制品", "调味类", "粮油", "干货", "饮品")
    val unitOptions = listOf("个", "斤", "克", "两", "根", "颗", "袋", "瓶", "盒")
    val storageOptions = listOf("常温", "冷藏", "冷冻")

    // 计算过期日期字符串
    val computedExpiryDate: String? = when (expiryMode) {
        "days" -> {
            val days = shelfLifeDays.toIntOrNull()
            if (days != null && days > 0) {
                today.plusDays(days.toLong()).toString()
            } else null
        }
        "date" -> {
            try {
                val date = LocalDate.of(selectedYear, selectedMonth, selectedDay)
                if (!date.isBefore(today)) date.toString() else null
            } catch (e: Exception) { null }
        }
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBatchMode) "批量添加食材" else "添加食材") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 模式切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isBatchMode,
                        onClick = { isBatchMode = false },
                        label = { Text("单个添加") }
                    )
                    FilterChip(
                        selected = isBatchMode,
                        onClick = { isBatchMode = true },
                        label = { Text("批量添加") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isBatchMode) {
                    Text(
                        "输入食材名称，用逗号、空格或换行分隔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = batchText,
                        onValueChange = { batchText = it },
                        label = { Text("食材列表") },
                        placeholder = { Text("例如：鸡蛋,西红柿,青椒,牛肉") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                } else {
                    // 食材名称
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("食材名称 *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 分类选择
                    Text("分类", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    ChipRow(categoryOptions, category) { category = it }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 数量和单位
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("数量") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("单位") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    ChipRow(unitOptions, unit) { unit = it }

                    Spacer(modifier = Modifier.height(12.dp))

                    // === 过期日期区域 ===
                    Text("保质期", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    // 过期模式切换
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = expiryMode == "days",
                            onClick = { expiryMode = "days" },
                            label = { Text("保质天数") }
                        )
                        FilterChip(
                            selected = expiryMode == "date",
                            onClick = { expiryMode = "date" },
                            label = { Text("选择日期") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (expiryMode == "days") {
                        // 保质天数输入
                        OutlinedTextField(
                            value = shelfLifeDays,
                            onValueChange = { shelfLifeDays = it.filter { c -> c.isDigit() } },
                            label = { Text("保质天数") },
                            placeholder = { Text("例如：7") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                if (computedExpiryDate != null) {
                                    Text("过期日期：$computedExpiryDate",
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    } else {
                        // 年月日选择器
                        DateDropdownSelector(
                            year = selectedYear,
                            month = selectedMonth,
                            day = selectedDay,
                            minDate = today,
                            onYearChange = { selectedYear = it },
                            onMonthChange = { selectedMonth = it; adjustDay(selectedYear, it, selectedDay) { selectedDay = it } },
                            onDayChange = { selectedDay = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 存储方式
                    Text("存储方式", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    ChipRow(storageOptions, storageMethod) { storageMethod = it }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isBatchMode) {
                        if (batchText.isNotBlank()) {
                            onAddBatch(batchText)
                            onDismiss()
                        }
                    } else {
                        if (name.isNotBlank()) {
                            onAddSingle(
                                name,
                                category.takeIf { it.isNotBlank() },
                                quantity.toDoubleOrNull(),
                                unit.takeIf { it.isNotBlank() },
                                computedExpiryDate,
                                storageMethod.takeIf { it.isNotBlank() }
                            )
                            onDismiss()
                        }
                    }
                },
                enabled = if (isBatchMode) batchText.isNotBlank() else name.isNotBlank()
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

/**
 * 年月日下拉选择器
 */
@Composable
fun DateDropdownSelector(
    year: Int,
    month: Int,
    day: Int,
    minDate: LocalDate,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit
) {
    val years = (minDate.year..minDate.year + 3).toList()
    val months = (1..12).toList()
    val maxDay = try {
        LocalDate.of(year, month, 1).lengthOfMonth()
    } catch (e: Exception) { 28 }
    val days = (1..maxDay).toList()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 年
        DropdownSelector(
            label = "年",
            options = years,
            selected = year,
            display = { "${it}年" },
            onSelect = onYearChange,
            modifier = Modifier.weight(1.2f)
        )
        // 月
        DropdownSelector(
            label = "月",
            options = months,
            selected = month,
            display = { "${it}月" },
            onSelect = onMonthChange,
            modifier = Modifier.weight(1f)
        )
        // 日
        DropdownSelector(
            label = "日",
            options = days,
            selected = day.coerceAtMost(maxDay),
            display = { "${it}日" },
            onSelect = onDayChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    display: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = display(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(display(option), style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 调整日期使其合法 */
private fun adjustDay(year: Int, month: Int, currentDay: Int, onDayChange: (Int) -> Unit) {
    val maxDay = try {
        LocalDate.of(year, month, 1).lengthOfMonth()
    } catch (e: Exception) { 28 }
    if (currentDay > maxDay) {
        onDayChange(maxDay)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(if (selected == option) "" else option) },
                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
