package com.recipe.data.model

/**
 * AI识别的食材结果
 */
data class RecognizedIngredient(
    val name: String,
    val category: String,
    val freshness: String,  // FRESH/WILTING/SPOILING
    val estimatedWeight: String,
    val storageMethod: String,  // ROOM_TEMP/REFRIGERATE/FREEZE/DRY_COOL
    val shelfLife: Int
) {
    /**
     * 获取新鲜度显示文本
     */
    fun getFreshnessDisplay(): String {
        return when (freshness) {
            "FRESH" -> "新鲜"
            "WILTING" -> "微蔫"
            "SPOILING" -> "即将变质"
            else -> "未知"
        }
    }

    /**
     * 获取新鲜度颜色
     */
    fun getFreshnessColor(): androidx.compose.ui.graphics.Color {
        return when (freshness) {
            "FRESH" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
            "WILTING" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
            "SPOILING" -> androidx.compose.ui.graphics.Color(0xFFF44336)
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }

    /**
     * 获取保存方式显示文本
     */
    fun getStorageMethodDisplay(): String {
        return when (storageMethod) {
            "ROOM_TEMP" -> "常温"
            "REFRIGERATE" -> "冷藏"
            "FREEZE" -> "冷冻"
            "DRY_COOL" -> "干燥阴凉"
            else -> "冷藏"
        }
    }

    /**
     * 计算过期日期
     */
    fun getExpiryDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, shelfLife)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(calendar.time)
    }
}

/**
 * 识别请求
 */
data class RecognizeIngredientsRequest(
    val imageBase64: String
)

/**
 * 识别结果包装
 */
data class RecognitionResult(
    val items: List<RecognizedIngredient>
)
