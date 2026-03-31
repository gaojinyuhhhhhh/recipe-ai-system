package com.recipe.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 食材实体类
 * 对应功能: 1.1.2 食材管理模块
 */
@Entity
@Table(name = "ingredients")
data class Ingredient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    var userId: Long,  // 所属用户ID
    
    @Column(nullable = false, length = 100)
    var name: String,  // 食材名称
    
    @Column(length = 50)
    var category: String? = null,  // 类别: 蔬菜类/肉类/调味类/主食类等
    
    @Column
    var quantity: Double? = null,  // 数量/重量
    
    @Column(length = 20)
    var unit: String? = null,  // 单位: kg/g/个/瓶等
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var freshness: Freshness = Freshness.FRESH,  // 新鲜度
    
    @Column(nullable = false)
    var purchaseDate: LocalDate = LocalDate.now(),  // 购买日期
    
    @Column
    var expiryDate: LocalDate? = null,  // 保质期
    
    @Column
    var shelfLife: Int? = null,  // 保质天数(AI计算)
    
    @Enumerated(EnumType.STRING)
    @Column
    var storageMethod: StorageMethod? = null,  // 保存方式
    
    @Column(columnDefinition = "TEXT")
    var storageAdvice: String? = null,  // AI生成的保存建议
    
    @Column(length = 255)
    var imageUrl: String? = null,  // 食材图片
    
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,  // 备注
    
    @Column(nullable = false)
    var isConsumed: Boolean = false,  // 是否已消耗
    
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 计算剩余天数
     */
    fun getRemainingDays(): Int? {
        return expiryDate?.let {
            (it.toEpochDay() - LocalDate.now().toEpochDay()).toInt()
        }
    }
    
    /**
     * 判断优先级
     */
    fun getPriority(): ExpiryPriority {
        val remaining = getRemainingDays() ?: return ExpiryPriority.LOW
        return when {
            remaining <= 2 && freshness == Freshness.WILTING -> ExpiryPriority.HIGH
            remaining in 1..2 -> ExpiryPriority.HIGH
            remaining in 3..4 -> ExpiryPriority.MEDIUM
            remaining in 5..7 && freshness == Freshness.FRESH -> ExpiryPriority.LOW
            remaining in 5..7 -> ExpiryPriority.MEDIUM
            else -> ExpiryPriority.LOW
        }
    }
}

/**
 * 新鲜度枚举
 */
enum class Freshness(val display: String) {
    FRESH("新鲜"),
    WILTING("微蔫"),
    SPOILING("即将变质")
}

/**
 * 保存方式枚举
 */
enum class StorageMethod(val display: String) {
    ROOM_TEMP("常温"),
    REFRIGERATE("冷藏"),
    FREEZE("冷冻"),
    DRY_COOL("干燥阴凉处")
}

/**
 * 过期优先级枚举
 */
enum class ExpiryPriority(val display: String) {
    HIGH("高优先级"),
    MEDIUM("中优先级"),
    LOW("低优先级")
}
