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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    // 按新鲜度分组的食材
    private val _ingredientsByFreshness = MutableStateFlow<Map<String, List<Ingredient>>>(emptyMap())
    val ingredientsByFreshness: StateFlow<Map<String, List<Ingredient>>> = _ingredientsByFreshness

    // 按类别分组的食材
    private val _ingredientsByCategory = MutableStateFlow<Map<String, List<Ingredient>>>(emptyMap())
    val ingredientsByCategory: StateFlow<Map<String, List<Ingredient>>> = _ingredientsByCategory

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

    // 加载按新鲜度分组的食材
    fun loadIngredientsByFreshness() {
        viewModelScope.launch {
            try {
                val response = api.getIngredientsByFreshness()
                if (response.success) {
                    _ingredientsByFreshness.value = response.data ?: emptyMap()
                }
            } catch (e: Exception) {
                Log.e("IngredientVM", "加载新鲜度分组失败", e)
            }
        }
    }

    // 加载按类别分组的食材
    fun loadIngredientsByCategory() {
        viewModelScope.launch {
            try {
                val response = api.getIngredientsByCategory()
                if (response.success) {
                    _ingredientsByCategory.value = response.data ?: emptyMap()
                }
            } catch (e: Exception) {
                Log.e("IngredientVM", "加载类别分组失败", e)
            }
        }
    }

    // 手动添加单个食材
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
                    // 刷新所有食材数据源（包括分组数据）
                    loadIngredients()
                    loadIngredientsByFreshness()
                    loadIngredientsByCategory()
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

    // 批量添加食材（通过文本输入，后端解析）
    fun batchAddIngredients(text: String) {
        viewModelScope.launch {
            try {
                val response = api.batchAddIngredients(mapOf("ingredients" to text))
                if (response.success) {
                    val count = response.data?.size ?: 0
                    _toastMessage.value = "成功添加${count}种食材"
                    // 刷新所有食材数据源（包括分组数据）
                    loadIngredients()
                    loadIngredientsByFreshness()
                    loadIngredientsByCategory()
                    loadAlerts()
                } else {
                    _toastMessage.value = response.message ?: "添加失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "添加失败: ${e.message}"
            }
        }
    }

    // 编辑食材
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

    // 消耗食材
    fun consumeIngredient(id: Long) {
        viewModelScope.launch {
            try {
                val response = api.consumeIngredient(id)
                if (response.success) {
                    _toastMessage.value = "已标记为已消耗"
                    // 刷新所有相关数据源
                    loadIngredients()
                    loadIngredientsByFreshness()
                    loadIngredientsByCategory()
                    loadAlerts()
                } else {
                    _toastMessage.value = response.message ?: "操作失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "操作失败: ${e.message}"
            }
        }
    }

    // AI识别食材（新API）
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

    // 批量添加识别的食材到食材库
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

    // 解析数量
    private fun parseQuantity(weightStr: String): Double? {
        return try {
            val regex = Regex("([0-9.]+)")
            val match = regex.find(weightStr)
            match?.groupValues?.get(1)?.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    // 解析单位
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

    // 删除食材
    fun deleteIngredient(id: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteIngredient(id)
                if (response.success) {
                    _toastMessage.value = "已删除"
                    loadIngredients()
                } else {
                    _toastMessage.value = "删除失败: ${response.message}"
                }
            } catch (e: Exception) {
                _toastMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    // 存储方式中文映射为后端枚举值
    private fun mapStorageMethod(display: String): String {
        return when (display) {
            "常温" -> "ROOM_TEMP"
            "冷藏" -> "REFRIGERATE"
            "冷冻" -> "FREEZE"
            "干燥阴凉处" -> "DRY_COOL"
            else -> display
        }
    }

    // 将用户输入的日期规范化为 yyyy-MM-dd 格式
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