package com.recipe.data.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Ingredient(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val category: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val freshness: String = "FRESH",
    val purchaseDate: String,
    val expiryDate: String? = null,
    val shelfLife: Int? = null,
    val storageMethod: String? = null,
    val storageAdvice: String? = null,
    val imageUrl: String? = null,
    val notes: String? = null,
    val isConsumed: Boolean = false
) {
    fun getRemainingDays(): Int? {
        if (expiryDate == null) return null
        return try {
            val expiry = parseFlexibleDate(expiryDate)
            ChronoUnit.DAYS.between(LocalDate.now(), expiry).toInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFlexibleDate(dateStr: String): LocalDate {
        // 支持 yyyy-MM-dd 和 yyyy-M-d 等格式
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-M-d"))
        }
    }
    
    fun getPriorityColor(): Color {
        val remaining = getRemainingDays() ?: return Color.Gray
        return when {
            remaining <= 2 -> Color.Red
            remaining in 3..4 -> Color(0xFFFF9800)
            else -> Color.Green
        }
    }
}
