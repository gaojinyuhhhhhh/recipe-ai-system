package com.recipe.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 采购清单实体类
 * 对应功能: 1.1.3 采购管理模块
 */
@Entity
@Table(name = "shopping_items")
data class ShoppingItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    var userId: Long,  // 所属用户ID
    
    @Column(nullable = false, length = 100)
    var name: String,  // 食材名称
    
    @Column(length = 50)
    var category: String? = null,  // 类别
    
    @Column
    var quantity: Double? = null,  // 推荐采购量
    
    @Column(length = 20)
    var unit: String? = null,  // 单位
    
    @Column(columnDefinition = "TEXT")
    var aiAdvice: String? = null,  // AI采购建议(如"建议少量购买")
    
    @Column
    var recipeId: Long? = null,  // 关联的食谱ID(如果是从食谱导入)
    
    @Column(nullable = false)
    var isCompleted: Boolean = false,  // 是否已完成
    
    @Column
    var completedAt: LocalDateTime? = null,  // 完成时间
    
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
