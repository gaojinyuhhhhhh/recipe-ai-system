package com.recipe

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * AI智能食谱管理系统 - 后端启动类
 * 
 * 功能：
 * - 启动Spring Boot应用
 * - 开启缓存支持
 * - 开启定时任务(用于食材过期提醒)
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
class RecipeApplication

fun main(args: Array<String>) {
    runApplication<RecipeApplication>(*args)
    println("""
        ╔═══════════════════════════════════════════════╗
        ║   AI智能食谱管理系统 Backend Started! 🚀      ║
        ║   API文档: http://localhost:8080/api/doc      ║
        ║   健康检查: http://localhost:8080/api/health  ║
        ╚═══════════════════════════════════════════════╝
    """.trimIndent())
}
