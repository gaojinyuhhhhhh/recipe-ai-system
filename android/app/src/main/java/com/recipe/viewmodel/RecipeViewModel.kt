package com.recipe.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.recipe.data.local.AppDatabase
import com.recipe.data.local.LocalRecipeEntity
import com.recipe.data.local.TokenManager
import com.recipe.data.model.Recipe
import com.recipe.data.model.RecipeComment
import com.recipe.data.model.RecipeDetail
import com.recipe.data.model.RecipeIngredient
import com.recipe.data.model.RecipeStep
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 食谱模块核心ViewModel
 *
 * 职责范围：
 * 1. 社区食谱 — 热门浏览、搜索、详情、收藏、评论、创建/编辑/删除
 * 2. 本地食谱 — CRUD、上传到社区、从社区下载到本地、下架
 * 3. AI辅助 — 根据菜名生成完整食谱信息
 *
 * 数据流：
 * - 社区数据通过 [api]（Retrofit）请求后端 REST 接口
 * - 本地数据通过 [dao]（Room）持久化到 SQLite
 * - 上传/下载操作会同时更新两端状态
 *
 * syncStatus 状态机：
 *   LOCAL → UPLOADED（上传成功后回填 serverId）
 *   DOWNLOADED（从社区下载到本地时创建）
 */
class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val api = RetrofitClient.api
    private val gson = Gson()
    private val dao = AppDatabase.getInstance(application).localRecipeDao()

    // ==================== 社区食谱状态 ====================

    // 热门食谱
    private val _hotRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val hotRecipes: StateFlow<List<Recipe>> = _hotRecipes

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<Recipe>>(emptyList())
    val searchResults: StateFlow<List<Recipe>> = _searchResults

    // 我的云端食谱(个人中心用)
    private val _myRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val myRecipes: StateFlow<List<Recipe>> = _myRecipes

    // 我的收藏
    private val _myFavorites = MutableStateFlow<List<Recipe>>(emptyList())
    val myFavorites: StateFlow<List<Recipe>> = _myFavorites

    // 我的评论
    private val _myComments = MutableStateFlow<List<RecipeComment>>(emptyList())
    val myComments: StateFlow<List<RecipeComment>> = _myComments

    // 食谱详情
    private val _recipeDetail = MutableStateFlow<RecipeDetail?>(null)
    val recipeDetail: StateFlow<RecipeDetail?> = _recipeDetail

    // 食谱评论列表
    private val _recipeComments = MutableStateFlow<List<RecipeComment>>(emptyList())
    val recipeComments: StateFlow<List<RecipeComment>> = _recipeComments

    // ==================== 本地食谱状态 ====================

    private val _localRecipes = MutableStateFlow<List<LocalRecipeEntity>>(emptyList())
    val localRecipes: StateFlow<List<LocalRecipeEntity>> = _localRecipes

    private val _currentLocalRecipe = MutableStateFlow<LocalRecipeEntity?>(null)
    val currentLocalRecipe: StateFlow<LocalRecipeEntity?> = _currentLocalRecipe

    // ==================== 通用状态 ====================

    private val _createSuccess = MutableStateFlow(false)
    val createSuccess: StateFlow<Boolean> = _createSuccess

    private val _editSuccess = MutableStateFlow(false)
    val editSuccess: StateFlow<Boolean> = _editSuccess

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // 下拉刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        loadHotRecipes()
        loadLocalRecipes()
        // 启动时自动同步本地食谱（跨设备同步）
        syncLocalRecipes()
    }

    // ==================== 本地食谱操作 ====================

    /** 加载本地食谱列表 */
    fun loadLocalRecipes() {
        val userId = TokenManager.getUserId()
        if (userId == 0L) return
        viewModelScope.launch {
            dao.getRecipesByUser(userId).collectLatest { recipes ->
                _localRecipes.value = recipes
            }
        }
    }

    /** 刷新本地食谱（下拉刷新用） */
    fun refreshLocalRecipes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 先执行云端同步，再重新加载本地食谱
                syncLocalRecipes()
                loadLocalRecipes()
                // 同时刷新我的云端食谱（用于同步状态）
                loadMyRecipes()
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "刷新失败: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 本地食谱云端同步（跨设备同步核心逻辑）
     * 策略：
     * 1. 将本地未同步的食谱上传到云端，回填 serverId
     * 2. 从云端拉取用户的私有食谱，将本地不存在的插入到 Room
     */
    fun syncLocalRecipes() {
        val userId = TokenManager.getUserId()
        if (userId == 0L) return
        viewModelScope.launch {
            try {
                // Step 1: 上传本地未同步的食谱到云端
                val unsyncedRecipes = dao.getUnsyncedRecipes(userId)
                if (unsyncedRecipes.isNotEmpty()) {
                    val uploadList = unsyncedRecipes.map { recipe ->
                        mapOf<String, Any?>(
                            "title" to recipe.title,
                            "description" to recipe.description,
                            "coverImage" to recipe.coverImage,
                            "ingredients" to recipe.ingredients,
                            "steps" to recipe.steps,
                            "cookingTime" to recipe.cookingTime,
                            "difficulty" to recipe.difficulty,
                            "cuisine" to recipe.cuisine,
                            "tags" to recipe.tags,
                            "isAiGenerated" to false
                        )
                    }
                    val uploadResponse = api.uploadLocalRecipes(uploadList)
                    if (uploadResponse.success && uploadResponse.data != null) {
                        // 回填 serverId 到本地记录
                        val serverRecipes = uploadResponse.data
                        for (i in unsyncedRecipes.indices) {
                            if (i < serverRecipes.size) {
                                val serverId = serverRecipes[i].id
                                if (serverId != null) {
                                    dao.updateSyncStatus(
                                        unsyncedRecipes[i].id,
                                        "SYNCED",
                                        serverId
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 2: 从云端拉取用户的私有食谱，合并到本地
                val syncResponse = api.getLocalSyncRecipes()
                if (syncResponse.success && syncResponse.data != null) {
                    val existingServerIds = dao.getAllServerIds(userId).toSet()
                    val newRecipes = syncResponse.data
                        .filter { it.id != null && it.id !in existingServerIds }
                        .map { recipe ->
                            LocalRecipeEntity(
                                serverId = recipe.id,
                                userId = userId,
                                title = recipe.title,
                                description = recipe.description,
                                coverImage = recipe.coverImage,
                                ingredients = recipe.ingredients,
                                steps = recipe.steps,
                                cookingTime = recipe.cookingTime,
                                difficulty = recipe.difficulty,
                                cuisine = recipe.cuisine,
                                tags = recipe.tags,
                                syncStatus = "SYNCED"
                            )
                        }
                    if (newRecipes.isNotEmpty()) {
                        dao.insertAll(newRecipes)
                        Log.d("RecipeViewModel", "云端同步: 新增 ${newRecipes.size} 个食谱")
                    }
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "本地食谱同步失败: ${e.message}")
            }
        }
    }

    /** 创建本地食谱 */
    fun createLocalRecipe(
        title: String,
        description: String?,
        ingredientsList: List<Map<String, Any?>>,
        stepsList: List<Map<String, Any?>>,
        cookingTime: Int?,
        difficulty: String,
        cuisine: String?,
        tags: List<String>
    ) {
        if (title.isBlank()) {
            _toastMessage.value = "请输入食谱标题"
            return
        }
        if (ingredientsList.isEmpty()) {
            _toastMessage.value = "请至少添加一种食材"
            return
        }
        if (stepsList.isEmpty()) {
            _toastMessage.value = "请至少添加一个步骤"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entity = LocalRecipeEntity(
                    userId = TokenManager.getUserId(),
                    title = title,
                    description = description,
                    ingredients = gson.toJson(ingredientsList),
                    steps = gson.toJson(stepsList),
                    cookingTime = cookingTime,
                    difficulty = difficulty,
                    cuisine = cuisine,
                    tags = if (tags.isNotEmpty()) gson.toJson(tags) else null,
                    syncStatus = "LOCAL"
                )
                dao.insert(entity)
                _toastMessage.value = "食谱已保存到本地"
                _createSuccess.value = true
            } catch (e: Exception) {
                _toastMessage.value = "保存失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 更新本地食谱 */
    fun updateLocalRecipe(
        localId: Long,
        title: String,
        description: String?,
        ingredientsList: List<Map<String, Any?>>,
        stepsList: List<Map<String, Any?>>,
        cookingTime: Int?,
        difficulty: String,
        cuisine: String?,
        tags: List<String>
    ) {
        if (title.isBlank()) {
            _toastMessage.value = "请输入食谱标题"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existing = dao.getRecipeById(localId)
                if (existing != null) {
                    val updated = existing.copy(
                        title = title,
                        description = description,
                        ingredients = gson.toJson(ingredientsList),
                        steps = gson.toJson(stepsList),
                        cookingTime = cookingTime,
                        difficulty = difficulty,
                        cuisine = cuisine,
                        tags = if (tags.isNotEmpty()) gson.toJson(tags) else null,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.update(updated)
                    _toastMessage.value = "食谱已更新"
                    _editSuccess.value = true
                }
            } catch (e: Exception) {
                _toastMessage.value = "更新失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 删除本地食谱 */
    fun deleteLocalRecipe(localId: Long) {
        viewModelScope.launch {
            try {
                // 先查询本地食谱，判断是否有云端同步记录
                val localRecipe = dao.getRecipeById(localId)
                val serverId = localRecipe?.serverId

                // 删除本地记录
                dao.deleteById(localId)
                _currentLocalRecipe.value = null

                // 如果有 serverId，说明已同步到云端，需要同时删除云端副本
                // 否则下次登录同步时云端食谱会被重新拉取回来
                if (serverId != null) {
                    try {
                        api.deleteRecipe(serverId)
                        Log.d("RecipeViewModel", "云端同步食谱已删除: serverId=$serverId")
                    } catch (e: Exception) {
                        Log.w("RecipeViewModel", "云端删除失败(不影响本地删除): ${e.message}")
                    }
                }

                _toastMessage.value = "已删除"
            } catch (e: Exception) {
                _toastMessage.value = "删除失败"
            }
        }
    }

    /**
     * 下架食谱（从社区移除）
     * @param serverId 云端食谱ID
     * @param localId 本地食谱ID
     */
    fun unpublishRecipe(serverId: Long, localId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.unpublishRecipe(serverId)
                if (response.success) {
                    // 更新本地同步状态为LOCAL（未发布）
                    dao.updateSyncStatus(localId, "LOCAL", null)
                    _toastMessage.value = "食谱已从社区下架"
                    // 刷新本地详情
                    _currentLocalRecipe.value = dao.getRecipeById(localId)
                } else {
                    _toastMessage.value = response.message ?: "下架失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "下架失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 加载本地食谱详情 */
    fun loadLocalRecipeDetail(localId: Long) {
        viewModelScope.launch {
            _currentLocalRecipe.value = dao.getRecipeById(localId)
        }
    }

    /** 上传本地食谱到社区 */
    fun uploadToCommuity(localId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val local = dao.getRecipeById(localId) ?: throw Exception("食谱不存在")
                val request = mutableMapOf<String, Any?>(
                    "title" to local.title,
                    "description" to local.description,
                    "ingredients" to local.ingredients,
                    "steps" to local.steps,
                    "difficulty" to local.difficulty,
                    "isPublic" to true
                )
                if (local.cookingTime != null) request["cookingTime"] = local.cookingTime
                if (!local.cuisine.isNullOrBlank()) request["cuisine"] = local.cuisine
                if (!local.tags.isNullOrBlank()) request["tags"] = local.tags

                val response = api.createRecipe(request)
                if (response.success) {
                    // 上传成功后从响应中提取云端ID，回填到本地记录
                    // Gson将数字默认反序列化为Double，需要转Long
                    val serverData = response.data
                    val serverId = try {
                        val json = gson.toJson(serverData)
                        val map = gson.fromJson<Map<String, Any>>(json, object : TypeToken<Map<String, Any>>() {}.type)
                        (map["id"] as? Double)?.toLong()
                    } catch (e: Exception) { null }

                    dao.updateSyncStatus(localId, "UPLOADED", serverId)
                    _toastMessage.value = "已成功发布到食谱社区！"
                    // 刷新本地详情
                    _currentLocalRecipe.value = dao.getRecipeById(localId)
                    loadHotRecipes()
                } else {
                    _toastMessage.value = response.message ?: "上传失败"
                }
            } catch (e: HttpException) {
                _toastMessage.value = "上传失败: 网络错误"
            } catch (e: Exception) {
                _toastMessage.value = "上传失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 从社区下载食谱到本地 */
    fun downloadToLocal(recipeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = TokenManager.getUserId()
                // 检查是否已下载
                val existing = dao.getByServerId(recipeId, userId)
                if (existing != null) {
                    _toastMessage.value = "该食谱已在本地食谱中"
                    _isLoading.value = false
                    return@launch
                }

                // 获取食谱详情
                val detailResponse = api.getRecipeDetail(recipeId)
                if (!detailResponse.success || detailResponse.data == null) {
                    _toastMessage.value = "获取食谱信息失败"
                    _isLoading.value = false
                    return@launch
                }
                val json = gson.toJson(detailResponse.data)
                val detail = gson.fromJson(json, RecipeDetail::class.java)
                val recipe = detail.recipe

                // 获取社区食谱的原始作者名，下载到本地后用于展示来源
                var authorName: String? = null
                try {
                    val authorResponse = api.getRecipeAuthor(recipeId)
                    if (authorResponse.success && authorResponse.data != null) {
                        authorName = authorResponse.data["nickname"] as? String
                    }
                } catch (_: Exception) {}

                // 保存到本地
                val localRecipe = LocalRecipeEntity(
                    serverId = recipe.id,
                    userId = userId,
                    title = recipe.title,
                    description = recipe.description,
                    coverImage = recipe.coverImage,
                    ingredients = recipe.ingredients,
                    steps = recipe.steps,
                    cookingTime = recipe.cookingTime,
                    difficulty = recipe.difficulty,
                    cuisine = recipe.cuisine,
                    tags = recipe.tags,
                    syncStatus = "DOWNLOADED",
                    originalAuthor = authorName
                )
                dao.insert(localRecipe)
                _toastMessage.value = "已下载到本地食谱"
            } catch (e: Exception) {
                _toastMessage.value = "下载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== 社区食谱操作 ====================

    /** 加载社区热门食谱（按热度排序，最多20条） */
    fun loadHotRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getHotRecipes(20)
                if (response.success && response.data != null) {
                    _hotRecipes.value = response.data
                }
            } catch (e: HttpException) {
                Log.e("RecipeVM", "Load hot recipes failed: ${e.code()}")
            } catch (e: Exception) {
                Log.e("RecipeVM", "Load hot recipes error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 搜索社区食谱，支持关键词、菜系、难度多维度筛选 */
    fun searchRecipes(keyword: String, cuisine: String? = null, difficulty: String? = null) {
        _searchQuery.value = keyword
        if (keyword.isBlank() && cuisine == null && difficulty == null) {
            _isSearching.value = false
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.searchRecipes(
                    keyword = keyword.takeIf { it.isNotBlank() },
                    cuisine = cuisine,
                    difficulty = difficulty
                )
                if (response.success && response.data != null) {
                    _searchResults.value = response.data
                }
            } catch (e: HttpException) {
                _errorMessage.value = "搜索失败"
            } catch (e: Exception) {
                _errorMessage.value = "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    fun loadRecipeDetail(recipeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _recipeDetail.value = null
            try {
                val response = api.getRecipeDetail(recipeId)
                if (response.success && response.data != null) {
                    val json = gson.toJson(response.data)
                    val detail = gson.fromJson(json, RecipeDetail::class.java)
                    _recipeDetail.value = detail
                }
            } catch (e: Exception) {
                Log.e("RecipeVM", "Load recipe detail error", e)
                _errorMessage.value = "加载详情失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 加载食谱评论列表 */
    fun loadRecipeComments(recipeId: Long) {
        viewModelScope.launch {
            try {
                val response = api.getRecipeComments(recipeId)
                if (response.success && response.data != null) {
                    _recipeComments.value = response.data
                }
            } catch (e: Exception) {
                Log.e("RecipeVM", "Load comments error", e)
            }
        }
    }

    /** 切换收藏状态（收藏/取消收藏），并同步更新详情页的收藏标志 */
    fun toggleFavorite(recipeId: Long) {
        viewModelScope.launch {
            try {
                val response = api.toggleFavorite(recipeId)
                if (response.success) {
                    _toastMessage.value = response.message ?: if (response.data == true) "已收藏" else "已取消收藏"
                    _recipeDetail.value?.let { detail ->
                        _recipeDetail.value = detail.copy(isFavorited = response.data ?: false)
                    }
                } else {
                    _toastMessage.value = response.message ?: "操作失败"
                }
            } catch (e: HttpException) {
                val errorMsg = when (e.code()) {
                    401 -> "登录已过期，请重新登录"
                    403 -> "没有权限执行此操作"
                    else -> "网络错误(${e.code()})"
                }
                android.util.Log.e("RecipeViewModel", "toggleFavorite HTTP error: ${e.code()}", e)
                _toastMessage.value = errorMsg
            } catch (e: Exception) {
                android.util.Log.e("RecipeViewModel", "toggleFavorite error", e)
                _toastMessage.value = "操作失败: ${e.message}"
            }
        }
    }

    /** 发表评论，成功后自动刷新详情和评论列表 */
    fun addComment(recipeId: Long, content: String, rating: Int?) {
        if (content.isBlank()) {
            _toastMessage.value = "请输入评论内容"
            return
        }
        viewModelScope.launch {
            try {
                val request = mutableMapOf<String, Any?>("content" to content)
                if (rating != null) request["rating"] = rating
                val response = api.addComment(recipeId, request)
                if (response.success) {
                    _toastMessage.value = "评论成功"
                    loadRecipeDetail(recipeId)
                    loadRecipeComments(recipeId)
                }
            } catch (e: Exception) {
                _toastMessage.value = "评论失败"
            }
        }
    }

    /**
     * 创建社区食谱（直接发布到云端）
     * 注意：ingredients/steps/tags 需要序列化为 JSON 字符串后传给后端
     */
    fun createRecipe(
        title: String,
        description: String?,
        ingredientsList: List<Map<String, Any?>>,
        stepsList: List<Map<String, Any?>>,
        cookingTime: Int?,
        difficulty: String,
        cuisine: String?,
        tags: List<String>
    ) {
        if (title.isBlank()) {
            _toastMessage.value = "请输入食谱标题"
            return
        }
        if (ingredientsList.isEmpty()) {
            _toastMessage.value = "请至少添加一种食材"
            return
        }
        if (stepsList.isEmpty()) {
            _toastMessage.value = "请至少添加一个步骤"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = mutableMapOf<String, Any?>(
                    "title" to title,
                    "description" to description,
                    "ingredients" to gson.toJson(ingredientsList),
                    "steps" to gson.toJson(stepsList),
                    "difficulty" to difficulty,
                    "isPublic" to true
                )
                if (cookingTime != null) request["cookingTime"] = cookingTime
                if (!cuisine.isNullOrBlank()) request["cuisine"] = cuisine
                if (tags.isNotEmpty()) request["tags"] = gson.toJson(tags)

                val response = api.createRecipe(request)
                if (response.success) {
                    _toastMessage.value = response.message ?: "创建成功"
                    _createSuccess.value = true
                    loadHotRecipes()
                } else {
                    _toastMessage.value = response.message ?: "创建失败"
                }
            } catch (e: HttpException) {
                _toastMessage.value = "创建失败"
            } catch (e: Exception) {
                _toastMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetCreateSuccess() { _createSuccess.value = false }
    fun resetEditSuccess() { _editSuccess.value = false }

    /** 编辑云端社区食谱 */
    fun updateRecipe(
        recipeId: Long,
        title: String,
        description: String?,
        ingredientsList: List<Map<String, Any?>>,
        stepsList: List<Map<String, Any?>>,
        cookingTime: Int?,
        difficulty: String,
        cuisine: String?,
        tags: List<String>
    ) {
        if (title.isBlank()) {
            _toastMessage.value = "请输入食谱标题"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = mutableMapOf<String, Any?>(
                    "title" to title,
                    "description" to description,
                    "ingredients" to gson.toJson(ingredientsList),
                    "steps" to gson.toJson(stepsList),
                    "difficulty" to difficulty,
                    "isPublic" to true
                )
                if (cookingTime != null) request["cookingTime"] = cookingTime
                if (!cuisine.isNullOrBlank()) request["cuisine"] = cuisine
                if (tags.isNotEmpty()) request["tags"] = gson.toJson(tags)

                val response = api.updateRecipe(recipeId, request)
                if (response.success) {
                    _toastMessage.value = response.message ?: "更新成功"
                    _editSuccess.value = true
                } else {
                    _toastMessage.value = response.message ?: "更新失败"
                }
            } catch (e: HttpException) {
                _toastMessage.value = "更新失败"
            } catch (e: Exception) {
                _toastMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 加载当前用户发布的云端食谱（个人中心 - 我的食谱） */
    fun loadMyRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getMyRecipes()
                if (response.success && response.data != null) {
                    _myRecipes.value = response.data
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 加载当前用户的收藏列表 */
    fun loadMyFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getFavorites()
                if (response.success && response.data != null) {
                    _myFavorites.value = response.data
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 加载当前用户的评论列表 */
    fun loadMyComments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getMyComments()
                if (response.success && response.data != null) {
                    _myComments.value = response.data
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteComment(commentId)
                if (response.success) {
                    _toastMessage.value = "已删除"
                    loadMyComments()
                }
            } catch (e: Exception) {
                _toastMessage.value = "删除失败"
            }
        }
    }

    /** 删除云端食谱，成功后刷新热门、我的食谱、收藏列表 */
    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteRecipe(recipeId)
                if (response.success) {
                    _toastMessage.value = "已删除"
                    loadHotRecipes()
                    loadMyRecipes()
                    loadMyFavorites()
                    _recipeDetail.value = null
                }
            } catch (e: Exception) {
                _toastMessage.value = "删除失败"
            }
        }
    }

    // ==================== 工具方法 ====================

    /** 将 JSON 字符串反序列化为食材列表（用于UI展示） */
    fun parseIngredients(json: String): List<RecipeIngredient> {
        return try {
            val type = object : TypeToken<List<RecipeIngredient>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { emptyList() }
    }

    /** 将 JSON 字符串反序列化为步骤列表（用于UI展示） */
    fun parseSteps(json: String): List<RecipeStep> {
        return try {
            val type = object : TypeToken<List<RecipeStep>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { emptyList() }
    }

    fun parseTags(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { emptyList() }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearToast() { _toastMessage.value = null }
    fun showToast(message: String) { _toastMessage.value = message }

    // ==================== AI辅助创建食谱 ====================

    /**
     * AI辅助创建食谱 - 根据菜名生成完整食谱信息
     */
    suspend fun assistCreateRecipe(title: String): com.recipe.ui.recipe.AiGeneratedRecipe? {
        _isLoading.value = true
        return try {
            val response = api.assistCreateRecipe(
                mapOf("title" to title, "servings" to 2)
            )
            if (response.success && response.data != null) {
                parseAiGeneratedRecipe(response.data)
            } else {
                _toastMessage.value = response.message ?: "生成失败"
                null
            }
        } catch (e: Exception) {
            _toastMessage.value = "AI生成失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }

    private fun parseAiGeneratedRecipe(data: Map<String, *>): com.recipe.ui.recipe.AiGeneratedRecipe {
        val ingredientsList = (data["ingredients"] as? List<*>)?.mapNotNull { ing ->
            (ing as? Map<*, *>)?.let {
                com.recipe.ui.recipe.AiIngredient(
                    name = it["name"] as? String ?: "",
                    quantity = (it["quantity"] as? Number)?.toDouble(),
                    unit = it["unit"] as? String,
                    notes = it["notes"] as? String
                )
            }
        } ?: emptyList()

        val stepsList = (data["steps"] as? List<*>)?.mapNotNull { step ->
            (step as? Map<*, *>)?.let {
                com.recipe.ui.recipe.AiStep(
                    step = (it["step"] as? Number)?.toInt() ?: 0,
                    content = it["content"] as? String ?: "",
                    duration = (it["duration"] as? Number)?.toInt(),
                    temperature = it["temperature"] as? String,
                    tips = it["tips"] as? String
                )
            }
        } ?: emptyList()

        val difficulty = data["difficulty"] as? String ?: "MEDIUM"
        val difficultyDisplay = when (difficulty) {
            "EASY" -> "简单"
            "MEDIUM" -> "中等"
            "HARD" -> "困难"
            else -> difficulty
        }

        return com.recipe.ui.recipe.AiGeneratedRecipe(
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            ingredients = ingredientsList,
            steps = stepsList,
            cookingTime = (data["cookingTime"] as? Number)?.toInt() ?: 30,
            difficulty = difficulty,
            difficultyDisplay = difficultyDisplay,
            cuisine = data["cuisine"] as? String ?: "",
            tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )
    }
}
