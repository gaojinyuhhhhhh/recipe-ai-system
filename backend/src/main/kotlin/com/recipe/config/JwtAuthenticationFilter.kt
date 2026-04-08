package com.recipe.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT认证过滤器
 * 从请求头中提取Token，验证后将用户信息写入SecurityContext和request attribute
 */
@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null && jwtUtil.validateToken(token)) {
            val userId = jwtUtil.getUserIdFromToken(token)
            val username = jwtUtil.getUsernameFromToken(token)

            if (userId != null) {
                // 将userId写入request attribute，供Controller使用
                request.setAttribute("jwtUserId", userId)
                request.setAttribute("jwtUsername", username)

                // 设置Spring Security认证上下文
                val authentication = UsernamePasswordAuthenticationToken(
                    userId, null, emptyList()
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * 从Authorization头中提取Bearer Token
     */
    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else null
    }
}
