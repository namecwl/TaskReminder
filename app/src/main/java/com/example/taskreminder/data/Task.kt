package com.example.taskreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val note: String = "",
    val dueTime: Long = 0L,
    val repeatRule: String = RepeatRule.NONE,
    val repeatInterval: Int = 1,
    val repeatDaysOfWeek: String = "",
    val repeatDayOfMonth: Int = 1,
    val advanceMinutes: Int = 5,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val enabled: Boolean = true,
    val streak: Int = 0,
    val totalCompletions: Int = 0,
    val lastCompletedDay: Long = 0L
)


