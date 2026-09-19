package com.example.taskreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.taskreminder.data.Task

class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(task: Task) {
        cancel(task)
        if (!task.enabled || task.isCompleted) return
        val now = System.currentTimeMillis()

        if (task.advanceMinutes > 0) {
            val advanceTime = task.dueTime - task.advanceMinutes * 60_000L
            if (advanceTime > now) setAlarm(task.id, advanceTime, isAdvance = true)
        }
        if (task.dueTime > now) setAlarm(task.id, task.dueTime, isAdvance = false)
    }

    private fun setAlarm(taskId: Long, time: Long, isAdvance: Boolean) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (isAdvance) ACTION_ADVANCE else ACTION_DUE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_IS_ADVANCE, isAdvance)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(taskId, isAdvance),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        }
    }

    fun cancel(task: Task) {
        cancelAlarm(task.id, isAdvance = false)
        cancelAlarm(task.id, isAdvance = true)
    }

    private fun cancelAlarm(taskId: Long, isAdvance: Boolean) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (isAdvance) ACTION_ADVANCE else ACTION_DUE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(taskId, isAdvance),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun snooze(task: Task, minutes: Int = 5) {
        val time = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(task.id, time, isAdvance = false)
    }

    private fun requestCode(taskId: Long, isAdvance: Boolean): Int =
        (taskId * 2 + if (isAdvance) 1 else 0).toInt()

    companion object {
        const val ACTION_DUE = "com.example.taskreminder.ACTION_DUE"
        const val ACTION_ADVANCE = "com.example.taskreminder.ACTION_ADVANCE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_IS_ADVANCE = "is_advance"
    }
}
