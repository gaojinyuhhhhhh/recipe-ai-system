package com.recipe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.recipe.entity.Recipe
import com.recipe.entity.Ingredient
import com.recipe.entity.User
import com.recipe.repository.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 智能推荐服务
 * 对应功能: 1.1.5 智能食谱推荐模块
 */
@Service
class RecommendService(
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val userRepository: UserRepository,
    private val favoriteRepository: RecipeFavoriteRepository,
    private val objectMapper: ObjectMapper
) {
    
    /**
     * 个性化推荐食谱
     * 缓存1小时
     */
    @Cacheable(value = ["recommendations"], key = "#userId")
    fun getRecommendations(userId: Long, limit: Int = 20): RecommendationResult {
        val user = userRepository.findById(userId).orElse(null) 
            ?: throw Exception("用户不存在")
        
        val userIngredients = ingredientRepository.findByUserIdAndIsConsumedFalse(userId)
        val allRecipes = recipeRepository.findByIsPublicTrue()
        
        // 计算推荐分数
        val scoredRecipes = allRecipes.map { recipe ->
            ScoredRecipe(
                recipe = recipe,
                score = calculateRecommendationScore(recipe, user, userIngredients)
            )
        }.sortedByDescending { it.score }
        
        // 分类推荐
        val expiringRecipes = getExpiringIngredientRecipes(userIngredients, allRecipes)
        val personalizedRecipes = getPersonalizedRecipes(user, allRecipes)
        val hotRecipes = recipeRepository.findByIsPublicTrueOrderByFavoriteCountDesc().take(10)
        
        return RecommendationResult(
            all = scoredRecipes.take(limit).map { it.recipe },
            expiringIngredients = expiringRecipes.take(5),
            personalized = personalizedRecipes.take(10),
            hot = hotRecipes,
            scores = scoredRecipes.take(limit).associate { it.recipe.id!! to it.score }
        )
    }
    
    /**
     * 临期食材优先推荐
     */
    fun getExpiringIngredientRecipes(
        userIngredients: List<Ingredient>,
        allRecipes: List<Recipe>
    ): List<Recipe> {
        // 筛选7天内到期的食材
        val expiringIngredients = userIngredients.filter { ingredient ->
            val remaining = ingredient.getRemainingDays()
            remaining != null && remaining <= 7
        }
        
        if (expiringIngredients.isEmpty()) return emptyList()
        
        val expiringNames = expiringIngredients.map { it.name }.toSet()
        
        // 找到包含这些食材的食谱
        return allRecipes.filter { recipe ->
            val ingredients: List<RecipeIngredientSimple> = parseIngredients(recipe.ingredients)
            ingredients.any { it.name in expiringNames }
        }.sortedByDescending { recipe ->
            // 按包含的临期食材数量排序
            val ingredients: List<RecipeIngredientSimple> = parseIngredients(recipe.ingredients)
            ingredients.count { it.name in expiringNames }
        }
    }
    
    /**
     * 个性化推荐(基于用户偏好)
     */
    fun getPersonalizedRecipes(user: User, allRecipes: List<Recipe>): List<Recipe> {
        // 解析用户偏好
        val preferences = try {
            if (user.preferences != null) {
                objectMapper.readValue<UserPreferences>(user.preferences!!)
            } else null
        } catch (e: Exception) {
            null
        }
        
        // 获取用户收藏历史
        val favorites = favoriteRepository.findByUserId(user.id!!)
        val favoriteRecipeIds = favorites.map { it.recipeId }.toSet()
        val favoriteRecipes = recipeRepository.findAllById(favoriteRecipeIds)
        
        // 分析用户喜好的菜系、难度
        val favoriteCuisines = favoriteRecipes.mapNotNull { it.cuisine }.groupingBy { it }.eachCount()
        val preferredCuisine = favoriteCuisines.maxByOrNull { it.value }?.key
        
        // 筛选匹配用户偏好的食谱
        return allRecipes.filter { recipe ->
            var match = true
            
            // 菜系匹配
            if (preferences?.cuisines != null) {
                match = match && (recipe.cuisine in preferences.cuisines || recipe.cuisine == preferredCuisine)
            }
            
            // 口味匹配
            if (preferences?.tastes != null) {
                match = match && preferences.tastes.any { recipe.tags?.contains(it) == true }
            }
            
            // 饮食需求匹配
            if (preferences?.diet != null) {
                match = match && recipe.tags?.contains(preferences.diet) == true
            }
            
            match
        }.sortedByDescending { it.favoriteCount }
    }
    
    /**
     * 计算推荐分数
     */
    private fun calculateRecommendationScore(
        recipe: Recipe,
        user: User,
        userIngredients: List<Ingredient>
    ): Double {
        var score = 0.0
        
        val recipeIngredients: List<RecipeIngredientSimple> = parseIngredients(recipe.ingredients)
        val userIngredientNames = userIngredients.map { it.name }.toSet()
        
        // 1. 临期食材优先(权重40%)
        val expiringCount = userIngredients.count { ingredient ->
            val remaining = ingredient.getRemainingDays()
            remaining != null && remaining <= 7 && ingredient.name in recipeIngredients.map { it.name }
        }
        score += expiringCount * 40.0
        
        // 2. 用户偏好匹配(权重30%)
        try {
            if (user.preferences != null) {
                val prefs: UserPreferences = objectMapper.readValue(user.preferences!!)
                if (recipe.cuisine in (prefs.cuisines ?: emptyList())) score += 30.0
                if (prefs.tastes?.any { recipe.tags?.contains(it) == true } == true) score += 20.0
            }
        } catch (e: Exception) {}
        
        // 3. 食材匹配度(权重20%)
        val matchedIngredients = recipeIngredients.count { it.name in userIngredientNames }
        val matchRate = matchedIngredients.toDouble() / recipeIngredients.size
        score += matchRate * 20.0
        
        // 4. 热度(权重10%)
        score += (recipe.favoriteCount / 100.0) * 10.0
        
        return score
    }
    
    /**
     * 智能组合临期食材
     */
    fun smartCombineExpiring(userId: Long): List<IngredientCombination> {
        val userIngredients = ingredientRepository.findByUserIdAndIsConsumedFalse(userId)
        val expiringIngredients = userIngredients.filter { ingredient ->
            val remaining = ingredient.getRemainingDays()
            remaining != null && remaining <= 7
        }
        
        if (expiringIngredients.size < 2) return emptyList()
        
        val allRecipes = recipeRepository.findByIsPublicTrue()
        val combinations = mutableListOf<IngredientCombination>()
        
        // 找到能同时使用多个临期食材的食谱
        allRecipes.forEach { recipe ->
            val recipeIngredients: List<RecipeIngredientSimple> = parseIngredients(recipe.ingredients)
            val matchedExpiring = expiringIngredients.filter { ingredient ->
                recipeIngredients.any { it.name == ingredient.name }
            }
            
            if (matchedExpiring.size >= 2) {
                combinations.add(
                    IngredientCombination(
                        recipe = recipe,
                        matchedIngredients = matchedExpiring,
                        matchCount = matchedExpiring.size
                    )
                )
            }
        }
        
        return combinations.sortedByDescending { it.matchCount }
    }
    
    /**
     * 解析食谱食材
     */
    private fun parseIngredients(ingredientsJson: String): List<RecipeIngredientSimple> {
        return try {
            objectMapper.readValue(ingredientsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * 评分后的食谱
 */
data class ScoredRecipe(
    val recipe: Recipe,
    val score: Double
)

/**
 * 推荐结果
 */
data class RecommendationResult(
    val all: List<Recipe>,              // 全部推荐
    val expiringIngredients: List<Recipe>,  // 临期食材快用
    val personalized: List<Recipe>,     // 我的口味专属
    val hot: List<Recipe>,              // 热门AI优化食谱
    val scores: Map<Long, Double>       // 各食谱的推荐分数
)

/**
 * 食材智能组合
 */
data class IngredientCombination(
    val recipe: Recipe,
    val matchedIngredients: List<Ingredient>,
    val matchCount: Int
)

/**
 * 简化的食谱食材
 */
data class RecipeIngredientSimple(
    val name: String
)

/**
 * 用户偏好
 */
data class UserPreferences(
    val cuisines: List<String>?,  // 菜系
    val tastes: List<String>?,    // 口味
    val diet: String?             // 饮食需求
)
