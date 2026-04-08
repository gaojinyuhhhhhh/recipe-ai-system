package com.recipe.controller

import org.springframework.security.core.context.SecurityContextHolder

/**
 * Controller基类
 * 提供从JWT认证上下文中提取当前用户ID的公共方法
 */
abstract class BaseController {

    /**
     * 从当前认证上下文提取用户ID
     */
    protected fun currentUserId(): Long {
        return SecurityContextHolder.getContext().authentication?.principal as? Long
            ?: throw Exception("未登录或Token已过期")
    }
}
