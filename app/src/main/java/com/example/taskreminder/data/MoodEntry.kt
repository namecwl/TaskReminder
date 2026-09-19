package com.example.taskreminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 一条心情记录。
 *
 * 同一天允许插入多条记录，createdAt 用于在回顾页面按时间排列。
 */
@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moodKey: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MoodEntry>>

    @Insert
    suspend fun insert(entry: MoodEntry): Long

    @Delete
    suspend fun delete(entry: MoodEntry)
}
