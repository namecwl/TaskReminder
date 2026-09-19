package com.example.taskreminder.util

import com.example.taskreminder.data.RepeatRule
import java.util.Calendar

object NaturalLanguageParser {

    data class Parsed(
        val title: String,
        val dueTime: Long,
        val repeatRule: String = RepeatRule.NONE,
        val repeatInterval: Int = 1,
        val repeatDaysOfWeek: String = "",
        val repeatDayOfMonth: Int = 1
    )

    fun parse(input: String): Parsed {
        val raw = input.trim()
        if (raw.isEmpty()) return Parsed("", System.currentTimeMillis())

        var text = raw
        var repeatRule = RepeatRule.NONE
        var repeatInterval = 1
        var repeatDaysOfWeek = ""
        var repeatDayOfMonth = 1
        var explicitDate = -1L
        var hour = -1
        var minute = -1

        val intervalRegex = Regex("每隔([一二两三四五六七八九十\\d]{1,3})天")
        val intervalMatch = intervalRegex.find(text)
        if (intervalMatch != null) {
            repeatRule = RepeatRule.INTERVAL
            repeatInterval = chineseNumToInt(intervalMatch.groupValues[1]).coerceAtLeast(1)
            text = text.replace(intervalMatch.value, " ")
        }

        if (repeatRule == RepeatRule.NONE && Regex("每天|每日|天天|每晚|每早|每晨").containsMatchIn(text)) {
            repeatRule = RepeatRule.DAILY
            text = text.replace(Regex("每天|每日|天天|每晚|每早|每晨"), " ")
        }

        if (repeatRule == RepeatRule.NONE && Regex("每个?工作日|周一到周五|周一至周五").containsMatchIn(text)) {
            repeatRule = RepeatRule.WEEKDAY
            text = text.replace(Regex("每个?工作日|周一到周五|周一至周五"), " ")
        }

        if (repeatRule == RepeatRule.NONE) {
            val weekDayMap = mapOf(
                "一" to 1, "二" to 2, "三" to 3, "四" to 4,
                "五" to 5, "六" to 6, "日" to 7, "天" to 7
            )
            val weeklyRegex = Regex("每(?:周|星期|礼拜)([一二三四五六日天])")
            val ms = weeklyRegex.findAll(text).toList()
            if (ms.isNotEmpty()) {
                repeatRule = RepeatRule.WEEKLY
                val days = ms.mapNotNull { weekDayMap[it.groupValues[1]] }.sorted().distinct()
                repeatDaysOfWeek = days.joinToString(",")
                text = text.replace(weeklyRegex, " ")
            }
        }

        if (repeatRule == RepeatRule.NONE) {
            val monthlyRegex = Regex("每(?:个)?月([一二两三四五六七八九十\\d]{1,3})[号日]")
            val m = monthlyRegex.find(text)
            if (m != null) {
                repeatRule = RepeatRule.MONTHLY
                repeatDayOfMonth = chineseNumToInt(m.groupValues[1]).coerceIn(1, 31)
                text = text.replace(monthlyRegex, " ")
            }
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (Regex("大后天").containsMatchIn(text)) {
            explicitDate = today.timeInMillis + 3 * 86_400_000L
            text = text.replace(Regex("大后天"), " ")
        } else if (Regex("后天").containsMatchIn(text)) {
            explicitDate = today.timeInMillis + 2 * 86_400_000L
            text = text.replace(Regex("后天"), " ")
        } else if (Regex("明天|明日|明儿|明早|明晚").containsMatchIn(text)) {
            explicitDate = today.timeInMillis + 86_400_000L
            text = text.replace(Regex("明天|明日|明儿|明早|明晚"), " ")
        } else if (Regex("今天|今日|今儿|今晚|今早").containsMatchIn(text)) {
            explicitDate = today.timeInMillis
            text = text.replace(Regex("今天|今日|今儿|今晚|今早"), " ")
        } else {
            val mdRegex = Regex("([一二两三四五六七八九十\\d]{1,3})月([一二两三四五六七八九十\\d]{1,3})[日号]")
            val mdMatch = mdRegex.find(text)
            if (mdMatch != null) {
                val month = chineseNumToInt(mdMatch.groupValues[1]).coerceIn(1, 12)
                val day = chineseNumToInt(mdMatch.groupValues[2]).coerceIn(1, 31)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis < today.timeInMillis) cal.add(Calendar.YEAR, 1)
                explicitDate = cal.timeInMillis
                text = text.replace(mdRegex, " ")
            } else {
                val mdRegex2 = Regex("(?<![\\d])(\\d{1,2})[./\\-](\\d{1,2})(?![\\d])")
                val m2 = mdRegex2.find(text)
                if (m2 != null) {
                    val month = m2.groupValues[1].toIntOrNull() ?: 0
                    val day = m2.groupValues[2].toIntOrNull() ?: 0
                    if (month in 1..12 && day in 1..31) {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.MONTH, month - 1)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (cal.timeInMillis < today.timeInMillis) cal.add(Calendar.YEAR, 1)
                        explicitDate = cal.timeInMillis
                        text = text.replace(mdRegex2, " ")
                    }
                }
            }
        }

        var period = -1
        val periodRegex = Regex("凌晨|清晨|早上|早晨|上午|中午|正午|下午|傍晚|晚上|夜里|夜间")
        val periodMatch = periodRegex.find(text)
        if (periodMatch != null) {
            period = when (periodMatch.value) {
                "凌晨" -> 0
                "清晨", "早上", "早晨" -> 1
                "上午" -> 2
                "中午", "正午" -> 3
                "下午" -> 4
                "傍晚" -> 5
                "晚上", "夜里", "夜间" -> 6
                else -> -1
            }
            text = text.substring(0, periodMatch.range.first) + " " +
                text.substring(periodMatch.range.last + 1)
        }

        val timeRegex1 = Regex("(\\d{1,2})[:：点](\\d{1,2})?分?")
        val t1 = timeRegex1.find(text)
        if (t1 != null) {
            hour = t1.groupValues[1].toIntOrNull() ?: -1
            minute = t1.groupValues[2].toIntOrNull() ?: 0
            text = text.replace(t1.value, " ")
        } else {
            val timeRegex2 = Regex(
                "([一二两三四五六七八九十]{1,3})[点時时]" +
                    "(半|一刻|三刻|[一二两三四五六七八九十]{1,3}分?)?"
            )
            val t2 = timeRegex2.find(text)
            if (t2 != null) {
                hour = chineseNumToInt(t2.groupValues[1])
                val minStr = t2.groupValues[2]
                minute = when {
                    minStr == "半" -> 30
                    minStr == "一刻" -> 15
                    minStr == "三刻" -> 45
                    minStr.isBlank() -> 0
                    else -> chineseNumToInt(minStr.removeSuffix("分"))
                }
                text = text.replace(t2.value, " ")
            }
        }

        if (hour in 0..23 && period >= 0) {
            when (period) {
                0 -> if (hour == 12) hour = 0
                1, 2 -> if (hour == 12) hour = 0
                3 -> if (hour in 1..10) hour += 12
                4 -> if (hour in 1..11) hour += 12
                5 -> if (hour in 1..5) hour += 12
                6 -> if (hour in 1..11) hour += 12
            }
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = if (explicitDate > 0) explicitDate else today.timeInMillis
        }

        if (hour in 0..23) {
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (explicitDate < 0) {
                val nowMs = System.currentTimeMillis()
                if (cal.timeInMillis <= nowMs) cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else if (explicitDate > 0) {
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else {
            cal.timeInMillis = System.currentTimeMillis() + 3_600_000L
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        val cleanTitle = text.replace(Regex("\\s+"), " ").trim()
        val finalTitle = cleanTitle.ifBlank { raw }

        return Parsed(
            title = finalTitle,
            dueTime = cal.timeInMillis,
            repeatRule = repeatRule,
            repeatInterval = repeatInterval,
            repeatDaysOfWeek = repeatDaysOfWeek,
            repeatDayOfMonth = repeatDayOfMonth
        )
    }

    private fun chineseNumToInt(s: String): Int {
        if (s.isEmpty()) return 0
        if (s.all { it.isDigit() }) return s.toIntOrNull() ?: 0
        val map = mapOf(
            '零' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3,
            '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8,
            '九' to 9, '十' to 10
        )
        if (s == "十") return 10
        if (s.length == 1) return map[s[0]] ?: 0
        if (s.length == 2) {
            if (s[0] == '十') return 10 + (map[s[1]] ?: 0)
            if (s[1] == '十') return (map[s[0]] ?: 0) * 10
        }
        if (s.length == 3 && s[1] == '十') {
            return (map[s[0]] ?: 0) * 10 + (map[s[2]] ?: 0)
        }
        return 0
    }
}
