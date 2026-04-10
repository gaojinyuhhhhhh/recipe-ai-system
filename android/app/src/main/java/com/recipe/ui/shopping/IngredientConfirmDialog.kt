package com.recipe.ui.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recipe.data.model.ShoppingItem

/**
 * AI推断的食材信息
 */
data class InferredIngredientInfo(
    val shelfLife: Int = 7,
    val storageMethod: String = "REFRIGERATED",
    val storageAdvice: String = "建议冷藏保存",
    val freshness: String = "FRESH",
    val actualQuantity: Double? = null,  // 用户实际购买的数量（可修改）
    val unit: String? = null             // 单位
) {
    fun getStorageMethodDisplay(): String = when (storageMethod) {
        "REFRIGERATED" -> "冷藏"
        "FROZEN" -> "冷冻"
        "ROOM_TEMP" -> "常温"
        else -> "冷藏"
    }

    fun getFreshnessDisplay(): String = when (freshness) {
        "FRESH" -> "新鲜"
        "WILTING" -> "微蔫"
        "SPOILED" -> "变质"
        else -> "新鲜"
    }
}

/**
 * 食材确认对话框
 */
@Composable
fun IngredientConfirmDialog(
    item: ShoppingItem,
    inferredInfo: InferredIngredientInfo,
    onDismiss: () -> Unit,
    onConfirm: (InferredIngredientInfo) -> Unit
) {
    var shelfLife by remember { mutableStateOf(inferredInfo.shelfLife) }
    var storageMethod by remember { mutableStateOf(inferredInfo.storageMethod) }
    var storageAdvice by remember { mutableStateOf(inferredInfo.storageAdvice) }
    // 用户可修改的实际购买数量，默认为购物清单中的数量
    var actualQuantity by remember { mutableStateOf(item.quantity?.toString() ?: "") }
    var actualUnit by remember { mutableStateOf(item.unit ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("确认食材信息")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 食材名称
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 实际购买数量（可编辑）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = actualQuantity,
                        onValueChange = { actualQuantity = it },
                        label = { Text("实际数量") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = actualUnit,
                        onValueChange = { actualUnit = it },
                        label = { Text("单位") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }

                // 提示：购物清单中的原始数量
                if (item.quantity != null && item.unit != null) {
                    Text(
                        text = "待采购: ${item.quantity}${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI推断信息提示
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "AI智能推断",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            inferredInfo.storageAdvice,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 保质期
                OutlinedTextField(
                    value = shelfLife.toString(),
                    onValueChange = { newValue ->
                        shelfLife = newValue.toIntOrNull() ?: shelfLife
                    },
                    label = { Text("保质期（天）") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 保存方式
                Text(
                    "保存方式",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StorageMethodChip(
                        label = "冷藏",
                        selected = storageMethod == "REFRIGERATED",
                        onClick = { storageMethod = "REFRIGERATED" }
                    )
                    StorageMethodChip(
                        label = "冷冻",
                        selected = storageMethod == "FROZEN",
                        onClick = { storageMethod = "FROZEN" }
                    )
                    StorageMethodChip(
                        label = "常温",
                        selected = storageMethod == "ROOM_TEMP",
                        onClick = { storageMethod = "ROOM_TEMP" }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 保存建议
                OutlinedTextField(
                    value = storageAdvice,
                    onValueChange = { storageAdvice = it },
                    label = { Text("保存建议") },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        InferredIngredientInfo(
                            shelfLife = shelfLife,
                            storageMethod = storageMethod,
                            storageAdvice = storageAdvice,
                            freshness = inferredInfo.freshness,
                            actualQuantity = actualQuantity.toDoubleOrNull(),
                            unit = actualUnit.takeIf { it.isNotBlank() }
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加到食材库")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageMethodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}
