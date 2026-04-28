package com.recipe.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.recipe.MainActivity

/**
 * 烹饪模式通知管理器
 *
 * 职责：
 * - 创建通知渠道（COOKING_REMINDER）
 * - 在步骤计时结束时发送通知（即使App在后台）
 * - 点击通知回到烹饪界面
 */
object CookingNotificationManager {
    private const val TAG = "CookingNotify"
    const val CHANNEL_ID = "cooking_reminder"
    private const val CHANNEL_NAME = "烹饪提醒"
    const val EXTRA_NAVIGATE_TO = "navigate_to"

    /**
     * 创建通知渠道（Android 8.0+ 必须）
     * 应在 Application 或 MainActivity.onCreate 中尽早调用
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "烹饪步骤完成时提醒你进入下一步"
                // 启用震动+声音（高重要性渠道默认开启，显式声明更保险）
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.i(TAG, "通知渠道已创建: $CHANNEL_ID")
        }
    }

    /**
     * 发送步骤完成通知
     *
     * @param context 上下文
     * @param stepIndex 当前已完成的步骤索引（0-based）
     * @param totalSteps 总步骤数
     * @param stepContent 已完成的步骤内容
     * @param nextStepContent 下一步骤内容（null表示已是最后一步）
     * @param recipeTitle 食谱标题
     */
    fun showStepComplete(
        context: Context,
        stepIndex: Int,
        totalSteps: Int,
        stepContent: String,
        nextStepContent: String?,
        recipeTitle: String
    ) {
        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "缺少通知权限，跳过通知")
                return
            }
        }

        val title: String
        val body: String

        if (nextStepContent != null) {
            title = "步骤 ${stepIndex + 1} 完成！"
            body = "该进行步骤 ${stepIndex + 2}/$totalSteps：$nextStepContent"
        } else {
            title = "最后一步完成！"
            body = "所有步骤已完成，享受你的美食吧！"
        }

        // 点击通知打开MainActivity并导航到烹饪模式
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVIGATE_TO, "cooking_mode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 全屏Intent（用于锁屏或后台时强制弹窗提醒，类似闹钟）
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(recipeTitle)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenIntent, true)  // 强制弹窗（类似闹钟）
            .build()

        try {
            NotificationManagerCompat.from(context).notify(stepIndex + 1, notification)
            Log.i(TAG, "通知已发送: 步骤${stepIndex + 1}完成, next=${nextStepContent ?: "无"}")
        } catch (e: Exception) {
            Log.e(TAG, "发送通知失败", e)
        }
    }
}
