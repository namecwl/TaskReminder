package com.example.taskreminder.util

import com.example.taskreminder.data.RepeatRule
import java.util.Calendar
import java.util.Locale

/**
 * 中文自然语言任务解析器。
 *
 * 支持时间、日期、重复规则和提前提醒的基础表达。解析分成四步：
 * 1. 提取重复规则；
 * 2. 提取日期、星期或相对时间；
 * 3. 提取时间，并在未指定上午/下午时按当前时间选择最近的一次；
 * 4. 清理时间词和动作连接词，得到真正需要展示的任务标题。
 */
object NaturalLanguageParser {

    data class Parsed(
        val title: String,
        val dueTime: Long,
        val repeatRule: String = RepeatRule.NONE,
        val repeatInterval: Int = 1,
        val repeatDaysOfWeek: String = "",
        val repeatDayOfMonth: Int = 1
    )

    private enum class Period {
        EARLY_MORNING,
        MORNING,
        NOON,
        AFTERNOON,
        EVENING
    }

    fun parse(input: String): Parsed = parse(input, System.currentTimeMillis())

    /**
     * 允许测试固定“当前时间”，避免单元测试结果随执行时刻变化。
     */
    internal fun parse(input: String, nowMillis: Long): Parsed {
        val raw = input.trim()
        if (raw.isEmpty()) return Parsed("", nowMillis)

        var text = raw
        var repeatRule = RepeatRule.NONE
        var repeatInterval = 1
        var repeatDaysOfWeek = ""
        var repeatDayOfMonth = 1
        var explicitDate: Long? = null
        var explicitWeekday = false
        var period: Period? = null
        var durationMillis: Long? = null
        var hour = -1
        var minute = 0

        // 1. 重复规则优先提取，避免“每天/每周”被当成普通标题内容。
        val intervalMatch = Regex("每(?:隔)?([一二两三四五六七八九十\\d]{1,3})天").find(text)
        if (intervalMatch != null) {
            repeatRule = RepeatRule.INTERVAL
            repeatInterval = chineseNumToInt(intervalMatch.groupValues[1]).coerceAtLeast(1)
            text = text.replace(intervalMatch.value, " ")
        }

        if (repeatRule == RepeatRule.NONE) {
            val dailyToken = Regex("每天|每日|天天|每晚|每早|每晨").find(text)
            if (dailyToken != null) {
                repeatRule = RepeatRule.DAILY
                when (dailyToken.value) {
                    "每晚" -> period = Period.EVENING
                    "每早", "每晨" -> period = Period.MORNING
                }
                text = text.replace(Regex("每天|每日|天天|每晚|每早|每晨"), " ")
            }
        }

        if (repeatRule == RepeatRule.NONE && Regex("每个?工作日|周一到周五|周一至周五").containsMatchIn(text)) {
            repeatRule = RepeatRule.WEEKDAY
            text = text.replace(Regex("每个?工作日|周一到周五|周一至周五"), " ")
        }

        if (repeatRule == RepeatRule.NONE) {
            val weekDayMap = weekDayMap()
            val weeklyRegex = Regex("每(?:周|星期|礼拜)([一二三四五六日天])")
            val matches = weeklyRegex.findAll(text).toList()
            if (matches.isNotEmpty()) {
                repeatRule = RepeatRule.WEEKLY
                repeatDaysOfWeek = matches.mapNotNull { weekDayMap[it.groupValues[1]] }
                    .distinct()
                    .sorted()
                    .joinToString(",")
                text = text.replace(weeklyRegex, " ")
            }
        }

        if (repeatRule == RepeatRule.NONE) {
            val monthlyRegex = Regex("每(?:个)?月([一二两三四五六七八九十\\d]{1,3})[号日]")
            val match = monthlyRegex.find(text)
            if (match != null) {
                repeatRule = RepeatRule.MONTHLY
                repeatDayOfMonth = chineseNumToInt(match.groupValues[1]).coerceIn(1, 31)
                text = text.replace(monthlyRegex, " ")
            }
        }

        // 2. 相对时间，例如“20分钟后”“一小时后”。
        val halfHourRegex = Regex("(?:再过|过|等)?半(?:个)?小时后")
        if (halfHourRegex.containsMatchIn(text)) {
            durationMillis = 30 * 60_000L
            text = text.replace(halfHourRegex, " ")
        } else {
            val durationRegex = Regex("(?:再过|过|等)?([一二两三四五六七八九十\\d]{1,3})(分钟|小时|个小时|钟头)(?:后|之后)")
            val match = durationRegex.find(text)
            if (match != null) {
                val amount = chineseNumToInt(match.groupValues[1]).coerceAtLeast(1)
                val unit = match.groupValues[2]
                durationMillis = if (unit == "分钟") {
                    amount * 60_000L
                } else {
                    amount * 60 * 60_000L
                }
                text = text.replace(match.value, " ")
            }
        }

        // 3. 日期：相对日期、具体日期和一次性星期。
        val relativeDayRegex = Regex("大后天|后天|明天|明日|明儿|明晚|明早|今天|今日|今儿|今晚")
        val relativeDay = relativeDayRegex.find(text)
        if (relativeDay != null) {
            val token = relativeDay.value
            val dayOffset = when (token) {
                "大后天" -> 3
                "后天" -> 2
                "明天", "明日", "明儿", "明晚", "明早" -> 1
                else -> 0
            }
            explicitDate = addDays(startOfDay(nowMillis), dayOffset)
            when (token) {
                "明晚", "今晚" -> period = Period.EVENING
                "明早" -> period = Period.MORNING
            }
            text = text.replace(relativeDay.value, " ")
        }

        if (explicitDate == null) {
            val monthDayRegex = Regex("([一二两三四五六七八九十\\d]{1,3})月([一二两三四五六七八九十\\d]{1,3})[日号]")
            val match = monthDayRegex.find(text)
            if (match != null) {
                explicitDate = dateForMonthDay(
                    nowMillis,
                    chineseNumToInt(match.groupValues[1]),
                    chineseNumToInt(match.groupValues[2])
                )
                text = text.replace(match.value, " ")
            }
        }

        if (explicitDate == null) {
            val numericDateRegex = Regex("(?<!\\d)(\\d{1,2})[./\\-](\\d{1,2})(?!\\d)")
            val match = numericDateRegex.find(text)
            if (match != null) {
                val month = match.groupValues[1].toIntOrNull() ?: 0
                val day = match.groupValues[2].toIntOrNull() ?: 0
                if (month in 1..12 && day in 1..31) {
                    explicitDate = dateForMonthDay(nowMillis, month, day)
                    text = text.replace(match.value, " ")
                }
            }
        }

        if (explicitDate == null && repeatRule == RepeatRule.NONE) {
            val weekdayRegex = Regex("(下|这|本)?(?:周|星期|礼拜)([一二三四五六日天])")
            val match = weekdayRegex.find(text)
            if (match != null) {
                val target = weekDayMap()[match.groupValues[2]] ?: 1
                explicitDate = dateForWeekday(nowMillis, target, match.groupValues[1])
                explicitWeekday = true
                text = text.replace(match.value, " ")
            }
        }

        // 4. 时间段和时间。时间段会决定 1-11 点采用上午还是下午。
        val periodRegex = Regex("凌晨|清晨|早上|早晨|上午|中午|正午|下午|傍晚|晚上|夜里|夜间")
        val periodMatch = periodRegex.find(text)
        if (periodMatch != null) {
            period = when (periodMatch.value) {
                "凌晨" -> Period.EARLY_MORNING
                "清晨", "早上", "早晨", "上午" -> Period.MORNING
                "中午", "正午" -> Period.NOON
                "下午" -> Period.AFTERNOON
                "傍晚", "晚上", "夜里", "夜间" -> Period.EVENING
                else -> period
            }
            text = text.replaceFirst(periodMatch.value, " ")
        }

        val numericTime = Regex("(?<!\\d)(\\d{1,2})(?::|：|点|时)(?:(\\d{1,2})分?|(半|一刻|三刻))?")
            .find(text)
        if (numericTime != null) {
            hour = numericTime.groupValues[1].toIntOrNull() ?: -1
            minute = when {
                numericTime.groupValues[2].isNotBlank() -> numericTime.groupValues[2].toIntOrNull() ?: 0
                numericTime.groupValues[3] == "半" -> 30
                numericTime.groupValues[3] == "一刻" -> 15
                numericTime.groupValues[3] == "三刻" -> 45
                else -> 0
            }
            text = text.replace(numericTime.value, " ")
        } else {
            val chineseTime = Regex(
                "([一二两三四五六七八九十]{1,3})(?:点|时)(?:(半|一刻|三刻)|([一二两三四五六七八九十]{1,3})分?)?"
            ).find(text)
            if (chineseTime != null) {
                hour = chineseNumToInt(chineseTime.groupValues[1])
                minute = when {
                    chineseTime.groupValues[2] == "半" -> 30
                    chineseTime.groupValues[2] == "一刻" -> 15
                    chineseTime.groupValues[2] == "三刻" -> 45
                    chineseTime.groupValues[3].isNotBlank() -> chineseNumToInt(chineseTime.groupValues[3])
                    else -> 0
                }
                text = text.replace(chineseTime.value, " ")
            }
        }

        val dueTime = durationMillis?.let { nowMillis + it }
            ?: resolveDueTime(
                nowMillis = nowMillis,
                hour = hour,
                minute = minute,
                period = period,
                explicitDate = explicitDate,
                explicitWeekday = explicitWeekday
            )

        val finalTitle = cleanTitle(text).ifBlank { raw }
        return Parsed(
            title = finalTitle,
            dueTime = dueTime,
            repeatRule = repeatRule,
            repeatInterval = repeatInterval,
            repeatDaysOfWeek = repeatDaysOfWeek,
            repeatDayOfMonth = repeatDayOfMonth
        )
    }

    private fun resolveDueTime(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        period: Period?,
        explicitDate: Long?,
        explicitWeekday: Boolean
    ): Long {
        val safeMinute = minute.coerceIn(0, 59)
        val todayStart = startOfDay(nowMillis)

        if (hour !in 0..23) {
            if (explicitDate != null) {
                var candidate = calendarAt(startOfDay(explicitDate), 9, 0)
                if (candidate <= nowMillis) {
                    candidate = addDays(candidate, if (explicitWeekday) 7 else 1)
                }
                return candidate
            }
            return nextRoundedHour(nowMillis)
        }

        if (explicitDate != null) {
            val dateStart = startOfDay(explicitDate)
            if (period != null) {
                var candidate = periodCandidate(dateStart, hour, safeMinute, period)
                if (candidate <= nowMillis) {
                    candidate = addDays(candidate, if (explicitWeekday) 7 else 1)
                }
                return candidate
            }

            if (hour in 1..11) {
                val morning = calendarAt(dateStart, hour, safeMinute)
                val evening = calendarAt(dateStart, hour + 12, safeMinute)
                if (isSameDay(dateStart, nowMillis)) {
                    return listOf(morning, evening).firstOrNull { it > nowMillis }
                        ?: addDays(morning, if (explicitWeekday) 7 else 1)
                }
                return morning
            }

            if (hour == 12) {
                val noon = calendarAt(dateStart, 12, safeMinute)
                if (isSameDay(dateStart, nowMillis) && noon <= nowMillis) {
                    return addDays(calendarAt(dateStart, 0, safeMinute), 1)
                }
                return noon
            }

            var candidate = calendarAt(dateStart, hour, safeMinute)
            if (candidate <= nowMillis) {
                candidate = addDays(candidate, if (explicitWeekday) 7 else 1)
            }
            return candidate
        }

        if (period != null) {
            var candidate = periodCandidate(todayStart, hour, safeMinute, period)
            if (candidate <= nowMillis) candidate = addDays(candidate, 1)
            return candidate
        }

        // 未写“早上/晚上”时，1-11 点同时尝试上午和下午，取当前时间之后最近的一次。
        val candidates = when (hour) {
            0 -> listOf(
                calendarAt(todayStart, 0, safeMinute),
                addDays(calendarAt(todayStart, 0, safeMinute), 1)
            )
            12 -> listOf(
                calendarAt(todayStart, 12, safeMinute),
                addDays(calendarAt(todayStart, 0, safeMinute), 1)
            )
            in 1..11 -> listOf(
                calendarAt(todayStart, hour, safeMinute),
                calendarAt(todayStart, hour + 12, safeMinute),
                addDays(calendarAt(todayStart, hour, safeMinute), 1)
            )
            else -> listOf(
                calendarAt(todayStart, hour, safeMinute),
                addDays(calendarAt(todayStart, hour, safeMinute), 1)
            )
        }
        return candidates.firstOrNull { it > nowMillis } ?: candidates.last()
    }

    private fun periodCandidate(dayStart: Long, rawHour: Int, minute: Int, period: Period): Long {
        val hour = when (period) {
            Period.EARLY_MORNING -> if (rawHour == 12) 0 else rawHour
            Period.MORNING -> if (rawHour == 12) 0 else rawHour
            Period.NOON -> if (rawHour in 1..10) rawHour + 12 else rawHour
            Period.AFTERNOON -> if (rawHour in 1..11) rawHour + 12 else rawHour
            Period.EVENING -> when (rawHour) {
                12 -> 0
                in 1..11 -> rawHour + 12
                else -> rawHour
            }
        }
        return if (period == Period.EVENING && rawHour == 12) {
            addDays(calendarAt(dayStart, 0, minute), 1)
        } else {
            calendarAt(dayStart, hour, minute)
        }
    }

    /**
     * 删除时间词后，再清理“去、要、提醒我、记得”等连接词。
     * 例如“晚上8点去吃饭”最终得到“吃饭”，而不是“去吃饭”或“八点吃饭”。
     */
    private fun cleanTitle(text: String): String {
        var result = text
            .replace(Regex("[,，。；;、]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val leadingFillers = Regex("^(?:然后|到时候|到时|记得|提醒我|请|帮我|麻烦|要|去|准备|打算|别忘了|别忘|叫我|安排)")
        var previous: String
        do {
            previous = result
            result = result.replace(leadingFillers, "").trim()
        } while (result != previous && result.isNotEmpty())

        result = result
            .replace(Regex("(?:的时候)$"), "")
            .replace(Regex("^(?:的|了|在|于)+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return result
    }

    private fun dateForMonthDay(nowMillis: Long, month: Int, day: Int): Long {
        val safeMonth = month.coerceIn(1, 12)
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidate = Calendar.getInstance().apply {
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, safeMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            val safeDay = day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.DAY_OF_MONTH, safeDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.timeInMillis < startOfDay(nowMillis)) {
            candidate.add(Calendar.YEAR, 1)
        }
        return candidate.timeInMillis
    }

    private fun dateForWeekday(nowMillis: Long, targetIsoDay: Int, prefix: String): Long {
        val nowIsoDay = isoDayOfWeek(nowMillis)
        val days = if (prefix == "下") {
            val daysToNextMonday = 7 - (nowIsoDay - 1)
            daysToNextMonday + (targetIsoDay - 1)
        } else {
            (targetIsoDay - nowIsoDay + 7) % 7
        }
        return addDays(startOfDay(nowMillis), days)
    }

    private fun nextRoundedHour(nowMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun addDays(timeMillis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    private fun calendarAt(dayStart: Long, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dayStart
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean =
        startOfDay(firstMillis) == startOfDay(secondMillis)

    private fun isoDayOfWeek(timeMillis: Long): Int = when (
        Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.DAY_OF_WEEK)
    ) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    private fun weekDayMap(): Map<String, Int> = mapOf(
        "一" to 1,
        "二" to 2,
        "三" to 3,
        "四" to 4,
        "五" to 5,
        "六" to 6,
        "日" to 7,
        "天" to 7
    )

    private fun chineseNumToInt(value: String): Int {
        if (value.isEmpty()) return 0
        if (value.all { it.isDigit() }) return value.toIntOrNull() ?: 0
        val map = mapOf(
            '零' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3,
            '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8,
            '九' to 9, '十' to 10
        )
        if (value == "十") return 10
        if (value.length == 1) return map[value[0]] ?: 0
        if (value.length == 2) {
            if (value[0] == '十') return 10 + (map[value[1]] ?: 0)
            if (value[1] == '十') return (map[value[0]] ?: 0) * 10
        }
        if (value.length == 3 && value[1] == '十') {
            return (map[value[0]] ?: 0) * 10 + (map[value[2]] ?: 0)
        }
        return 0
    }
}
