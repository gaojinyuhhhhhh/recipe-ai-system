package com.recipe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.recipe.controller.MissingIngredientItem
import com.recipe.entity.ShoppingItem
import com.recipe.entity.Recipe
import com.recipe.repository.ShoppingRepository
import com.recipe.repository.RecipeRepository
import com.recipe.repository.IngredientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 采购管理服务
 * 对应功能: 1.1.3 采购管理模块
 */
@Service
class ShoppingService(
    private val shoppingRepository: ShoppingRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val ingredientService: IngredientService,
    private val objectMapper: ObjectMapper
) {
    
    /**
     * 手动添加采购项
     */
    @Transactional
    fun addItem(item: ShoppingItem): ShoppingItem {
        // AI推荐采购量
        if (item.quantity == null) {
            item.quantity = calculateRecommendedQuantity(
                item.name,
                item.category,
                item.userId
            )
            item.aiAdvice = generatePurchaseAdvice(item.name, item.quantity ?: 0.0)
        }
        
        return shoppingRepository.save(item)
    }
    
    /**
     * 从食谱导入食材
     */
    @Transactional
    fun importFromRecipe(userId: Long, recipeId: Long): List<ShoppingItem> {
        val recipe = recipeRepository.findById(recipeId)
            .orElseThrow { Exception("食谱不存在") }
        
        // 解析食谱的食材清单
        val recipeIngredients: List<RecipeIngredientItem> = 
            objectMapper.readValue(recipe.ingredients)
        
        // 获取用户已有食材
        val existingIngredients = ingredientRepository.findByUserIdAndIsConsumedFalse(userId)
        val existingNames = existingIngredients.map { it.name }.toSet()
        
        // 筛选缺少的食材
        val missingIngredients = recipeIngredients.filter { 
            it.name !in existingNames 
        }
        
        // 创建采购项
        return missingIngredients.map { ingredient ->
            val item = ShoppingItem(
                userId = userId,
                name = ingredient.name,
                category = categorizeIngredient(ingredient.name),
                quantity = ingredient.quantity,
                unit = ingredient.unit,
                recipeId = recipeId
            )
            
            item.aiAdvice = generatePurchaseAdvice(ingredient.name, ingredient.quantity)
            shoppingRepository.save(item)
        }
    }
    
    /**
     * 批量合并多个食谱的采购清单
     */
    @Transactional
    fun mergeRecipes(userId: Long, recipeIds: List<Long>): MergedShoppingList {
        val allIngredients = mutableMapOf<String, MutableList<RecipeIngredientItem>>()
        
        // 收集所有食材
        recipeIds.forEach { recipeId ->
            val recipe = recipeRepository.findById(recipeId).orElse(null) ?: return@forEach
            val ingredients: List<RecipeIngredientItem> = objectMapper.readValue(recipe.ingredients)
            
            ingredients.forEach { ingredient ->
                allIngredients.getOrPut(ingredient.name) { mutableListOf() }.add(ingredient)
            }
        }
        
        // 合并去重，计算总量
        val mergedItems = allIngredients.map { (name, items) ->
            val totalQuantity = items.sumOf { it.quantity }
            val unit = items.first().unit
            
            MergedShoppingItem(
                name = name,
                quantity = totalQuantity,
                unit = unit,
                category = categorizeIngredient(name),
                fromRecipes = items.size
            )
        }
        
        // 按类别分组
        val grouped = mergedItems.groupBy { it.category }
        
        // 推荐采购顺序(蔬菜类->肉类->调味类)
        val orderPriority = mapOf(
            "蔬菜类" to 1,
            "肉蛋类" to 2,
            "调味类" to 3,
            "主食类" to 4,
            "其他" to 5
        )
        
        val sortedGroups = grouped.entries.sortedBy { orderPriority[it.key] ?: 99 }
        
        return MergedShoppingList(
            items = mergedItems,
            groupedByCategory = sortedGroups.associate { it.key to it.value },
            recommendedOrder = sortedGroups.flatMap { it.value }
        )
    }
    
    /**
     * 批量勾选完成
     */
    @Transactional
    fun completeItems(userId: Long, itemIds: List<Long>): Int {
        var count = 0
        itemIds.forEach { id ->
            val item = shoppingRepository.findById(id).orElse(null)
            if (item != null && item.userId == userId) {
                item.isCompleted = true
                item.completedAt = LocalDateTime.now()
                shoppingRepository.save(item)
                count++
            }
        }
        return count
    }
    
    /**
     * 批量同步到食材库
     */
    @Transactional
    fun syncToIngredients(userId: Long, itemIds: List<Long>): List<com.recipe.entity.Ingredient> {
        val ingredients = mutableListOf<com.recipe.entity.Ingredient>()
        
        itemIds.forEach { id ->
            val item = shoppingRepository.findById(id).orElse(null)
            if (item != null && item.userId == userId && item.isCompleted) {
                // 创建食材
                val ingredient = com.recipe.entity.Ingredient(
                    userId = userId,
                    name = item.name,
                    category = item.category,
                    quantity = item.quantity,
                    unit = item.unit
                )
                
                val saved = ingredientService.addIngredient(ingredient)
                ingredients.add(saved)
                
                // 删除采购项
                shoppingRepository.delete(item)
            }
        }
        
        return ingredients
    }
    
    /**
     * 查询用户的采购清单
     */
    fun getUserShoppingList(userId: Long, completed: Boolean = false): List<ShoppingItem> {
        return if (completed) {
            shoppingRepository.findByUserIdAndIsCompletedTrue(userId)
        } else {
            shoppingRepository.findByUserIdAndIsCompletedFalse(userId)
        }
    }
    
    /**
     * 删除采购项
     */
    @Transactional
    fun deleteItem(id: Long, userId: Long) {
        val item = shoppingRepository.findById(id)
            .orElseThrow { Exception("采购项不存在") }
        
        if (item.userId != userId) {
            throw Exception("无权限操作")
        }
        
        shoppingRepository.delete(item)
    }

    /**
     * 缺少食材一键加入采购清单
     */
    @Transactional
    fun addMissingIngredients(userId: Long, ingredients: List<MissingIngredientItem>): List<ShoppingItem> {
        return ingredients.map { ingredient ->
            val item = ShoppingItem(
                userId = userId,
                name = ingredient.name,
                category = categorizeIngredient(ingredient.name),
                quantity = ingredient.quantity,
                unit = ingredient.unit
            )
            item.aiAdvice = generatePurchaseAdvice(ingredient.name, ingredient.quantity ?: 0.0)
            shoppingRepository.save(item)
        }
    }
    
    /**
     * AI计算推荐采购量
     */
    private fun calculateRecommendedQuantity(
        name: String,
        category: String?,
        userId: Long
    ): Double {
        // 获取用户信息(家庭人数、烹饪频率)
        // 简化实现：根据类别返回推荐量
        return when (category) {
            "蔬菜类" -> 500.0  // 500g
            "肉蛋类" -> 300.0  // 300g
            "主食类" -> 1000.0 // 1kg
            "调味类" -> 100.0  // 100g
            else -> 200.0
        }
    }
    
    /**
     * 生成AI采购建议
     */
    private fun generatePurchaseAdvice(name: String, quantity: Double): String {
        // 判断是否易过期食材
        val perishable = listOf("生菜", "菠菜", "豆腐", "鲜肉", "牛奶", "酸奶")
        
        return if (perishable.any { name.contains(it) }) {
            "⚠️ 易过期食材，建议少量购买"
        } else {
            "✓ 推荐采购量: ${quantity}${getDefaultUnit(name)}"
        }
    }
    
    /**
     * 食材分类
     */
    private fun categorizeIngredient(name: String): String {
        return when {
            name.contains("肉") || name.contains("鸡") || name.contains("鱼") || name.contains("蛋") -> "肉蛋类"
            name.contains("菜") || name.contains("瓜") || name.contains("豆") || name.contains("菇") -> "蔬菜类"
            name.contains("米") || name.contains("面") || name.contains("粉") -> "主食类"
            name.contains("油") || name.contains("盐") || name.contains("酱") || name.contains("醋") -> "调味类"
            else -> "其他"
        }
    }
    
    private fun getDefaultUnit(name: String): String {
        return when {
            name.contains("鸡蛋") || name.contains("土豆") -> "个"
            name.contains("葱") || name.contains("蒜") -> "根"
            else -> "g"
        }
    }
}

/**
 * 食谱食材项
 */
data class RecipeIngredientItem(
    val name: String,
    val quantity: Double,
    val unit: String
)

/**
 * 合并后的采购项
 */
data class MergedShoppingItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val fromRecipes: Int  // 来自几个食谱
)

/**
 * 合并后的采购清单
 */
data class MergedShoppingList(
    val items: List<MergedShoppingItem>,
    val groupedByCategory: Map<String, List<MergedShoppingItem>>,
    val recommendedOrder: List<MergedShoppingItem>
)
