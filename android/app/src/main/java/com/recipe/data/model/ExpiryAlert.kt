package com.recipe.data.model

data class ExpiryAlert(
    val ingredientId: Long,
    val ingredientName: String,
    val message: String,
    val daysRemaining: Int,
    val alertLevel: String = "WARNING",
    val quickSolution: String? = null
)
