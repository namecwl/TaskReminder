package com.example.taskreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return
        val action = intent.action

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TaskDatabase.getInstance(appContext).taskDao()
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
                            val next = RepeatRule.nextTrigger(task, task.dueTime + 60_000L)
                            if (next != null) {
                                val updated = task.copy(dueTime = next)
                                dao.update(updated)
                                scheduler.schedule(updated)
                            } else {
                                dao.update(
                                    task.copy(
                                        isCompleted = true,
                                        completedAt = System.currentTimeMillis()
                                    )
                                )
                            }
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

    companion object {
        const val ACTION_COMPLETE = "com.example.taskreminder.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.taskreminder.ACTION_SNOOZE"
    }
}