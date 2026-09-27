package com.example.taskreminder.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.taskreminder.alarm.AlarmScheduler
import com.example.taskreminder.alarm.NotificationHelper
import com.example.taskreminder.data.HabitCheckIn
import com.example.taskreminder.data.HabitCheckInMode
import com.example.taskreminder.data.HabitStatus
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import com.example.taskreminder.data.TaskDatabase
import com.example.taskreminder.util.BackupData
import com.example.taskreminder.util.BackupImportResult
import com.example.taskreminder.util.BackupUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    /** taskId -> dayStart -> HabitCheckIn。 */
    val habitRecords: StateFlow<Map<Long, Map<Long, HabitCheckIn>>> = habitDao.observeAll()
        .map { records ->
            records.groupBy { it.taskId }.mapValues { (_, taskRecords) ->
                taskRecords.associateBy { it.dayStart }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        // 兼容旧数据库：把原来的 streak/lastCompletedDay 转成每日记录。
        viewModelScope.launch(Dispatchers.IO) {
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
            val normalized = task.copy(
                habitTarget = task.habitTarget.coerceAtLeast(1),
                habitUnit = task.habitUnit.trim().ifBlank { "次" },
                habitIcon = task.habitIcon.trim().ifBlank { "✅" }
            )
            if (normalized.id == 0L) {
                val id = dao.insert(normalized)
                var saved = normalized.copy(id = id)
                if (saved.repeatRule != RepeatRule.NONE) {
                    if (saved.streak > 0 || saved.totalCompletions > 0) {
                        seedHabitRecords(saved)
                        saved = recomputeHabitStats(id) ?: saved
                    }
                }
                if (!saved.isCompleted && saved.enabled && !saved.habitArchived) {
                    scheduler.schedule(saved)
                }
            } else {
                val previous = dao.getById(normalized.id)
                if (previous != null) scheduler.cancel(previous)
                dao.update(normalized)

                if (normalized.repeatRule != RepeatRule.NONE) {
                    val statsChanged = previous == null ||
                        previous.streak != normalized.streak ||
                        previous.totalCompletions != normalized.totalCompletions ||
                        previous.lastCompletedDay != normalized.lastCompletedDay
                    if (statsChanged) {
                        habitDao.deleteForTask(normalized.id)
                        seedHabitRecords(normalized)
                    } else if (habitDao.countForTask(normalized.id) == 0 &&
                        (normalized.streak > 0 || normalized.totalCompletions > 0)
                    ) {
                        seedHabitRecords(normalized)
                    }
                    val recomputed = recomputeHabitStats(normalized.id) ?: normalized
                    if (!recomputed.isCompleted && recomputed.enabled && !recomputed.habitArchived) {
                        scheduler.schedule(recomputed)
                    }
                } else if (!normalized.isCompleted && normalized.enabled) {
                    scheduler.schedule(normalized)
                }
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            scheduler.cancel(task)
            NotificationHelper.cancel(getApplication(), task.id)
            if (task.repeatRule != RepeatRule.NONE) habitDao.deleteForTask(task.id)
            dao.delete(task)
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            if (task.repeatRule != RepeatRule.NONE) {
                toggleHabitToday(task)
            } else {
                if (!task.isCompleted) {
                    scheduler.cancel(task)
                    NotificationHelper.cancel(getApplication(), task.id)
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

    private suspend fun toggleHabitToday(task: Task) {
        val today = todayStartMillis()
        val current = habitDao.getDay(task.id, today)
        if (current?.status == HabitStatus.DONE) {
            habitDao.deleteDay(task.id, today)
        } else {
            val nextAmount = when (task.habitCheckInMode) {
                HabitCheckInMode.AUTO -> (current?.amount ?: 0).coerceAtLeast(0) + 1
                else -> task.habitTarget.coerceAtLeast(1)
            }
            val status = if (nextAmount >= task.habitTarget.coerceAtLeast(1)) {
                HabitStatus.DONE
            } else {
                HabitStatus.PROGRESS
            }
            habitDao.upsert(
                HabitCheckIn(
                    taskId = task.id,
                    dayStart = today,
                    status = status,
                    amount = nextAmount,
                    note = current?.note.orEmpty()
                )
            )
            if (status == HabitStatus.DONE) {
                NotificationHelper.cancel(getApplication(), task.id)
            }
        }
        recomputeHabitStats(task.id)
    }

    /** 直接设置某一天的状态；日历补记和详情页共用。 */
    fun setHabitStatus(
        task: Task,
        dayStart: Long,
        status: String?,
        amount: Int = task.habitTarget,
        note: String = ""
    ) {
        viewModelScope.launch {
            setHabitStatusInternal(task.id, dayStart, status, amount, note)
            if (status == HabitStatus.DONE && startOfDay(dayStart) == todayStartMillis()) {
                NotificationHelper.cancel(getApplication(), task.id)
            }
        }
    }

    /** 手动记录完成量，达到目标时自动转为已完成。 */
    fun recordHabitAmount(task: Task, dayStart: Long, amount: Int, note: String) {
        viewModelScope.launch {
            val normalizedAmount = amount.coerceAtLeast(0)
            val status = when {
                normalizedAmount >= task.habitTarget.coerceAtLeast(1) -> HabitStatus.DONE
                normalizedAmount > 0 || note.isNotBlank() -> HabitStatus.PROGRESS
                else -> null
            }
            setHabitStatusInternal(task.id, dayStart, status, normalizedAmount, note)
            if (status == HabitStatus.DONE && startOfDay(dayStart) == todayStartMillis()) {
                NotificationHelper.cancel(getApplication(), task.id)
            }
        }
    }

    fun setHabitArchived(task: Task, archived: Boolean) {
        viewModelScope.launch {
            val updated = task.copy(habitArchived = archived)
            if (archived) {
                scheduler.cancel(updated)
                NotificationHelper.cancel(getApplication(), updated.id)
            }
            dao.update(updated)
            if (!archived && updated.enabled && !updated.isCompleted) {
                scheduler.schedule(updated)
            }
        }
    }

    private suspend fun setHabitStatusInternal(
        taskId: Long,
        dayStart: Long,
        status: String?,
        amount: Int = 1,
        note: String = ""
    ) {
        val normalizedDay = startOfDay(dayStart)
        if (status == null) {
            habitDao.deleteDay(taskId, normalizedDay)
        } else {
            val safeAmount = when (status) {
                HabitStatus.DONE -> amount.coerceAtLeast(1)
                HabitStatus.FAILED -> 0
                else -> amount.coerceAtLeast(0)
            }
            habitDao.upsert(
                HabitCheckIn(
                    taskId = taskId,
                    dayStart = normalizedDay,
                    status = status,
                    amount = safeAmount,
                    note = note.trim()
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
                    amount = task.habitTarget.coerceAtLeast(1),
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
                    amount = task.habitTarget.coerceAtLeast(1),
                    updatedAt = now
                )
            )
        }
    }

    private suspend fun recomputeHabitStats(taskId: Long): Task? {
        val task = dao.getById(taskId) ?: return null
        val records = habitDao.getForTask(taskId)
        if (records.isEmpty()) {
            val cleared = task.copy(streak = 0, totalCompletions = 0, lastCompletedDay = 0L)
            if (cleared != task) dao.update(cleared)
            return cleared
        }

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
            val json = withContext(Dispatchers.Default) { BackupUtil.toJson(data) }
            onReady(json)
        }
    }

    fun importBackup(
        json: String,
        onSuccess: (BackupImportResult) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.Default) { BackupUtil.fromJson(json) }
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
                    if (!saved.isCompleted && saved.enabled && !saved.habitArchived) {
                        scheduler.schedule(saved)
                    }
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
                if (!saved.isCompleted && saved.enabled && !saved.habitArchived) {
                    scheduler.schedule(saved)
                }
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
