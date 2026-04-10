package com.recipe.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.recipe.entity.AiRating
import com.recipe.entity.Difficulty
import org.springframework.stereotype.Service

/**
 * 食谱生成服务
 * 对应功能: 1.1.6 AI个性化定制模块
 */
@Service
class RecipeGenerator(
    private val aiClient: TongYiAiClient,
    private val objectMapper: ObjectMapper
) {
    
    /**
     * AI定制食谱
     * @param request 定制请求
     * @return 生成的食谱
     */
    fun generateRecipe(request: RecipeGenerationRequest): GeneratedRecipe {
        val systemPrompt = """
            你是一位资深营养师和星级厨师。你需要根据用户需求定制专属食谱。
            
            要求：
            1. 食材用量精确到克/毫升
            2. 步骤详细且包含时间(秒为单位)
            3. 调味比例准确
            4. 提供营养配比信息
            5. 考虑烹饪设备差异(燃气灶/电磁炉)
        """.trimIndent()
        
        val prompt = buildPrompt(request)
        
        try {
            val response = aiClient.generateText(prompt, systemPrompt, temperature = 0.8)
            val jsonContent = extractJson(response.content)
            return objectMapper.readValue(jsonContent)
        } catch (e: Exception) {
            throw Exception("食谱生成失败: ${e.message}", e)
        }
    }
    
    /**
     * AI食谱质量评级
     */
    fun rateRecipe(
        ingredients: String,
        steps: String,
        title: String
    ): RecipeRating {
        val prompt = """
            请对以下食谱进行质量评级：
            
            标题: $title
            食材: $ingredients
            步骤: $steps
            
            从以下4个维度评分(0-100分):
            1. ingredient: 食材搭配合理性
            2. nutrition: 营养均衡性
            3. steps: 步骤完整性和清晰度
            4. difficulty: 难度适配性(是否适合目标人群)
            
            综合评级: S(90+) / A(80-89) / B(70-79) / C(60-69)
            
            返回JSON格式(纯JSON无其他文字):
            {
              "rating": "A",
              "scores": {
                "ingredient": 85,
                "nutrition": 88,
                "steps": 82,
                "difficulty": 90
              },
              "suggestions": ["建议1", "建议2"]
            }
        """.trimIndent()
        
        try {
            val response = aiClient.generateText(prompt)
            val jsonContent = extractJson(response.content)
            return objectMapper.readValue(jsonContent)
        } catch (e: Exception) {
            // 返回默认评级
            return RecipeRating(
                rating = "B",
                scores = mapOf(
                    "ingredient" to 75,
                    "nutrition" to 75,
                    "steps" to 75,
                    "difficulty" to 75
                ),
                suggestions = listOf("无法自动评级，请手动检查")
            )
        }
    }
    
    /**
     * AI优化食谱
     */
    fun optimizeRecipe(
        title: String,
        ingredients: String,
        steps: String,
        suggestions: List<String>
    ): GeneratedRecipe {
        val systemPrompt = """
            你是一位资深营养师和星级厨师。你需要根据优化建议改进食谱。
            保持原食谱的风格和难度，只做必要的优化。
        """.trimIndent()
        
        val prompt = """
            原食谱:
            标题: $title
            食材: $ingredients
            步骤: $steps
            
            优化建议:
            ${suggestions.joinToString("\n") { "- $it" }}
            
            请优化这个食谱，返回完整的优化后版本(JSON格式,同食谱生成格式)。
        """.trimIndent()
        
        try {
            val response = aiClient.generateText(prompt, systemPrompt)
            val jsonContent = extractJson(response.content)
            return objectMapper.readValue(jsonContent)
        } catch (e: Exception) {
            throw Exception("食谱优化失败: ${e.message}", e)
        }
    }
    
    /**
     * 构建生成提示词
     */
    private fun buildPrompt(request: RecipeGenerationRequest): String {
        return """
            请生成一个食谱，要求如下：
            
            ${if (request.availableIngredients.isNotEmpty()) 
                "可用食材: ${request.availableIngredients.joinToString(", ")}" 
                else ""}
            ${if (request.cuisineType != null) "菜系: ${request.cuisineType}" else ""}
            ${if (request.taste != null) "口味: ${request.taste}" else ""}
            ${if (request.cookingTime != null) "烹饪时长: ${request.cookingTime}分钟内" else ""}
            ${if (request.difficulty != null) "难度: ${request.difficulty}" else ""}
            ${if (request.dietaryRestrictions.isNotEmpty()) 
                "饮食禁忌: ${request.dietaryRestrictions.joinToString(", ")}" 
                else ""}
            ${if (request.nutritionGoals.isNotEmpty()) 
                "营养需求: ${request.nutritionGoals.joinToString(", ")}" 
                else ""}
            ${if (request.cookingEquipment != null) 
                "烹饪设备: ${request.cookingEquipment}" 
                else ""}
            
            **重要要求**:
            1. 食材用量必须具体精确，禁止使用"适量"、"少许"、"适量即可"等模糊词汇
            2. 食材用量规范：
               - 蔬菜类：使用克(g)，如 200g、500g
               - 肉类：使用克(g)，如 300g、500g
               - 液体：使用毫升(ml)，如 50ml、100ml
               - 蛋类：使用个，如 2个、3个
               - 调料：使用克(g)或毫升(ml)，如 5g、10ml
               - 主食：使用克(g)，如 200g米饭
            3. 步骤描述要详细，包含火候、时间等关键信息
            
            返回JSON格式(纯JSON无其他文字):
            {
              "title": "菜名",
              "description": "简短描述",
              "ingredients": [
                {"name": "食材名", "quantity": 数字, "unit": "单位", "notes": "备注(可选)"}
              ],
              "steps": [
                {"step": 序号, "content": "步骤内容", "duration": 时长秒数, "temperature": "温度(可选)", "tips": "提示(可选)"}
              ],
              "cookingTime": 总时长分钟,
              "difficulty": "EASY/MEDIUM/HARD",
              "cuisine": "菜系",
              "tags": ["标签1", "标签2"],
              "nutrition": {
                "calories": "卡路里",
                "protein": "蛋白质g",
                "carbs": "碳水g",
                "fat": "脂肪g"
              }
            }
        """.trimIndent()
    }
    
    private fun extractJson(content: String): String {
        var json = content.trim()
        if (json.startsWith("```json")) {
            json = json.removePrefix("```json").removeSuffix("```").trim()
        } else if (json.startsWith("```")) {
            json = json.removePrefix("```").removeSuffix("```").trim()
        }
        return json
    }
}

/**
 * 食谱生成请求
 */
data class RecipeGenerationRequest(
    val availableIngredients: List<String> = emptyList(),  // 可用食材
    val cuisineType: String? = null,  // 菜系
    val taste: String? = null,  // 口味
    val cookingTime: Int? = null,  // 时长限制(分钟)
    val difficulty: String? = null,  // 难度
    val dietaryRestrictions: List<String> = emptyList(),  // 饮食禁忌
    val nutritionGoals: List<String> = emptyList(),  // 营养需求(减脂/控糖等)
    val cookingEquipment: String? = null,  // 烹饪设备
    val servings: Int = 2  // 份数
)

/**
 * 生成的食谱
 */
data class GeneratedRecipe(
    val title: String,
    val description: String?,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStep>,
    val cookingTime: Int,
    val difficulty: String,
    val cuisine: String?,
    val tags: List<String>,
    val nutrition: NutritionInfo?
)

data class RecipeIngredient(
    val name: String,
    val quantity: Double,
    val unit: String,
    val notes: String?
)

data class RecipeStep(
    val step: Int,
    val content: String,
    val duration: Int,  // 秒
    val temperature: String?,
    val tips: String?
)

data class NutritionInfo(
    val calories: String,
    val protein: String,
    val carbs: String,
    val fat: String
)

/**
 * 食谱评级结果
 */
data class RecipeRating(
    val rating: String,  // S/A/B/C
    val scores: Map<String, Int>,
    val suggestions: List<String>
) {
    fun getAiRating(): AiRating {
        return when (rating) {
            "S" -> AiRating.S
            "A" -> AiRating.A
            "B" -> AiRating.B
            else -> AiRating.C
        }
    }
}
