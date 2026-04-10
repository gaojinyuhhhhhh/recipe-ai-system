package com.recipe.data.local

import androidx.room.*

/**
 * 本地食谱实体 - Room数据库
 * 存储用户的本地食谱，支持上传到社区
 */
@Entity(tableName = "local_recipes")
data class LocalRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 云端食谱ID（上传后回填，未上传时为null） */
    val serverId: Long? = null,

    /** 用户ID */
    val userId: Long = 0,

    /** 食谱标题 */
    val title: String = "",

    /** 食谱描述 */
    val description: String? = null,

    /** 封面图 */
    val coverImage: String? = null,

    /** 食材清单 JSON: [{"name":"鸡蛋","quantity":2,"unit":"个"}] */
    val ingredients: String = "[]",

    /** 烹饪步骤 JSON: [{"step":1,"content":"打散鸡蛋","duration":60}] */
    val steps: String = "[]",

    /** 烹饪时长(分钟) */
    val cookingTime: Int? = null,

    /** 难度: EASY/MEDIUM/HARD */
    val difficulty: String = "MEDIUM",

    /** 菜系 */
    val cuisine: String? = null,

    /** 标签 JSON: ["减脂","低糖"] */
    val tags: String? = null,

    /** 同步状态: LOCAL=仅本地, UPLOADED=已上传, DOWNLOADED=从社区下载 */
    val syncStatus: String = "LOCAL",

    /** 原始社区食谱作者名（从社区下载时记录） */
    val originalAuthor: String? = null,

    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),

    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
)
