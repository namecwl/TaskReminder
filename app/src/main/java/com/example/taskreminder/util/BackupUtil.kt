package com.example.taskreminder.util

import android.content.Context
import android.net.Uri
import com.example.taskreminder.data.Task
import org.json.JSONArray
import org.json.JSONObject

object BackupUtil {

    fun toJson(tasks: List<Task>): String {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(JSONObject().apply {
                put("title", t.title)
                put("note", t.note)
                put("dueTime", t.dueTime)
                put("repeatRule", t.repeatRule)
                put("repeatInterval", t.repeatInterval)
                put("repeatDaysOfWeek", t.repeatDaysOfWeek)
                put("repeatDayOfMonth", t.repeatDayOfMonth)
                put("advanceMinutes", t.advanceMinutes)
                put("isCompleted", t.isCompleted)
                put("completedAt", t.completedAt ?: JSONObject.NULL)
                put("createdAt", t.createdAt)
                put("enabled", t.enabled)
                put("streak", t.streak)
                put("lastCompletedDay", t.lastCompletedDay)
            })
        }
        return JSONObject().apply {
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
            put("tasks", arr)
        }.toString(2)
    }

    fun fromJson(json: String): List<Task> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("tasks") ?: return emptyList()
        val list = mutableListOf<Task>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Task(
                    id = 0,
                    title = o.optString("title"),
                    note = o.optString("note"),
                    dueTime = o.optLong("dueTime"),
                    repeatRule = o.optString("repeatRule", "NONE"),
                    repeatInterval = o.optInt("repeatInterval", 1),
                    repeatDaysOfWeek = o.optString("repeatDaysOfWeek"),
                    repeatDayOfMonth = o.optInt("repeatDayOfMonth", 1),
                    advanceMinutes = o.optInt("advanceMinutes", 5),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedAt = if (o.isNull("completedAt")) null else o.optLong("completedAt"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    enabled = o.optBoolean("enabled", true),
                    streak = o.optInt("streak", 0),
                    lastCompletedDay = o.optLong("lastCompletedDay", 0L)
                )
            )
        }
        return list
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
