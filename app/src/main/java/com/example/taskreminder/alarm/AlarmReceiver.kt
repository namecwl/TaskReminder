package com.example.taskreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        val isAdvance = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_ADVANCE, false)
        if (taskId <= 0) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TaskDatabase.getInstance(appContext).taskDao()
                val task = dao.getById(taskId) ?: return@launch
                if (task.isCompleted || !task.enabled) return@launch

                NotificationHelper.show(appContext, task, isAdvance)

                if (!isAdvance && task.repeatRule != RepeatRule.NONE) {
                    val next = RepeatRule.nextTrigger(task, task.dueTime + 60_000L)
                    if (next != null) {
                        val updated = task.copy(dueTime = next)
                        dao.update(updated)
                        AlarmScheduler(appContext).schedule(updated)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}