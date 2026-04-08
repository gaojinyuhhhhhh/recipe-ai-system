package com.recipe.data.model

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val timestamp: Long
)