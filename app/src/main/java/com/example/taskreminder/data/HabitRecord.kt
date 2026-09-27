package com.example.taskreminder.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 习惯每日状态。 */
object HabitStatus {
    const val DONE = "DONE"
    const val PROGRESS = "PROGRESS"
    const val FAILED = "FAILED"
}

/** 习惯打卡方式。 */
object HabitCheckInMode {
    const val COMPLETE = "COMPLETE"
    const val AUTO = "AUTO"
    const val MANUAL = "MANUAL"
}

@Entity(tableName = "habit_check_ins", primaryKeys = ["taskId", "dayStart"])
data class HabitCheckIn(
    val taskId: Long,
    val dayStart: Long,
    val status: String,
    val amount: Int = 1,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_check_ins ORDER BY dayStart ASC")
    fun observeAll(): Flow<List<HabitCheckIn>>

    @Query("SELECT * FROM habit_check_ins ORDER BY dayStart ASC")
    suspend fun getAll(): List<HabitCheckIn>

    @Query("SELECT * FROM habit_check_ins WHERE taskId = :taskId ORDER BY dayStart ASC")
    suspend fun getForTask(taskId: Long): List<HabitCheckIn>

    @Query("SELECT * FROM habit_check_ins WHERE taskId = :taskId AND dayStart = :dayStart LIMIT 1")
    suspend fun getDay(taskId: Long, dayStart: Long): HabitCheckIn?

    @Query("SELECT COUNT(*) FROM habit_check_ins WHERE taskId = :taskId")
    suspend fun countForTask(taskId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: HabitCheckIn)

    @Query("DELETE FROM habit_check_ins WHERE taskId = :taskId AND dayStart = :dayStart")
    suspend fun deleteDay(taskId: Long, dayStart: Long)

    @Query("DELETE FROM habit_check_ins WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)
}
