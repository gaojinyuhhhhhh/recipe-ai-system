package com.recipe.ai

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 通义千问AI客户端
 * 对应功能: 1.1.6 AI个性化定制模块
 * 
 * 功能:
 * - 文本生成(食谱定制、优化建议)
 * - 图像识别(食材识别)
 * - 语音合成(烹饪指导)
 */
@Component
class TongYiAiClient(
    @Value("\${ai.tongyi.api-key}") private val apiKey: String,
    @Value("\${ai.tongyi.base-url}") private val baseUrl: String,
    @Value("\${ai.tongyi.model.text}") private val textModel: String,
    @Value("\${ai.tongyi.model.vision}") private val visionModel: String,
    @Value("\${ai.tongyi.timeout}") private val timeout: Long,
    @Value("\${ai.tongyi.max-tokens}") private val maxTokens: Int,
    private val objectMapper: ObjectMapper
) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.MILLISECONDS)
        .readTimeout(timeout, TimeUnit.MILLISECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * 文本生成 - 通用方法
     */
    fun generateText(
        prompt: String,
        systemPrompt: String? = null,
        temperature: Double = 0.7
    ): AiResponse {
        val messages = mutableListOf<Map<String, String>>()
        
        if (systemPrompt != null) {
            messages.add(mapOf("role" to "system", "content" to systemPrompt))
        }
        messages.add(mapOf("role" to "user", "content" to prompt))
        
        val requestBody = mapOf(
            "model" to textModel,
            "input" to mapOf("messages" to messages),
            "parameters" to mapOf(
                "max_tokens" to maxTokens,
                "temperature" to temperature,
                "result_format" to "message"
            )
        )
        
        return executeRequest("$baseUrl/services/aigc/text-generation/generation", requestBody)
    }
    
    /**
     * 图像识别 - 食材识别
     */
    fun recognizeImage(
        imageBase64: String,
        prompt: String = "请识别图片中的食材，返回JSON格式: {\"items\": [{\"name\": \"食材名\", \"category\": \"类别\", \"freshness\": \"新鲜度\", \"estimatedWeight\": \"预估重量\"}]}"
    ): AiResponse {
        val messages = listOf(
            mapOf(
                "role" to "user",
                "content" to listOf(
                    mapOf("image" to "data:image/jpeg;base64,$imageBase64"),
                    mapOf("text" to prompt)
                )
            )
        )
        
        val requestBody = mapOf(
            "model" to visionModel,
            "input" to mapOf("messages" to messages),
            "parameters" to mapOf("max_tokens" to maxTokens)
        )
        
        return executeRequest("$baseUrl/services/aigc/multimodal-generation/generation", requestBody)
    }
    
    /**
     * 执行HTTP请求
     */
    private fun executeRequest(url: String, requestBody: Map<String, Any>): AiResponse {
        val jsonBody = objectMapper.writeValueAsString(requestBody)
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()
        
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("AI API request failed: ${response.code} - $responseBody")
            }
            
            return parseResponse(responseBody)
        }
    }
    
    /**
     * 解析AI响应
     */
    private fun parseResponse(responseBody: String): AiResponse {
        val jsonResponse = objectMapper.readTree(responseBody)
        
        val output = jsonResponse.get("output")
        val text = output?.get("text")?.asText() 
            ?: output?.get("choices")?.get(0)?.get("message")?.get("content")?.asText()
            ?: throw Exception("Failed to parse AI response")
        
        val usage = jsonResponse.get("usage")
        val inputTokens = usage?.get("input_tokens")?.asInt() ?: 0
        val outputTokens = usage?.get("output_tokens")?.asInt() ?: 0
        
        return AiResponse(
            content = text,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = inputTokens + outputTokens
        )
    }
}

/**
 * AI响应数据类
 */
data class AiResponse(
    val content: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)