package com.example.taskreminder.alarm

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.taskreminder.data.HabitCheckIn
import com.example.taskreminder.data.HabitStatus
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_COPY_DAILY) {
            val text = intent.getStringExtra(EXTRA_DAILY_TEXT).orEmpty()
            if (text.isNotBlank()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("每日文案", text))
                Toast.makeText(context, "文案已复制", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TaskDatabase.getInstance(appContext)
                val dao = database.taskDao()
                val habitDao = database.habitDao()
                val task = dao.getById(taskId) ?: return@launch
                val scheduler = AlarmScheduler(appContext)

                when (action) {
                    ACTION_COMPLETE -> {
                        scheduler.cancel(task)
                        if (task.repeatRule == RepeatRule.NONE) {
                            dao.update(
                                task.copy(
                                    isCompleted = true,
                                    completedAt = System.currentTimeMillis()
                                )
                            )
                        } else {
                            completeHabit(dao, habitDao, task, scheduler)
                        }
                    }
                    ACTION_SNOOZE -> {
                        scheduler.cancel(task)
                        scheduler.snooze(task, 5)
                    }
                }
                NotificationHelper.cancel(appContext, taskId)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun completeHabit(
        dao: com.example.taskreminder.data.TaskDao,
        habitDao: com.example.taskreminder.data.HabitDao,
        task: com.example.taskreminder.data.Task,
        scheduler: AlarmScheduler
    ) {
        val todayStart = todayStartMillis()
        habitDao.upsert(
            HabitCheckIn(
                taskId = task.id,
                dayStart = todayStart,
                status = HabitStatus.DONE
            )
        )
        val doneDays = habitDao.getForTask(task.id)
            .asSequence()
            .filter { it.status == HabitStatus.DONE }
            .map { it.dayStart }
            .toSet()
        val total = doneDays.size
        val lastCompletedDay = doneDays.maxOrNull() ?: 0L
        val streak = calculateCurrentStreak(doneDays, todayStart)
        val next = RepeatRule.nextTrigger(task, task.dueTime + 60_000L)
        if (next != null) {
            val updated = task.copy(
                dueTime = next,
                lastCompletedDay = lastCompletedDay,
                streak = streak,
                totalCompletions = total,
                completedAt = System.currentTimeMillis()
            )
            dao.update(updated)
            scheduler.schedule(updated)
        } else {
            dao.update(
                task.copy(
                    isCompleted = true,
                    lastCompletedDay = lastCompletedDay,
                    streak = streak,
                    totalCompletions = total,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun calculateCurrentStreak(doneDays: Set<Long>, today: Long): Int {
        if (doneDays.isEmpty()) return 0
        val yesterday = today - 86_400_000L
        var cursor = when {
            today in doneDays -> today
            yesterday in doneDays -> yesterday
            else -> return 0
        }
        var count = 0
        while (cursor in doneDays) {
            count++
            cursor -= 86_400_000L
        }
        return count
    }

    private fun todayStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        const val ACTION_COMPLETE = "com.example.taskreminder.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.taskreminder.ACTION_SNOOZE"
        const val ACTION_COPY_DAILY = "com.example.taskreminder.ACTION_COPY_DAILY"
        const val EXTRA_DAILY_TEXT = "daily_text"
    }
}

