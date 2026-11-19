package com.ziweistar.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「不知道出生时辰」按子时（00:00）推演的行为验证（用户要求第 3 项）。
 *
 * 约定：勾选「时辰不详」后 draft 会把 birthHour / birthMinute 归零，
 * 因此这里直接校验 0 点排盘结果落在子时，且四柱中的时柱为庚子（丁日五鼠遁）。
 */
class PreviewBirthUnknownHourTest {

    @Test
    fun `零时零分排盘落在子时`() {
        val natal = AlmanacEngine.previewBirth(1995, 6, 15, 0, 0)
        assertTrue(
            "实际: ${natal.lunarBirthText}",
            natal.lunarBirthText.endsWith("子时"),
        )
    }

    @Test
    fun `丁日零时应为庚子时`() {
        val natal = AlmanacEngine.previewBirth(1995, 6, 15, 0, 0)
        // 五鼠遁：丁壬庚子居 → 丁日 00:00 时柱为庚子
        assertTrue(
            "实际四柱: ${natal.fourPillars}",
            natal.fourPillars.contains("庚子时"),
        )
    }

    @Test
    fun `上午一时仍属于子时`() {
        val natal = AlmanacEngine.previewBirth(1995, 6, 15, 0, 30)
        assertTrue(
            "实际: ${natal.lunarBirthText}",
            natal.lunarBirthText.endsWith("子时"),
        )
    }

    @Test
    fun `同一日的年柱月柱日柱不受时辰归零影响`() {
        val zero = AlmanacEngine.previewBirth(1995, 6, 15, 0, 0)
        val noon = AlmanacEngine.previewBirth(1995, 6, 15, 12, 0)
        assertEquals(zero.yearPillar, noon.yearPillar)
        assertEquals(zero.monthPillar, noon.monthPillar)
        assertEquals(zero.dayPillar, noon.dayPillar)
        // 时柱必须不同
        assertTrue(zero.timePillar != noon.timePillar)
    }
}
