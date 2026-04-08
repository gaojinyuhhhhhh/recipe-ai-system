package com.recipe.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 简易Markdown渲染组件
 * 支持: **粗体**, *斜体*, ### 标题, - 列表, 1. 有序列表, --- 分割线, `代码`
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val lines = markdown.split("\n")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when {
                // 分割线
                trimmed.matches(Regex("^-{3,}$")) || trimmed.matches(Regex("^\\*{3,}$")) -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(
                        color = textColor.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // 空行
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // H1
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
                // H2
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
                // H3
                trimmed.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
                // H4+
                trimmed.startsWith("#### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("#### ")),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
                // 无序列表
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val content = trimmed.removePrefix("- ").removePrefix("* ")
                    Row {
                        Text("  •  ", color = textColor, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = parseInlineMarkdown(content),
                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // 有序列表
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val match = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
                    if (match != null) {
                        val num = match.groupValues[1]
                        val content = match.groupValues[2]
                        Row {
                            Text(
                                "  $num.  ",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = parseInlineMarkdown(content),
                                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // 普通文本
                else -> {
                    Text(
                        text = parseInlineMarkdown(trimmed),
                        style = MaterialTheme.typography.bodyMedium.copy(color = textColor)
                    )
                }
            }
            i++
        }
    }
}

/**
 * 解析行内Markdown：**粗体**, *斜体*, `代码`
 */
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            when {
                // **粗体**
                remaining.startsWith("**") -> {
                    val endIdx = remaining.indexOf("**", 2)
                    if (endIdx > 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(remaining.substring(2, endIdx))
                        }
                        remaining = remaining.substring(endIdx + 2)
                    } else {
                        append("**")
                        remaining = remaining.substring(2)
                    }
                }
                // `代码`
                remaining.startsWith("`") -> {
                    val endIdx = remaining.indexOf("`", 1)
                    if (endIdx > 1) {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            background = Color(0x20808080)
                        )) {
                            append(remaining.substring(1, endIdx))
                        }
                        remaining = remaining.substring(endIdx + 1)
                    } else {
                        append("`")
                        remaining = remaining.substring(1)
                    }
                }
                // *斜体* (不和**冲突)
                remaining.startsWith("*") && !remaining.startsWith("**") -> {
                    val endIdx = remaining.indexOf("*", 1)
                    if (endIdx > 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(remaining.substring(1, endIdx))
                        }
                        remaining = remaining.substring(endIdx + 1)
                    } else {
                        append("*")
                        remaining = remaining.substring(1)
                    }
                }
                // 普通字符
                else -> {
                    val nextSpecial = findNextSpecial(remaining)
                    if (nextSpecial > 0) {
                        append(remaining.substring(0, nextSpecial))
                        remaining = remaining.substring(nextSpecial)
                    } else {
                        append(remaining)
                        remaining = ""
                    }
                }
            }
        }
    }
}

private fun findNextSpecial(text: String): Int {
    val chars = listOf("**", "*", "`")
    var minIdx = text.length
    for (ch in chars) {
        val idx = text.indexOf(ch)
        if (idx in 1 until minIdx) {
            minIdx = idx
        }
    }
    return if (minIdx < text.length) minIdx else -1
}
