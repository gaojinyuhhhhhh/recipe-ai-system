package com.recipe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.recipe.controller.MissingIngredientItem
import com.recipe.controller.CustomIngredientInfo
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
        
        // 推荐采购顺序
        val orderPriority = mapOf(
            "蔬菜" to 1,
            "水果" to 2,
            "肉类" to 3,
            "海鲜" to 4,
            "蛋奶" to 5,
            "调味品" to 6,
            "主食" to 7,
            "饮品" to 8,
            "其他" to 9
        )
        
        val sortedGroups = grouped.entries.sortedBy { orderPriority[it.key] ?: 99 }
        
        return MergedShoppingList(
            items = mergedItems,
            groupedByCategory = sortedGroups.associate { it.key to it.value },
            recommendedOrder = sortedGroups.flatMap { it.value }
        )
    }
    
    /**
     * 批量勾选完成并同步到食材库（带AI推断信息，支持用户自定义）
     */
    @Transactional
    fun completeAndAddWithAiInfo(
        userId: Long,
        itemIds: List<Long>,
        aiClient: com.recipe.ai.TongYiAiClient,
        customInfo: CustomIngredientInfo? = null
    ): List<com.recipe.entity.Ingredient> {
        val ingredients = mutableListOf<com.recipe.entity.Ingredient>()
        
        itemIds.forEach { id ->
            val item = shoppingRepository.findById(id).orElse(null)
            if (item != null && item.userId == userId && !item.isCompleted) {
                // 1. 标记完成
                item.isCompleted = true
                item.completedAt = LocalDateTime.now()
                shoppingRepository.save(item)
                
                // 2. 获取食材信息（优先使用用户自定义，否则AI推断）
                val aiInfo = if (customInfo != null) {
                    // 使用用户自定义信息
                    AiIngredientInfo(
                        shelfLife = customInfo.shelfLife,
                        storageMethod = customInfo.storageMethod,
                        storageAdvice = customInfo.storageAdvice,
                        freshness = customInfo.freshness
                    )
                } else {
                    // AI推断保质期信息
                    try {
                        inferIngredientInfo(item.name, aiClient)
                    } catch (e: Exception) {
                        // 默认值
                        AiIngredientInfo(7, "REFRIGERATED", "建议冷藏保存", "FRESH")
                    }
                }
                
                // 3. 创建食材（带AI信息）
                // 使用用户自定义的实际数量（如果有），否则使用购物清单中的数量
                val finalQuantity = customInfo?.actualQuantity ?: item.quantity
                val finalUnit = customInfo?.unit ?: item.unit
                
                val purchaseDate = java.time.LocalDate.now()
                val expiryDate = purchaseDate.plusDays(aiInfo.shelfLife.toLong())
                
                val ingredient = com.recipe.entity.Ingredient(
                    userId = userId,
                    name = item.name,
                    category = item.category,
                    quantity = finalQuantity,
                    unit = finalUnit,
                    purchaseDate = purchaseDate,
                    expiryDate = expiryDate,
                    shelfLife = aiInfo.shelfLife,
                    storageMethod = mapStorageMethod(aiInfo.storageMethod),
                    storageAdvice = aiInfo.storageAdvice,
                    freshness = mapFreshness(aiInfo.freshness)
                )
                
                val saved = ingredientService.addIngredient(ingredient)
                ingredients.add(saved)
                
                // 4. 删除采购项
                shoppingRepository.delete(item)
            }
        }
        
        return ingredients
    }
    
    /**
     * AI推断食材信息
     */
    private fun inferIngredientInfo(
        name: String,
        aiClient: com.recipe.ai.TongYiAiClient
    ): AiIngredientInfo {
        val systemPrompt = """
            你是一位专业的食材保存专家。根据食材名称推断保质期和保存建议。
            返回JSON格式，不要有其他文字。
        """.trimIndent()
        
        val prompt = """
            食材: $name
            
            请推断并返回JSON格式:
            {
              "shelfLife": 保质天数(数字),
              "storageMethod": "保存方式(REFRIGERATED, FROZEN, ROOM_TEMP)",
              "storageAdvice": "保存建议提示(简短)",
              "freshness": "新鲜度(FRESH, WILTING, SPOILED)"
            }
        """.trimIndent()
        
        val response = aiClient.generateText(prompt, systemPrompt, temperature = 0.3)
        val json = response.content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
        return mapper.readValue(json, AiIngredientInfo::class.java)
    }

    /**
     * 批量勾选完成并同步到食材库
     */
    @Transactional
    fun completeItems(userId: Long, itemIds: List<Long>): Int {
        var count = 0
        val completedItems = mutableListOf<ShoppingItem>()
        
        // 1. 标记完成
        itemIds.forEach { id ->
            val item = shoppingRepository.findById(id).orElse(null)
            if (item != null && item.userId == userId && !item.isCompleted) {
                item.isCompleted = true
                item.completedAt = LocalDateTime.now()
                shoppingRepository.save(item)
                completedItems.add(item)
                count++
            }
        }
        
        // 2. 自动同步到食材库
        completedItems.forEach { item ->
            val ingredient = com.recipe.entity.Ingredient(
                userId = userId,
                name = item.name,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit
            )
            ingredientService.addIngredient(ingredient)
            // 删除已同步的采购项
            shoppingRepository.delete(item)
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
            "蔬菜" -> 500.0  // 500g
            "水果" -> 800.0  // 800g
            "肉类" -> 500.0  // 500g
            "海鲜" -> 400.0  // 400g
            "蛋奶" -> 300.0  // 300g
            "调味品" -> 100.0  // 100g
            "主食" -> 1000.0 // 1kg
            "饮品" -> 1000.0 // 1L
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
     * 食材分类（与食材库统一）
     */
    private fun categorizeIngredient(name: String): String {
        return when {
            // 蔬菜
            name.contains("菜") || name.contains("瓜") || name.contains("豆") || name.contains("菇") ||
            name.contains("茄") || name.contains("椒") || name.contains("笋") || name.contains("葱") ||
            name.contains("蒜") || name.contains("姜") || name.contains("芹") || name.contains("菠") -> "蔬菜"
            // 水果
            name.contains("果") || name.contains("瓜") || name.contains("莓") || name.contains("桃") ||
            name.contains("梨") || name.contains("苹") || name.contains("橙") || name.contains("柠") ||
            name.contains("芒") || name.contains("西") || name.contains("柿") || name.contains("蕉") -> "水果"
            // 肉类
            name.contains("肉") || name.contains("鸡") || name.contains("鸭") || name.contains("鹅") ||
            name.contains("牛") || name.contains("羊") || name.contains("猪") || name.contains("排") ||
            name.contains("腿") -> "肉类"
            // 海鲜
            name.contains("鱼") || name.contains("虾") || name.contains("蟹") || name.contains("贝") ||
            name.contains("鱿") || name.contains("参") || name.contains("鲍") || name.contains("蚝") ||
            name.contains("蛤") || name.contains("螺") || name.contains("带") -> "海鲜"
            // 蛋奶
            name.contains("蛋") || name.contains("奶") || name.contains("乳") || name.contains("酪") ||
            name.contains("黄") || name.contains("奶") -> "蛋奶"
            // 调味品
            name.contains("油") || name.contains("盐") || name.contains("酱") || name.contains("醋") ||
            name.contains("糖") || name.contains("料") || name.contains("粉") || name.contains("精") ||
            name.contains("辣") || name.contains("椒") || name.contains("蒜") -> "调味品"
            // 主食
            name.contains("米") || name.contains("面") || name.contains("粉") || name.contains("馒") ||
            name.contains("包") || name.contains("饺") || name.contains("饼") || name.contains("粮") ||
            name.contains("麦") || name.contains("玉") -> "主食"
            // 饮品
            name.contains("水") || name.contains("茶") || name.contains("酒") || name.contains("汁") ||
            name.contains("饮") || name.contains("汤") || name.contains("咖") || name.contains("可") -> "饮品"
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

    /**
     * 映射保存方式（AI返回值 -> 实体枚举）
     */
    private fun mapStorageMethod(aiValue: String): com.recipe.entity.StorageMethod {
        return when (aiValue.uppercase()) {
            "REFRIGERATED", "REFRIGERATE" -> com.recipe.entity.StorageMethod.REFRIGERATE
            "FROZEN", "FREEZE" -> com.recipe.entity.StorageMethod.FREEZE
            "ROOM_TEMP" -> com.recipe.entity.StorageMethod.ROOM_TEMP
            "DRY_COOL" -> com.recipe.entity.StorageMethod.DRY_COOL
            else -> com.recipe.entity.StorageMethod.REFRIGERATE
        }
    }

    /**
     * 映射新鲜度（AI返回值 -> 实体枚举）
     */
    private fun mapFreshness(aiValue: String): com.recipe.entity.Freshness {
        return when (aiValue.uppercase()) {
            "FRESH" -> com.recipe.entity.Freshness.FRESH
            "WILTING" -> com.recipe.entity.Freshness.WILTING
            "SPOILED", "SPOILING" -> com.recipe.entity.Freshness.SPOILING
            else -> com.recipe.entity.Freshness.FRESH
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

/**
 * AI推断的食材信息
 */
data class AiIngredientInfo(
    val shelfLife: Int,
    val storageMethod: String,
    val storageAdvice: String,
    val freshness: String
)
