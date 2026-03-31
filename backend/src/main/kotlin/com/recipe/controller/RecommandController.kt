package com.recipe.controller

import com.recipe.service.RecommendService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 智能推荐控制器
 * 对应功能: 1.1.5 智能食谱推荐模块
 */
@RestController
@RequestMapping("/recommend")
class RecommendController(
    private val recommendService: RecommendService
) {
    
    /**
     * 获取个性化推荐
     * 
     * 返回多个推荐分区：
     * - all: 综合推荐(按分数排序)
     * - expiringIngredients: 临期食材快用
     * - personalized: 我的口味专属
     * - hot: 热门AI优化食谱
     */
    @GetMapping
    fun getRecommendations(
        @RequestHeader("user-id") userId: Long,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val result = recommendService.getRecommendations(userId, limit)
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "推荐失败"))
        }
    }
    
    /**
     * 智能组合临期食材
     * 
     * 返回能同时使用多个临期食材的食谱组合
     */
    @GetMapping("/smart-combine")
    fun smartCombine(
        @RequestHeader("user-id") userId: Long
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val combinations = recommendService.smartCombineExpiring(userId)
            ResponseEntity.ok(ApiResponse.success(combinations))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "组合失败"))
        }
    }
}