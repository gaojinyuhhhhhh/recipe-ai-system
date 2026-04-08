package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.recipe.data.model.Recipe
import com.recipe.data.model.RecipeComment
import com.recipe.data.model.RecipeDetail
import com.recipe.data.model.RecipeIngredient
import com.recipe.data.model.RecipeStep
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RecipeViewModel : ViewModel() {
    private val api = RetrofitClient.api
    private val gson = Gson()

    // 热门食谱
    private val _hotRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val hotRecipes: StateFlow<List<Recipe>> = _hotRecipes

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<Recipe>>(emptyList())
    val searchResults: StateFlow<List<Recipe>> = _searchResults

    // 我的食谱
    private val _myRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val myRecipes: StateFlow<List<Recipe>> = _myRecipes

    // 我的收藏
    private val _myFavorites = MutableStateFlow<List<Recipe>>(emptyList())
    val myFavorites: StateFlow<List<Recipe>> = _myFavorites

    // 我的评论
    private val _myComments = MutableStateFlow<List<RecipeComment>>(emptyList())
    val myComments: StateFlow<List<RecipeComment>> = _myComments

    // 创建状态
    private val _createSuccess = MutableStateFlow(false)
    val createSuccess: StateFlow<Boolean> = _createSuccess

    // 编辑状态
    private val _editSuccess = MutableStateFlow(false)
    val editSuccess: StateFlow<Boolean> = _editSuccess

    // 食谱详情
    private val _recipeDetail = MutableStateFlow<RecipeDetail?>(null)
    val recipeDetail: StateFlow<RecipeDetail?> = _recipeDetail

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 操作提示
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 是否正在搜索
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    init {
        loadHotRecipes()
    }

    /**
     * 加载热门食谱
     */
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

    /**
     * 搜索食谱
     */
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

    /**
     * 清除搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    /**
     * 加载食谱详情
     */
    fun loadRecipeDetail(recipeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _recipeDetail.value = null
            try {
                val response = api.getRecipeDetail(recipeId)
                if (response.success && response.data != null) {
                    // 后端返回 RecipeDetail 对象，需要从 Map 解析
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

    /**
     * 收藏/取消收藏
     */
    fun toggleFavorite(recipeId: Long) {
        viewModelScope.launch {
            try {
                val response = api.toggleFavorite(recipeId)
                if (response.success) {
                    _toastMessage.value = response.message ?: if (response.data == true) "已收藏" else "已取消收藏"
                    // 刷新详情中的收藏状态
                    _recipeDetail.value?.let { detail ->
                        _recipeDetail.value = detail.copy(isFavorited = response.data ?: false)
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "操作失败"
            }
        }
    }

    /**
     * 添加评论
     */
    fun addComment(recipeId: Long, content: String, rating: Int?) {
        if (content.isBlank()) {
            _toastMessage.value = "请输入评论内容"
            return
        }
        viewModelScope.launch {
            try {
                val request = mutableMapOf<String, Any?>(
                    "content" to content
                )
                if (rating != null) request["rating"] = rating
                val response = api.addComment(recipeId, request)
                if (response.success) {
                    _toastMessage.value = "评论成功"
                    // 刷新详情
                    loadRecipeDetail(recipeId)
                }
            } catch (e: Exception) {
                _toastMessage.value = "评论失败"
            }
        }
    }

    /**
     * 解析食材JSON
     */
    fun parseIngredients(json: String): List<RecipeIngredient> {
        return try {
            val type = object : TypeToken<List<RecipeIngredient>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析步骤JSON
     */
    fun parseSteps(json: String): List<RecipeStep> {
        return try {
            val type = object : TypeToken<List<RecipeStep>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析标签JSON
     */
    fun parseTags(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 创建食谱
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

    fun resetCreateSuccess() {
        _createSuccess.value = false
    }

    fun resetEditSuccess() {
        _editSuccess.value = false
    }

    /**
     * 更新食谱
     */
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

    /**
     * 加载我的食谱
     */
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

    /**
     * 加载我的收藏
     */
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

    /**
     * 加载我的评论
     */
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

    /**
     * 删除评论
     */
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

    /**
     * 删除食谱
     */
    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteRecipe(recipeId)
                if (response.success) {
                    _toastMessage.value = "已删除"
                    // 删除后同步刷新所有相关列表
                    loadHotRecipes()
                    loadMyRecipes()
                    loadMyFavorites()
                    // 清除当前详情
                    _recipeDetail.value = null
                }
            } catch (e: Exception) {
                _toastMessage.value = "删除失败"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * 显示提示消息
     */
    fun showToast(message: String) {
        _toastMessage.value = message
    }
}
