package com.example.taskreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, MoodEntry::class, HabitCheckIn::class], version = 5, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun moodDao(): MoodDao
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile private var INSTANCE: TaskDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN streak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN lastCompletedDay INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS mood_entries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "moodKey TEXT NOT NULL, " +
                        "note TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN totalCompletions INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE tasks SET totalCompletions = streak")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS habit_check_ins (" +
                        "taskId INTEGER NOT NULL, " +
                        "dayStart INTEGER NOT NULL, " +
                        "status TEXT NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(taskId, dayStart))"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN habitColor TEXT NOT NULL DEFAULT '#4B8DF8'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN habitIcon TEXT NOT NULL DEFAULT '✅'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN habitTarget INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN habitUnit TEXT NOT NULL DEFAULT '次'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN habitCheckInMode TEXT NOT NULL DEFAULT 'COMPLETE'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN habitLogEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN habitArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habit_check_ins ADD COLUMN amount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habit_check_ins ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): TaskDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_reminder.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}




