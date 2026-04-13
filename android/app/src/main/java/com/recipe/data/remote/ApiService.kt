package com.recipe.data.remote

import com.recipe.data.model.ApiResponse
import com.recipe.data.model.ExpiryAlert
import com.recipe.data.model.Ingredient
import com.recipe.data.model.PreferenceAnalysis
import com.recipe.data.model.Recipe
import com.recipe.data.model.RecipeComment
import com.recipe.data.model.RecipeDetail
import com.recipe.data.model.RecognizedIngredient
import com.recipe.data.model.ShoppingItem
import com.recipe.data.model.SuggestedPreferencesResponse
import com.recipe.data.model.UserAiProfile
import retrofit2.http.*

interface ApiService {
    companion object {
        //const val BASE_URL = "http://10.0.2.2:8080/api/"  // 模拟器使用
       const val BASE_URL = "http://192.168.185.95:8080/api/"  // 真机使用 - WiFi IP
    }

    // ==================== 用户认证 ====================

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<Map<String, Any>>

    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<Map<String, Any>>

    @GET("users/me")
    suspend fun getUserInfo(): ApiResponse<UserInfo>

    @PUT("users/me")
    suspend fun updateUser(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Any>

    @PUT("users/preferences")
    suspend fun setPreferences(@Body request: Map<String, String>): ApiResponse<String>

    @POST("users/reset-ai-profile")
    suspend fun resetAiProfile(): ApiResponse<String>

    @PUT("users/change-password")
    suspend fun changePassword(@Body request: Map<String, String>): ApiResponse<String>

    // ==================== 食材管理 ====================

    @POST("ingredients/recognize")
    suspend fun recognizeIngredient(@Body request: RecognizeRequest): ApiResponse<List<Ingredient>>

    @GET("ingredients")
    suspend fun getIngredients(): ApiResponse<List<Ingredient>>

    @GET("ingredients/alerts")
    suspend fun getExpiryAlerts(): ApiResponse<List<ExpiryAlert>>

    @GET("ingredients/expiring")
    suspend fun getExpiringIngredients(): ApiResponse<List<Ingredient>>

    @POST("ingredients")
    suspend fun addIngredient(@Body ingredient: Ingredient): ApiResponse<Ingredient>

    @POST("ingredients/batch")
    suspend fun batchAddIngredients(@Body request: Map<String, String>): ApiResponse<List<Ingredient>>

    @PUT("ingredients/{id}")
    suspend fun updateIngredient(
        @Path("id") id: Long,
        @Body ingredient: Ingredient
    ): ApiResponse<Ingredient>

    @DELETE("ingredients/{id}")
    suspend fun deleteIngredient(@Path("id") id: Long): ApiResponse<Any>

    @PUT("ingredients/{id}/consume")
    suspend fun consumeIngredient(@Path("id") id: Long): ApiResponse<Ingredient>

    @GET("ingredients/by-freshness")
    suspend fun getIngredientsByFreshness(): ApiResponse<Map<String, @JvmSuppressWildcards List<Ingredient>>>

    @GET("ingredients/by-category")
    suspend fun getIngredientsByCategory(): ApiResponse<Map<String, @JvmSuppressWildcards List<Ingredient>>>

    // ==================== 食谱社区 ====================

    @POST("recipes")
    suspend fun createRecipe(@Body recipe: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Any>

    @GET("recipes/{id}")
    suspend fun getRecipeDetail(@Path("id") id: Long): ApiResponse<Any>

    @GET("recipes/search")
    suspend fun searchRecipes(
        @Query("keyword") keyword: String? = null,
        @Query("cuisine") cuisine: String? = null,
        @Query("difficulty") difficulty: String? = null
    ): ApiResponse<List<Recipe>>

    @GET("recipes/hot")
    suspend fun getHotRecipes(@Query("limit") limit: Int = 10): ApiResponse<List<Recipe>>

    @GET("recipes/my")
    suspend fun getMyRecipes(): ApiResponse<List<Recipe>>

    @GET("recipes/favorites")
    suspend fun getFavorites(): ApiResponse<List<Recipe>>

    @POST("recipes/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: Long): ApiResponse<Boolean>

    @POST("recipes/{id}/comments")
    suspend fun addComment(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): ApiResponse<Any>

    @DELETE("recipes/comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Long): ApiResponse<Any>

    @GET("recipes/my/comments")
    suspend fun getMyComments(): ApiResponse<List<RecipeComment>>

    @PUT("recipes/{id}")
    suspend fun updateRecipe(
        @Path("id") id: Long,
        @Body recipe: Map<String, @JvmSuppressWildcards Any?>
    ): ApiResponse<Any>

    @DELETE("recipes/{id}")
    suspend fun deleteRecipe(@Path("id") id: Long): ApiResponse<Any>

    @POST("recipes/{id}/optimize")
    suspend fun optimizeRecipe(@Path("id") id: Long): ApiResponse<Any>

    @GET("recipes/{id}/comments")
    suspend fun getRecipeComments(@Path("id") id: Long): ApiResponse<List<RecipeComment>>

    @POST("recipes/{id}/clone")
    suspend fun cloneRecipe(@Path("id") id: Long): ApiResponse<Recipe>

    @POST("recipes/{id}/unpublish")
    suspend fun unpublishRecipe(@Path("id") id: Long): ApiResponse<Recipe>

    @GET("recipes/{id}/author")
    suspend fun getRecipeAuthor(@Path("id") id: Long): ApiResponse<Map<String, @JvmSuppressWildcards Any?>>

    // ==================== 采购清单 ====================

    @POST("shopping")
    suspend fun addShoppingItem(@Body item: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<ShoppingItem>

    @GET("shopping")
    suspend fun getShoppingList(
        @Query("completed") completed: Boolean = false
    ): ApiResponse<List<ShoppingItem>>

    @PUT("shopping/complete")
    suspend fun completeShoppingItems(@Body request: Map<String, @JvmSuppressWildcards List<Long>>): ApiResponse<Int>

    @POST("shopping/sync-to-ingredients")
    suspend fun syncToIngredients(@Body request: Map<String, @JvmSuppressWildcards List<Long>>): ApiResponse<Any>

    @POST("shopping/import-recipe/{recipeId}")
    suspend fun importFromRecipe(@Path("recipeId") recipeId: Long): ApiResponse<List<ShoppingItem>>

    @DELETE("shopping/{id}")
    suspend fun deleteShoppingItem(@Path("id") id: Long): ApiResponse<Any>

    // ==================== 智能推荐 ====================

    @GET("recommend")
    suspend fun getRecommendations(@Query("limit") limit: Int = 20): ApiResponse<Any>

    @GET("recommend/smart-combine")
    suspend fun smartCombine(): ApiResponse<Any>

    // ==================== AI功能 ====================

    @POST("ai/generate-recipe")
    suspend fun generateRecipe(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Any>

    @POST("ai/quick-recipe")
    suspend fun quickRecipe(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Any>

    @POST("ai/chat")
    suspend fun aiChat(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Any>

    @POST("ai/save-recipe")
    suspend fun saveGeneratedRecipe(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Any>

    @POST("ai/add-missing-to-shopping")
    suspend fun addMissingToShopping(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Any>

    @POST("ai/recognize-ingredients")
    suspend fun recognizeIngredients(@Body request: Map<String, String>): ApiResponse<List<RecognizedIngredient>>

    @POST("ai/suggest-by-ingredients")
    suspend fun suggestByIngredients(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Map<String, @JvmSuppressWildcards Any?>>

    @POST("ai/ingredient-info")
    suspend fun inferIngredientInfo(@Body request: Map<String, String>): ApiResponse<IngredientInfoResponse>

    @POST("ai/complete-and-add")
    suspend fun completeAndAddToIngredients(@Body request: CompleteAndAddRequest): ApiResponse<List<Ingredient>>

    @POST("ai/assist-create-recipe")
    suspend fun assistCreateRecipe(@Body request: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Map<String, @JvmSuppressWildcards Any?>>

    // ==================== 用户行为与AI学习 ====================

    @GET("user-behavior/profile")
    suspend fun getAiProfile(): ApiResponse<UserAiProfile>

    @GET("user-behavior/analysis")
    suspend fun getPreferenceAnalysis(): ApiResponse<PreferenceAnalysis>

    @GET("user-behavior/suggested-preferences")
    suspend fun getSuggestedPreferences(): ApiResponse<SuggestedPreferencesResponse>

    @POST("user-behavior/apply-suggested-preferences")
    suspend fun applySuggestedPreferences(): ApiResponse<Map<String, @JvmSuppressWildcards Any?>>
}

// ==================== 请求/响应数据类 ====================

/**
 * AI推断食材信息响应
 */
data class IngredientInfoResponse(
    val shelfLife: Int,
    val storageMethod: String,
    val storageAdvice: String,
    val freshness: String
)

/**
 * 完成并添加到食材库请求
 */
data class CompleteAndAddRequest(
    val itemIds: List<Long>,
    val customInfo: CustomIngredientInfo? = null
)

/**
 * 用户自定义食材信息
 */
data class CustomIngredientInfo(
    val shelfLife: Int,
    val storageMethod: String,
    val storageAdvice: String,
    val freshness: String,
    val actualQuantity: Double? = null,
    val unit: String? = null
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val phone: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val id: Long,
    val username: String,
    val nickname: String?,
    val token: String
)

data class UserInfo(
    val id: Long,
    val username: String,
    val nickname: String?,
    val phone: String?,
    val avatar: String?,
    val preferences: String?,
    val aiProfile: String?,
    val familySize: Int,
    val cookingFrequency: Int,
    val createdAt: String?
)
