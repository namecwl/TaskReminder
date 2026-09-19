package com.example.taskreminder.data

import java.util.Calendar

object RepeatRule {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val INTERVAL = "INTERVAL"
    const val WEEKDAY = "WEEKDAY"

    fun nextTrigger(task: Task, after: Long): Long? {
        if (task.repeatRule == NONE) return null
        val baseCal = Calendar.getInstance().apply { timeInMillis = task.dueTime }
        val hour = baseCal.get(Calendar.HOUR_OF_DAY)
        val minute = baseCal.get(Calendar.MINUTE)

        return when (task.repeatRule) {
            DAILY -> {
                val cal = calAt(after, hour, minute)
                if (cal.timeInMillis <= after) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            WEEKDAY -> {
                val cal = calAt(after, hour, minute)
                if (cal.timeInMillis <= after) cal.add(Calendar.DAY_OF_YEAR, 1)
                var guard = 0
                while (isWeekend(cal) && guard < 10) {
                    cal.add(Calendar.DAY_OF_YEAR, 1); guard++
                }
                cal.timeInMillis
            }
            WEEKLY -> {
                val days = task.repeatDaysOfWeek.split(",")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.toIntOrNull() }
                    .toSet()
                if (days.isEmpty()) return null
                val cal = calAt(after, hour, minute)
                if (cal.timeInMillis <= after) cal.add(Calendar.DAY_OF_YEAR, 1)
                var guard = 0
                while (isoDayOfWeek(cal) !in days && guard < 14) {
                    cal.add(Calendar.DAY_OF_YEAR, 1); guard++
                }
                if (guard >= 14) null else cal.timeInMillis
            }
            MONTHLY -> {
                val targetDay = task.repeatDayOfMonth.coerceIn(1, 31)
                val now = Calendar.getInstance().apply { timeInMillis = after }
                var year = now.get(Calendar.YEAR)
                var month = now.get(Calendar.MONTH)
                var result: Long? = null
                for (i in 0..24) {
                    val cand = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, 1)
                        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                        set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (cand.timeInMillis > after) { result = cand.timeInMillis; break }
                    month++
                    if (month > 11) { month = 0; year++ }
                }
                result
            }
            INTERVAL -> {
                val n = task.repeatInterval.coerceAtLeast(1)
                val cal = Calendar.getInstance().apply {
                    timeInMillis = task.dueTime
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                var guard = 0
                while (cal.timeInMillis <= after && guard < 10000) {
                    cal.add(Calendar.DAY_OF_YEAR, n); guard++
                }
                if (guard >= 10000) null else cal.timeInMillis
            }
            else -> null
        }
    }

    private fun calAt(after: Long, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            timeInMillis = after
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun isWeekend(c: Calendar): Boolean {
        val d = c.get(Calendar.DAY_OF_WEEK)
        return d == Calendar.SATURDAY || d == Calendar.SUNDAY
    }

    private fun isoDayOfWeek(c: Calendar): Int = when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}