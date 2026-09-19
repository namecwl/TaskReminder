package com.example.taskreminder.util

import com.example.taskreminder.data.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NaturalLanguageParserTest {

    @Test
    fun eveningTimeAndConnectorAreRemovedFromTitle() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 10, 0)
        val parsed = NaturalLanguageParser.parse("晚上8点去吃饭", now)

        assertEquals("吃饭", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 19, 20, 0)
    }

    @Test
    fun ambiguousEightUsesMorningWhenItIsCloser() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 7, 0)
        val parsed = NaturalLanguageParser.parse("8点去洗澡", now)

        assertEquals("洗澡", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 19, 8, 0)
    }

    @Test
    fun ambiguousEightUsesEveningWhenItIsCloser() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 10, 0)
        val parsed = NaturalLanguageParser.parse("8点去洗澡", now)

        assertEquals("洗澡", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 19, 20, 0)
    }

    @Test
    fun ambiguousEightMovesToNextMorningAfterEvening() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 21, 0)
        val parsed = NaturalLanguageParser.parse("8点去洗澡", now)

        assertEquals("洗澡", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 20, 8, 0)
    }

    @Test
    fun chineseMonthDayWithoutSuffixIsNotMistakenForTime() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 10, 0)
        val parsed = NaturalLanguageParser.parse("九月二十去医院复查", now)

        assertEquals("医院复查", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 20, 9, 0)
    }

    @Test
    fun monthDayWithSpacesAndFullwidthNumberIsParsed() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 10, 0)
        val parsed = NaturalLanguageParser.parse("九月 １３ 日去体检", now)

        assertEquals("体检", parsed.title)
        assertDate(parsed.dueTime, 2027, Calendar.SEPTEMBER, 13, 9, 0)
    }

    @Test
    fun numericMonthDayWithoutSuffixIsParsed() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 10, 0)
        val parsed = NaturalLanguageParser.parse("9月20交房租", now)

        assertEquals("交房租", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 20, 9, 0)
    }
    @Test
    fun tomorrowAfternoonIsParsedCorrectly() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 10, 0)
        val parsed = NaturalLanguageParser.parse("明天下午三点开会", now)

        assertEquals("开会", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 20, 15, 0)
    }

    @Test
    fun dailyHabitKeepsOnlySubjectInTitle() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 7, 0)
        val parsed = NaturalLanguageParser.parse("每天八点吃药", now)

        assertEquals("吃药", parsed.title)
        assertEquals(RepeatRule.DAILY, parsed.repeatRule)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 19, 8, 0)
    }

    @Test
    fun reminderPrefixIsRemovedFromTitle() {
        val now = time(2026, Calendar.SEPTEMBER, 19, 10, 0)
        val parsed = NaturalLanguageParser.parse("8点提醒我拿快递", now)

        assertEquals("拿快递", parsed.title)
        assertDate(parsed.dueTime, 2026, Calendar.SEPTEMBER, 19, 20, 0)
    }

    private fun assertDate(
        millis: Long,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ) {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(minute, calendar.get(Calendar.MINUTE))
    }

    private fun time(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}


