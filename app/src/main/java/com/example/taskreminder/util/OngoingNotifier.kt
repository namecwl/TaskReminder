package com.example.taskreminder.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taskreminder.MainActivity
import com.example.taskreminder.R
import com.example.taskreminder.alarm.NotificationActionReceiver

/**
 * 常驻通知工具。
 *
 * 通知只承担两件事：证明应用仍在运行，以及显示当天文案。
 * 节日文案优先，普通日期使用 assets/daily_quotes.txt 中的本地好句。
 */
object OngoingNotifier {
    const val CHANNEL_ID = "task_reminder_daily_v3"
    const val NOTIFICATION_ID = 10000

    private const val LEGACY_CHANNEL_ID = "task_reminder_ongoing_v2"
    private const val ACCENT_COLOR = 0xFF4B63F6

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每日文案常驻",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "常驻显示当天的节日文案或好词好句"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    /** 任务变化时刷新通知，实际内容仍由日期和节假日决定。 */
    fun update(context: Context) {
        ensureChannel(context)
        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, build(context))
        } catch (_: SecurityException) {
            // Notification permission can be revoked while the app is running.
        }
    }

    fun build(context: Context): Notification {
        val copy = DailyCopyProvider.forDate(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COPY_DAILY
            putExtra(NotificationActionReceiver.EXTRA_DAILY_TEXT, copy.text)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            20_001,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(copy.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.text))
            .setContentIntent(openPendingIntent)
            .setColor(ACCENT_COLOR.toInt())
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "复制", copyPendingIntent)
            .build()
    }
}
