package com.recipe.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 食谱实体类
 * 对应功能: 1.1.4 食谱社区模块
 */
@Entity
@Table(name = "recipes")
data class Recipe(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    var userId: Long,  // 创建者ID
    
    @Column(nullable = false, length = 200)
    var title: String,  // 食谱标题
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,  // 描述
    
    @Column(length = 255)
    var coverImage: String? = null,  // 封面图
    
    // 食材清单(JSON格式)
    @Column(columnDefinition = "TEXT", nullable = false)
    var ingredients: String,  // [{"name":"鸡蛋","quantity":2,"unit":"个"}]
    
    // 烹饪步骤(JSON格式)
    @Column(columnDefinition = "TEXT", nullable = false)
    var steps: String,  // [{"step":1,"content":"打散鸡蛋","duration":60,"image":"..."}]
    
    @Column
    var cookingTime: Int? = null,  // 烹饪时长(分钟)
    
    @Enumerated(EnumType.STRING)
    @Column
    var difficulty: Difficulty = Difficulty.MEDIUM,  // 难度
    
    @Column(length = 50)
    var cuisine: String? = null,  // 菜系
    
    // 标签(JSON数组)
    @Column(columnDefinition = "TEXT")
    var tags: String? = null,  // ["减脂","低糖","快手"]
    
    // AI质量评级
    @Enumerated(EnumType.STRING)
    @Column
    var aiRating: AiRating? = null,  // S/A/B/C
    
    // AI评分详情(JSON格式)
    @Column(columnDefinition = "TEXT")
    var aiRatingDetail: String? = null,  // {"ingredient":90,"nutrition":85,"steps":88,"difficulty":92}
    
    // AI优化建议
    @Column(columnDefinition = "TEXT")
    var aiSuggestion: String? = null,
    
    @Column(nullable = false)
    var isAiOptimized: Boolean = false,  // 是否AI优化版
    
    @Column
    var originalRecipeId: Long? = null,  // 原始食谱ID(如果是优化版)
    
    @Column(nullable = false)
    var viewCount: Long = 0,  // 浏览量
    
    @Column(nullable = false)
    var favoriteCount: Long = 0,  // 收藏量
    
    @Column(nullable = false)
    var commentCount: Long = 0,  // 评论量
    
    @Column(nullable = false)
    var isPublic: Boolean = true,  // 是否公开
    
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

/**
 * 难度枚举
 */
enum class Difficulty(val display: String) {
    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难")
}

/**
 * AI评级枚举
 */
enum class AiRating(val display: String, val score: Int) {
    S("S级优秀", 90),
    A("A级良好", 80),
    B("B级合格", 70),
    C("C级待改进", 60)
}

/**
 * 食谱评论实体类
 */
@Entity
@Table(name = "recipe_comments")
data class RecipeComment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    var recipeId: Long,
    
    @Column(nullable = false)
    var userId: Long,
    
    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,
    
    @Column
    var rating: Int? = null,  // 评分1-5
    
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 食谱收藏实体类
 */
@Entity
@Table(name = "recipe_favorites")
data class RecipeFavorite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    var recipeId: Long,
    
    @Column(nullable = false)
    var userId: Long,
    
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
