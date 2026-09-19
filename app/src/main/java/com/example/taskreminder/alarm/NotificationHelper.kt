package com.example.taskreminder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taskreminder.MainActivity
import com.example.taskreminder.R
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    private const val CHANNEL_ID = "task_reminder_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "任务提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "任务到点和提前提醒"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                    val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        sound,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun show(context: Context, task: Task, isAdvance: Boolean) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_task_id", task.id)
        }
        val openPi = PendingIntent.getActivity(
            context, task.id.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completePi = actionPi(context, task.id, NotificationActionReceiver.ACTION_COMPLETE, 10)
        val snoozePi = actionPi(context, task.id, NotificationActionReceiver.ACTION_SNOOZE, 20)

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.dueTime))
        val title = if (isAdvance) "即将开始：${task.title}" else task.title
        val content = buildString {
            if (isAdvance) append("${task.advanceMinutes} 分钟后 · ")
            append(timeStr)
            if (task.note.isNotBlank()) append(" · ${task.note}")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .addAction(0, "完成", completePi)
            .addAction(0, "稍后5分钟", snoozePi)
            .addAction(0, "打开App", openPi)

        try {
            NotificationManagerCompat.from(context)
                .notify(notificationId(task.id), builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun actionPi(
        context: Context,
        taskId: Long,
        action: String,
        baseCode: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            (taskId * 100 + baseCode).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId))
    }

    private fun notificationId(taskId: Long): Int = (taskId % Int.MAX_VALUE).toInt()
}
