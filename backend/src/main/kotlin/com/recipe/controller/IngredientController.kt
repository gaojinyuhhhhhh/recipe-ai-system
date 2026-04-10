package com.recipe.controller

import com.recipe.dto.ApiResponse
import com.recipe.entity.Ingredient
import com.recipe.service.IngredientService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 食材管理控制器
 * 对应功能: 1.1.2 食材管理模块
 */
@RestController
@RequestMapping("/ingredients")
class IngredientController(
    private val ingredientService: IngredientService
) : BaseController() {

    /**
     * AI拍照识别食材
     */
    @PostMapping("/recognize")
    fun recognizeIngredient(
        @RequestBody request: RecognizeRequest
    ): ResponseEntity<ApiResponse<List<Ingredient>>> {
        return try {
            val userId = currentUserId()
            val ingredients = ingredientService.recognizeAndAdd(userId, request.imageBase64)
            ResponseEntity.ok(ApiResponse.success(ingredients, "识别成功，共发现${ingredients.size}种食材"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "识别失败"))
        }
    }

    /**
     * 手动添加食材
     */
    @PostMapping
    fun addIngredient(
        @RequestBody ingredient: Ingredient
    ): ResponseEntity<ApiResponse<Ingredient>> {
        return try {
            val userId = currentUserId()
            ingredient.userId = userId
            val saved = ingredientService.addIngredient(ingredient)
            ResponseEntity.ok(ApiResponse.success(saved, "添加成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "添加失败"))
        }
    }

    /**
     * 批量快速录入
     */
    @PostMapping("/batch")
    fun batchAdd(
        @RequestBody request: BatchAddRequest
    ): ResponseEntity<ApiResponse<List<Ingredient>>> {
        return try {
            val userId = currentUserId()
            val ingredients = ingredientService.batchAdd(userId, request.ingredients)
            ResponseEntity.ok(ApiResponse.success(ingredients, "批量添加成功，共${ingredients.size}种食材"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "批量添加失败"))
        }
    }

    /**
     * 查询用户所有食材
     */
    @GetMapping
    fun getIngredients(): ResponseEntity<ApiResponse<List<Ingredient>>> {
        val userId = currentUserId()
        val ingredients = ingredientService.getUserIngredients(userId)
        return ResponseEntity.ok(ApiResponse.success(ingredients))
    }

    /**
     * 更新食材
     */
    @PutMapping("/{id}")
    fun updateIngredient(
        @PathVariable id: Long,
        @RequestBody ingredient: Ingredient
    ): ResponseEntity<ApiResponse<Ingredient>> {
        return try {
            val userId = currentUserId()
            val updated = ingredientService.updateIngredient(id, userId, ingredient)
            ResponseEntity.ok(ApiResponse.success(updated, "更新成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "更新失败"))
        }
    }

    /**
     * 删除食材
     */
    @DeleteMapping("/{id}")
    fun deleteIngredient(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            ingredientService.deleteIngredient(id, userId)
            ResponseEntity.ok(ApiResponse.success("deleted", "删除成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "删除失败"))
        }
    }

    /**
     * 标记食材已消耗
     */
    @PutMapping("/{id}/consume")
    fun consumeIngredient(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Ingredient>> {
        return try {
            val userId = currentUserId()
            val ingredient = ingredientService.markAsConsumed(id, userId)
            ResponseEntity.ok(ApiResponse.success(ingredient, "已标记为消耗"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "操作失败"))
        }
    }

    /**
     * 获取过期提醒
     */
    @GetMapping("/alerts")
    fun getExpiryAlerts(): ResponseEntity<ApiResponse<List<ExpiryAlertResponse>>> {
        val userId = currentUserId()
        val alerts = ingredientService.getExpiryAlerts(userId)
        val response = alerts.map { alert ->
            ExpiryAlertResponse(
                ingredientId = alert.ingredient.id!!,
                ingredientName = alert.ingredient.name,
                message = alert.message,
                daysRemaining = alert.ingredient.getRemainingDays() ?: 0,
                alertLevel = alert.priority.name,
                quickSolution = alert.quickSolution
            )
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 获取临期食材
     */
    @GetMapping("/expiring")
    fun getExpiringIngredients(): ResponseEntity<ApiResponse<List<Ingredient>>> {
        val userId = currentUserId()
        val ingredients = ingredientService.getExpiringIngredients(userId)
        return ResponseEntity.ok(ApiResponse.success(ingredients))
    }

    /**
     * 获取按新鲜度分组的食材
     * 返回: { "expiringSoon": [...], "fresh": [...], "longTerm": [...] }
     */
    @GetMapping("/by-freshness")
    fun getIngredientsByFreshness(): ResponseEntity<ApiResponse<Map<String, List<Ingredient>>>> {
        val userId = currentUserId()
        val ingredients = ingredientService.getUserIngredients(userId)
        
        // 按剩余天数分组
        val grouped = ingredients.groupBy { ingredient ->
            val remaining = ingredient.getRemainingDays() ?: Int.MAX_VALUE
            when {
                remaining <= 3 -> "expiringSoon"  // 3天内过期
                remaining <= 7 -> "fresh"          // 4-7天
                else -> "longTerm"                 // 7天以上
            }
        }
        
        // 确保所有分组都存在
        val result = mapOf(
            "expiringSoon" to (grouped["expiringSoon"] ?: emptyList()),
            "fresh" to (grouped["fresh"] ?: emptyList()),
            "longTerm" to (grouped["longTerm"] ?: emptyList())
        )
        
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 获取按类别分组的食材
     * 返回: { "肉类": [...], "蔬菜类": [...], ... }
     */
    @GetMapping("/by-category")
    fun getIngredientsByCategory(): ResponseEntity<ApiResponse<Map<String, List<Ingredient>>>> {
        val userId = currentUserId()
        val ingredients = ingredientService.getUserIngredients(userId)
        
        // 按类别分组，null类别的归为"未分类"
        val grouped = ingredients.groupBy { 
            it.category ?: "未分类"
        }.toSortedMap()  // 按类别名称排序
        
        return ResponseEntity.ok(ApiResponse.success(grouped))
    }
}

data class RecognizeRequest(val imageBase64: String)
data class BatchAddRequest(val ingredients: String)

/**
 * 过期提醒响应（适配Android端格式）
 */
data class ExpiryAlertResponse(
    val ingredientId: Long,
    val ingredientName: String,
    val message: String,
    val daysRemaining: Int,
    val alertLevel: String,
    val quickSolution: String? = null
)
