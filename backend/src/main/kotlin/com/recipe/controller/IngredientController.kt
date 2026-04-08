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
    fun getExpiryAlerts(): ResponseEntity<ApiResponse<List<com.recipe.service.ExpiryAlert>>> {
        val userId = currentUserId()
        val alerts = ingredientService.getExpiryAlerts(userId)
        return ResponseEntity.ok(ApiResponse.success(alerts))
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
}

data class RecognizeRequest(val imageBase64: String)
data class BatchAddRequest(val ingredients: String)
