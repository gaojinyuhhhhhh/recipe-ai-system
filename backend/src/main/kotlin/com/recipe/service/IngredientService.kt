package com.recipe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.recipe.ai.IngredientRecognizer
import com.recipe.entity.Ingredient
import com.recipe.entity.ExpiryPriority
import com.recipe.repository.IngredientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 食材管理服务
 * 对应功能: 1.1.2 食材管理模块
 */
@Service
class IngredientService(
    private val ingredientRepository: IngredientRepository,
    private val ingredientRecognizer: IngredientRecognizer,
    private val objectMapper: ObjectMapper
) {
    
    /**
     * AI拍照识别添加食材
     */
    @Transactional
    fun recognizeAndAdd(userId: Long, imageBase64: String): List<Ingredient> {
        val recognized = ingredientRecognizer.recognize(imageBase64)
        
        return recognized.map { item ->
            val ingredient = Ingredient(
                userId = userId,
                name = item.name,
                category = item.category,
                freshness = item.getFreshnessEnum(),
                purchaseDate = LocalDate.now(),
                expiryDate = item.getExpiryDate(),
                shelfLife = item.shelfLife,
                storageMethod = item.getStorageMethodEnum()
            )
            
            // 生成保存建议
            ingredient.storageAdvice = ingredientRecognizer.generateStorageAdvice(
                item.name,
                ingredient.freshness,
                ingredient.category
            )
            
            ingredientRepository.save(ingredient)
        }
    }
    
    /**
     * 手动添加食材
     */
    @Transactional
    fun addIngredient(ingredient: Ingredient): Ingredient {
        // 如果没有保存建议，自动生成
        if (ingredient.storageAdvice.isNullOrBlank()) {
            ingredient.storageAdvice = ingredientRecognizer.generateStorageAdvice(
                ingredient.name,
                ingredient.freshness,
                ingredient.category
            )
        }
        
        return ingredientRepository.save(ingredient)
    }
    
    /**
     * 批量快速录入(逗号/空格分隔)
     */
    @Transactional
    fun batchAdd(userId: Long, ingredientsText: String): List<Ingredient> {
        val names = ingredientsText.split(Regex("[,，\\s]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        return names.map { name ->
            val ingredient = Ingredient(
                userId = userId,
                name = name,
                purchaseDate = LocalDate.now()
            )
            ingredientRepository.save(ingredient)
        }
    }
    
    /**
     * 更新食材
     */
    @Transactional
    fun updateIngredient(id: Long, userId: Long, updates: Ingredient): Ingredient {
        val existing = ingredientRepository.findById(id)
            .orElseThrow { Exception("食材不存在") }
        
        if (existing.userId != userId) {
            throw Exception("无权限操作")
        }
        
        existing.apply {
            name = updates.name
            category = updates.category
            quantity = updates.quantity
            unit = updates.unit
            freshness = updates.freshness
            purchaseDate = updates.purchaseDate
            expiryDate = updates.expiryDate
            shelfLife = updates.shelfLife
            storageMethod = updates.storageMethod
            notes = updates.notes
        }
        
        return ingredientRepository.save(existing)
    }
    
    /**
     * 删除食材
     */
    @Transactional
    fun deleteIngredient(id: Long, userId: Long) {
        val ingredient = ingredientRepository.findById(id)
            .orElseThrow { Exception("食材不存在") }
        
        if (ingredient.userId != userId) {
            throw Exception("无权限操作")
        }
        
        ingredientRepository.delete(ingredient)
    }
    
    /**
     * 标记为已消耗
     */
    @Transactional
    fun markAsConsumed(id: Long, userId: Long): Ingredient {
        val ingredient = ingredientRepository.findById(id)
            .orElseThrow { Exception("食材不存在") }
        
        if (ingredient.userId != userId) {
            throw Exception("无权限操作")
        }
        
        ingredient.isConsumed = true
        return ingredientRepository.save(ingredient)
    }
    
    /**
     * 查询用户所有食材
     */
    fun getUserIngredients(userId: Long): List<Ingredient> {
        return ingredientRepository.findByUserIdAndIsConsumedFalse(userId)
    }
    
    /**
     * 查询用户所有未过期的食材（用于推荐食谱等场景）
     * 排除已过期（remainingDays < 0）的食材，避免推荐使用过期食材
     */
    fun getAvailableIngredients(userId: Long): List<Ingredient> {
        return ingredientRepository.findByUserIdAndIsConsumedFalse(userId)
            .filter { ingredient ->
                val remaining = ingredient.getRemainingDays()
                remaining == null || remaining >= 0
            }
    }
    
    /**
     * 查询临期食材(7天内)
     */
    fun getExpiringIngredients(userId: Long): List<Ingredient> {
        val threshold = LocalDate.now().plusDays(7)
        return ingredientRepository.findExpiringIngredients(userId, threshold)
    }
    
    /**
     * 按优先级分组食材
     */
    fun getIngredientsByPriority(userId: Long): Map<ExpiryPriority, List<Ingredient>> {
        val ingredients = ingredientRepository.findByUserIdAndIsConsumedFalseOrderByExpiryDateAsc(userId)
        return ingredients.groupBy { it.getPriority() }
    }
    
    /**
     * 获取过期提醒列表
     */
    fun getExpiryAlerts(userId: Long): List<ExpiryAlert> {
        val priorityMap = getIngredientsByPriority(userId)
        val alerts = mutableListOf<ExpiryAlert>()
        
        // 高优先级提醒
        priorityMap[ExpiryPriority.HIGH]?.forEach { ingredient ->
            val remaining = ingredient.getRemainingDays() ?: 0
            val message = when {
                remaining <= 0 -> "${ingredient.name}已过期，请尽快处理"
                remaining == 1 -> "${ingredient.name}明天到期，建议今日食用"
                else -> "${ingredient.name}还有${remaining}天到期"
            }
            
            // 如果微蔫，生成快手方案
            val quickSolution = if (ingredient.freshness.name == "WILTING") {
                ingredientRecognizer.generateQuickUsageSolution(ingredient.name, remaining)
            } else null
            
            alerts.add(ExpiryAlert(
                ingredient = ingredient,
                priority = ExpiryPriority.HIGH,
                message = message,
                quickSolution = quickSolution
            ))
        }
        
        // 中优先级提醒
        priorityMap[ExpiryPriority.MEDIUM]?.forEach { ingredient ->
            val remaining = ingredient.getRemainingDays() ?: 0
            alerts.add(ExpiryAlert(
                ingredient = ingredient,
                priority = ExpiryPriority.MEDIUM,
                message = "${ingredient.name}还有${remaining}天到期，请注意使用"
            ))
        }
        
        return alerts
    }
}

/**
 * 过期提醒数据类
 */
data class ExpiryAlert(
    val ingredient: Ingredient,
    val priority: ExpiryPriority,
    val message: String,
    val quickSolution: String? = null
)
