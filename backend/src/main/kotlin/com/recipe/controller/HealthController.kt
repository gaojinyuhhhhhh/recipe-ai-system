package com.recipe.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 健康检查和系统信息控制器
 */
@RestController
@RequestMapping
class HealthController {
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to "UP",
            "service" to "recipe-ai-backend",
            "version" to "1.0.0",
            "timestamp" to LocalDateTime.now()
        )
        return ResponseEntity.ok(response)
    }
    
    /**
     * 系统信息
     */
    @GetMapping("/info")
    fun info(): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "name" to "AI智能食谱管理系统",
            "description" to "基于通义千问AI的智能食谱管理平台",
            "version" to "1.0.0",
            "features" to listOf(
                "AI食材识别",
                "智能过期提醒",
                "AI食谱生成",
                "智能推荐",
                "食谱社区"
            )
        )
        return ResponseEntity.ok(response)
    }
}