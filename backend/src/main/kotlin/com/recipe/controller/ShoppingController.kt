package com.recipe.controller

import com.recipe.dto.ApiResponse
import com.recipe.entity.ShoppingItem
import com.recipe.service.ShoppingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 采购管理控制器
 * 对应功能: 1.1.3 采购管理模块
 */
@RestController
@RequestMapping("/shopping")
class ShoppingController(
    private val shoppingService: ShoppingService
) : BaseController() {
    
    /**
     * 手动添加采购项
     */
    @PostMapping
    fun addItem(
        @RequestBody item: ShoppingItem
    ): ResponseEntity<ApiResponse<ShoppingItem>> {
        return try {
            val userId = currentUserId()
            item.userId = userId
            val saved = shoppingService.addItem(item)
            ResponseEntity.ok(ApiResponse.success(saved, "添加成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "添加失败"))
        }
    }
    
    /**
     * 从食谱导入缺少的食材
     */
    @PostMapping("/import-recipe/{recipeId}")
    fun importFromRecipe(
        @PathVariable recipeId: Long
    ): ResponseEntity<ApiResponse<List<ShoppingItem>>> {
        return try {
            val userId = currentUserId()
            val items = shoppingService.importFromRecipe(userId, recipeId)
            ResponseEntity.ok(ApiResponse.success(items, "已导入${items.size}个缺少的食材"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "导入失败"))
        }
    }
    
    /**
     * 批量合并多个食谱的采购清单
     */
    @PostMapping("/merge-recipes")
    fun mergeRecipes(
        @RequestBody request: MergeRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val result = shoppingService.mergeRecipes(userId, request.recipeIds)
            ResponseEntity.ok(ApiResponse.success(result, "合并成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "合并失败"))
        }
    }
    
    /**
     * 查询采购清单
     */
    @GetMapping
    fun getShoppingList(
        @RequestParam(defaultValue = "false") completed: Boolean
    ): ResponseEntity<ApiResponse<List<ShoppingItem>>> {
        val userId = currentUserId()
        val items = shoppingService.getUserShoppingList(userId, completed)
        return ResponseEntity.ok(ApiResponse.success(items))
    }
    
    /**
     * 批量勾选完成
     */
    @PutMapping("/complete")
    fun completeItems(
        @RequestBody request: CompleteRequest
    ): ResponseEntity<ApiResponse<Int>> {
        return try {
            val userId = currentUserId()
            val count = shoppingService.completeItems(userId, request.itemIds)
            ResponseEntity.ok(ApiResponse.success(count, "已完成${count}个采购项"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "操作失败"))
        }
    }
    
    /**
     * 批量同步到食材库
     */
    @PostMapping("/sync-to-ingredients")
    fun syncToIngredients(
        @RequestBody request: SyncRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val ingredients = shoppingService.syncToIngredients(userId, request.itemIds)
            ResponseEntity.ok(ApiResponse.success(ingredients, "已同步${ingredients.size}个食材到食材库"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "同步失败"))
        }
    }
    
    /**
     * 删除采购项
     */
    @DeleteMapping("/{id}")
    fun deleteItem(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            shoppingService.deleteItem(id, userId)
            ResponseEntity.ok(ApiResponse.success("deleted", "删除成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "删除失败"))
        }
    }
    
    /**
     * 清空所有已完成采购项
     */
    @DeleteMapping("/completed/clear")
    fun clearCompletedItems(): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            val count = shoppingService.clearAllCompleted(userId)
            ResponseEntity.ok(ApiResponse.success("cleared", "已清空 $count 个采购项"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "清空失败"))
        }
    }
}

data class MergeRequest(val recipeIds: List<Long>)
data class CompleteRequest(val itemIds: List<Long>)
data class SyncRequest(val itemIds: List<Long>)
