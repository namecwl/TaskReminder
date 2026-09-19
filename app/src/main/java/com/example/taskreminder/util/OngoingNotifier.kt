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
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 常驻通知的构造和更新工具。
 *
 * 通知通道使用 v2 新 ID，确保旧版本以 IMPORTANCE_MIN 创建的通道不会继续影响显示。
 * 没有待办时也会保留一条运行中通知，避免服务状态在通知栏中消失。
 */
object OngoingNotifier {
    const val CHANNEL_ID = "task_reminder_ongoing_v2"
    const val NOTIFICATION_ID = 10000

    private const val LEGACY_CHANNEL_ID = "ongoing_channel"
    private const val ACCENT_COLOR = 0xFF4F6BFF

    /** 创建低打扰但可在状态栏显示的常驻通道，并清理旧通道。 */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "任务常驻提醒",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "常驻显示最近待办和距离提醒的时间"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Keeps the notification fresh while the app UI is collecting data.
     * The foreground service uses the same builder, so the content is identical.
     */
    fun update(context: Context, tasks: List<Task>) {
        ensureChannel(context)
        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, build(context, tasks))
        } catch (_: SecurityException) {
            // Notification permission can be revoked while the app is running.
        }
    }

    /** 根据当前任务列表构造常驻通知，服务和 App 界面共用此逻辑。 */
    fun build(context: Context, tasks: List<Task>): Notification {
        val snapshot = buildSnapshot(tasks)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(snapshot.title)
            .setContentText(snapshot.text)
            .setSubText("任务提醒")
            .setContentInfo("${snapshot.activeCount} 项")
            .setContentIntent(openPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(snapshot.text))
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
            .addAction(0, "打开", openPendingIntent)

        snapshot.nearest?.let { task ->
            val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COMPLETE
                putExtra(com.example.taskreminder.alarm.AlarmScheduler.EXTRA_TASK_ID, task.id)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                10_000 + (task.id % 100_000).toInt(),
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "完成", completePendingIntent)
        }

        return builder.build()
    }

    private data class Snapshot(
        val title: String,
        val text: String,
        val activeCount: Int,
        val nearest: Task?
    )

    /** 计算最近任务、待办数量和用于通知栏展示的简短文案。 */
    private fun buildSnapshot(tasks: List<Task>): Snapshot {
        val now = System.currentTimeMillis()
        val todayStart = todayStartMillis(now)
        val tomorrowStart = todayStart + 86_400_000L

        val active = tasks.asSequence()
            .filter { it.enabled && !it.isCompleted }
            .filter { task ->
                task.repeatRule == RepeatRule.NONE || task.lastCompletedDay != todayStart
            }
            .sortedBy { it.dueTime }
            .toList()

        val nearest = active.firstOrNull()
        val todayCount = active.count { it.dueTime < tomorrowStart }
        if (nearest == null) {
            return Snapshot(
                title = "任务提醒正在运行",
                text = "暂无待办，点击这里添加一条任务",
                activeCount = 0,
                nearest = null
            )
        }

        val state = if (nearest.dueTime <= now) {
            val overdue = formatDiff(now - nearest.dueTime)
            "已逾期 $overdue"
        } else {
            "${formatDate(nearest.dueTime, todayStart)} ${formatTime(nearest.dueTime)} · " +
                formatDiff(nearest.dueTime - now)
        }
        val todayText = if (todayCount > 0) " · 今日 $todayCount 项" else ""
        val text = "$state · 共 ${active.size} 项待办$todayText"
        return Snapshot(
            title = nearest.title.ifBlank { "未命名任务" },
            text = text,
            activeCount = active.size,
            nearest = nearest
        )
    }

    private fun todayStartMillis(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun formatTime(time: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))

    private fun formatDate(time: Long, todayStart: Long): String = when {
        time < todayStart + 86_400_000L -> "今天"
        time < todayStart + 2 * 86_400_000L -> "明天"
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(time))
    }

    private fun formatDiff(diff: Long): String {
        if (diff <= 0L) return "刚刚"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}天${hours}小时"
            hours > 0 -> "${hours}小时${minutes}分"
            minutes > 0 -> "${minutes}分钟"
            else -> "不到1分钟"
        }
    }
}



