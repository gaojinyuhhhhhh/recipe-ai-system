package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI推荐的食谱摘要数据
 * 用于展示推荐列表，包含菜名、简介、主要食材、时间等摘要信息
 * 点击后进入 RecipeDetailFromRecommendScreen 生成完整食谱
 */
data class SuggestedRecipe(
    val name: String = "",
    val description: String = "",
    val mainIngredients: List<String> = emptyList(),
    val cookingTime: Int = 0,
    val difficulty: String = "MEDIUM",
    val tags: List<String> = emptyList(),
    val briefSteps: String = ""
) {
    fun getDifficultyDisplay(): String = when (difficulty) {
        "EASY" -> "简单"
        "MEDIUM" -> "中等"
        "HARD" -> "困难"
        else -> difficulty
    }
}

/**
 * AI智能推荐ViewModel
 *
 * 职责范围：
 * - 根据用户食材库中的食材，调用AI推荐合适的食谱
 * - 支持传入用户偏好（如“清淡”“快手菜”等）细化推荐结果
 * - 解析AI返回的非结构化数据为 [SuggestedRecipe] 列表
 *
 * 数据流：
 *   食材列表 + 用户偏好 → suggestByIngredients API → 解析 Map → SuggestedRecipe 列表
 */
class RecommendViewModel : ViewModel() {
    private val api = RetrofitClient.api

    private val _suggestedRecipes = MutableStateFlow<List<SuggestedRecipe>>(emptyList())
    val suggestedRecipes: StateFlow<List<SuggestedRecipe>> = _suggestedRecipes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    // 记住使用的食材列表
    private val _usedIngredients = MutableStateFlow<List<String>>(emptyList())
    val usedIngredients: StateFlow<List<String>> = _usedIngredients

    fun clearToast() { _toastMessage.value = null }
    fun clearError() { _error.value = null }

    /**
     * 基于食材列表调用AI推荐食谱
     * @param ingredients 用户食材库中的食材名称列表
     * @param preferences 可选的用户偏好，如“清淡、不要辣、快手菜”等
     */
    fun suggestByIngredients(ingredients: List<String>, preferences: String? = null) {
        if (ingredients.isEmpty()) {
            _error.value = "请先添加一些食材"
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _usedIngredients.value = ingredients

                val request = mutableMapOf<String, Any?>(
                    "ingredients" to ingredients
                )
                if (preferences != null) {
                    request["preferences"] = preferences
                }

                val response = api.suggestByIngredients(request)
                if (response.success) {
                    val data = response.data
                    if (data != null) {
                        val recipes = parseRecipes(data)
                        _suggestedRecipes.value = recipes
                        if (recipes.isEmpty()) {
                            _error.value = "暂时没有推荐结果，请稍后再试"
                        }
                    } else {
                        _error.value = "推荐结果为空"
                    }
                } else {
                    _error.value = response.message ?: "推荐失败"
                }
            } catch (e: Exception) {
                _error.value = "推荐失败: ${e.message}"
                Log.e("RecommendVM", "推荐失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 解析AI返回的食谱推荐数据
     * 后端返回格式: { "recipes": [ {"name":..., "cookingTime":...}, ... ] }
     * 注意 cookingTime 可能是 Number 或 String，需要兼容处理
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseRecipes(data: Map<String, Any?>): List<SuggestedRecipe> {
        return try {
            val recipesList = data["recipes"] as? List<Map<String, Any?>> ?: return emptyList()
            recipesList.map { map ->
                SuggestedRecipe(
                    name = (map["name"] as? String) ?: "",
                    description = (map["description"] as? String) ?: "",
                    mainIngredients = (map["mainIngredients"] as? List<String>) ?: emptyList(),
                    cookingTime = when (val t = map["cookingTime"]) {
                        is Number -> t.toInt()
                        is String -> t.toIntOrNull() ?: 0
                        else -> 0
                    },
                    difficulty = (map["difficulty"] as? String) ?: "MEDIUM",
                    tags = (map["tags"] as? List<String>) ?: emptyList(),
                    briefSteps = (map["briefSteps"] as? String) ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("RecommendVM", "解析食谱失败", e)
            emptyList()
        }
    }
}
