// Ingredient.kt
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
        val expiry = LocalDate.parse(expiryDate)
        return ChronoUnit.DAYS.between(LocalDate.now(), expiry).toInt()
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
