package com.recipe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.recipe.ai.RecipeGenerator
import com.recipe.entity.*
import com.recipe.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 食谱管理服务
 * 对应功能: 1.1.4 食谱社区模块
 */
@Service
class RecipeService(
    private val recipeRepository: RecipeRepository,
    private val commentRepository: RecipeCommentRepository,
    private val favoriteRepository: RecipeFavoriteRepository,
    private val ingredientRepository: IngredientRepository,
    private val recipeGenerator: RecipeGenerator,
    private val objectMapper: ObjectMapper
) {
    
    /**
     * 创建食谱
     */
    @Transactional
    fun createRecipe(recipe: Recipe): Recipe {
        // AI质量评级
        val rating = recipeGenerator.rateRecipe(
            recipe.ingredients,
            recipe.steps,
            recipe.title
        )
        
        recipe.aiRating = rating.getAiRating()
        recipe.aiRatingDetail = objectMapper.writeValueAsString(rating.scores)
        recipe.aiSuggestion = rating.suggestions.joinToString("\n")
        
        return recipeRepository.save(recipe)
    }
    
    /**
     * AI优化食谱
     */
    @Transactional
    fun optimizeRecipe(recipeId: Long, userId: Long): Recipe {
        val original = recipeRepository.findById(recipeId)
            .orElseThrow { Exception("食谱不存在") }
        
        if (original.userId != userId) {
            throw Exception("只能优化自己的食谱")
        }
        
        // 获取AI优化建议
        val suggestions = original.aiSuggestion?.split("\n") ?: emptyList()
        if (suggestions.isEmpty()) {
            throw Exception("该食谱暂无优化建议")
        }
        
        // AI生成优化版本
        val optimized = recipeGenerator.optimizeRecipe(
            original.title,
            original.ingredients,
            original.steps,
            suggestions
        )
        
        // 创建新的优化版食谱
        val optimizedRecipe = Recipe(
            userId = userId,
            title = "${original.title} (AI优化版)",
            description = optimized.description,
            ingredients = objectMapper.writeValueAsString(optimized.ingredients),
            steps = objectMapper.writeValueAsString(optimized.steps),
            cookingTime = optimized.cookingTime,
            difficulty = Difficulty.valueOf(optimized.difficulty),
            cuisine = optimized.cuisine,
            tags = objectMapper.writeValueAsString(optimized.tags),
            isAiOptimized = true,
            originalRecipeId = recipeId
        )
        
        return recipeRepository.save(optimizedRecipe)
    }
    
    /**
     * 更新食谱
     */
    @Transactional
    fun updateRecipe(id: Long, userId: Long, updates: Recipe): Recipe {
        val existing = recipeRepository.findById(id)
            .orElseThrow { Exception("食谱不存在") }
        
        if (existing.userId != userId) {
            throw Exception("无权限操作")
        }
        
        existing.apply {
            title = updates.title
            description = updates.description
            coverImage = updates.coverImage
            ingredients = updates.ingredients
            steps = updates.steps
            cookingTime = updates.cookingTime
            difficulty = updates.difficulty
            cuisine = updates.cuisine
            tags = updates.tags
            isPublic = updates.isPublic
        }
        
        // 重新评级
        val rating = recipeGenerator.rateRecipe(
            existing.ingredients,
            existing.steps,
            existing.title
        )
        existing.aiRating = rating.getAiRating()
        existing.aiRatingDetail = objectMapper.writeValueAsString(rating.scores)
        existing.aiSuggestion = rating.suggestions.joinToString("\n")
        
        return recipeRepository.save(existing)
    }
    
    /**
     * 删除食谱
     */
    @Transactional
    fun deleteRecipe(id: Long, userId: Long) {
        val recipe = recipeRepository.findById(id)
            .orElseThrow { Exception("食谱不存在") }
        
        if (recipe.userId != userId) {
            throw Exception("无权限操作")
        }
        
        // 删除关联的评论和收藏
        commentRepository.findByRecipeIdOrderByCreatedAtDesc(id).forEach {
            commentRepository.delete(it)
        }
        
        favoriteRepository.findByUserId(userId)
            .filter { it.recipeId == id }
            .forEach { favoriteRepository.delete(it) }
        
        recipeRepository.delete(recipe)
    }
    
    /**
     * 查询食谱详情(带食材可制作状态)
     */
    fun getRecipeDetail(recipeId: Long, userId: Long?): RecipeDetail {
        val recipe = recipeRepository.findById(recipeId)
            .orElseThrow { Exception("食谱不存在") }
        
        // 增加浏览量
        recipe.viewCount++
        recipeRepository.save(recipe)
        
        var canMake = false
        var missingIngredients: List<String> = emptyList()
        
        if (userId != null) {
            // 检查用户是否有足够的食材
            val userIngredients = ingredientRepository.findByUserIdAndIsConsumedFalse(userId)
            val userIngredientNames = userIngredients.map { it.name }.toSet()
            
            val recipeIngredients: List<com.recipe.ai.RecipeIngredient> = 
                objectMapper.readValue(recipe.ingredients, objectMapper.typeFactory.constructCollectionType(
                    List::class.java, com.recipe.ai.RecipeIngredient::class.java
                ))
            
            missingIngredients = recipeIngredients
                .map { it.name }
                .filter { it !in userIngredientNames }
            
            canMake = missingIngredients.isEmpty()
        }
        
        return RecipeDetail(
            recipe = recipe,
            canMake = canMake,
            missingIngredients = missingIngredients,
            isFavorited = userId?.let { 
                favoriteRepository.existsByUserIdAndRecipeId(it, recipeId) 
            } ?: false
        )
    }
    
    /**
     * 搜索食谱
     */
    fun searchRecipes(
        keyword: String?,
        cuisine: String?,
        difficulty: Difficulty?,
        tags: List<String>?
    ): List<Recipe> {
        var recipes = if (keyword != null) {
            recipeRepository.searchRecipes(keyword)
        } else {
            recipeRepository.findByIsPublicTrue()
        }
        
        // 筛选菜系
        if (cuisine != null) {
            recipes = recipes.filter { it.cuisine == cuisine }
        }
        
        // 筛选难度
        if (difficulty != null) {
            recipes = recipes.filter { it.difficulty == difficulty }
        }
        
        // 筛选标签
        if (!tags.isNullOrEmpty()) {
            recipes = recipes.filter { recipe ->
                tags.any { tag -> recipe.tags?.contains(tag) == true }
            }
        }
        
        return recipes
    }
    
    /**
     * 添加评论
     */
    @Transactional
    fun addComment(comment: RecipeComment): RecipeComment {
        val saved = commentRepository.save(comment)
        
        // 更新评论数
        val recipe = recipeRepository.findById(comment.recipeId).orElse(null)
        recipe?.let {
            it.commentCount++
            recipeRepository.save(it)
        }
        
        return saved
    }
    
    /**
     * 收藏/取消收藏
     */
    @Transactional
    fun toggleFavorite(userId: Long, recipeId: Long): Boolean {
        val existing = favoriteRepository.findByUserIdAndRecipeId(userId, recipeId)
        
        return if (existing != null) {
            // 取消收藏
            favoriteRepository.delete(existing)
            
            val recipe = recipeRepository.findById(recipeId).orElse(null)
            recipe?.let {
                it.favoriteCount = maxOf(0, it.favoriteCount - 1)
                recipeRepository.save(it)
            }
            false
        } else {
            // 添加收藏
            val favorite = RecipeFavorite(userId = userId, recipeId = recipeId)
            favoriteRepository.save(favorite)
            
            val recipe = recipeRepository.findById(recipeId).orElse(null)
            recipe?.let {
                it.favoriteCount++
                recipeRepository.save(it)
            }
            true
        }
    }
    
    /**
     * 查询用户收藏的食谱
     */
    fun getUserFavorites(userId: Long): List<Recipe> {
        val favorites = favoriteRepository.findByUserId(userId)
        val recipeIds = favorites.map { it.recipeId }
        return recipeRepository.findAllById(recipeIds)
    }
    
    /**
     * 查询用户的食谱
     */
    fun getUserRecipes(userId: Long): List<Recipe> {
        return recipeRepository.findByUserId(userId)
    }
    
    /**
     * 热门食谱
     */
    fun getHotRecipes(limit: Int = 10): List<Recipe> {
        return recipeRepository.findByIsPublicTrueOrderByFavoriteCountDesc()
            .take(limit)
    }
}

/**
 * 食谱详情(含可制作状态)
 */
data class RecipeDetail(
    val recipe: Recipe,
    val canMake: Boolean,
    val missingIngredients: List<String>,
    val isFavorited: Boolean
)
