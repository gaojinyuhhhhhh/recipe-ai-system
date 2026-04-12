package com.recipe.controller

import com.recipe.dto.ApiResponse
import com.recipe.service.UserBehaviorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 用户行为与AI学习控制器
 */
@RestController
@RequestMapping("/user-behavior")
class UserBehaviorController(
    private val userBehaviorService: UserBehaviorService
) : BaseController() {

    /**
     * 获取用户AI画像
     */
    @GetMapping("/profile")
    fun getAiProfile(): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val profile = userBehaviorService.getUserAiProfile(userId)
            ResponseEntity.ok(ApiResponse.success(profile ?: "暂无数据"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "获取失败"))
        }
    }

    /**
     * 获取偏好分析结果
     */
    @GetMapping("/analysis")
    fun getPreferenceAnalysis(): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val analysis = userBehaviorService.analyzePreferences(userId)
            ResponseEntity.ok(ApiResponse.success(analysis))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "分析失败"))
        }
    }

    /**
     * 获取AI推荐的偏好设置
     * 基于用户行为自动生成的偏好建议
     */
    @GetMapping("/suggested-preferences")
    fun getSuggestedPreferences(): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val preferences = userBehaviorService.convertToUserPreferences(userId)
            val analysis = userBehaviorService.analyzePreferences(userId)
            
            if (preferences != null) {
                ResponseEntity.ok(ApiResponse.success(
                    mapOf(
                        "preferences" to preferences,
                        "message" to "基于您的${analysis.totalInteractions}次交互行为分析得出",
                        "confidence" to calculateConfidence(analysis.totalInteractions)
                    )
                ))
            } else {
                ResponseEntity.ok(ApiResponse.success(
                    mapOf(
                        "message" to "交互数据不足，请多使用收藏、评论等功能",
                        "minRequired" to 5
                    )
                ))
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "生成失败"))
        }
    }

    /**
     * 应用AI推荐的偏好设置
     */
    @PostMapping("/apply-suggested-preferences")
    fun applySuggestedPreferences(): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val suggestedPrefs = userBehaviorService.convertToUserPreferences(userId)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("交互数据不足，无法生成偏好建议"))
            
            // 这里需要调用UserService来保存偏好
            // 简化处理，返回建议的偏好供前端保存
            ResponseEntity.ok(ApiResponse.success(
                mapOf(
                    "suggestedPreferences" to suggestedPrefs,
                    "message" to "请确认是否应用这些偏好设置"
                )
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "应用失败"))
        }
    }

    private fun calculateConfidence(interactions: Int): Double {
        return when {
            interactions >= 50 -> 0.95
            interactions >= 30 -> 0.85
            interactions >= 20 -> 0.75
            interactions >= 10 -> 0.60
            interactions >= 5 -> 0.40
            else -> 0.20
        }
    }
}
