package com.example.taskreminder.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 每日心情和短日记存储。
 *
 * 使用 SharedPreferences 按日期分别保存，避免为了一个轻量心情模块修改 Room 表结构。
 * 心情使用稳定的英文 key，界面负责显示对应中文和表情。
 */
data class DailyMood(
    val moodKey: String = "calm",
    val note: String = "",
    val recorded: Boolean = false
)

object MoodStore {
    private const val PREFS_NAME = "daily_mood"
    private const val MOOD_PREFIX = "mood_"
    private const val NOTE_PREFIX = "note_"

    fun load(context: Context): DailyMood {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = todayKey()
        return DailyMood(
            moodKey = prefs.getString(MOOD_PREFIX + key, "calm") ?: "calm",
            note = prefs.getString(NOTE_PREFIX + key, "") ?: "",
            recorded = prefs.contains(MOOD_PREFIX + key)
        )
    }

    fun save(context: Context, moodKey: String, note: String) {
        val key = todayKey()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(MOOD_PREFIX + key, moodKey)
            .putString(NOTE_PREFIX + key, note.trim())
            .apply()
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
