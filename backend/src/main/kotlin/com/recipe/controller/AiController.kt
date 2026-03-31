package com.recipe.controller

import com.recipe.ai.RecipeGenerationRequest
import com.recipe.ai.RecipeGenerator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * AI功能控制器
 * 对应功能: 1.1.6 AI个性化定制模块
 */
@RestController
@RequestMapping("/ai")
class AiController(
    private val recipeGenerator: RecipeGenerator
) {
    
    /**
     * AI定制食谱
     * 
     * 请求示例:
     * POST /api/ai/generate-recipe
     * {
     *   "availableIngredients": ["西红柿", "鸡蛋"],
     *   "cuisineType": "家常菜",
     *   "taste": "微辣",
     *   "cookingTime": 20,
     *   "difficulty": "EASY",
     *   "dietaryRestrictions": ["不吃海鲜"],
     *   "nutritionGoals": ["减脂", "低糖"],
     *   "cookingEquipment": "燃气灶",
     *   "servings": 2
     * }
     */
    @PostMapping("/generate-recipe")
    fun generateRecipe(
        @RequestHeader("user-id") userId: Long,
        @RequestBody request: RecipeGenerationRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val recipe = recipeGenerator.generateRecipe(request)
            ResponseEntity.ok(ApiResponse.success(recipe, "AI食谱生成成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "生成失败"))
        }
    }
    
    /**
     * AI快速食谱(基于用户现有食材)
     */
    @PostMapping("/quick-recipe")
    fun quickRecipe(
        @RequestHeader("user-id") userId: Long,
        @RequestBody request: QuickRecipeRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val generationRequest = RecipeGenerationRequest(
                availableIngredients = request.ingredients,
                cookingTime = request.timeLimit ?: 30,
                difficulty = "EASY"
            )
            
            val recipe = recipeGenerator.generateRecipe(generationRequest)
            ResponseEntity.ok(ApiResponse.success(recipe, "快手食谱生成成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "生成失败"))
        }
    }
    
    /**
     * AI对话式烹饪助手
     * 
     * 示例：
     * - "我有西红柿和鸡蛋，能做什么菜？"
     * - "怎么做宫保鸡丁？"
     * - "糖醋排骨的糖醋比例是多少？"
     */
    @PostMapping("/chat")
    fun chat(
        @RequestHeader("user-id") userId: Long,
        @RequestBody request: ChatRequest
    ): ResponseEntity<ApiResponse<ChatResponse>> {
        return try {
            // TODO: 实现AI对话功能
            // 可以使用通义千问的对话API
            val response = ChatResponse(
                reply = "抱歉，AI对话功能正在开发中",
                suggestions = emptyList()
            )
            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "对话失败"))
        }
    }
}

data class QuickRecipeRequest(
    val ingredients: List<String>,
    val timeLimit: Int?
)

data class ChatRequest(
    val message: String,
    val context: List<String>? = null  // 对话上下文
)

data class ChatResponse(
    val reply: String,
    val suggestions: List<String>  // 建议的后续问题
)
