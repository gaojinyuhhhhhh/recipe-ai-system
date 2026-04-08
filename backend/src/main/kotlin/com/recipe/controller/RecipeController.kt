package com.recipe.controller

import com.recipe.dto.ApiResponse
import com.recipe.entity.*
import com.recipe.service.RecipeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 食谱管理控制器
 * 对应功能: 1.1.4 食谱社区模块
 */
@RestController
@RequestMapping("/recipes")
class RecipeController(
    private val recipeService: RecipeService
) : BaseController() {
    
    /**
     * 创建食谱
     */
    @PostMapping
    fun createRecipe(
        @RequestBody recipe: Recipe
    ): ResponseEntity<ApiResponse<Recipe>> {
        return try {
            val userId = currentUserId()
            recipe.userId = userId
            val saved = recipeService.createRecipe(recipe)
            ResponseEntity.ok(ApiResponse.success(saved, "创建成功，AI评级: ${saved.aiRating?.display}"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "创建失败"))
        }
    }
    
    /**
     * AI优化食谱
     */
    @PostMapping("/{id}/optimize")
    fun optimizeRecipe(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Recipe>> {
        return try {
            val userId = currentUserId()
            val optimized = recipeService.optimizeRecipe(id, userId)
            ResponseEntity.ok(ApiResponse.success(optimized, "已生成AI优化版食谱"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "优化失败"))
        }
    }
    
    /**
     * 更新食谱
     */
    @PutMapping("/{id}")
    fun updateRecipe(
        @PathVariable id: Long,
        @RequestBody recipe: Recipe
    ): ResponseEntity<ApiResponse<Recipe>> {
        return try {
            val userId = currentUserId()
            val updated = recipeService.updateRecipe(id, userId, recipe)
            ResponseEntity.ok(ApiResponse.success(updated, "更新成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "更新失败"))
        }
    }
    
    /**
     * 删除食谱
     */
    @DeleteMapping("/{id}")
    fun deleteRecipe(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            recipeService.deleteRecipe(id, userId)
            ResponseEntity.ok(ApiResponse.success("deleted", "删除成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "删除失败"))
        }
    }
    
    /**
     * 查询食谱详情
     */
    @GetMapping("/{id}")
    fun getRecipeDetail(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = try { currentUserId() } catch (e: Exception) { null }
            val detail = recipeService.getRecipeDetail(id, userId)
            ResponseEntity.ok(ApiResponse.success(detail))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "查询失败"))
        }
    }
    
    /**
     * 搜索食谱
     */
    @GetMapping("/search")
    fun searchRecipes(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) cuisine: String?,
        @RequestParam(required = false) difficulty: Difficulty?,
        @RequestParam(required = false) tags: List<String>?
    ): ResponseEntity<ApiResponse<List<Recipe>>> {
        val recipes = recipeService.searchRecipes(keyword, cuisine, difficulty, tags)
        return ResponseEntity.ok(ApiResponse.success(recipes))
    }
    
    /**
     * 热门食谱
     */
    @GetMapping("/hot")
    fun getHotRecipes(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<List<Recipe>>> {
        val recipes = recipeService.getHotRecipes(limit)
        return ResponseEntity.ok(ApiResponse.success(recipes))
    }
    
    /**
     * 查询用户的食谱
     */
    @GetMapping("/my")
    fun getMyRecipes(): ResponseEntity<ApiResponse<List<Recipe>>> {
        val userId = currentUserId()
        val recipes = recipeService.getUserRecipes(userId)
        return ResponseEntity.ok(ApiResponse.success(recipes))
    }
    
    /**
     * 查询用户收藏的食谱
     */
    @GetMapping("/favorites")
    fun getFavorites(): ResponseEntity<ApiResponse<List<Recipe>>> {
        val userId = currentUserId()
        val recipes = recipeService.getUserFavorites(userId)
        return ResponseEntity.ok(ApiResponse.success(recipes))
    }
    
    /**
     * 收藏/取消收藏
     */
    @PostMapping("/{id}/favorite")
    fun toggleFavorite(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Boolean>> {
        return try {
            val userId = currentUserId()
            val isFavorited = recipeService.toggleFavorite(userId, id)
            val message = if (isFavorited) "已收藏" else "已取消收藏"
            ResponseEntity.ok(ApiResponse.success(isFavorited, message))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "操作失败"))
        }
    }
    
    /**
     * 添加评论
     */
    @PostMapping("/{id}/comments")
    fun addComment(
        @PathVariable id: Long,
        @RequestBody request: CommentRequest
    ): ResponseEntity<ApiResponse<RecipeComment>> {
        return try {
            val userId = currentUserId()
            val comment = RecipeComment(
                recipeId = id,
                userId = userId,
                content = request.content,
                rating = request.rating
            )
            val saved = recipeService.addComment(comment)
            ResponseEntity.ok(ApiResponse.success(saved, "评论成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "评论失败"))
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            recipeService.deleteComment(commentId, userId)
            ResponseEntity.ok(ApiResponse.success("deleted", "评论已删除"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "删除失败"))
        }
    }

    /**
     * 查询用户历史评论
     */
    @GetMapping("/my/comments")
    fun getMyComments(): ResponseEntity<ApiResponse<List<RecipeComment>>> {
        return try {
            val userId = currentUserId()
            val comments = recipeService.getUserComments(userId)
            ResponseEntity.ok(ApiResponse.success(comments))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "查询失败"))
        }
    }
}

data class CommentRequest(
    val content: String,
    val rating: Int?
)
