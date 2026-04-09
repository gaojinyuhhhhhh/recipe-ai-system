package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI生成的完整食谱详情
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

class RecipeDetailViewModel : ViewModel() {
    private val api = RetrofitClient.api

    private val _recipe = MutableStateFlow<GeneratedRecipeDetail?>(null)
    val recipe: StateFlow<GeneratedRecipeDetail?> = _recipe

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    fun clearError() { _error.value = null }

    /**
     * 基于菜名和食材生成完整食谱
     */
    fun generateFullRecipe(recipeName: String, mainIngredients: List<String>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _isSaved.value = false

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
     * 保存生成的食谱到个人食谱
     */
    fun saveRecipe() {
        val currentRecipe = _recipe.value ?: return

        viewModelScope.launch {
            try {
                val request = mapOf<String, Any?>(
                    "title" to currentRecipe.name,
                    "description" to currentRecipe.description,
                    "ingredients" to currentRecipe.ingredients.map { 
                        mapOf("name" to it, "amount" to "适量")
                    },
                    "steps" to currentRecipe.steps.mapIndexed { index, step ->
                        mapOf(
                            "orderNum" to (index + 1),
                            "description" to step,
                            "duration" to (currentRecipe.cookingTime / currentRecipe.steps.size)
                        )
                    },
                    "cookingTime" to currentRecipe.cookingTime,
                    "difficulty" to currentRecipe.difficulty,
                    "servings" to currentRecipe.servings,
                    "tips" to currentRecipe.tips,
                    "tags" to currentRecipe.tags
                )

                val response = api.saveGeneratedRecipe(request)
                if (response.success) {
                    _isSaved.value = true
                } else {
                    _error.value = response.message ?: "保存失败"
                }
            } catch (e: Exception) {
                _error.value = "保存失败: ${e.message}"
                Log.e("RecipeDetailVM", "保存食谱失败", e)
            }
        }
    }

    /**
     * 解析AI返回的完整食谱数据
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseRecipeDetail(data: Any?): GeneratedRecipeDetail {
        if (data == null) return GeneratedRecipeDetail()

        return try {
            when (data) {
                is Map<*, *> -> {
                    GeneratedRecipeDetail(
                        name = (data["name"] as? String) ?: "",
                        description = (data["description"] as? String) ?: "",
                        ingredients = (data["ingredients"] as? List<String>) ?: emptyList(),
                        steps = (data["steps"] as? List<String>) ?: emptyList(),
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
                        nutrition = (data["nutrition"] as? String) ?: "",
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
