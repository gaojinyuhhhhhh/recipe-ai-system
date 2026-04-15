package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.local.TokenManager
import com.recipe.data.model.ExpiryAlert
import com.recipe.data.model.Ingredient
import com.recipe.data.model.RecognizedIngredient
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 食材管理ViewModel
 *
 * 职责范围：
 * 1. 食材CRUD — 手动添加、批量添加、编辑、删除、消耗
 * 2. 食材分组 — 按新鲜度（即将过期/新鲜/长期）和类别（10大类）分组
 * 3. 过期提醒 — 加载临期/过期食材提醒
 * 4. AI拍照识别 — 图片识别食材并批量入库
 *
 * 数据流：
 * - 所有数据通过 [api] 与后端交互（食材存储在服务端数据库）
 * - [ingredientsByFreshness] 和 [ingredientsByCategory] 由 [_ingredients] 自动派生，
 *   使用 StateFlow.map 实现数据变化时自动重新分组
 */
class IngredientViewModel : ViewModel() {
    private val api = RetrofitClient.api

    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients

    private val _alerts = MutableStateFlow<List<ExpiryAlert>>(emptyList())
    val alerts: StateFlow<List<ExpiryAlert>> = _alerts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    // AI识别结果
    private val _recognizedIngredients = MutableStateFlow<List<RecognizedIngredient>>(emptyList())
    val recognizedIngredients: StateFlow<List<RecognizedIngredient>> = _recognizedIngredients

    /**
     * 类别名称映射表（兼容旧数据，与后端保持一致）
     * 早期数据可能使用“蔬菜”“调味品”等旧名称，需要映射为当前标准名称
     */
    private val categoryMapping = mapOf(
        "蔬菜" to "蔬菜类",
        "调味品" to "调味类",
        "主食" to "粮油",
        "其他" to "未分类",
        "肉蛋类" to "肉类"
    )

    /**
     * 按新鲜度分组 — 由 _ingredients 自动派生，食材列表变化时自动重新分组
     * 分组规则：expiringSoon(≤3天) / fresh(4-7天) / longTerm(>7天)
     */
    val ingredientsByFreshness: StateFlow<Map<String, List<Ingredient>>> = _ingredients
        .map { list -> groupByFreshness(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * 按类别分组 — 由 _ingredients 自动派生，食材列表变化时自动重新分组
     * 类别名称经过 [categoryMapping] 统一后按字母顺序排序
     */
    val ingredientsByCategory: StateFlow<Map<String, List<Ingredient>>> = _ingredients
        .map { list -> groupByCategory(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun clearToast() { _toastMessage.value = null }
    fun clearError() { _error.value = null }
    fun clearRecognized() { _recognizedIngredients.value = emptyList() }

    // 加载食材列表
    fun loadIngredients() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val userId = TokenManager.getUserId()
                Log.d("IngredientVM", "Loading ingredients for userId: $userId")
                val response = api.getIngredients()
                Log.d("IngredientVM", "Response: success=${response.success}, data size=${response.data?.size}")
                if (response.success) {
                    _ingredients.value = response.data ?: emptyList()
                    _error.value = null  // 清除之前的错误状态
                    Log.d("IngredientVM", "Loaded ${_ingredients.value.size} ingredients")
                } else {
                    _error.value = response.message
                    Log.e("IngredientVM", "Error: ${response.message}")
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
                Log.e("IngredientVM", "Exception: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }

    // 加载过期提醒
    fun loadAlerts() {
        viewModelScope.launch {
            try {
                val response = api.getExpiryAlerts()
                if (response.success) {
                    _alerts.value = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("IngredientVM", "加载提醒失败", e)
            }
        }
    }

    // 本地按新鲜度分组（与后端逻辑一致）
    private fun groupByFreshness(ingredients: List<Ingredient>): Map<String, List<Ingredient>> {
        val grouped = ingredients.groupBy { ingredient ->
            val remaining = ingredient.getRemainingDays() ?: Int.MAX_VALUE
            when {
                remaining <= 3 -> "expiringSoon"  // 3天内过期
                remaining <= 7 -> "fresh"          // 4-7天
                else -> "longTerm"                 // 7天以上
            }
        }
        return mapOf(
            "expiringSoon" to (grouped["expiringSoon"] ?: emptyList()),
            "fresh" to (grouped["fresh"] ?: emptyList()),
            "longTerm" to (grouped["longTerm"] ?: emptyList())
        )
    }

    // 本地按类别分组（与后端逻辑一致）
    private fun groupByCategory(ingredients: List<Ingredient>): Map<String, List<Ingredient>> {
        return ingredients.groupBy {
            val rawCategory = it.category ?: "未分类"
            categoryMapping[rawCategory] ?: rawCategory
        }.toSortedMap()
    }

    /** 手动添加单个食材，购买日期自动填充为当天，存储方式会从中文映射为后端枚举值 */
    fun addIngredient(
        name: String,
        category: String? = null,
        quantity: Double? = null,
        unit: String? = null,
        expiryDate: String? = null,
        storageMethod: String? = null
    ) {
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken()
                Log.d("IngredientVM", "Token: ${token?.take(20)}... (null=${token == null})")
                val ingredient = Ingredient(
                    userId = TokenManager.getUserId(),
                    name = name,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    purchaseDate = LocalDate.now().toString(),
                    expiryDate = expiryDate?.let { normalizeDate(it) },
                    storageMethod = storageMethod?.let { mapStorageMethod(it) }
                )
                val response = api.addIngredient(ingredient)
                if (response.success) {
                    _toastMessage.value = "添加成功"
                    loadIngredients()
                    loadAlerts()
                } else {
                    _toastMessage.value = response.message ?: "添加失败"
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if ("403" in msg) {
                    _toastMessage.value = "登录已过期，请退出重新登录"
                } else {
                    _toastMessage.value = "添加失败: $msg"
                }
            }
        }
    }

    /** 批量添加食材（通过文本输入，由后端智能解析） */
    fun batchAddIngredients(text: String) {
        viewModelScope.launch {
            try {
                val response = api.batchAddIngredients(mapOf("ingredients" to text))
                if (response.success) {
                    val count = response.data?.size ?: 0
                    _toastMessage.value = "成功添加${count}种食材"
                    loadIngredients()
                    loadAlerts()
                } else {
                    _toastMessage.value = response.message ?: "添加失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "添加失败: ${e.message}"
            }
        }
    }

    /** 编辑食材信息，存储方式会经过中文→后端枚举值映射 */
    fun updateIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            try {
                val mapped = ingredient.copy(
                    storageMethod = ingredient.storageMethod?.let { mapStorageMethod(it) }
                )
                val response = api.updateIngredient(mapped.id!!, mapped)
                if (response.success) {
                    _toastMessage.value = "修改成功"
                    loadIngredients()
                    loadAlerts()
                } else {
                    _toastMessage.value = response.message ?: "修改失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "修改失败: ${e.message}"
            }
        }
    }

    /** 标记食材为已消耗（从食材库中移除） */
    fun consumeIngredient(id: Long) {
        viewModelScope.launch {
            try {
                val response = api.consumeIngredient(id)
                if (response.success) {
                    _toastMessage.value = "已标记为已消耗"
                    loadIngredients()
                    loadAlerts()
                } else {
                    _toastMessage.value = response.message ?: "操作失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "操作失败: ${e.message}"
            }
        }
    }

    /**
     * AI拍照识别食材
     * 流程：拍照→压缩为Base64→发送到后端 AI 接口→返回识别结果列表
     * @param onSuccess 识别成功的回调，用于触发页面跳转
     */
    fun recognizeImage(imageBase64: String, onSuccess: (List<RecognizedIngredient>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _loading.value = true
                Log.d("IngredientVM", "开始识别图片，Base64长度: ${imageBase64.length}")
                
                val response = api.recognizeIngredients(mapOf("imageBase64" to imageBase64))
                Log.d("IngredientVM", "识别响应: success=${response.success}, dataSize=${response.data?.size}, message=${response.message}")
                
                if (response.success) {
                    val items = response.data ?: emptyList()
                    _recognizedIngredients.value = items
                    Log.d("IngredientVM", "识别成功，共 ${items.size} 种食材")
                    onSuccess(items)
                } else {
                    _error.value = response.message ?: "识别失败"
                    Log.e("IngredientVM", "识别失败: ${response.message}")
                }
            } catch (e: Exception) {
                _error.value = "识别失败: ${e.message}"
                Log.e("IngredientVM", "识别异常", e)
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * 批量添加AI识别的食材到食材库
     * 将 [RecognizedIngredient] 转换为 [Ingredient]，解析重量字符串为数值+单位，
     * 计算保质期和存储方式，然后逐个调用添加接口
     */
    fun addRecognizedIngredients(ingredients: List<RecognizedIngredient>) {
        viewModelScope.launch {
            try {
                _loading.value = true
                var successCount = 0
                ingredients.forEach { recognized ->
                    val ingredient = Ingredient(
                        userId = TokenManager.getUserId(),
                        name = recognized.name,
                        category = recognized.category,
                        quantity = parseQuantity(recognized.estimatedWeight),
                        unit = parseUnit(recognized.estimatedWeight),
                        purchaseDate = LocalDate.now().toString(),
                        expiryDate = recognized.getExpiryDate(),
                        storageMethod = mapStorageMethod(recognized.getStorageMethodDisplay())
                    )
                    try {
                        val response = api.addIngredient(ingredient)
                        if (response.success) successCount++
                    } catch (e: Exception) {
                        Log.e("IngredientVM", "添加食材失败: ${recognized.name}", e)
                    }
                }
                _toastMessage.value = "成功添加 $successCount 种食材"
                loadIngredients()
                loadAlerts()
                clearRecognized()
            } catch (e: Exception) {
                _toastMessage.value = "添加失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /** 从重量字符串中提取数值部分，如 "500g" → 500.0 */
    private fun parseQuantity(weightStr: String): Double? {
        return try {
            val regex = Regex("([0-9.]+)")
            val match = regex.find(weightStr)
            match?.groupValues?.get(1)?.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    /** 从重量字符串中提取单位部分，如 "500g" → "g"，支持中英文单位 */
    private fun parseUnit(weightStr: String): String? {
        return when {
            weightStr.contains("g") || weightStr.contains("克") -> "g"
            weightStr.contains("kg") || weightStr.contains("千克") -> "kg"
            weightStr.contains("ml") || weightStr.contains("毫升") -> "ml"
            weightStr.contains("L") || weightStr.contains("升") -> "L"
            weightStr.contains("个") -> "个"
            weightStr.contains("根") -> "根"
            weightStr.contains("把") -> "把"
            weightStr.contains("包") -> "包"
            weightStr.contains("瓶") -> "瓶"
            weightStr.contains("盒") -> "盒"
            weightStr.contains("斤") -> "斤"
            else -> null
        }
    }

    /**
     * 删除食材（乐观更新模式）
     * 先从本地列表立即移除，分组数据自动派生更新，
     * 然后异步调用后端删除，失败则重新加载回滚
     */
    fun deleteIngredient(id: Long) {
        viewModelScope.launch {
            try {
                // 乐观更新：立即从本地列表中移除，分组数据自动派生更新
                _ingredients.value = _ingredients.value.filter { it.id != id }

                val response = api.deleteIngredient(id)
                if (response.success) {
                    _toastMessage.value = "已删除"
                    loadIngredients()
                    loadAlerts()
                } else {
                    _toastMessage.value = "删除失败: ${response.message}"
                    loadIngredients()
                }
            } catch (e: Exception) {
                _toastMessage.value = "删除失败: ${e.message}"
                loadIngredients()
            }
        }
    }

    /** 存储方式中文映射为后端枚举值，如 "冷藏" → "REFRIGERATE" */
    private fun mapStorageMethod(display: String): String {
        return when (display) {
            "常温" -> "ROOM_TEMP"
            "冷藏" -> "REFRIGERATE"
            "冷冻" -> "FREEZE"
            "干燥阴凉处" -> "DRY_COOL"
            else -> display
        }
    }

    /** 将用户输入的日期规范化为 yyyy-MM-dd 格式，支持 "-"/"/"/"." 分隔符 */
    private fun normalizeDate(dateStr: String): String {
        return try {
            // 尝试解析常见格式并输出标准格式
            val parts = dateStr.trim().split("-", "/", ".")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].padStart(2, '0')
                val day = parts[2].padStart(2, '0')
                "$year-$month-$day"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }
}