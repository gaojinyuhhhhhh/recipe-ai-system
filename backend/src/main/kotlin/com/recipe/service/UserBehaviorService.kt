package com.recipe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.recipe.entity.*
import com.recipe.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 用户行为追踪与AI学习服务
 * 通过分析用户行为自动学习偏好
 */
@Service
class UserBehaviorService(
    private val userRepository: UserRepository,
    private val recipeRepository: RecipeRepository,
    private val favoriteRepository: RecipeFavoriteRepository,
    private val commentRepository: RecipeCommentRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * 行为类型枚举
     */
    enum class BehaviorType(val weight: Int) {
        VIEW(1),           // 浏览
        FAVORITE(5),       // 收藏
        UNFAVORITE(-3),    // 取消收藏
        COMMENT(3),        // 评论
        COMPLETE(4),       // 制作完成
        DOWNLOAD(4),       // 下载到本地
        SHARE(2)           // 分享
    }

    /**
     * 记录用户行为
     */
    @Transactional
    fun recordBehavior(userId: Long, recipeId: Long, behaviorType: BehaviorType) {
        // 获取用户信息
        val user = userRepository.findById(userId).orElse(null) ?: return
        
        // 获取食谱信息
        val recipe = recipeRepository.findById(recipeId).orElse(null) ?: return
        
        // 解析当前AI画像
        val aiProfile = parseAiProfile(user.aiProfile)
        
        // 根据行为类型更新画像
        val updatedProfile = when (behaviorType) {
            BehaviorType.FAVORITE -> learnFromFavorite(aiProfile, recipe, behaviorType.weight)
            BehaviorType.UNFAVORITE -> learnFromUnfavorite(aiProfile, recipe, behaviorType.weight)
            BehaviorType.COMMENT -> learnFromComment(aiProfile, recipe, behaviorType.weight)
            BehaviorType.COMPLETE -> learnFromComplete(aiProfile, recipe, behaviorType.weight)
            BehaviorType.VIEW -> learnFromView(aiProfile, recipe, behaviorType.weight)
            BehaviorType.DOWNLOAD -> learnFromDownload(aiProfile, recipe, behaviorType.weight)
            BehaviorType.SHARE -> learnFromShare(aiProfile, recipe, behaviorType.weight)
        }
        
        // 保存更新后的画像
        user.aiProfile = objectMapper.writeValueAsString(updatedProfile)
        userRepository.save(user)
    }

    /**
     * 从收藏行为学习
     */
    private fun learnFromFavorite(
        profile: UserAiProfile,
        recipe: Recipe,
        weight: Int
    ): UserAiProfile {
        return profile.copy(
            cuisinePreferences = updatePreferenceMap(
                profile.cuisinePreferences,
                recipe.cuisine,
                weight
            ),
            tastePreferences = updatePreferenceMap(
                profile.tastePreferences,
                extractTags(recipe),
                weight
            ),
            difficultyPreference = updateDifficultyPreference(
                profile.difficultyPreference,
                recipe.difficulty.name,
                weight
            ),
            nutritionPreferences = updatePreferenceMap(
                profile.nutritionPreferences,
                extractNutritionTags(recipe),
                weight
            ),
            totalInteractions = profile.totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * 从取消收藏学习
     */
    private fun learnFromUnfavorite(
        profile: UserAiProfile,
        recipe: Recipe,
        weight: Int
    ): UserAiProfile {
        return profile.copy(
            cuisinePreferences = updatePreferenceMap(
                profile.cuisinePreferences,
                recipe.cuisine,
                weight
            ),
            tastePreferences = updatePreferenceMap(
                profile.tastePreferences,
                extractTags(recipe),
                weight
            ),
            dislikedIngredients = updateDislikedIngredients(profile, recipe),
            totalInteractions = profile.totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * 从评论行为学习
     */
    private fun learnFromComment(
        profile: UserAiProfile,
        recipe: Recipe,
        weight: Int
    ): UserAiProfile {
        return profile.copy(
            cuisinePreferences = updatePreferenceMap(
                profile.cuisinePreferences,
                recipe.cuisine,
                weight
            ),
            tastePreferences = updatePreferenceMap(
                profile.tastePreferences,
                extractTags(recipe),
                weight
            ),
            totalInteractions = profile.totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * 从完成制作学习
     */
    private fun learnFromComplete(
        profile: UserAiProfile,
        recipe: Recipe,
        weight: Int
    ): UserAiProfile {
        return profile.copy(
            cuisinePreferences = updatePreferenceMap(
                profile.cuisinePreferences,
                recipe.cuisine,
                weight
            ),
            difficultyPreference = updateDifficultyPreference(
                profile.difficultyPreference,
                recipe.difficulty.name,
                weight
            ),
            cookingTimePreference = updateCookingTimePreference(
                profile.cookingTimePreference,
                recipe.cookingTime,
                weight
            ),
            totalInteractions = profile.totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * 从浏览行为学习
     */
    private fun learnFromView(
        profile: UserAiProfile,
        recipe: Recipe,
        weight: Int
    ): UserAiProfile {
        return profile.copy(
            cuisinePreferences = updatePreferenceMap(
                profile.cuisinePreferences,
                recipe.cuisine,
                weight
            ),
            totalInteractions = profile.totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * 从下载行为学习
     */
    private fun learnFromDownload(
        profile: UserAiProfile,
        recipe: Recipe,
        weight: Int
    ): UserAiProfile {
        return profile.copy(
            cuisinePreferences = updatePreferenceMap(
                profile.cuisinePreferences,
                recipe.cuisine,
                weight
            ),
            totalInteractions = profile.totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * 从分享行为学习
     */
    private fun learnFromShare(
        profile: UserAiProfile,
        recipe: Recipe,
        weight: Int
    ): UserAiProfile {
        return profile.copy(
            cuisinePreferences = updatePreferenceMap(
                profile.cuisinePreferences,
                recipe.cuisine,
                weight
            ),
            totalInteractions = profile.totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * 获取用户AI画像
     */
    fun getUserAiProfile(userId: Long): UserAiProfile? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        return parseAiProfile(user.aiProfile)
    }

    /**
     * 分析用户偏好并生成建议
     */
    fun analyzePreferences(userId: Long): PreferenceAnalysis {
        val profile = getUserAiProfile(userId) ?: return PreferenceAnalysis()
        
        // 获取用户收藏历史
        val favorites = favoriteRepository.findByUserId(userId)
        val favoriteRecipes = recipeRepository.findAllById(favorites.map { it.recipeId })
        
        // 分析最喜欢的菜系
        val topCuisines = profile.cuisinePreferences
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }
        
        // 分析最喜欢的口味
        val topTastes = profile.tastePreferences
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }
        
        // 分析营养目标
        val topNutrition = profile.nutritionPreferences
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }
        
        // 推荐难度
        val recommendedDifficulty = profile.difficultyPreference
            .entries
            .maxByOrNull { it.value }
            ?.key ?: "MEDIUM"
        
        // 推荐烹饪时长
        val recommendedCookingTime = profile.cookingTimePreference.average
            .takeIf { it > 0 }
            ?.toInt() ?: 30
        
        return PreferenceAnalysis(
            topCuisines = topCuisines,
            topTastes = topTastes,
            topNutrition = topNutrition,
            recommendedDifficulty = recommendedDifficulty,
            recommendedCookingTime = recommendedCookingTime,
            dislikedIngredients = profile.dislikedIngredients,
            totalInteractions = profile.totalInteractions,
            confidenceScore = calculateConfidenceScore(profile)
        )
    }

    /**
     * 将AI画像转换为UserPreferences格式
     */
    fun convertToUserPreferences(userId: Long): UserPreferences? {
        val analysis = analyzePreferences(userId)
        if (analysis.totalInteractions < 5) return null  // 数据不足
        
        return UserPreferences(
            cuisines = analysis.topCuisines.map { it.first },
            tastes = analysis.topTastes.map { it.first },
            diet = emptyList(),
            difficulty = analysis.recommendedDifficulty,
            maxCookingTime = analysis.recommendedCookingTime + 15,  // 留有余地
            cookingScene = null,
            nutritionGoals = analysis.topNutrition.map { it.first },
            cookingEquipment = emptyList(),
            dislikedIngredients = analysis.dislikedIngredients,
            familySize = null,
            cookingFrequency = null
        )
    }

    // ==================== 辅助方法 ====================

    private fun parseAiProfile(json: String?): UserAiProfile {
        return try {
            if (json != null) {
                objectMapper.readValue(json, UserAiProfile::class.java)
            } else {
                UserAiProfile()
            }
        } catch (e: Exception) {
            UserAiProfile()
        }
    }

    private fun updatePreferenceMap(
        current: Map<String, Int>,
        key: String?,
        weight: Int
    ): Map<String, Int> {
        if (key == null) return current
        return current.toMutableMap().apply {
            this[key] = (this[key] ?: 0) + weight
        }
    }

    private fun updatePreferenceMap(
        current: Map<String, Int>,
        keys: List<String>,
        weight: Int
    ): Map<String, Int> {
        return current.toMutableMap().apply {
            keys.forEach { key ->
                this[key] = (this[key] ?: 0) + weight
            }
        }
    }

    private fun updateDifficultyPreference(
        current: Map<String, Int>,
        difficulty: String,
        weight: Int
    ): Map<String, Int> {
        return current.toMutableMap().apply {
            this[difficulty] = (this[difficulty] ?: 0) + weight
        }
    }

    private fun updateCookingTimePreference(
        current: CookingTimePreference,
        cookingTime: Int?,
        weight: Int
    ): CookingTimePreference {
        if (cookingTime == null) return current
        
        val newCount = current.count + 1
        val newAverage = ((current.average * current.count) + cookingTime) / newCount
        
        return current.copy(
            average = newAverage,
            count = newCount
        )
    }

    private fun updateDislikedIngredients(
        profile: UserAiProfile,
        recipe: Recipe
    ): List<String> {
        // 从取消收藏的食谱中提取可能的忌口食材
        // 这里简化处理，实际可以结合更多分析
        return profile.dislikedIngredients
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

    private fun calculateConfidenceScore(profile: UserAiProfile): Double {
        // 根据交互次数计算置信度
        return when {
            profile.totalInteractions >= 50 -> 0.95
            profile.totalInteractions >= 30 -> 0.85
            profile.totalInteractions >= 20 -> 0.75
            profile.totalInteractions >= 10 -> 0.60
            profile.totalInteractions >= 5 -> 0.40
            else -> 0.20
        }
    }
}

/**
 * 用户AI画像数据类
 */
data class UserAiProfile(
    val cuisinePreferences: Map<String, Int> = emptyMap(),      // 菜系偏好分数
    val tastePreferences: Map<String, Int> = emptyMap(),        // 口味偏好分数
    val difficultyPreference: Map<String, Int> = emptyMap(),    // 难度偏好分数
    val nutritionPreferences: Map<String, Int> = emptyMap(),    // 营养目标偏好
    val cookingTimePreference: CookingTimePreference = CookingTimePreference(),
    val dislikedIngredients: List<String> = emptyList(),        // 可能不喜欢的食材
    val totalInteractions: Int = 0,                             // 总交互次数
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

/**
 * 烹饪时间偏好
 */
data class CookingTimePreference(
    val average: Double = 0.0,
    val count: Int = 0
)

/**
 * 偏好分析结果
 */
data class PreferenceAnalysis(
    val topCuisines: List<Pair<String, Int>> = emptyList(),
    val topTastes: List<Pair<String, Int>> = emptyList(),
    val topNutrition: List<Pair<String, Int>> = emptyList(),
    val recommendedDifficulty: String = "MEDIUM",
    val recommendedCookingTime: Int = 30,
    val dislikedIngredients: List<String> = emptyList(),
    val totalInteractions: Int = 0,
    val confidenceScore: Double = 0.0
)
