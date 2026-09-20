package com.example.taskreminder.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.taskreminder.alarm.AlarmScheduler
import com.example.taskreminder.data.HabitCheckIn
import com.example.taskreminder.data.HabitStatus
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import com.example.taskreminder.data.TaskDatabase
import com.example.taskreminder.util.BackupData
import com.example.taskreminder.util.BackupImportResult
import com.example.taskreminder.util.BackupUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val database = TaskDatabase.getInstance(app)
    private val dao = database.taskDao()
    private val moodDao = database.moodDao()
    private val habitDao = database.habitDao()
    private val scheduler = AlarmScheduler(app)

    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * taskId -> dayStart -> status。
     * UI 用它展示今天是否完成、未完成，以及最近 7 天和历史日历。
     */
    val habitRecords: StateFlow<Map<Long, Map<Long, String>>> = habitDao.observeAll()
        .map { records ->
            records.groupBy { it.taskId }.mapValues { (_, taskRecords) ->
                taskRecords.associate { it.dayStart to it.status }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        // 兼容旧数据库：把原来的 streak/lastCompletedDay 转成每日记录。
        viewModelScope.launch {
            dao.getAll()
                .filter { it.repeatRule != RepeatRule.NONE }
                .forEach { task ->
                    if (habitDao.countForTask(task.id) == 0 &&
                        (task.streak > 0 || task.totalCompletions > 0)
                    ) {
                        seedHabitRecords(task)
                        recomputeHabitStats(task.id)
                    }
                }
        }
    }

    fun save(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val id = dao.insert(task)
                var saved = task.copy(id = id)
                if (saved.repeatRule != RepeatRule.NONE) {
                    if (saved.streak > 0 || saved.totalCompletions > 0) {
                        seedHabitRecords(saved)
                        saved = recomputeHabitStats(id) ?: saved
                    }
                }
                if (!saved.isCompleted && saved.enabled) scheduler.schedule(saved)
            } else {
                val previous = dao.getById(task.id)
                scheduler.cancel(task)
                dao.update(task)

                if (task.repeatRule != RepeatRule.NONE) {
                    val statsChanged = previous == null ||
                        previous.streak != task.streak ||
                        previous.totalCompletions != task.totalCompletions ||
                        previous.lastCompletedDay != task.lastCompletedDay
                    if (statsChanged) {
                        habitDao.deleteForTask(task.id)
                        seedHabitRecords(task)
                    } else if (habitDao.countForTask(task.id) == 0 &&
                        (task.streak > 0 || task.totalCompletions > 0)
                    ) {
                        seedHabitRecords(task)
                    }
                    val recomputed = recomputeHabitStats(task.id) ?: task
                    if (!recomputed.isCompleted && recomputed.enabled) scheduler.schedule(recomputed)
                } else if (!task.isCompleted && task.enabled) {
                    scheduler.schedule(task)
                }
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            scheduler.cancel(task)
            if (task.repeatRule != RepeatRule.NONE) habitDao.deleteForTask(task.id)
            dao.delete(task)
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            if (task.repeatRule != RepeatRule.NONE) {
                val today = todayStartMillis()
                val current = habitDao.getForTask(task.id)
                    .firstOrNull { it.dayStart == today }
                    ?.status
                // 重复点击已完成的圆环时保持幂等，不允许因为重复打卡减少累计天数。
                if (current != HabitStatus.DONE) {
                    setHabitStatusInternal(
                        taskId = task.id,
                        dayStart = today,
                        status = HabitStatus.DONE
                    )
                }
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

    /** 供习惯日历使用：完成、未完成或恢复为待打卡。 */
    fun setHabitStatus(task: Task, dayStart: Long, status: String?) {
        viewModelScope.launch {
            setHabitStatusInternal(task.id, dayStart, status)
        }
    }

    private suspend fun setHabitStatusInternal(taskId: Long, dayStart: Long, status: String?) {
        if (status == null) {
            habitDao.deleteDay(taskId, dayStart)
        } else {
            habitDao.upsert(
                HabitCheckIn(
                    taskId = taskId,
                    dayStart = startOfDay(dayStart),
                    status = status
                )
            )
        }
        recomputeHabitStats(taskId)
    }

    private suspend fun seedHabitRecords(task: Task) {
        val streak = task.streak.coerceAtLeast(0)
        val total = max(task.totalCompletions, streak)
        if (total <= 0) return

        val endDay = if (task.lastCompletedDay > 0) {
            startOfDay(task.lastCompletedDay)
        } else {
            addDays(todayStartMillis(), -1)
        }
        val now = System.currentTimeMillis()

        repeat(streak) { index ->
            habitDao.upsert(
                HabitCheckIn(
                    taskId = task.id,
                    dayStart = addDays(endDay, -index),
                    status = HabitStatus.DONE,
                    updatedAt = now
                )
            )
        }

        val remaining = total - streak
        repeat(remaining) { index ->
            // 历史总天数如果多于当前连续天数，用带间隔的记录保留累计值，避免伪造连续打卡。
            habitDao.upsert(
                HabitCheckIn(
                    taskId = task.id,
                    dayStart = addDays(endDay, -(streak + 2 + index * 2)),
                    status = HabitStatus.DONE,
                    updatedAt = now
                )
            )
        }
    }

    private suspend fun recomputeHabitStats(taskId: Long): Task? {
        val task = dao.getById(taskId) ?: return null
        val records = habitDao.getForTask(taskId)
        if (records.isEmpty()) return task

        val doneDays = records.asSequence()
            .filter { it.status == HabitStatus.DONE }
            .map { it.dayStart }
            .toSet()
        val total = doneDays.size
        val lastCompletedDay = doneDays.maxOrNull() ?: 0L
        val today = todayStartMillis()
        val failedToday = records.any {
            it.dayStart == today && it.status == HabitStatus.FAILED
        }
        val streak = if (failedToday) 0 else calculateCurrentStreak(doneDays, today)
        val updated = task.copy(
            streak = streak,
            totalCompletions = total,
            lastCompletedDay = lastCompletedDay
        )
        dao.update(updated)
        return updated
    }

    private fun calculateCurrentStreak(doneDays: Set<Long>, today: Long): Int {
        if (doneDays.isEmpty()) return 0
        val yesterday = addDays(today, -1)
        var cursor = when {
            today in doneDays -> today
            yesterday in doneDays -> yesterday
            else -> return 0
        }
        var count = 0
        while (cursor in doneDays) {
            count++
            cursor = addDays(cursor, -1)
        }
        return count
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val data = BackupData(
                tasks = dao.getAll(),
                moods = moodDao.getAll(),
                habits = habitDao.getAll()
            )
            onReady(BackupUtil.toJson(data))
        }
    }

    fun importBackup(
        json: String,
        onSuccess: (BackupImportResult) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = BackupUtil.fromJson(json)
                val taskIdMap = mutableMapOf<Long, Long>()
                database.withTransaction {
                    data.tasks.forEach { source ->
                        val newId = dao.insert(source.copy(id = 0))
                        if (source.id != 0L) taskIdMap[source.id] = newId
                    }
                    data.habits.forEach { habit ->
                        val newTaskId = taskIdMap[habit.taskId] ?: return@forEach
                        habitDao.upsert(habit.copy(taskId = newTaskId))
                    }
                    data.moods.forEach { mood ->
                        moodDao.insert(mood.copy(id = 0))
                    }
                }
                data.tasks.forEach { source ->
                    val newId = taskIdMap[source.id] ?: return@forEach
                    val saved = source.copy(id = newId)
                    if (!saved.isCompleted && saved.enabled) scheduler.schedule(saved)
                }
                onSuccess(
                    BackupImportResult(
                        taskCount = data.tasks.size,
                        moodCount = data.moods.size,
                        habitCount = data.habits.size
                    )
                )
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun clearCompleted() {
        viewModelScope.launch { dao.clearCompleted() }
    }

    fun importTasks(list: List<Task>) {
        viewModelScope.launch {
            list.forEach { source ->
                val fresh = source.copy(id = 0)
                val id = dao.insert(fresh)
                var saved = fresh.copy(id = id)
                if (saved.repeatRule != RepeatRule.NONE &&
                    (saved.streak > 0 || saved.totalCompletions > 0)
                ) {
                    seedHabitRecords(saved)
                    saved = recomputeHabitStats(id) ?: saved
                }
                if (!saved.isCompleted && saved.enabled) scheduler.schedule(saved)
            }
        }
    }

    private fun todayStartMillis(): Long = startOfDay(System.currentTimeMillis())

    private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun addDays(timeMillis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis
}


