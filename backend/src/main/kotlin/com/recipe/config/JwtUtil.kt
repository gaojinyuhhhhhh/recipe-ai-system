package com.recipe.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT工具类
 * 负责Token的生成、解析和验证
 */
@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {

    private val key: SecretKey by lazy {
        // 确保密钥至少256位
        val keyBytes = secret.toByteArray(StandardCharsets.UTF_8)
        Keys.hmacShaKeyFor(keyBytes)
    }

    /**
     * 生成JWT Token
     */
    fun generateToken(userId: Long, username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    /**
     * 从Token中提取用户ID
     */
    fun getUserIdFromToken(token: String): Long? {
        return try {
            val claims = parseToken(token)
            claims.subject.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从Token中提取用户名
     */
    fun getUsernameFromToken(token: String): String? {
        return try {
            val claims = parseToken(token)
            claims["username"] as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 验证Token是否有效
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = parseToken(token)
            !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 解析Token
     */
    private fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
