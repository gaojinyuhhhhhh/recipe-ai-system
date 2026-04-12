package com.recipe.event

import com.recipe.service.UserBehaviorService

/**
 * 用户行为事件
 */
data class UserBehaviorEvent(
    val userId: Long,
    val recipeId: Long,
    val behaviorType: UserBehaviorService.BehaviorType,
    val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()
)
