package com.example.taskreminder.util

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
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object OngoingNotifier {
    private const val CHANNEL_ID = "ongoing_channel"
    private const val NOTIFICATION_ID = 10000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "最近任务",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "常驻显示最近的一条待办"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun update(context: Context, tasks: List<Task>) {
        ensureChannel(context)
        val now = System.currentTimeMillis()
        val upcoming = tasks
            .filter { !it.isCompleted && it.dueTime > now - 60_000L }
            .minByOrNull { it.dueTime }

        if (upcoming == null) {
            cancel(context)
            return
        }

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(upcoming.dueTime))
        val content = "$timeStr · ${formatDiff(upcoming.dueTime - now)}"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("最近：${upcoming.title}")
            .setContentText(content)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun formatDiff(diff: Long): String {
        if (diff <= 0) return "已过期"
        val totalMin = TimeUnit.MILLISECONDS.toMinutes(diff)
        val days = totalMin / (60 * 24)
        val hours = (totalMin % (60 * 24)) / 60
        val mins = totalMin % 60
        return when {
            days > 0 -> "还有${days}天${hours}小时"
            hours > 0 -> "还有${hours}小时${mins}分"
            else -> "还有${mins}分"
        }
    }
}