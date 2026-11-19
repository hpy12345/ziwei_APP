package com.ziweistar.app.engine

import com.ziweistar.app.engine.model.AlmanacSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * AlmanacEngine 确定性测试（纯 JVM，依赖 app/libs/lunar-1.7.4.jar）。
 *
 * 只测「同一输入 → 同一输出」与格式化口径，不锁死具体干支值（节气换年依赖历法库内部实现，
 * 属于被测引擎的职责范围，测试不应复述一份实现）。
 */
class AlmanacEngineTest {

    private lateinit var originalTz: TimeZone

    @Before
    fun fixTimeZone() {
        // 固定时区，避免固定时间戳在不同默认时区下落到不同自然日
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTz)
    }

    /** 用固定时区构造毫秒时间戳（clear() 归零毫秒，避免跨秒抖动） */
    private fun millisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun formatDate_fixedTimestamp_returnsExpectedYmd() {
        val fixed = millisOf(2026, 11, 18, 12, 0)
        assertEquals("2026-11-18", AlmanacEngine.formatDate(Date(fixed)))
    }

    @Test
    fun formatDate_isDeterministicAndMatchesYmdShape() {
        val fixed = Date(millisOf(2026, 1, 5, 9, 30))
        val first = AlmanacEngine.formatDate(fixed)
        val second = AlmanacEngine.formatDate(fixed)
        assertEquals(first, second)
        assertTrue("formatDate=$first should match yyyy-MM-dd", Regex("""^\d{4}-\d{2}-\d{2}$""").matches(first))
        assertEquals("2026-01-05", first)
    }

    @Test
    fun formatIso_fixedTimestamp_returnsFullDateTime() {
        val fixed = Date(millisOf(2026, 11, 18, 7, 59))
        assertEquals("2026-11-18 07:59:00", AlmanacEngine.formatIso(fixed))
    }

    @Test
    fun snapshot_sameDateTwice_returnsEqualSnapshot() {
        val now = Date(millisOf(2026, 11, 18, 12, 0))
        val first: AlmanacSnapshot = AlmanacEngine.snapshot(now)
        val second: AlmanacSnapshot = AlmanacEngine.snapshot(now)
        // AlmanacSnapshot 是 data class，字段级一致即整体相等
        assertEquals(first, second)
        assertEquals(first.solarDate, second.solarDate)
        assertEquals(first.ganZhiDay, second.ganZhiDay)
        assertEquals(first.xiu, second.xiu)
        assertEquals(first.zhiXing, second.zhiXing)
    }

    @Test
    fun snapshot_solarDate_isYmdAndEqualsFormatDate() {
        val now = Date(millisOf(2026, 11, 18, 12, 0))
        val snap = AlmanacEngine.snapshot(now)
        assertEquals("2026-11-18", snap.solarDate)
        assertEquals(AlmanacEngine.formatDate(now), snap.solarDate)
        assertTrue(Regex("""^\d{4}-\d{2}-\d{2}$""").matches(snap.solarDate))
    }

    @Test
    fun snapshot_fillsCoreAlmanacFields() {
        val snap = AlmanacEngine.snapshot(Date(millisOf(2026, 11, 18, 12, 0)))
        // 历法库能推出来的字段不应为空（runCatching 兜底会给出 ""，故非空即说明库调用成功）
        assertTrue("ganZhiDay 为空", snap.ganZhiDay.isNotBlank())
        assertTrue("lunarText 为空", snap.lunarText.isNotBlank())
        assertTrue("dayGan 为空", snap.dayGan.isNotBlank())
        assertTrue("dayZhi 为空", snap.dayZhi.isNotBlank())
        assertTrue("zhiXing 为空", snap.zhiXing.isNotBlank())
        assertTrue("xiu 为空", snap.xiu.isNotBlank())
        assertTrue("xiuZheng 为空", snap.xiuZheng.isNotBlank())
        // isWoodFireDay / isMetalWaterDay 应恰有一侧成立（天干必属木火或土金水）
        assertTrue(snap.isWoodFireDay != snap.isMetalWaterDay)
    }

    @Test
    fun snapshot_differentDays_produceDifferentSolarDate() {
        val a = AlmanacEngine.snapshot(Date(millisOf(2026, 11, 18, 12, 0)))
        val b = AlmanacEngine.snapshot(Date(millisOf(2026, 11, 19, 12, 0)))
        assertEquals("2026-11-18", a.solarDate)
        assertEquals("2026-11-19", b.solarDate)
        assertFalse(a.solarDate == b.solarDate)
    }

    @Test
    fun xiuElementGroup_mapsSevenLuminaries() {
        assertEquals(AlmanacEngine.XiuGroup.WATER_MOON, AlmanacEngine.xiuElementGroup("水"))
        assertEquals(AlmanacEngine.XiuGroup.WATER_MOON, AlmanacEngine.xiuElementGroup("月"))
        assertEquals(AlmanacEngine.XiuGroup.FIRE_SUN, AlmanacEngine.xiuElementGroup("火"))
        assertEquals(AlmanacEngine.XiuGroup.FIRE_SUN, AlmanacEngine.xiuElementGroup("日"))
        assertEquals(AlmanacEngine.XiuGroup.WOOD_METAL_EARTH, AlmanacEngine.xiuElementGroup("木"))
        assertEquals(AlmanacEngine.XiuGroup.WOOD_METAL_EARTH, AlmanacEngine.xiuElementGroup("金"))
        assertEquals(AlmanacEngine.XiuGroup.WOOD_METAL_EARTH, AlmanacEngine.xiuElementGroup("土"))
        // 未知值走兜底分组
        assertEquals(AlmanacEngine.XiuGroup.WOOD_METAL_EARTH, AlmanacEngine.xiuElementGroup(""))
        assertEquals(AlmanacEngine.XiuGroup.WOOD_METAL_EARTH, AlmanacEngine.xiuElementGroup("不明"))
    }

    @Test
    fun dayBoundaryHelpers_areConsistent() {
        val base = millisOf(2026, 11, 18, 15, 30)
        val start = AlmanacEngine.startOfDay(base)
        val end = AlmanacEngine.endOfDay(base)
        assertEquals(millisOf(2026, 11, 18, 0, 0), start)
        assertEquals(start + 24 * 3600_000L - 1, end)
        assertTrue(start <= base && base <= end)
        assertEquals(1, AlmanacEngine.daysBetween(base, millisOf(2026, 11, 19, 8, 0)))
        assertEquals(0, AlmanacEngine.daysBetween(base, millisOf(2026, 11, 18, 23, 59)))
    }

    @Test
    fun natalChart_sameBirthTwice_returnsEqualChart() {
        val birth = Date(millisOf(1996, 3, 21, 8, 15))
        val a = AlmanacEngine.natalChart(birth)
        val b = AlmanacEngine.natalChart(birth)
        assertEquals(a, b)
        assertTrue(a.fourPillars.isNotBlank())
        assertEquals(birth.time, a.birthMillis)
    }

    @Test
    fun previewBirth_sameArgumentsTwice_returnsEqualChart() {
        val a = AlmanacEngine.previewBirth(1996, 3, 21, 8, 15)
        val b = AlmanacEngine.previewBirth(1996, 3, 21, 8, 15)
        assertEquals(a, b)
        assertEquals(a.birthMillis, b.birthMillis)
    }
}
