package com.recipe.data.model

/**
 * 采购清单项
 */
data class ShoppingItem(
    val id: Long? = null,
    val userId: Long = 0,
    val name: String = "",
    val category: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val aiAdvice: String? = null,
    val recipeId: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun getCategoryDisplay(): String {
        return category ?: "其他"
    }

    fun getQuantityDisplay(): String {
        if (quantity == null) return ""
        val qStr = if (quantity == quantity.toLong().toDouble()) {
            quantity.toLong().toString()
        } else {
            quantity.toString()
        }
        return "$qStr${unit ?: "g"}"
    }
}
