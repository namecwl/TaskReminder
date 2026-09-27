package com.example.taskreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taskreminder.data.HabitStatus
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        val isAdvance = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_ADVANCE, false)
        if (taskId <= 0) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TaskDatabase.getInstance(appContext)
                val dao = database.taskDao()
                val task = dao.getById(taskId) ?: return@launch
                if (task.isCompleted || !task.enabled || task.habitArchived) return@launch

                // 用户已经手动打卡时，当天不再弹提醒；到点闹钟只负责推进下一次。
                if (task.repeatRule != RepeatRule.NONE) {
                    val occurrenceDay = startOfDay(task.dueTime)
                    val done = database.habitDao()
                        .getDay(task.id, occurrenceDay)
                        ?.status == HabitStatus.DONE
                    if (done) {
                        if (!isAdvance) scheduleNextHabit(dao, task, appContext)
                        return@launch
                    }
                }

                NotificationHelper.show(appContext, task, isAdvance)

                if (!isAdvance && task.repeatRule != RepeatRule.NONE) {
                    scheduleNextHabit(dao, task, appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun scheduleNextHabit(
        dao: com.example.taskreminder.data.TaskDao,
        task: com.example.taskreminder.data.Task,
        context: Context
    ) {
        val next = RepeatRule.nextTrigger(task, task.dueTime + 60_000L) ?: return
        val updated = task.copy(dueTime = next)
        dao.update(updated)
        AlarmScheduler(context).schedule(updated)
    }

    private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
