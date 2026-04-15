package com.recipe.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.recipe.data.local.AppDatabase
import com.recipe.data.local.LocalRecipeEntity
import com.recipe.data.local.TokenManager
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * AI生成的完整食谱详情
 * 用于展示AI推荐食谱的完整信息，包含食材、步骤、营养、小贴士等
 * 注意：ingredients 和 steps 都是格式化字符串列表，非结构化对象
 */
data class GeneratedRecipeDetail(
    val name: String = "",
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val cookingTime: Int = 0,
    val difficulty: String = "MEDIUM",
    val servings: Int = 2,
    val tips: String = "",
    val nutrition: String = "",
    val tags: List<String> = emptyList()
) {
    fun getDifficultyDisplay(): String = when (difficulty) {
        "EASY" -> "简单"
        "MEDIUM" -> "中等"
        "HARD" -> "困难"
        else -> difficulty
    }
}

/**
 * AI食谱详情页ViewModel
 *
 * 职责范围：
 * 1. 调用后端AI接口生成完整食谱
 * 2. 将生成的食谱导入到本地食谱（Room数据库）
 * 3. 将生成的食谱发布到食谱社区（云端）
 * 4. 通过标题查询本地数据库防止重复导入/上传
 *
 * 数据流：
 * - AI生成: 后端 generateRecipe API → 解析为 GeneratedRecipeDetail
 * - 导入本地: GeneratedRecipeDetail → 解析食材字符串 → LocalRecipeEntity → Room
 * - 发布社区: GeneratedRecipeDetail → 解析+序列化为JSON → saveGeneratedRecipe API
 *
 * 重复防护：
 * - 生成食谱后自动 [checkExistingStatus] 检查本地是否已存在同名食谱
 * - 导入/上传前再次检查防止并发操作
 *
 * 注意：每次导航到该页面会创建新实例，因此通过数据库查询恢复状态而非内存缓存
 */
class RecipeDetailViewModel(private val application: Application) : androidx.lifecycle.ViewModel() {
    private val api = RetrofitClient.api
    private val gson = Gson()
    private val dao by lazy { AppDatabase.getInstance(application).localRecipeDao() }

    /** ViewModelProvider.Factory 工厂，用于在 Compose 中创建需要 Application 参数的 ViewModel */
    companion object {
        fun createFactory(application: Application): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return RecipeDetailViewModel(application) as T
                }
            }
        }
    }

    private val _recipe = MutableStateFlow<GeneratedRecipeDetail?>(null)
    val recipe: StateFlow<GeneratedRecipeDetail?> = _recipe

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved      // 是否已发布到社区

    private val _isImported = MutableStateFlow(false)
    val isImported: StateFlow<Boolean> = _isImported  // 是否已导入到本地

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun clearError() { _error.value = null }
    fun clearToast() { _toastMessage.value = null }

    /**
     * 基于菜名和食材生成完整食谱
     */
    fun generateFullRecipe(recipeName: String, mainIngredients: List<String>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _isSaved.value = false
                _isImported.value = false

                val request = mapOf<String, Any?>(
                    "availableIngredients" to mainIngredients,
                    "cuisineType" to "家常菜",
                    "cookingTime" to 30,
                    "difficulty" to "EASY",
                    "servings" to 2
                )

                val response = api.generateRecipe(request)
                if (response.success) {
                    val recipeDetail = parseRecipeDetail(response.data)
                    _recipe.value = recipeDetail.copy(name = recipeName)

                    // 检查本地是否已导入过相同标题的食谱
                    checkExistingStatus(recipeName)
                } else {
                    _error.value = response.message ?: "生成食谱失败"
                }
            } catch (e: Exception) {
                _error.value = "生成失败: ${e.message}"
                Log.e("RecipeDetailVM", "生成食谱失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 检查本地是否已存在同名食谱，设置初始状态
     */
    private suspend fun checkExistingStatus(recipeName: String) {
        try {
            val userId = TokenManager.getUserId()
            if (userId == 0L) return

            // 检查是否已导入到本地
            val localExisting = dao.getByTitle(userId, recipeName)
            if (localExisting != null) {
                _isImported.value = true
            }

            // 检查是否已上传到社区（本地有记录syncStatus=UPLOADED）
            val uploadedExisting = dao.getUploadedByTitle(userId, recipeName)
            if (uploadedExisting != null) {
                _isSaved.value = true
            }
        } catch (e: Exception) {
            Log.e("RecipeDetailVM", "检查已有食谱状态失败", e)
        }
    }

    /**
     * 保存生成的食谱到个人食谱（云端）
     * 注意：currentRecipe.ingredients 是格式化字符串列表如 ["鸭肉 500g", "番茄 300g"]
     * 需要解析出 name, quantity, unit 分别存储
     */
    fun saveRecipe() {
        val currentRecipe = _recipe.value ?: return
        if (_isSaved.value) {
            _toastMessage.value = "该食谱已发布到社区"
            return
        }

        viewModelScope.launch {
            try {
                // 检查登录状态
                val token = TokenManager.getToken()
                if (token.isNullOrBlank()) {
                    _error.value = "请先登录"
                    return@launch
                }
                Log.d("RecipeDetailVM", "Saving recipe with token length: ${token.length}")

                // 解析食材字符串，如 "鸭肉 500g" -> name="鸭肉", quantity=500, unit="g"
                val ingredientsList = currentRecipe.ingredients.map { ingredientStr ->
                    parseIngredientString(ingredientStr)
                }

                // 后端期望 ingredients/steps/tags 是 JSON 字符串，不是对象列表
                val stepsList = currentRecipe.steps.mapIndexed { index, stepContent ->
                    mapOf(
                        "step" to (index + 1),
                        "content" to stepContent,
                        "duration" to if (currentRecipe.steps.isNotEmpty()) {
                            (currentRecipe.cookingTime * 60 / currentRecipe.steps.size).coerceAtLeast(60)
                        } else 60
                    )
                }

                val request = mapOf<String, Any?>(
                    "title" to currentRecipe.name,
                    "description" to currentRecipe.description,
                    "ingredients" to gson.toJson(ingredientsList),  // 序列化为 JSON 字符串
                    "steps" to gson.toJson(stepsList),              // 序列化为 JSON 字符串
                    "cookingTime" to currentRecipe.cookingTime,
                    "difficulty" to currentRecipe.difficulty,
                    "servings" to currentRecipe.servings,
                    "tips" to currentRecipe.tips,
                    "tags" to if (currentRecipe.tags.isNotEmpty()) gson.toJson(currentRecipe.tags) else null  // 序列化为 JSON 字符串
                )

                Log.d("RecipeDetailVM", "Request: $request")
                val response = api.saveGeneratedRecipe(request)
                if (response.success) {
                    _isSaved.value = true
                    Log.d("RecipeDetailVM", "Recipe saved successfully")
                } else {
                    _error.value = response.message ?: "保存失败"
                    Log.e("RecipeDetailVM", "Save failed: ${response.message}")
                }
            } catch (e: HttpException) {
                val errorMsg = when (e.code()) {
                    401 -> "登录已过期，请重新登录"
                    403 -> "没有权限执行此操作，请检查登录状态"
                    else -> "网络错误(${e.code()}): ${e.message}"
                }
                _error.value = errorMsg
                Log.e("RecipeDetailVM", "HTTP error ${e.code()}", e)
            } catch (e: Exception) {
                _error.value = "保存失败: ${e.message}"
                Log.e("RecipeDetailVM", "保存食谱失败", e)
            }
        }
    }

    /**
     * 导入AI生成的食谱到本地食谱（保存到Room数据库）
     * 注意：currentRecipe.ingredients 已经是格式化字符串列表如 ["鸭肉 500g", "番茄 300g"]
     * 需要解析出 name, quantity, unit 分别存储
     */
    fun importToLocalRecipes() {
        val currentRecipe = _recipe.value ?: return

        viewModelScope.launch {
            try {
                val userId = TokenManager.getUserId()
                if (userId == 0L) {
                    _toastMessage.value = "请先登录"
                    return@launch
                }

                // 检查本地是否已存在同名食谱
                val existing = dao.getByTitle(userId, currentRecipe.name)
                if (existing != null) {
                    _isImported.value = true
                    _toastMessage.value = "该食谱已在本地食谱中"
                    return@launch
                }

                // 解析食材字符串，如 "鸭肉 500g" -> name="鸭肉", quantity=500, unit="g"
                val ingredientsList = currentRecipe.ingredients.map { ingredientStr ->
                    parseIngredientString(ingredientStr)
                }

                // 构建步骤JSON - 步骤是字符串列表，需要解析出结构
                val stepsList = currentRecipe.steps.mapIndexed { index, stepContent ->
                    mapOf(
                        "step" to (index + 1),
                        "content" to stepContent,
                        "duration" to if (currentRecipe.steps.isNotEmpty()) {
                            (currentRecipe.cookingTime * 60 / currentRecipe.steps.size).coerceAtLeast(60)
                        } else 60
                    )
                }

                val localRecipe = LocalRecipeEntity(
                    userId = userId,
                    title = currentRecipe.name,
                    description = currentRecipe.description,
                    ingredients = gson.toJson(ingredientsList),
                    steps = gson.toJson(stepsList),
                    cookingTime = currentRecipe.cookingTime,
                    difficulty = currentRecipe.difficulty,
                    cuisine = "家常菜",
                    tags = if (currentRecipe.tags.isNotEmpty()) gson.toJson(currentRecipe.tags) else null,
                    syncStatus = "LOCAL"
                )

                dao.insert(localRecipe)
                _isImported.value = true
                _toastMessage.value = "已导入到本地食谱"
            } catch (e: Exception) {
                _toastMessage.value = "导入失败: ${e.message}"
                Log.e("RecipeDetailVM", "导入本地食谱失败", e)
            }
        }
    }

    /**
     * 解析食材字符串，如 "鸭肉 500g" -> {"name":"鸭肉", "quantity":500, "unit":"g"}
     * 支持格式："name quantityunit", "name quantity unit", "name"
     */
    private fun parseIngredientString(ingredientStr: String): Map<String, Any?> {
        // 尝试匹配 "名称 数量单位" 或 "名称 数量 单位" 格式
        // 如："鸭肉 500g", "鸡蛋 2个", "盐 5g"
        val regex = "^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z%]+)?$".toRegex()
        val match = regex.find(ingredientStr.trim())
        
        return if (match != null) {
            val (name, quantityStr, unit) = match.destructured
            mapOf(
                "name" to name.trim(),
                "quantity" to quantityStr.toDoubleOrNull(),
                "unit" to (unit.takeIf { it.isNotBlank() } ?: "")
            )
        } else {
            // 无法解析，整个字符串作为name
            mapOf(
                "name" to ingredientStr.trim(),
                "quantity" to null,
                "unit" to ""
            )
        }
    }

    /**
     * 解析AI返回的完整食谱数据
     * 后端返回格式不统一，需要兼容多种情况：
     * - ingredients: 可能是字符串列表或对象列表 [{"name":"...", "quantity":...}]
     * - steps: 可能是字符串列表或对象列表 [{"content":"..."}]
     * - nutrition: 可能是字符串或对象 {"calories":"...", ...}
     * - 数值类型: 可能是 Number 或 String
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseRecipeDetail(data: Any?): GeneratedRecipeDetail {
        if (data == null) return GeneratedRecipeDetail()

        return try {
            when (data) {
                is Map<*, *> -> {
                    // 解析食材列表 - 后端返回对象列表，提取name、quantity、unit字段
                    val ingredients = when (val ingData = data["ingredients"]) {
                        is List<*> -> ingData.mapNotNull { item ->
                            when (item) {
                                is String -> item  // 已经是字符串
                                is Map<*, *> -> {
                                    // 从对象提取完整食材信息：name + quantity + unit
                                    val name = item["name"] as? String ?: return@mapNotNull null
                                    val quantity = item["quantity"]
                                    val unit = item["unit"] as? String ?: ""
                                    
                                    // 格式化食材描述
                                    when {
                                        quantity is Number && unit.isNotBlank() -> "$name ${quantity.toInt()}$unit"
                                        quantity is Number -> "$name ${quantity.toInt()}"
                                        quantity is String && quantity.isNotBlank() && unit.isNotBlank() -> "$name $quantity$unit"
                                        quantity is String && quantity.isNotBlank() -> "$name $quantity"
                                        else -> name
                                    }
                                }
                                else -> null
                            }
                        }
                        else -> emptyList()
                    }

                    // 解析步骤列表 - 后端返回对象列表，提取content字段
                    val steps = when (val stepData = data["steps"]) {
                        is List<*> -> stepData.mapNotNull { item ->
                            when (item) {
                                is String -> item  // 已经是字符串
                                is Map<*, *> -> item["content"] as? String  // 从对象提取content
                                else -> null
                            }
                        }
                        else -> emptyList()
                    }

                    // 解析营养信息 - 可能是对象或字符串
                    val nutrition = when (val nutData = data["nutrition"]) {
                        is String -> nutData
                        is Map<*, *> -> {
                            val calories = nutData["calories"] as? String ?: ""
                            val protein = nutData["protein"] as? String ?: ""
                            val carbs = nutData["carbs"] as? String ?: ""
                            val fat = nutData["fat"] as? String ?: ""
                            "热量: $calories, 蛋白质: $protein, 碳水: $carbs, 脂肪: $fat"
                        }
                        else -> ""
                    }

                    GeneratedRecipeDetail(
                        name = (data["title"] as? String) ?: (data["name"] as? String) ?: "",
                        description = (data["description"] as? String) ?: "",
                        ingredients = ingredients,
                        steps = steps,
                        cookingTime = when (val t = data["cookingTime"]) {
                            is Number -> t.toInt()
                            is String -> t.toIntOrNull() ?: 0
                            else -> 0
                        },
                        difficulty = (data["difficulty"] as? String) ?: "MEDIUM",
                        servings = when (val s = data["servings"]) {
                            is Number -> s.toInt()
                            is String -> s.toIntOrNull() ?: 2
                            else -> 2
                        },
                        tips = (data["tips"] as? String) ?: "",
                        nutrition = nutrition,
                        tags = (data["tags"] as? List<String>) ?: emptyList()
                    )
                }
                else -> GeneratedRecipeDetail()
            }
        } catch (e: Exception) {
            Log.e("RecipeDetailVM", "解析食谱详情失败", e)
            GeneratedRecipeDetail()
        }
    }
}
