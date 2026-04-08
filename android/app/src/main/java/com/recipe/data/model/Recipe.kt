package com.recipe.data.model

/**
 * 食谱数据模型
 */
data class Recipe(
    val id: Long? = null,
    val userId: Long = 0,
    val title: String = "",
    val description: String? = null,
    val coverImage: String? = null,
    val ingredients: String = "[]",  // JSON: [{"name":"鸡蛋","quantity":2,"unit":"个"}]
    val steps: String = "[]",        // JSON: [{"step":1,"content":"打散鸡蛋","duration":60}]
    val cookingTime: Int? = null,
    val difficulty: String = "MEDIUM",
    val cuisine: String? = null,
    val tags: String? = null,        // JSON: ["减脂","低糖"]
    val aiRating: String? = null,
    val aiRatingDetail: String? = null,
    val aiSuggestion: String? = null,
    val isAiOptimized: Boolean = false,
    val originalRecipeId: Long? = null,
    val viewCount: Long = 0,
    val favoriteCount: Long = 0,
    val commentCount: Long = 0,
    val isPublic: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun getDifficultyDisplay(): String = when (difficulty) {
        "EASY" -> "简单"
        "MEDIUM" -> "中等"
        "HARD" -> "困难"
        else -> difficulty
    }

    fun getAiRatingDisplay(): String? = when (aiRating) {
        "S" -> "S级优秀"
        "A" -> "A级良好"
        "B" -> "B级合格"
        "C" -> "C级待改进"
        else -> aiRating
    }
}

/**
 * 食谱详情（含可制作状态）
 */
data class RecipeDetail(
    val recipe: Recipe,
    val canMake: Boolean = false,
    val missingIngredients: List<String> = emptyList(),
    val isFavorited: Boolean = false
)

/**
 * 食谱食材项
 */
data class RecipeIngredient(
    val name: String = "",
    val quantity: Any? = null,
    val unit: String? = null
)

/**
 * 食谱步骤项
 */
data class RecipeStep(
    val step: Int = 0,
    val content: String = "",
    val duration: Int? = null,
    val image: String? = null
)

/**
 * 食谱评论
 */
data class RecipeComment(
    val id: Long? = null,
    val recipeId: Long = 0,
    val userId: Long = 0,
    val content: String = "",
    val rating: Int? = null,
    val createdAt: String? = null
)
