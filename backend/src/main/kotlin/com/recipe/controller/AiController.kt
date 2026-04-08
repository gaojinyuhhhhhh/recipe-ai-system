package com.recipe.controller

import com.recipe.ai.RecipeGenerationRequest
import com.recipe.ai.RecipeGenerator
import com.recipe.ai.TongYiAiClient
import com.recipe.dto.ApiResponse
import com.recipe.service.RecipeService
import com.recipe.service.ShoppingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * AI功能控制器
 * 对应功能: 1.1.6 AI个性化定制模块
 */
@RestController
@RequestMapping("/ai")
class AiController(
    private val recipeGenerator: RecipeGenerator,
    private val aiClient: TongYiAiClient,
    private val recipeService: RecipeService,
    private val shoppingService: ShoppingService
) : BaseController() {
    
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
        @RequestBody request: RecipeGenerationRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            currentUserId() // 验证登录状态
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
        @RequestBody request: QuickRecipeRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            currentUserId() // 验证登录状态
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
        @RequestBody request: ChatRequest
    ): ResponseEntity<ApiResponse<ChatResponse>> {
        return try {
            currentUserId()

            val systemPrompt = """
                你是一位资深营养师和星级厨师，名叫"食智助手"。
                你擅长：
                1. 根据用户提供的食材推荐菜品
                2. 解答烹饪技巧、调味比例、火候控制等问题
                3. 提供营养搭配建议
                4. 食材保存和处理建议
                
                回答要求：
                - 简洁实用，通俗易懂
                - 用量精确到克/毫升
                - 步骤清晰，时间明确
                - 必要时提供替代方案
            """.trimIndent()

            // 拼接上下文历史
            val contextStr = if (!request.context.isNullOrEmpty()) {
                "之前的对话\u4e0a\u4e0b\u6587:\n" + request.context.joinToString("\n")
            } else ""

            val fullPrompt = if (contextStr.isNotBlank()) {
                "$contextStr\n\n用户新问题: ${request.message}"
            } else {
                request.message
            }

            val aiResponse = aiClient.generateText(fullPrompt, systemPrompt, temperature = 0.8)

            // 生成建议的后续问题
            val suggestions = generateSuggestions(request.message)

            val response = ChatResponse(
                reply = aiResponse.content.trim(),
                suggestions = suggestions
            )
            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "对话失败"))
        }
    }

    /**
     * AI定制食谱一键保存到个人食谱
     */
    @PostMapping("/save-recipe")
    fun saveGeneratedRecipe(
        @RequestBody request: SaveRecipeRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val recipe = recipeService.saveGeneratedRecipe(userId, request)
            ResponseEntity.ok(ApiResponse.success(recipe, "食谱已保存到个人食谱"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "保存失败"))
        }
    }

    /**
     * 缺少食材一键加入采购清单
     */
    @PostMapping("/add-missing-to-shopping")
    fun addMissingToShopping(
        @RequestBody request: MissingIngredientsRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val items = shoppingService.addMissingIngredients(userId, request.ingredients)
            ResponseEntity.ok(ApiResponse.success(items, "已将${items.size}个缺少食材加入采购清单"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "添加失败"))
        }
    }

    /**
     * 生成建议的后续问题
     */
    private fun generateSuggestions(userMessage: String): List<String> {
        return when {
            userMessage.contains("食材") || userMessage.contains("材料") -> listOf(
                "这些食材怎么保存最新鲜？",
                "还能做其他什么菜？",
                "这道菜的营养成分是什么？"
            )
            userMessage.contains("做法") || userMessage.contains("怎么做") -> listOf(
                "有更简单的做法吗？",
                "调味料可以替换吗？",
                "需要注意什么火候？"
            )
            userMessage.contains("减脂") || userMessage.contains("营养") -> listOf(
                "推荐其他减脂食谱",
                "每天应该吃多少卡路里？",
                "有哪些高蛋白食材推荐？"
            )
            else -> listOf(
                "推荐一个快手家常菜",
                "今天吃什么比较好？",
                "有什么简单的汤推荐吗？"
            )
        }
    }
}

data class QuickRecipeRequest(
    val ingredients: List<String>,
    val timeLimit: Int?
)

data class ChatRequest(
    val message: String,
    val context: List<String>? = null
)

data class ChatResponse(
    val reply: String,
    val suggestions: List<String>
)

/**
 * AI生成食谱保存请求
 */
data class SaveRecipeRequest(
    val title: String,
    val description: String? = null,
    val ingredients: String,  // JSON格式
    val steps: String,        // JSON格式
    val cookingTime: Int? = null,
    val difficulty: String = "MEDIUM",
    val cuisine: String? = null,
    val tags: String? = null   // JSON格式
)

/**
 * 缺少食材请求
 */
data class MissingIngredientsRequest(
    val ingredients: List<MissingIngredientItem>
)

data class MissingIngredientItem(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null
)
