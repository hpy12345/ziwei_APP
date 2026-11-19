package com.ziweistar.app.engine

import com.ziweistar.app.data.db.SessionEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * ReportEngine 周期结算测试（纯 JVM；SessionEntity 是 Room 的普通 data class，可脱离数据库构造）。
 */
class ReportEngineTest {

    private lateinit var originalTz: TimeZone

    @Before
    fun fixTimeZone() {
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTz)
    }

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private fun calendarOf(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    // ---------------- weekOf ----------------

    @Test
    fun weekOf_keyHasWPrefix() {
        val p = ReportEngine.weekOf(millisOf(2026, 11, 18, 15, 0))
        assertTrue("key=${p.key}", p.key.startsWith("W"))
        assertTrue("key=${p.key}", Regex("""^W\d{8}$""").matches(p.key))
        assertEquals(ReportEngine.Kind.WEEK, p.kind)
    }

    @Test
    fun weekOf_startIsMondayMidnight_andSpanIsSevenDays() {
        // 2026-11-18 是周三，本周一应为 2026-11-16
        val p = ReportEngine.weekOf(millisOf(2026, 11, 18, 15, 0))
        val start = calendarOf(p.startMillis)
        assertEquals(Calendar.MONDAY, start.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, start.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, start.get(Calendar.MINUTE))
        assertEquals(0, start.get(Calendar.SECOND))
        assertEquals(0, start.get(Calendar.MILLISECOND))
        assertEquals(16, start.get(Calendar.DAY_OF_MONTH))
        assertEquals("W20261116", p.key)
        assertTrue("start=${p.startMillis} end=${p.endMillis}", p.startMillis <= p.endMillis)
        assertEquals(7L * 86400_000L - 1, p.endMillis - p.startMillis)
    }

    @Test
    fun weekOf_sundayBelongsToPreviousMondayWeek() {
        // 2026-11-22 是周日，应归到 2026-11-16 起的那一周
        val p = ReportEngine.weekOf(millisOf(2026, 11, 22, 23, 30))
        assertEquals("W20261116", p.key)
        assertEquals(Calendar.MONDAY, calendarOf(p.startMillis).get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun weekOf_sameWeekDifferentDays_shareKey() {
        val mon = ReportEngine.weekOf(millisOf(2026, 11, 16, 0, 1))
        val wed = ReportEngine.weekOf(millisOf(2026, 11, 18, 12, 0))
        val sun = ReportEngine.weekOf(millisOf(2026, 11, 22, 23, 59))
        assertEquals(mon.key, wed.key)
        assertEquals(mon.key, sun.key)
        assertEquals(mon.startMillis, wed.startMillis)
        assertEquals(mon.endMillis, sun.endMillis)
        // 跨周则 key 变化
        val nextMon = ReportEngine.weekOf(millisOf(2026, 11, 23, 0, 1))
        assertFalse(mon.key == nextMon.key)
        assertTrue(nextMon.startMillis > mon.endMillis)
    }

    // ---------------- monthOf ----------------

    @Test
    fun monthOf_keyHasMPrefix_andStartsAtFirstDayMidnight() {
        val p = ReportEngine.monthOf(millisOf(2026, 11, 18, 15, 0))
        assertEquals(ReportEngine.Kind.MONTH, p.kind)
        assertTrue("key=${p.key}", p.key.startsWith("M"))
        assertEquals("M202611", p.key)

        val start = calendarOf(p.startMillis)
        assertEquals(1, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.NOVEMBER, start.get(Calendar.MONTH))
        assertEquals(2026, start.get(Calendar.YEAR))
        assertEquals(0, start.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, start.get(Calendar.MINUTE))
        assertEquals(0, start.get(Calendar.SECOND))
        assertEquals(0, start.get(Calendar.MILLISECOND))
        assertTrue("start=${p.startMillis} end=${p.endMillis}", p.startMillis <= p.endMillis)
    }

    @Test
    fun monthOf_endIsLastMillisOfMonth() {
        val p = ReportEngine.monthOf(millisOf(2026, 11, 18, 15, 0))
        val end = calendarOf(p.endMillis)
        assertEquals(30, end.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, end.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, end.get(Calendar.MINUTE))
        assertEquals(59, end.get(Calendar.SECOND))
        assertEquals(999, end.get(Calendar.MILLISECOND))
        assertEquals(30L * 86400_000L - 1, p.endMillis - p.startMillis)

        // 闰年 2 月有 29 天
        val feb2028 = ReportEngine.monthOf(millisOf(2028, 2, 10, 9, 0))
        assertEquals("M202802", feb2028.key)
        assertEquals(29L * 86400_000L - 1, feb2028.endMillis - feb2028.startMillis)
    }

    @Test
    fun monthOf_sameMonthDifferentDays_shareKey() {
        val a = ReportEngine.monthOf(millisOf(2026, 11, 1, 0, 1))
        val b = ReportEngine.monthOf(millisOf(2026, 11, 30, 23, 59))
        assertEquals(a.key, b.key)
        assertEquals(a.startMillis, b.startMillis)
        assertEquals(a.endMillis, b.endMillis)
        assertFalse(a.key == ReportEngine.monthOf(millisOf(2026, 12, 1, 0, 1)).key)
    }

    // ---------------- compute ----------------

    @Test
    fun compute_emptySessions_hasNoData() {
        val period = ReportEngine.monthOf(millisOf(2026, 11, 18, 15, 0))
        val stats = ReportEngine.compute(period, emptyList(), isMale = true)
        assertEquals(0, stats.totalCount)
        assertEquals(0, stats.activeDays)
        assertFalse(stats.hasData)
        assertEquals(24, stats.hourHistogram.size)
        assertTrue(stats.climaxDistribution.isEmpty())
        assertEquals(0, stats.severeCount)
        assertEquals(0, stats.streakPeak)
    }

    @Test
    fun compute_sessionsOutsidePeriod_areIgnored() {
        val period = ReportEngine.monthOf(millisOf(2026, 11, 18, 15, 0))
        val outside = session(
            endedAt = millisOf(2026, 10, 31, 23, 0),
            startedAt = millisOf(2026, 10, 31, 22, 0),
        )
        val stats = ReportEngine.compute(period, listOf(outside), isMale = true)
        assertEquals(0, stats.totalCount)
        assertFalse(stats.hasData)
    }

    @Test
    fun compute_withSessions_countsInRangeOnly() {
        val period = ReportEngine.monthOf(millisOf(2026, 11, 18, 15, 0))
        val inside1 = session(
            endedAt = millisOf(2026, 11, 17, 21, 0),
            startedAt = millisOf(2026, 11, 17, 20, 40),
        )
        val inside2 = session(
            endedAt = millisOf(2026, 11, 18, 22, 0),
            startedAt = millisOf(2026, 11, 18, 21, 40),
        )
        val outside = session(
            endedAt = millisOf(2026, 10, 5, 21, 0),
            startedAt = millisOf(2026, 10, 5, 20, 40),
        )
        val stats = ReportEngine.compute(period, listOf(inside1, inside2, outside), isMale = true)
        assertEquals(2, stats.totalCount)
        assertEquals(2, stats.activeDays)
        assertTrue(stats.hasData)
        assertEquals(24, stats.hourHistogram.size)
        assertEquals(1, stats.maxDayCount)
    }

    @Test
    fun compute_isDeterministic() {
        val period = ReportEngine.weekOf(millisOf(2026, 11, 18, 15, 0))
        val sessions = listOf(
            session(endedAt = millisOf(2026, 11, 17, 21, 0), startedAt = millisOf(2026, 11, 17, 20, 0)),
            session(endedAt = millisOf(2026, 11, 18, 22, 0), startedAt = millisOf(2026, 11, 18, 21, 0)),
        )
        assertEquals(
            ReportEngine.compute(period, sessions, isMale = true),
            ReportEngine.compute(period, sessions, isMale = true),
        )
    }

    /** 构造一条最小可用记录（其余字段取合理默认值） */
    private fun session(endedAt: Long, startedAt: Long): SessionEntity = SessionEntity(
        startedAt = startedAt,
        endedAt = endedAt,
        dayKey = AlmanacEngine.formatDate(java.util.Date(endedAt)),
        durationSeconds = (endedAt - startedAt).toInt() / 1000,
        pausedSeconds = 0,
        methodCodes = "M_H01",
        methodNames = "五指正宗",
        starId = null,
        starName = "广寒白月光",
        starAttribute = "PURE_LOVE",
        mediums = "FANTASY",
        venue = "BEDROOM",
        climax = "FLY",
        satisfactionTenths = 9,
        adverse = "",
    )
}
