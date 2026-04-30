package com.recipe.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 本地食谱DAO - Room数据访问
 */
@Dao
interface LocalRecipeDao {

    /** 查询当前用户的所有本地食谱(按更新时间倒序) */
    @Query("SELECT * FROM local_recipes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getRecipesByUser(userId: Long): Flow<List<LocalRecipeEntity>>

    /** 查询单个食谱 */
    @Query("SELECT * FROM local_recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): LocalRecipeEntity?

    /** 通过serverId查询（判断是否已下载） */
    @Query("SELECT * FROM local_recipes WHERE serverId = :serverId AND userId = :userId LIMIT 1")
    suspend fun getByServerId(serverId: Long, userId: Long): LocalRecipeEntity?

    /** 通过标题和用户ID查询（判断是否已导入） */
    @Query("SELECT * FROM local_recipes WHERE userId = :userId AND title = :title LIMIT 1")
    suspend fun getByTitle(userId: Long, title: String): LocalRecipeEntity?

    /** 通过标题和用户ID查询已上传的食谱 */
    @Query("SELECT * FROM local_recipes WHERE userId = :userId AND title = :title AND syncStatus = 'UPLOADED' LIMIT 1")
    suspend fun getUploadedByTitle(userId: Long, title: String): LocalRecipeEntity?

    /** 插入食谱 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: LocalRecipeEntity): Long

    /** 更新食谱 */
    @Update
    suspend fun update(recipe: LocalRecipeEntity)

    /** 删除食谱 */
    @Delete
    suspend fun delete(recipe: LocalRecipeEntity)

    /** 根据ID删除 */
    @Query("DELETE FROM local_recipes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 更新同步状态和serverId */
    @Query("UPDATE local_recipes SET syncStatus = :status, serverId = :serverId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, serverId: Long?, updatedAt: Long = System.currentTimeMillis())

    /** 搜索本地食谱 */
    @Query("SELECT * FROM local_recipes WHERE userId = :userId AND (title LIKE '%' || :keyword || '%' OR tags LIKE '%' || :keyword || '%') ORDER BY updatedAt DESC")
    suspend fun searchRecipes(userId: Long, keyword: String): List<LocalRecipeEntity>

    /** 获取本地食谱数量 */
    @Query("SELECT COUNT(*) FROM local_recipes WHERE userId = :userId")
    suspend fun getRecipeCount(userId: Long): Int

    /** 获取所有未同步的本地食谱（syncStatus=LOCAL且无serverId） */
    @Query("SELECT * FROM local_recipes WHERE userId = :userId AND serverId IS NULL AND syncStatus = 'LOCAL'")
    suspend fun getUnsyncedRecipes(userId: Long): List<LocalRecipeEntity>

    /** 获取所有已有serverId的食谱（用于同步时判重） */
    @Query("SELECT serverId FROM local_recipes WHERE userId = :userId AND serverId IS NOT NULL")
    suspend fun getAllServerIds(userId: Long): List<Long>

    /** 批量插入食谱 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(recipes: List<LocalRecipeEntity>)
}
