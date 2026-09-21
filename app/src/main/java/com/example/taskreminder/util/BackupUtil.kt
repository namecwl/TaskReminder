package com.example.taskreminder.util

import android.content.Context
import android.net.Uri
import com.example.taskreminder.data.HabitCheckIn
import com.example.taskreminder.data.MoodEntry
import com.example.taskreminder.data.Task
import org.json.JSONArray
import org.json.JSONObject

data class BackupImportResult(
    val taskCount: Int,
    val moodCount: Int,
    val habitCount: Int
)

data class BackupData(
    val tasks: List<Task> = emptyList(),
    val moods: List<MoodEntry> = emptyList(),
    val habits: List<HabitCheckIn> = emptyList()
)

/**
 * 完整备份任务、心情记录和习惯打卡记录。
 *
 * 导入任务时会重新分配 taskId，并通过 id 映射还原习惯记录，避免覆盖现有数据。
 */
object BackupUtil {

    fun toJson(data: BackupData): String {
        val taskArray = JSONArray()
        data.tasks.forEach { task ->
            taskArray.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("note", task.note)
                put("dueTime", task.dueTime)
                put("repeatRule", task.repeatRule)
                put("repeatInterval", task.repeatInterval)
                put("repeatDaysOfWeek", task.repeatDaysOfWeek)
                put("repeatDayOfMonth", task.repeatDayOfMonth)
                put("advanceMinutes", task.advanceMinutes)
                put("isCompleted", task.isCompleted)
                put("completedAt", task.completedAt ?: JSONObject.NULL)
                put("createdAt", task.createdAt)
                put("enabled", task.enabled)
                put("streak", task.streak)
                put("totalCompletions", task.totalCompletions)
                put("lastCompletedDay", task.lastCompletedDay)
            })
        }

        val moodArray = JSONArray()
        data.moods.forEach { mood ->
            moodArray.put(JSONObject().apply {
                put("moodKey", mood.moodKey)
                put("note", mood.note)
                put("createdAt", mood.createdAt)
            })
        }

        val habitArray = JSONArray()
        data.habits.forEach { habit ->
            habitArray.put(JSONObject().apply {
                put("taskId", habit.taskId)
                put("dayStart", habit.dayStart)
                put("status", habit.status)
                put("updatedAt", habit.updatedAt)
            })
        }

        return JSONObject().apply {
            put("version", 3)
            put("exportedAt", System.currentTimeMillis())
            put("tasks", taskArray)
            put("moods", moodArray)
            put("habits", habitArray)
        }.toString(2)
    }

    fun fromJson(json: String): BackupData {
        val root = JSONObject(json)
        return BackupData(
            tasks = root.optJSONArray("tasks")?.let(::parseTasks).orEmpty(),
            moods = root.optJSONArray("moods")?.let(::parseMoods).orEmpty(),
            habits = root.optJSONArray("habits")?.let(::parseHabits).orEmpty()
        )
    }

    private fun parseTasks(array: JSONArray): List<Task> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                Task(
                    id = item.optLong("id", 0L),
                    title = item.optString("title"),
                    note = item.optString("note"),
                    dueTime = item.optLong("dueTime"),
                    repeatRule = item.optString("repeatRule", "NONE"),
                    repeatInterval = item.optInt("repeatInterval", 1),
                    repeatDaysOfWeek = item.optString("repeatDaysOfWeek"),
                    repeatDayOfMonth = item.optInt("repeatDayOfMonth", 1),
                    advanceMinutes = item.optInt("advanceMinutes", 5),
                    isCompleted = item.optBoolean("isCompleted", false),
                    completedAt = if (item.isNull("completedAt")) null else item.optLong("completedAt"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    enabled = item.optBoolean("enabled", true),
                    streak = item.optInt("streak", 0),
                    totalCompletions = item.optInt(
                        "totalCompletions",
                        item.optInt("streak", 0)
                    ),
                    lastCompletedDay = item.optLong("lastCompletedDay", 0L)
                )
            )
        }
    }

    private fun parseMoods(array: JSONArray): List<MoodEntry> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                MoodEntry(
                    moodKey = item.optString("moodKey", "calm"),
                    note = item.optString("note"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }

    private fun parseHabits(array: JSONArray): List<HabitCheckIn> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                HabitCheckIn(
                    taskId = item.optLong("taskId"),
                    dayStart = item.optLong("dayStart"),
                    status = item.optString("status", "DONE"),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
    }

    fun writeToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun readFromUri(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: ""
}

