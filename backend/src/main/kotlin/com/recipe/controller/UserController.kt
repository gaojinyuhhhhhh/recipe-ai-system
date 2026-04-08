package com.recipe.controller

import com.recipe.config.JwtUtil
import com.recipe.dto.ApiResponse
import com.recipe.service.UserService
import com.recipe.service.UserUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 用户管理控制器
 * 对应功能: 1.1.1 用户管理模块
 */
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val jwtUtil: JwtUtil
) : BaseController() {
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<Any>> {
        return try {
            val user = userService.register(request.username, request.password, request.phone)
            val token = jwtUtil.generateToken(user.id!!, user.username)
            val userInfo = mapOf(
                "id" to user.id,
                "username" to user.username,
                "nickname" to user.nickname,
                "phone" to user.phone,
                "token" to token
            )
            ResponseEntity.ok(ApiResponse.success(userInfo, "注册成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "注册失败"))
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<Any>> {
        return try {
            val user = userService.login(request.username, request.password)
            val token = jwtUtil.generateToken(user.id!!, user.username)
            val response = mapOf(
                "id" to user.id,
                "username" to user.username,
                "nickname" to user.nickname,
                "token" to token
            )
            ResponseEntity.ok(ApiResponse.success(response, "登录成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "登录失败"))
        }
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/me")
    fun getUserInfo(): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val user = userService.getUserInfo(userId)
            val userInfo = mapOf(
                "id" to user.id,
                "username" to user.username,
                "nickname" to user.nickname,
                "phone" to user.phone,
                "avatar" to user.avatar,
                "preferences" to user.preferences,
                "aiProfile" to user.aiProfile,
                "familySize" to user.familySize,
                "cookingFrequency" to user.cookingFrequency,
                "createdAt" to user.createdAt
            )
            ResponseEntity.ok(ApiResponse.success(userInfo))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "查询失败"))
        }
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/me")
    fun updateUser(
        @RequestBody request: UserUpdateRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userId = currentUserId()
            val user = userService.updateUser(userId, request)
            ResponseEntity.ok(ApiResponse.success(user, "更新成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "更新失败"))
        }
    }
    
    /**
     * 设置用户偏好
     */
    @PutMapping("/preferences")
    fun setPreferences(
        @RequestBody request: PreferencesRequest
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            val user = userService.setPreferences(userId, request.preferences)
            ResponseEntity.ok(ApiResponse.success(user.preferences ?: "", "偏好设置成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "设置失败"))
        }
    }
    
    /**
     * 重置AI画像
     */
    @PostMapping("/reset-ai-profile")
    fun resetAiProfile(): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            userService.resetAiProfile(userId)
            ResponseEntity.ok(ApiResponse.success("reset", "AI画像已重置"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "重置失败"))
        }
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/change-password")
    fun changePassword(
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = currentUserId()
            userService.changePassword(userId, request.oldPassword, request.newPassword)
            ResponseEntity.ok(ApiResponse.success("changed", "密码修改成功"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "修改失败"))
        }
    }
}

data class RegisterRequest(
    val username: String,
    val password: String,
    val phone: String?
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class PreferencesRequest(
    val preferences: String  // JSON格式
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
