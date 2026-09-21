package com.example.taskreminder.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI 共用时间格式工具。
 *
 * [DateTimeFormatter] 是线程安全的，可在 Compose 重组和后台线程中复用，
 * 避免每条任务重复创建 SimpleDateFormat / Calendar。
 */
object AppTimeText {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE)
    private val weekdayDateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
    private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.getDefault())

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun time(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(timeFormatter)

    fun monthDay(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(shortDateFormatter)

    fun weekdayDate(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(weekdayDateFormatter)

    fun fileTimestamp(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(fileDateFormatter)

    fun startOfDay(timestamp: Long): Long =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun todayStart(): Long = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    fun addDays(timestamp: Long, days: Long): Long =
        Instant.ofEpochMilli(timestamp).atZone(zone).plusDays(days).toInstant().toEpochMilli()
}
