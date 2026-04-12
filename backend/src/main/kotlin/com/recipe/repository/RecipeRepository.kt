package com.recipe.repository

import com.recipe.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 用户数据访问接口
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?
    fun findByPhone(phone: String): User?
    fun existsByUsername(username: String): Boolean
    fun existsByPhone(phone: String): Boolean
}

/**
 * 食材数据访问接口
 */
@Repository
interface IngredientRepository : JpaRepository<Ingredient, Long> {
    // 查询用户的所有食材
    fun findByUserIdAndIsConsumedFalse(userId: Long): List<Ingredient>
    
    // 查询临期食材(7天内到期)
    @Query("SELECT i FROM Ingredient i WHERE i.userId = :userId AND i.isConsumed = false AND i.expiryDate <= :date")
    fun findExpiringIngredients(userId: Long, date: LocalDate): List<Ingredient>
    
    // 按优先级查询食材
    fun findByUserIdAndIsConsumedFalseOrderByExpiryDateAsc(userId: Long): List<Ingredient>
    
    // 按类别查询
    fun findByUserIdAndCategoryAndIsConsumedFalse(userId: Long, category: String): List<Ingredient>
}

/**
 * 采购清单数据访问接口
 */
@Repository
interface ShoppingRepository : JpaRepository<ShoppingItem, Long> {
    // 查询用户未完成的采购项
    fun findByUserIdAndIsCompletedFalse(userId: Long): List<ShoppingItem>
    
    // 查询已完成的采购项
    fun findByUserIdAndIsCompletedTrue(userId: Long): List<ShoppingItem>
    
    // 按类别分组
    fun findByUserIdAndIsCompletedFalseOrderByCategory(userId: Long): List<ShoppingItem>
}

/**
 * 食谱数据访问接口
 */
@Repository
interface RecipeRepository : JpaRepository<Recipe, Long> {
    // 查询用户的食谱
    fun findByUserId(userId: Long): List<Recipe>
    
    // 根据用户ID和标题查询（用于重复检测）
    fun findByUserIdAndTitle(userId: Long, title: String): Recipe?
    
    // 查询公开食谱
    fun findByIsPublicTrue(): List<Recipe>
    
    // 按菜系查询
    fun findByIsPublicTrueAndCuisine(cuisine: String): List<Recipe>
    
    // 按难度查询
    fun findByIsPublicTrueAndDifficulty(difficulty: Difficulty): List<Recipe>
    
    // 搜索食谱(标题或标签)
    @Query("SELECT r FROM Recipe r WHERE r.isPublic = true AND (r.title LIKE %:keyword% OR r.tags LIKE %:keyword%)")
    fun searchRecipes(keyword: String): List<Recipe>
    
    // 热门食谱(按收藏量排序)
    fun findByIsPublicTrueOrderByFavoriteCountDesc(): List<Recipe>
}

/**
 * 食谱评论数据访问接口
 */
@Repository
interface RecipeCommentRepository : JpaRepository<RecipeComment, Long> {
    fun findByRecipeIdOrderByCreatedAtDesc(recipeId: Long): List<RecipeComment>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<RecipeComment>
    fun countByRecipeId(recipeId: Long): Long
}

/**
 * 食谱收藏数据访问接口
 */
@Repository
interface RecipeFavoriteRepository : JpaRepository<RecipeFavorite, Long> {
    fun findByUserId(userId: Long): List<RecipeFavorite>
    fun findByUserIdAndRecipeId(userId: Long, recipeId: Long): RecipeFavorite?
    fun existsByUserIdAndRecipeId(userId: Long, recipeId: Long): Boolean
    fun countByRecipeId(recipeId: Long): Long
    fun deleteByUserIdAndRecipeId(userId: Long, recipeId: Long)
}
