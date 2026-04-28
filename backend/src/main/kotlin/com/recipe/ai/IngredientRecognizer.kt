package com.recipe.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.recipe.entity.Freshness
import com.recipe.entity.StorageMethod
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 食材识别服务
 * 对应功能: 1.1.2 食材管理模块 - 多模态AI拍照识别
 */
@Service
class IngredientRecognizer(
    private val aiClient: TongYiAiClient,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(IngredientRecognizer::class.java)
    
    /**
     * 识别食材图片
     * @param imageBase64 Base64编码的图片
     * @return 识别结果列表
     */
    fun recognize(imageBase64: String): List<RecognizedIngredient> {
        val prompt = """
            请识别图片中的所有食材，并返回JSON格式数据。
            要求：
            1. 准确识别食材名称(中文)
            2. 判断食材类别(蔬菜/水果/肉类/海鲜/蛋奶/调味品/主食/饮品/其他)
            3. 判断新鲜度(FRESH新鲜/WILTING微蔫/SPOILING即将变质)
            4. 预估重量(如: "200g", "3个", "1斤")
            5. 推荐保存方式(ROOM_TEMP常温/REFRIGERATE冷藏/FREEZE冷冻/DRY_COOL干燥阴凉处)
            6. 计算保质天数(从今天算起能保存多少天)
            
            返回格式(纯JSON,无其他文字):
            {
              "items": [
                {
                  "name": "西红柿",
                  "category": "蔬菜",
                  "freshness": "FRESH",
                  "estimatedWeight": "500g",
                  "storageMethod": "REFRIGERATE",
                  "shelfLife": 7
                }
              ]
            }
        """.trimIndent()
        
        try {
            log.info("开始识别食材图片，Base64长度: {}", imageBase64.length)
            val response = aiClient.recognizeImage(imageBase64, prompt)
            log.info("AI识别响应内容: {}", response.content.take(500))
            val jsonContent = extractJson(response.content)
            log.debug("提取的JSON: {}", jsonContent.take(500))
            val result: RecognitionResult = objectMapper.readValue(jsonContent)
            log.info("识别完成，共识别 {} 种食材", result.items.size)
            return result.items
        } catch (e: Exception) {
            log.error("食材识别失败: {}", e.message, e)
            throw Exception("食材识别失败: ${e.message}", e)
        }
    }
    
    /**
     * 生成保存建议
     */
    fun generateStorageAdvice(
        ingredientName: String,
        freshness: Freshness,
        category: String?
    ): String {
        val prompt = """
            请为以下食材生成保存建议：
            - 食材名称: $ingredientName
            - 新鲜度: ${freshness.display}
            - 类别: ${category ?: "未知"}
            
            要求：
            1. 简洁明了(50字以内)
            2. 实用可操作
            3. 如果是微蔫食材,建议尽快食用
            
            直接返回建议文本,无需其他说明。
        """.trimIndent()
        
        return try {
            aiClient.generateText(prompt).content.trim()
        } catch (e: Exception) {
            "请妥善保存，注意查看保质期"
        }
    }
    
    /**
     * 临期食材快手利用方案
     */
    fun generateQuickUsageSolution(ingredientName: String, remainingDays: Int): String {
        val prompt = """
            食材"$ingredientName"还有${remainingDays}天就要过期了。
            请提供1-2个极简快手利用方案：
            1. 烹饪方法简单(10分钟内完成)
            2. 不需要太多其他食材
            3. 能最大化利用该食材
            
            格式: 方案1: xxx  方案2: xxx
            要求简洁(100字以内)
        """.trimIndent()
        
        return try {
            aiClient.generateText(prompt).content.trim()
        } catch (e: Exception) {
            "建议尽快烹饪食用，或制作简单料理"
        }
    }
    
    /**
     * 从AI响应中提取JSON
     */
    private fun extractJson(content: String): String {
        // 移除可能的markdown代码块标记
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
 * 识别结果数据类
 */
data class RecognitionResult(
    val items: List<RecognizedIngredient>
)

data class RecognizedIngredient(
    val name: String,
    val category: String,
    val freshness: String,  // FRESH/WILTING/SPOILING
    val estimatedWeight: String,
    val storageMethod: String,  // ROOM_TEMP/REFRIGERATE/FREEZE/DRY_COOL
    val shelfLife: Int
) {
    fun getFreshnessEnum(): Freshness {
        return try {
            Freshness.valueOf(freshness)
        } catch (e: Exception) {
            Freshness.FRESH
        }
    }
    
    fun getStorageMethodEnum(): StorageMethod {
        return try {
            StorageMethod.valueOf(storageMethod)
        } catch (e: Exception) {
            StorageMethod.REFRIGERATE
        }
    }
    
    fun getExpiryDate(): LocalDate {
        return LocalDate.now().plusDays(shelfLife.toLong())
    }
}
