package com.recipe.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 用户实体类
 * 对应功能: 1.1.1 用户管理模块
 */
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(unique = true, nullable = false, length = 50)
    var username: String,
    
    @Column(unique = true, length = 11)
    var phone: String? = null,
    
    @Column(nullable = false)
    var password: String,  // 加密后的密码
    
    @Column(length = 50)
    var nickname: String? = null,
    
    @Column(length = 255)
    var avatar: String? = null,
    
    // 偏好标签(JSON格式存储)
    @Column(columnDefinition = "TEXT")
    var preferences: String? = null,  // {"cuisines":["川菜","粤菜"],"tastes":["微辣"],"diet":"减脂"}
    
    // AI学习的烹饪画像(JSON格式)
    @Column(columnDefinition = "TEXT")
    var aiProfile: String? = null,  // AI分析的用户画像
    
    // 家庭人数(用于采购量推荐)
    @Column
    var familySize: Int = 1,
    
    // 烹饪频率(次/周)
    @Column
    var cookingFrequency: Int = 7,
    
    @Column(nullable = false)
    var enabled: Boolean = true,
    
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
