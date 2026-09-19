package com.example.taskreminder.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskreminder.alarm.AlarmScheduler
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import com.example.taskreminder.data.TaskDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = TaskDatabase.getInstance(app).taskDao()
    private val scheduler = AlarmScheduler(app)

    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val id = dao.insert(task)
                val saved = task.copy(id = id)
                if (!saved.isCompleted && saved.enabled) scheduler.schedule(saved)
            } else {
                scheduler.cancel(task)
                dao.update(task)
                if (!task.isCompleted && task.enabled) scheduler.schedule(task)
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            scheduler.cancel(task)
            dao.delete(task)
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            if (task.repeatRule != RepeatRule.NONE) {
                toggleHabit(task)
            } else {
                if (!task.isCompleted) {
                    scheduler.cancel(task)
                    dao.update(
                        task.copy(isCompleted = true, completedAt = System.currentTimeMillis())
                    )
                } else {
                    val updated = task.copy(isCompleted = false, completedAt = null)
                    dao.update(updated)
                    scheduler.schedule(updated)
                }
            }
        }
    }

    private suspend fun toggleHabit(task: Task) {
        val todayStart = todayStartMillis()
        val yesterdayStart = todayStart - 86_400_000L

        if (task.lastCompletedDay == todayStart) {
            dao.update(
                task.copy(
                    lastCompletedDay = 0L,
                    streak = (task.streak - 1).coerceAtLeast(0),
                    completedAt = null
                )
            )
        } else {
            val newStreak = when (task.lastCompletedDay) {
                yesterdayStart -> task.streak + 1
                else -> 1
            }
            dao.update(
                task.copy(
                    lastCompletedDay = todayStart,
                    streak = newStreak,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearCompleted() {
        viewModelScope.launch { dao.clearCompleted() }
    }

    fun importTasks(list: List<Task>) {
        viewModelScope.launch {
            list.forEach { t ->
                val fresh = t.copy(id = 0)
                val id = dao.insert(fresh)
                val saved = fresh.copy(id = id)
                if (!saved.isCompleted && saved.enabled) scheduler.schedule(saved)
            }
        }
    }

    private fun todayStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
