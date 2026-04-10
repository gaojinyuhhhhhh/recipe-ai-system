package com.recipe.controller

import com.recipe.ai.IngredientRecognizer
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
    private val shoppingService: ShoppingService,
    private val ingredientRecognizer: IngredientRecognizer
) : BaseController() {
    
    /**
     * AI食材拍照识别
     * 
     * 请求示例:
     * POST /api/ai/recognize-ingredients
     * {
     *   "imageBase64": "/9j/4AAQSkZJRgABAQ..."
     * }
     */
    @PostMapping("/recognize-ingredients")
    fun recognizeIngredients(
        @RequestBody request: RecognizeIngredientsRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            currentUserId() // 验证登录状态
            val recognizedItems = ingredientRecognizer.recognize(request.imageBase64)
            ResponseEntity.ok(ApiResponse.success(recognizedItems, "食材识别成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "识别失败"))
        }
    }
    
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
     * AI辅助创建食谱 - 根据菜名生成完整食谱信息
     * 
     * 请求示例:
     * POST /api/ai/assist-create-recipe
     * {
     *   "title": "宫保鸡丁",
     *   "servings": 2
     * }
     */
    @PostMapping("/assist-create-recipe")
    fun assistCreateRecipe(
        @RequestBody request: AssistCreateRecipeRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            currentUserId() // 验证登录状态
            
            val systemPrompt = """
                你是一位资深厨师和营养师。根据用户提供的菜名，生成一份详细的食谱。
                
                **重要要求**:
                1. 食材用量必须具体精确，禁止使用"适量"、"少许"等模糊词汇
                2. 食材用量规范：
                   - 蔬菜类：使用克(g)，如 200g、500g
                   - 肉类：使用克(g)，如 300g、500g
                   - 液体：使用毫升(ml)，如 50ml、100ml
                   - 蛋类：使用个，如 2个、3个
                   - 调料：使用克(g)或毫升(ml)，如 5g、10ml
                3. 步骤描述要详细，包含火候、时间等关键信息
                4. 根据菜名推断菜系、难度、烹饪时长
                
                返回JSON格式(纯JSON无其他文字)。
            """.trimIndent()
            
            val prompt = """
                菜名: ${request.title}
                份量: ${request.servings}人份
                
                请生成完整食谱，返回JSON格式:
                {
                  "title": "菜名",
                  "description": "简短描述(50字以内)",
                  "ingredients": [
                    {"name": "食材名", "quantity": 数字, "unit": "单位(g/ml/个等)", "notes": "备注(可选)"}
                  ],
                  "steps": [
                    {"step": 序号, "content": "步骤内容", "duration": 时长秒数, "temperature": "温度(可选)", "tips": "提示(可选)"}
                  ],
                  "cookingTime": 总时长分钟,
                  "difficulty": "EASY/MEDIUM/HARD",
                  "cuisine": "菜系",
                  "tags": ["标签1", "标签2"]
                }
            """.trimIndent()
            
            val aiResponse = aiClient.generateText(prompt, systemPrompt, temperature = 0.7)
            val jsonContent = extractJson(aiResponse.content)
            
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
            val result = mapper.readValue(jsonContent, Map::class.java)
            
            ResponseEntity.ok(ApiResponse.success(result, "AI食谱生成成功，请检查确认"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error("AI生成失败: ${e.message}"))
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
     * AI基于现有食材智能推荐食谱
     *
     * 请求示例:
     * POST /api/ai/suggest-by-ingredients
     * {
     *   "ingredients": ["西红柿", "鸡蛋", "面粉"],
     *   "preferences": "快手菜"
     * }
     */
    @PostMapping("/suggest-by-ingredients")
    fun suggestByIngredients(
        @RequestBody request: SuggestByIngredientsRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            currentUserId()

            val systemPrompt = """
                你是一位资深厨师和营养师。根据用户提供的食材，推荐3-5道可以制作的菜品。
                要求简洁实用，适合家庭烹饪。
            """.trimIndent()

            val prefStr = if (request.preferences != null) "\n用户偏好: ${request.preferences}" else ""
            val prompt = """
                我冰箱里有这些食材: ${request.ingredients.joinToString("、")}
                $prefStr
                
                请推荐3-5道菜，返回JSON格式(纯JSON无其他文字):
                {
                  "recipes": [
                    {
                      "name": "菜名",
                      "description": "简短描述(20字以内)",
                      "mainIngredients": ["使用的主要食材"],
                      "cookingTime": 预估分钟数,
                      "difficulty": "EASY/MEDIUM/HARD",
                      "tags": ["标签"],
                      "briefSteps": "简要步骤(50字以内)"
                    }
                  ]
                }
            """.trimIndent()

            val response = aiClient.generateText(prompt, systemPrompt, temperature = 0.8)
            val jsonContent = extractJson(response.content)
            val result = com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
                .readValue(jsonContent, Map::class.java)
            ResponseEntity.ok(ApiResponse.success(result, "推荐成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "推荐失败"))
        }
    }

    /**
     * 从AI响应中提取JSON
     */
    private fun extractJson(content: String): String {
        var json = content.trim()
        val triple = "${'`'}${'`'}${'`'}"
        if (json.startsWith(triple + "json")) {
            json = json.removePrefix(triple + "json").removeSuffix(triple).trim()
        } else if (json.startsWith(triple)) {
            json = json.removePrefix(triple).removeSuffix(triple).trim()
        }
        return json
    }

    /**
     * 生成建议的后续问题
     */
    /**
     * AI推断食材保质期和保存信息
     * 
     * 请求示例:
     * POST /api/ai/ingredient-info
     * {
     *   "name": "西红柿"
     * }
     */
    @PostMapping("/ingredient-info")
    fun inferIngredientInfo(
        @RequestBody request: IngredientInfoRequest
    ): ResponseEntity<ApiResponse<IngredientInfoResponse>> {
        return try {
            currentUserId()
            
            val systemPrompt = """
                你是一位专业的食材保存专家。根据食材名称推断保质期和保存建议。
                返回JSON格式，不要有其他文字。
            """.trimIndent()
            
            val prompt = """
                食材: ${request.name}
                
                请推断并返回JSON格式:
                {
                  "shelfLife": 保质天数(数字),
                  "storageMethod": "保存方式(REfrigerated/冷藏, frozen/冷冻, room_temp/常温)",
                  "storageAdvice": "保存建议提示(简短)",
                  "freshness": "新鲜度(FRESH/新鲜, WILTING/微蔽, SPOILED/变质)"
                }
            """.trimIndent()
            
            val aiResponse = aiClient.generateText(prompt, systemPrompt, temperature = 0.3)
            val jsonContent = extractJson(aiResponse.content)
            
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
            val info = mapper.readValue(jsonContent, IngredientInfoResponse::class.java)
            
            ResponseEntity.ok(ApiResponse.success(info, "推断成功"))
        } catch (e: Exception) {
            // 如果AI失败，返回默认值
            val defaultInfo = IngredientInfoResponse(
                shelfLife = 7,
                storageMethod = "REFRIGERATED",
                storageAdvice = "建议冷藏保存",
                freshness = "FRESH"
            )
            ResponseEntity.ok(ApiResponse.success(defaultInfo, "使用默认值"))
        }
    }

    /**
     * 从采购清单完成并添加到食材库（带AI推断）
     * 
     * 请求示例:
     * POST /api/ai/complete-and-add
     * {
     *   "itemIds": [1],
     *   "customInfo": {
     *     "shelfLife": 7,
     *     "storageMethod": "REFRIGERATED",
     *     "storageAdvice": "建议冷藏保存",
     *     "freshness": "FRESH"
     *   }
     * }
     */
    @PostMapping("/complete-and-add")
    fun completeAndAddToIngredients(
        @RequestBody request: CompleteAndAddRequest
    ): ResponseEntity<ApiResponse<List<com.recipe.entity.Ingredient>>> {
        return try {
            val userId = currentUserId()
            val ingredients = shoppingService.completeAndAddWithAiInfo(
                userId, 
                request.itemIds, 
                aiClient,
                request.customInfo
            )
            ResponseEntity.ok(ApiResponse.success(ingredients, "已完成${ingredients.size}个采购项并添加到食材库"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "操作失败"))
        }
    }

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

/**
 * 食材识别请求
 */
data class RecognizeIngredientsRequest(
    val imageBase64: String
)

/**
 * AI基于食材推荐食谱请求
 */
data class SuggestByIngredientsRequest(
    val ingredients: List<String>,
    val preferences: String? = null  // 用户偏好：如"快手菜"、"减脂"等
)

/**
 * AI推断食材信息请求
 */
data class IngredientInfoRequest(
    val name: String
)

/**
 * AI推断食材信息响应
 */
data class IngredientInfoResponse(
    val shelfLife: Int,           // 保质天数
    val storageMethod: String,    // 保存方式
    val storageAdvice: String,    // 保存建议
    val freshness: String         // 新鲜度
)

/**
 * 完成并添加到食材库请求
 */
data class CompleteAndAddRequest(
    val itemIds: List<Long>,
    val customInfo: CustomIngredientInfo? = null  // 用户自定义的食材信息
)

/**
 * 用户自定义食材信息
 */
data class CustomIngredientInfo(
    val shelfLife: Int,              // 保质天数
    val storageMethod: String,       // 保存方式
    val storageAdvice: String,       // 保存建议
    val freshness: String,           // 新鲜度
    val actualQuantity: Double? = null,  // 用户实际购买的数量
    val unit: String? = null             // 单位
)

/**
 * AI辅助创建食谱请求
 */
data class AssistCreateRecipeRequest(
    val title: String,            // 菜名
    val servings: Int = 2         // 份数，默认2人份
)
