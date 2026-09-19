package com.example.taskreminder.alarm

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.taskreminder.data.TaskDatabase
import com.example.taskreminder.util.OngoingNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * 通知栏常驻前台服务。
 *
 * 该服务与数据库 Flow 保持连接，任务新增、完成、删除后会立即刷新通知；
 * 同时使用一分钟心跳刷新倒计时。START_STICKY 用于让系统在服务被回收后尽量恢复。
 */
class OngoingReminderService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        OngoingNotifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 要求 startForegroundService 后必须尽快调用 startForeground。
        // 首次先显示占位内容，随后数据库 Flow 会立即替换为真实任务。
        val initialNotification = OngoingNotifier.build(this, emptyList())
        try {
            startForeground(OngoingNotifier.NOTIFICATION_ID, initialNotification)
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (collectJob?.isActive != true) {
            collectJob = serviceScope.launch {
                val tasks = TaskDatabase.getInstance(applicationContext).taskDao().observeAll()
                // 数据库变化负责即时刷新，分钟心跳负责更新“还有多久”。
                val minuteTicker = flow {
                    while (true) {
                        emit(Unit)
                        delay(60_000L)
                    }
                }
                combine(tasks, minuteTicker) { latestTasks, _ -> latestTasks }
                    .catch { }
                    .collectLatest { latestTasks ->
                        val notification = OngoingNotifier.build(applicationContext, latestTasks)
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
        collectJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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




