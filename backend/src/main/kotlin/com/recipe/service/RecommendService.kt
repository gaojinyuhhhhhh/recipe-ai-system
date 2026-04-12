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
    private val objectMapper: ObjectMapper,
    private val userBehaviorService: UserBehaviorService
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
     * 个性化推荐(基于用户偏好) - 增强版
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
        
        // 分析用户喜好的菜系
        val favoriteCuisines = favoriteRecipes.mapNotNull { it.cuisine }.groupingBy { it }.eachCount()
        val preferredCuisine = favoriteCuisines.maxByOrNull { it.value }?.key
        
        // 计算每个食谱的偏好匹配分数
        val scoredRecipes = allRecipes.map { recipe ->
            var score = 0.0
            
            // 1. 菜系匹配 (权重25%)
            if (preferences?.cuisines != null) {
                if (recipe.cuisine in preferences.cuisines) score += 25.0
                else if (recipe.cuisine == preferredCuisine) score += 15.0
            }
            
            // 2. 口味匹配 (权重20%)
            if (preferences?.tastes != null) {
                val matchedTastes = preferences.tastes.count { recipe.tags?.contains(it) == true }
                score += (matchedTastes.toDouble() / preferences.tastes.size) * 20.0
            }
            
            // 3. 营养目标匹配 (权重20%)
            if (preferences?.nutritionGoals != null) {
                val matchedGoals = preferences.nutritionGoals.count { recipe.tags?.contains(it) == true }
                score += (matchedGoals.toDouble() / preferences.nutritionGoals.size) * 20.0
            }
            
            // 4. 难度匹配 (权重15%)
            if (preferences?.difficulty != null) {
                if (recipe.difficulty.name == preferences.difficulty) score += 15.0
                else if (preferences.difficulty == "EASY" && recipe.difficulty.name == "MEDIUM") score += 7.0
            }
            
            // 5. 烹饪时长匹配 (权重10%)
            val cookingTime = recipe.cookingTime
            if (preferences?.maxCookingTime != null && cookingTime != null) {
                if (cookingTime <= preferences.maxCookingTime) score += 10.0
                else if (cookingTime <= preferences.maxCookingTime + 10) score += 5.0
            }
            
            // 6. 饮食限制匹配 (权重10%) - 必须完全匹配
            if (preferences?.diet != null) {
                val matchedDiet = preferences.diet.count { recipe.tags?.contains(it) == true }
                score += (matchedDiet.toDouble() / preferences.diet.size) * 10.0
            }
            
            ScoredRecipe(recipe = recipe, score = score)
        }
        
        // 按分数排序，返回匹配度高的食谱
        return scoredRecipes
            .filter { it.score > 0 }  // 只返回有匹配的
            .sortedByDescending { it.score }
            .map { it.recipe }
    }
    
    /**
     * 计算推荐分数 - 增强偏好权重 + AI学习融合
     */
    private fun calculateRecommendationScore(
        recipe: Recipe,
        user: User,
        userIngredients: List<Ingredient>
    ): Double {
        var score = 0.0
        
        val recipeIngredients: List<RecipeIngredientSimple> = parseIngredients(recipe.ingredients)
        val userIngredientNames = userIngredients.map { it.name }.toSet()
        
        // 解析用户手动设置的偏好
        val prefs = try {
            if (user.preferences != null) {
                objectMapper.readValue<UserPreferences>(user.preferences!!)
            } else null
        } catch (e: Exception) { null }
        
        // 获取AI学习到的偏好画像
        val aiProfile = userBehaviorService.getUserAiProfile(user.id!!)
        
        // 1. 临期食材优先(权重25%)
        val expiringCount = userIngredients.count { ingredient ->
            val remaining = ingredient.getRemainingDays()
            remaining != null && remaining <= 7 && ingredient.name in recipeIngredients.map { it.name }
        }
        score += expiringCount * 25.0
        
        // 2. 用户手动偏好匹配(权重35%)
        if (prefs != null) {
            // 菜系匹配 (10%)
            if (recipe.cuisine in (prefs.cuisines ?: emptyList())) score += 10.0
            
            // 口味匹配 (8%)
            if (prefs.tastes?.any { recipe.tags?.contains(it) == true } == true) score += 8.0
            
            // 营养目标匹配 (8%)
            if (prefs.nutritionGoals?.any { recipe.tags?.contains(it) == true } == true) score += 8.0
            
            // 难度匹配 (5%)
            if (prefs.difficulty != null && recipe.difficulty.name == prefs.difficulty) score += 5.0
            
            // 烹饪时长匹配 (4%)
            val cookingTime = recipe.cookingTime
            if (prefs.maxCookingTime != null && cookingTime != null) {
                if (cookingTime <= prefs.maxCookingTime) score += 4.0
            }
        }
        
        // 3. AI学习偏好匹配(权重20%) - 自动学习到的偏好
        if (aiProfile != null) {
            // 学习到的菜系偏好 (8%)
            val cuisineScore = aiProfile.cuisinePreferences[recipe.cuisine] ?: 0
            score += (cuisineScore.coerceAtMost(50) / 50.0) * 8.0
            
            // 学习到的口味偏好 (6%)
            val tags = extractTags(recipe)
            val tasteScore = tags.sumOf { tag ->
                aiProfile.tastePreferences[tag] ?: 0
            }
            score += (tasteScore.coerceAtMost(30) / 30.0) * 6.0
            
            // 学习到的营养偏好 (6%)
            val nutritionTags = extractNutritionTags(recipe)
            val nutritionScore = nutritionTags.sumOf { tag ->
                aiProfile.nutritionPreferences[tag] ?: 0
            }
            score += (nutritionScore.coerceAtMost(30) / 30.0) * 6.0
        }
        
        // 4. 食材匹配度(权重15%)
        val matchedIngredients = recipeIngredients.count { it.name in userIngredientNames }
        val matchRate = matchedIngredients.toDouble() / recipeIngredients.size
        score += matchRate * 15.0
        
        // 5. 热度(权重5%)
        score += (recipe.favoriteCount / 100.0) * 5.0
        
        return score
    }
    
    private fun extractTags(recipe: Recipe): List<String> {
        return try {
            recipe.tags?.let {
                objectMapper.readValue(it, List::class.java) as List<String>
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun extractNutritionTags(recipe: Recipe): List<String> {
        val tags = extractTags(recipe)
        val nutritionKeywords = listOf("减脂", "低卡", "高蛋白", "增肌", "控糖", "低糖", "低脂", "素食")
        return tags.filter { it in nutritionKeywords }
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
 * 用户偏好 - 扩展版本
 */
data class UserPreferences(
    // 基础偏好
    val cuisines: List<String>?,        // 喜欢的菜系 [川菜, 粤菜, 西餐...]
    val tastes: List<String>?,          // 口味偏好 [清淡, 微辣, 酸甜...]
    val diet: List<String>?,            // 饮食限制 [素食, 清真, 无麸质, 低糖...]
    
    // 烹饪场景
    val difficulty: String?,            // 难度偏好 EASY/MEDIUM/HARD
    val maxCookingTime: Int?,           // 最大烹饪时长(分钟)
    val cookingScene: String?,          // 烹饪场景 [快手简餐, 周末大餐, 便当带饭, 宴客菜]
    
    // 营养目标
    val nutritionGoals: List<String>?,  // 营养目标 [减脂, 增肌, 控糖, 高蛋白, 均衡]
    
    // 设备与条件
    val cookingEquipment: List<String>?, // 烹饪设备 [燃气灶, 电磁炉, 烤箱, 空气炸锅]
    val dislikedIngredients: List<String>?, // 忌口食材 [海鲜, 牛羊肉, 鸡蛋...]
    
    // 家庭信息
    val familySize: Int?,               // 家庭人数
    val cookingFrequency: Int?          // 烹饪频率(次/周)
)
