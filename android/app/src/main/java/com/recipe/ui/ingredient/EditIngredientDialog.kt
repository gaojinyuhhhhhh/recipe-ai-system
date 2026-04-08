package com.recipe.ui.ingredient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recipe.data.model.Ingredient
import java.time.LocalDate

/**
 * 编辑食材对话框
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditIngredientDialog(
    ingredient: Ingredient,
    onDismiss: () -> Unit,
    onSave: (Ingredient) -> Unit,
    onDelete: (Long) -> Unit
) {
    var name by remember { mutableStateOf(ingredient.name) }
    var category by remember { mutableStateOf(ingredient.category ?: "") }
    var quantity by remember { mutableStateOf(ingredient.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(ingredient.unit ?: "") }
    var storageMethod by remember { mutableStateOf(ingredient.storageMethod ?: "") }
    var notes by remember { mutableStateOf(ingredient.notes ?: "") }

    // 过期日期
    val today = remember { LocalDate.now() }
    var expiryMode by remember { mutableStateOf(if (ingredient.expiryDate != null) "date" else "days") }
    var shelfLifeDays by remember { mutableStateOf("") }

    // 从已有过期日期初始化年月日
    val existingExpiry = remember {
        try { ingredient.expiryDate?.let { LocalDate.parse(it) } } catch (e: Exception) { null }
    }
    var selectedYear by remember { mutableIntStateOf(existingExpiry?.year ?: today.year) }
    var selectedMonth by remember { mutableIntStateOf(existingExpiry?.monthValue ?: today.monthValue) }
    var selectedDay by remember { mutableIntStateOf(existingExpiry?.dayOfMonth ?: today.dayOfMonth) }

    val categoryOptions = listOf("蔬菜", "水果", "肉类", "海鲜", "蛋奶", "调味品", "主食", "饮品", "其他")
    val storageOptions = listOf("常温", "冷藏", "冷冻")

    // 计算过期日期
    val computedExpiryDate: String? = when (expiryMode) {
        "days" -> {
            val days = shelfLifeDays.toIntOrNull()
            if (days != null && days > 0) today.plusDays(days.toLong()).toString() else null
        }
        "date" -> {
            try {
                val date = LocalDate.of(selectedYear, selectedMonth, selectedDay)
                date.toString()
            } catch (e: Exception) { null }
        }
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑食材") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 分类
                Text("分类", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categoryOptions.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = if (category == option) "" else option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 数量和单位
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Spacer(modifier = Modifier.height(12.dp))

                // === 过期日期区域 ===
                Text("保质期", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

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
                    DateDropdownSelector(
                        year = selectedYear,
                        month = selectedMonth,
                        day = selectedDay,
                        minDate = today,
                        onYearChange = { selectedYear = it },
                        onMonthChange = { sm ->
                            selectedMonth = sm
                            val maxDay = try { LocalDate.of(selectedYear, sm, 1).lengthOfMonth() } catch (e: Exception) { 28 }
                            if (selectedDay > maxDay) selectedDay = maxDay
                        },
                        onDayChange = { selectedDay = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 存储方式
                Text("存储方式", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    storageOptions.forEach { option ->
                        FilterChip(
                            selected = storageMethod == option,
                            onClick = { storageMethod = if (storageMethod == option) "" else option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 备注
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 删除按钮
                TextButton(
                    onClick = {
                        ingredient.id?.let { onDelete(it) }
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除此食材")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = ingredient.copy(
                        name = name,
                        category = category.takeIf { it.isNotBlank() },
                        quantity = quantity.toDoubleOrNull(),
                        unit = unit.takeIf { it.isNotBlank() },
                        expiryDate = computedExpiryDate,
                        storageMethod = storageMethod.takeIf { it.isNotBlank() },
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    onSave(updated)
                    onDismiss()
                },
                enabled = name.isNotBlank()
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
