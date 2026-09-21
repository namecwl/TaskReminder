package com.example.taskreminder.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskreminder.data.MoodEntry
import com.example.taskreminder.data.TaskDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 心情记录 ViewModel。
 *
 * 首页摘要和回顾列表都订阅同一份 Room Flow，保存后会自动刷新。
 */
class MoodViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = TaskDatabase.getInstance(app).moodDao()

    val entries: StateFlow<List<MoodEntry>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(moodKey: String, note: String) {
        viewModelScope.launch {
            dao.insert(
                MoodEntry(
                    moodKey = moodKey,
                    note = note.trim()
                )
            )
        }
    }

    fun delete(entry: MoodEntry) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
