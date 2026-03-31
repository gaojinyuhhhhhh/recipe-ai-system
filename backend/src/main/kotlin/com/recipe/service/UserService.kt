package com.recipe.service

import com.recipe.entity.User
import com.recipe.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 用户服务
 * 对应功能: 1.1.1 用户管理模块
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder = BCryptPasswordEncoder()
) {
    
    /**
     * 用户注册
     */
    @Transactional
    fun register(username: String, password: String, phone: String?): User {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(username)) {
            throw Exception("用户名已存在")
        }
        
        // 检查手机号是否存在
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw Exception("手机号已注册")
        }
        
        // 创建用户
        val user = User(
            username = username,
            password = passwordEncoder.encode(password),
            phone = phone,
            nickname = username
        )
        
        return userRepository.save(user)
    }
    
    /**
     * 用户登录
     */
    fun login(username: String, password: String): User {
        val user = userRepository.findByUsername(username)
            ?: throw Exception("用户名或密码错误")
        
        if (!passwordEncoder.matches(password, user.password)) {
            throw Exception("用户名或密码错误")
        }
        
        if (!user.enabled) {
            throw Exception("账号已被禁用")
        }
        
        return user
    }
    
    /**
     * 更新用户信息
     */
    @Transactional
    fun updateUser(userId: Long, updates: UserUpdateRequest): User {
        val user = userRepository.findById(userId)
            .orElseThrow { Exception("用户不存在") }
        
        updates.nickname?.let { user.nickname = it }
        updates.avatar?.let { user.avatar = it }
        updates.phone?.let { 
            if (it != user.phone && userRepository.existsByPhone(it)) {
                throw Exception("手机号已被使用")
            }
            user.phone = it
        }
        updates.familySize?.let { user.familySize = it }
        updates.cookingFrequency?.let { user.cookingFrequency = it }
        
        return userRepository.save(user)
    }
    
    /**
     * 设置用户偏好
     */
    @Transactional
    fun setPreferences(userId: Long, preferences: String): User {
        val user = userRepository.findById(userId)
            .orElseThrow { Exception("用户不存在") }
        
        user.preferences = preferences
        return userRepository.save(user)
    }
    
    /**
     * 更新AI画像
     */
    @Transactional
    fun updateAiProfile(userId: Long, aiProfile: String): User {
        val user = userRepository.findById(userId)
            .orElseThrow { Exception("用户不存在") }
        
        user.aiProfile = aiProfile
        return userRepository.save(user)
    }
    
    /**
     * 重置AI画像
     */
    @Transactional
    fun resetAiProfile(userId: Long): User {
        val user = userRepository.findById(userId)
            .orElseThrow { Exception("用户不存在") }
        
        user.aiProfile = null
        return userRepository.save(user)
    }
    
    /**
     * 修改密码
     */
    @Transactional
    fun changePassword(userId: Long, oldPassword: String, newPassword: String): User {
        val user = userRepository.findById(userId)
            .orElseThrow { Exception("用户不存在") }
        
        if (!passwordEncoder.matches(oldPassword, user.password)) {
            throw Exception("原密码错误")
        }
        
        user.password = passwordEncoder.encode(newPassword)
        return userRepository.save(user)
    }
    
    /**
     * 获取用户信息
     */
    fun getUserInfo(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { Exception("用户不存在") }
    }
}

/**
 * 用户更新请求
 */
data class UserUpdateRequest(
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val familySize: Int? = null,
    val cookingFrequency: Int? = null
)
