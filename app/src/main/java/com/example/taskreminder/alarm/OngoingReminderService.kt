package com.example.taskreminder.alarm

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.taskreminder.util.OngoingNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 通知栏常驻前台服务。
 *
 * 通知内容只显示当天文案：节日优先使用节日诗词/祝福，普通日期使用本地好句。
 * 服务在跨过零点后自动更新为下一天内容。
 */
class OngoingReminderService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        OngoingNotifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = OngoingNotifier.build(this)
        try {
            startForeground(OngoingNotifier.NOTIFICATION_ID, initialNotification)
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (updateJob?.isActive != true) {
            updateJob = serviceScope.launch {
                while (isActive) {
                    val waitMillis = millisUntilNextDay() + 1_000L
                    delay(waitMillis)
                    val notification = OngoingNotifier.build(applicationContext)
                    try {
                        NotificationManagerCompat.from(applicationContext)
                            .notify(OngoingNotifier.NOTIFICATION_ID, notification)
                    } catch (_: SecurityException) {
                        // Notification permission may have been revoked.
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun millisUntilNextDay(): Long = Calendar.getInstance().run {
        val now = timeInMillis
        val next = (clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        (next.timeInMillis - now).coerceAtLeast(1_000L)
    }

    companion object {
        /** 在通知权限和常驻通道可用时启动前台服务。 */
        fun start(context: Context) {
            if (!notificationsVisible(context)) return
            val intent = Intent(context, OngoingReminderService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // OEM background restrictions can reject a foreground start. The
                // onboarding screen explains how to grant autostart permission.
            }
        }

        private fun notificationsVisible(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = context.getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(OngoingNotifier.CHANNEL_ID)
                if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
            }
            return true
        }
    }
}

