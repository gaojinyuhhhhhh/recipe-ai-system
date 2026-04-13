package com.recipe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.recipe.ai.RecipeGenerator
import com.recipe.controller.SaveRecipeRequest
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
    private val userRepository: UserRepository,
    private val recipeGenerator: RecipeGenerator,
    private val objectMapper: ObjectMapper,
    private val userBehaviorService: UserBehaviorService
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
     * 注意：已发布到社区的食谱不能直接删除，需要先下架
     */
    @Transactional
    fun deleteRecipe(id: Long, userId: Long) {
        val recipe = recipeRepository.findById(id)
            .orElseThrow { Exception("食谱不存在") }
        
        if (recipe.userId != userId) {
            throw Exception("无权限操作")
        }
        
        // 已发布的食谱不能直接删除
        if (recipe.isPublic) {
            throw Exception("该食谱已发布到社区，请先从社区下架后再删除")
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
     * 下架食谱（从社区移除，转为私有）
     */
    @Transactional
    fun unpublishRecipe(id: Long, userId: Long): Recipe {
        val recipe = recipeRepository.findById(id)
            .orElseThrow { Exception("食谱不存在") }
        
        if (recipe.userId != userId) {
            throw Exception("无权限操作")
        }
        
        if (!recipe.isPublic) {
            throw Exception("该食谱未发布到社区")
        }
        
        recipe.isPublic = false
        return recipeRepository.save(recipe)
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
     * 搜索食谱(带作者信息)
     */
    fun searchRecipes(
        keyword: String?,
        cuisine: String?,
        difficulty: Difficulty?,
        tags: List<String>?
    ): List<Map<String, Any?>> {
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
        
        // 组装带作者信息的数据
        return recipes.map { recipe -> convertToRecipeWithAuthor(recipe) }
    }
    
    /**
     * 将Recipe转换为带作者信息的Map
     */
    private fun convertToRecipeWithAuthor(recipe: Recipe): Map<String, Any?> {
        val user = userRepository.findById(recipe.userId).orElse(null)
        return mapOf(
            "id" to recipe.id,
            "userId" to recipe.userId,
            "title" to recipe.title,
            "description" to recipe.description,
            "coverImage" to recipe.coverImage,
            "ingredients" to recipe.ingredients,
            "steps" to recipe.steps,
            "cookingTime" to recipe.cookingTime,
            "difficulty" to recipe.difficulty.name,
            "cuisine" to recipe.cuisine,
            "tags" to recipe.tags,
            "aiRating" to recipe.aiRating?.name,
            "aiRatingDetail" to recipe.aiRatingDetail,
            "aiSuggestion" to recipe.aiSuggestion,
            "isAiOptimized" to recipe.isAiOptimized,
            "originalRecipeId" to recipe.originalRecipeId,
            "viewCount" to recipe.viewCount,
            "favoriteCount" to recipe.favoriteCount,
            "commentCount" to recipe.commentCount,
            "isPublic" to recipe.isPublic,
            "createdAt" to recipe.createdAt.toString(),
            "updatedAt" to recipe.updatedAt.toString(),
            "authorName" to (user?.nickname ?: user?.username ?: "未知用户"),
            "authorAvatar" to user?.avatar,
            "isAiGenerated" to recipe.isAiGenerated,
            "isAiOptimized" to recipe.isAiOptimized
        )
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
        
        // 记录评论行为
        userBehaviorService.recordBehavior(comment.userId, comment.recipeId, UserBehaviorService.BehaviorType.COMMENT)
        
        return saved
    }

    /**
     * 删除评论(仅评论作者可删除)
     */
    @Transactional
    fun deleteComment(commentId: Long, userId: Long) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { Exception("评论不存在") }

        if (comment.userId != userId) {
            throw Exception("无权限删除此评论")
        }

        commentRepository.delete(comment)

        // 更新评论数
        val recipe = recipeRepository.findById(comment.recipeId).orElse(null)
        recipe?.let {
            it.commentCount = maxOf(0, it.commentCount - 1)
            recipeRepository.save(it)
        }
    }

    /**
     * 获取用户的所有历史评论（包含食谱标题）
     */
    fun getUserComments(userId: Long): List<Map<String, Any?>> {
        val comments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId)
        return comments.map { comment ->
            val recipe = recipeRepository.findById(comment.recipeId).orElse(null)
            mapOf(
                "id" to comment.id,
                "recipeId" to comment.recipeId,
                "recipeTitle" to (recipe?.title ?: "未知食谱"),
                "userId" to comment.userId,
                "content" to comment.content,
                "rating" to comment.rating,
                "createdAt" to comment.createdAt.toString()
            )
        }
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
            
            // 记录取消收藏行为
            userBehaviorService.recordBehavior(userId, recipeId, UserBehaviorService.BehaviorType.UNFAVORITE)
            
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
            
            // 记录收藏行为
            userBehaviorService.recordBehavior(userId, recipeId, UserBehaviorService.BehaviorType.FAVORITE)
            
            true
        }
    }
    
    /**
     * 查询用户收藏的食谱(带作者信息)
     */
    fun getUserFavorites(userId: Long): List<Map<String, Any?>> {
        val favorites = favoriteRepository.findByUserId(userId)
        val recipeIds = favorites.map { it.recipeId }
        val recipes = recipeRepository.findAllById(recipeIds)
        return recipes.map { convertToRecipeWithAuthor(it) }
    }
    
    /**
     * 查询用户的食谱(带作者信息)
     */
    fun getUserRecipes(userId: Long): List<Map<String, Any?>> {
        val recipes = recipeRepository.findByUserId(userId)
        return recipes.map { convertToRecipeWithAuthor(it) }
    }
    
    /**
     * 热门食谱(带作者信息)
     */
    fun getHotRecipes(limit: Int = 10): List<Map<String, Any?>> {
        val recipes = recipeRepository.findByIsPublicTrueOrderByFavoriteCountDesc()
            .take(limit)
        return recipes.map { convertToRecipeWithAuthor(it) }
    }

    /**
     * 获取食谱评论列表(含用户昵称)
     */
    fun getRecipeCommentsWithUser(recipeId: Long): List<Map<String, Any?>> {
        val comments = commentRepository.findByRecipeIdOrderByCreatedAtDesc(recipeId)
        return comments.map { comment ->
            val user = userRepository.findById(comment.userId).orElse(null)
            mapOf(
                "id" to comment.id,
                "recipeId" to comment.recipeId,
                "userId" to comment.userId,
                "username" to (user?.nickname ?: user?.username ?: "匿名用户"),
                "content" to comment.content,
                "rating" to comment.rating,
                "createdAt" to comment.createdAt.toString()
            )
        }
    }

    /**
     * 克隆/下载食谱到用户名下
     */
    @Transactional
    fun cloneRecipe(recipeId: Long, userId: Long): Recipe {
        val original = recipeRepository.findById(recipeId)
            .orElseThrow { Exception("食谱不存在") }

        val cloned = Recipe(
            userId = userId,
            title = original.title,
            description = original.description,
            coverImage = original.coverImage,
            ingredients = original.ingredients,
            steps = original.steps,
            cookingTime = original.cookingTime,
            difficulty = original.difficulty,
            cuisine = original.cuisine,
            tags = original.tags,
            aiRating = original.aiRating,
            aiRatingDetail = original.aiRatingDetail,
            aiSuggestion = original.aiSuggestion,
            isAiOptimized = original.isAiOptimized,
            isAiGenerated = original.isAiGenerated,  // 保留AI生成标记
            originalRecipeId = recipeId,
            isPublic = false
        )
        
        // 记录下载行为
        userBehaviorService.recordBehavior(userId, recipeId, UserBehaviorService.BehaviorType.DOWNLOAD)

        return recipeRepository.save(cloned)
    }

    /**
     * 获取食谱作者信息
     */
    fun getRecipeAuthor(recipeId: Long): Map<String, Any?> {
        val recipe = recipeRepository.findById(recipeId)
            .orElseThrow { Exception("食谱不存在") }
        val user = userRepository.findById(recipe.userId).orElse(null)
        return mapOf(
            "userId" to recipe.userId,
            "username" to (user?.username ?: "未知"),
            "nickname" to (user?.nickname ?: user?.username ?: "未知")
        )
    }

    /**
     * 保存AI生成的食谱到个人食谱
     * 统一使用对象列表格式存储ingredients/steps，确保与AI生成格式一致
     * 添加重复检测：同一用户24小时内不能发布相同标题的食谱
     */
    @Transactional
    fun saveGeneratedRecipe(userId: Long, request: SaveRecipeRequest): Recipe {
        // 检查是否已存在相同标题的食谱（同一用户，24小时内）
        val existingRecipe = recipeRepository.findByUserIdAndTitle(userId, request.title)
        if (existingRecipe != null) {
            // 检查是否在24小时内创建
            val hoursSinceCreation = java.time.Duration.between(
                existingRecipe.createdAt,
                java.time.LocalDateTime.now()
            ).toHours()
            if (hoursSinceCreation < 24) {
                throw Exception("您已在${hoursSinceCreation}小时前发布过相同标题的食谱，24小时内不能重复发布")
            }
        }
        
        // 验证并格式化ingredients（保持对象列表格式）
        val ingredientsJson = try {
            val ingredientsList: List<Map<String, Any?>> = objectMapper.readValue(
                request.ingredients, 
                objectMapper.typeFactory.constructCollectionType(List::class.java, Map::class.java)
            )
            objectMapper.writeValueAsString(ingredientsList)
        } catch (e: Exception) {
            request.ingredients  // 如果解析失败，保持原样
        }
        
        // 验证并格式化steps（保持对象列表格式）
        val stepsJson = try {
            val stepsList: List<Map<String, Any?>> = objectMapper.readValue(
                request.steps,
                objectMapper.typeFactory.constructCollectionType(List::class.java, Map::class.java)
            )
            objectMapper.writeValueAsString(stepsList)
        } catch (e: Exception) {
            request.steps  // 如果解析失败，保持原样
        }

        val recipe = Recipe(
            userId = userId,
            title = request.title,
            description = request.description,
            ingredients = ingredientsJson,
            steps = stepsJson,
            cookingTime = request.cookingTime,
            difficulty = try { Difficulty.valueOf(request.difficulty) } catch (e: Exception) { Difficulty.MEDIUM },
            cuisine = request.cuisine,
            tags = request.tags,
            isAiOptimized = true,
            isAiGenerated = true
        )

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
