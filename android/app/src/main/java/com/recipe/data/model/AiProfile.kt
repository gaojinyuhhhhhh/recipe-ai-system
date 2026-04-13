package com.recipe.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户AI画像数据
 */
data class UserAiProfile(
    @SerializedName("cuisinePreferences")
    val cuisinePreferences: Map<String, Int> = emptyMap(),
    
    @SerializedName("tastePreferences")
    val tastePreferences: Map<String, Int> = emptyMap(),
    
    @SerializedName("difficultyPreference")
    val difficultyPreference: Map<String, Int> = emptyMap(),
    
    @SerializedName("nutritionPreferences")
    val nutritionPreferences: Map<String, Int> = emptyMap(),
    
    @SerializedName("cookingTimePreference")
    val cookingTimePreference: CookingTimePreference = CookingTimePreference(),
    
    @SerializedName("dislikedIngredients")
    val dislikedIngredients: List<String> = emptyList(),
    
    @SerializedName("totalInteractions")
    val totalInteractions: Int = 0,
    
    @SerializedName("lastUpdated")
    val lastUpdated: String? = null
)

data class CookingTimePreference(
    @SerializedName("average")
    val average: Double = 0.0,
    
    @SerializedName("count")
    val count: Int = 0
)

/**
 * 偏好分析结果
 */
data class PreferenceAnalysis(
    @SerializedName("topCuisines")
    val topCuisines: List<PreferenceItem> = emptyList(),
    
    @SerializedName("topTastes")
    val topTastes: List<PreferenceItem> = emptyList(),
    
    @SerializedName("topNutrition")
    val topNutrition: List<PreferenceItem> = emptyList(),
    
    @SerializedName("recommendedDifficulty")
    val recommendedDifficulty: String = "MEDIUM",
    
    @SerializedName("recommendedCookingTime")
    val recommendedCookingTime: Int = 30,
    
    @SerializedName("dislikedIngredients")
    val dislikedIngredients: List<String> = emptyList(),
    
    @SerializedName("totalInteractions")
    val totalInteractions: Int = 0,
    
    @SerializedName("confidenceScore")
    val confidenceScore: Double = 0.0
)

data class PreferenceItem(
    @SerializedName("first")
    val name: String = "",
    
    @SerializedName("second")
    val score: Int = 0
)

/**
 * AI推荐的偏好设置
 */
data class SuggestedPreferencesResponse(
    @SerializedName("preferences")
    val preferences: UserPreferences? = null,
    
    @SerializedName("message")
    val message: String = "",
    
    @SerializedName("confidence")
    val confidence: Double = 0.0,
    
    @SerializedName("minRequired")
    val minRequired: Int? = null
)
