package com.recipe.data.model

/**
 * 用户偏好设置 - 扩展版
 */
data class UserPreferences(
    // 基础偏好
    val cuisines: List<String> = emptyList(),        // 喜欢的菜系 [川菜, 粤菜, 西餐...]
    val tastes: List<String> = emptyList(),          // 口味偏好 [清淡, 微辣, 酸甜...]
    val diet: List<String> = emptyList(),            // 饮食限制 [素食, 清真, 无麸质, 低糖...]

    // 烹饪场景
    val difficulty: String = "MEDIUM",               // 难度偏好 EASY/MEDIUM/HARD
    val maxCookingTime: Int = 60,                    // 最大烹饪时长(分钟)
    val cookingScene: String? = null,                // 烹饪场景 [快手简餐, 周末大餐, 便当带饭, 宴客菜]

    // 营养目标
    val nutritionGoals: List<String> = emptyList(),  // 营养目标 [减脂, 增肌, 控糖, 高蛋白, 均衡]

    // 设备与条件
    val cookingEquipment: List<String> = emptyList(), // 烹饪设备 [燃气灶, 电磁炉, 烤箱, 空气炸锅]
    val dislikedIngredients: List<String> = emptyList(), // 忌口食材 [海鲜, 牛羊肉, 鸡蛋...]

    // 家庭信息
    val familySize: Int = 2,                         // 家庭人数
    val cookingFrequency: Int = 7                    // 烹饪频率(次/周)
) {
    companion object {
        // 选项定义
        val CUISINE_OPTIONS = listOf("川菜", "粤菜", "湘菜", "鲁菜", "苏菜", "浙菜", "闽菜", "徽菜", "西餐", "日料", "韩餐", "东南亚菜")
        val TASTE_OPTIONS = listOf("清淡", "微辣", "中辣", "重辣", "酸甜", "咸鲜", "香辣", "酱香")
        val DIET_OPTIONS = listOf("素食", "清真", "无麸质", "低糖", "低脂", "低盐", "无海鲜", "无牛羊肉")
        val DIFFICULTY_OPTIONS = listOf("EASY" to "简单", "MEDIUM" to "中等", "HARD" to "困难")
        val COOKING_SCENE_OPTIONS = listOf("快手简餐", "周末大餐", "便当带饭", "宴客菜", "儿童餐", "老人餐")
        val NUTRITION_GOAL_OPTIONS = listOf("减脂", "增肌", "控糖", "高蛋白", "均衡饮食", "低卡路里")
        val EQUIPMENT_OPTIONS = listOf("燃气灶", "电磁炉", "烤箱", "空气炸锅", "电饭煲", "微波炉", "蒸锅")
        val DISLIKE_OPTIONS = listOf("海鲜", "牛羊肉", "猪肉", "鸡肉", "鸡蛋", "牛奶", "花生", "辣椒", "葱姜蒜", "香菜")
        val TIME_OPTIONS = listOf(15, 30, 45, 60, 90, 120)
    }

    /**
     * 转换为后端需要的 Map 格式
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "cuisines" to cuisines,
            "tastes" to tastes,
            "diet" to diet,
            "difficulty" to difficulty,
            "maxCookingTime" to maxCookingTime,
            "cookingScene" to cookingScene,
            "nutritionGoals" to nutritionGoals,
            "cookingEquipment" to cookingEquipment,
            "dislikedIngredients" to dislikedIngredients,
            "familySize" to familySize,
            "cookingFrequency" to cookingFrequency
        )
    }
}
