package com.recipe.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==================== 色彩系统 (清新绿色风格) ====================

// 主色调 - 清新绿色
val PrimaryGreen = Color(0xFF34C759)      // iOS绿，清新自然
val PrimaryGreenLight = Color(0xFF7FD67F) // 浅绿
val PrimaryGreenDark = Color(0xFF28A745)  // 深绿

// 次色调 - 柔和蓝绿（用于辅助）
val AccentTeal = Color(0xFF5AC8FA)
val AccentTealLight = Color(0xFF7FD6FA)

// 背景色 - 纯白为主
val PureWhite = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFF2F7F4)         // 极淡绿灰
val LightGray = Color(0xFFE8F0E9)        // 淡绿灰分隔线
val CardGray = Color(0xFFF9FBF9)         // 卡片背景

// 文字色
val TextPrimary = Color(0xFF1A1A1A)      // 近黑标题
val TextSecondary = Color(0xFF4A4A4A)    // 深灰正文
val TextTertiary = Color(0xFF8A8A8A)     // 浅灰辅助
val TextOnPrimary = Color(0xFFFFFFFF)

// 功能色
val ErrorRed = Color(0xFFFF3B30)         // iOS红
val SuccessGreen = Color(0xFF34C759)     // iOS绿
val WarningOrange = Color(0xFFFF9500)    // iOS橙
val InfoBlue = Color(0xFF007AFF)         // iOS蓝

// 状态色（食材新鲜度）
val FreshGreen = Color(0xFF34C759)       // 新鲜
val WarningYellow = Color(0xFFFF9500)    // 即将过期
val ExpiredRed = Color(0xFFFF3B30)       // 已过期

// 浅色主题配色方案 - 清新绿色风格
private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryGreen.copy(alpha = 0.12f),
    onPrimaryContainer = PrimaryGreenDark,
    secondary = AccentTeal,
    onSecondary = TextOnPrimary,
    secondaryContainer = AccentTealLight.copy(alpha = 0.12f),
    onSecondaryContainer = AccentTeal,
    background = PureWhite,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = OffWhite,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextOnPrimary,
    outline = LightGray
)

// ==================== 字体排版 (Apple Music风格 - 大号粗体) ====================

val RecipeTypography = Typography(
    // 超大标题 - 页面主标题（如"资料库"）
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp
    ),
    // 大标题 - 区块标题
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    // 中标题
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    // 标题 - 卡片标题、列表项标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    // 正文 - 描述文字
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),
    // 标签 - 小字说明
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = (-0.1).sp
    )
)

// ==================== 形状系统 (Apple Music风格 - 大圆角) ====================

val RecipeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),      // 卡片圆角
    extraLarge = RoundedCornerShape(28.dp)  // 大卡片/图片圆角
)

// ==================== 间距规范 ====================

object RecipeSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

// ==================== 主题应用 ====================

@Composable
fun RecipeAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = RecipeTypography,
        shapes = RecipeShapes,
        content = content
    )
}
