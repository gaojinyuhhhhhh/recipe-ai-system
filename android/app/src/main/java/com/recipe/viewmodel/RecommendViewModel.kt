package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI推荐的食谱数据
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
     * AI基于食材推荐食谱
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
     * 解析AI返回的食谱数据
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
