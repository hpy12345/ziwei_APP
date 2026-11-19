package com.ziweistar.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HashSelector 确定性测试。
 *
 * 设计文档 6.2-2 要求「同一天、同一用户，无论刷新多少次或换任何模型，
 * 必须抽中同一个人/同一项」，因此本测试只断言「同输入 → 同输出」的确定性，
 * 以及边界条件（空列表、n <= 0、range <= 0），不锁死具体哈希值以外无关的实现细节。
 */
class HashSelectorTest {

    @Test
    fun fnv1a32_emptyString_returnsOffsetBasis() {
        // FNV-1a 32bit offset basis = 2166136261（无符号），按 Int 截断为负数
        assertEquals(2166136261L.toInt(), HashSelector.fnv1a32(""))
    }

    @Test
    fun fnv1a32_sameInput_twiceSameResult() {
        val a = HashSelector.fnv1a32("2026-11-18_user-STAR")
        val b = HashSelector.fnv1a32("2026-11-18_user-STAR")
        assertEquals(a, b)
        assertNotEquals(a, HashSelector.fnv1a32("2026-11-19_user-STAR"))
    }

    @Test
    fun select_sameInputTwice_returnsSameItem() {
        val list = listOf("瑶池赤练仙", "广寒白月光", "清心明月", "洛水惊鸿", "巫山云雨")
        val first = HashSelector.select(list, "2026-11-18", "user-0001", "STAR")
        val second = HashSelector.select(list, "2026-11-18", "user-0001", "STAR")
        assertEquals(first, second)
        assertNotNull("非空列表不应返回 null", first)
        assertTrue("picked=$first 必须来自候选列表", first != null && list.contains(first))
    }

    @Test
    fun select_emptyList_returnsNull() {
        assertNull(HashSelector.select(emptyList<String>(), "2026-11-18", "u", "STAR"))
    }

    @Test
    fun select_indexAlwaysInsideListBounds() {
        val list = (1..7).map { "item-$it" }
        for (day in 1..28) {
            for (salt in listOf("STAR", "MEDIUM", "METHOD", "REALM")) {
                val picked = HashSelector.select(list, "2026-11-%02d".format(day), "user-x", salt)
                assertTrue("picked=$picked must come from list", picked != null && list.contains(picked))
            }
        }
    }

    @Test
    fun selectDistinct_returnsRequestedCountAndIsDeterministic() {
        val list = (1..12).map { "method-$it" }
        val first = HashSelector.selectDistinct(list, 3, "2026-11-18", "user-0001", "METHOD")
        val second = HashSelector.selectDistinct(list, 3, "2026-11-18", "user-0001", "METHOD")
        assertEquals(3, first.size)
        assertEquals(first, second)
        assertEquals(first.distinct(), first)
        assertTrue(list.containsAll(first))
    }

    @Test
    fun selectDistinct_degenerateInputs_returnEmpty() {
        assertTrue(HashSelector.selectDistinct(emptyList<String>(), 3, "2026-11-18", "u", "S").isEmpty())
        assertTrue(HashSelector.selectDistinct(listOf("a", "b"), 0, "2026-11-18", "u", "S").isEmpty())
        // n 超过列表长度时最多返回全部元素
        assertEquals(2, HashSelector.selectDistinct(listOf("a", "b"), 5, "2026-11-18", "u", "S").size)
    }

    @Test
    fun roll_sameInputTwice_sameValueWithinRange() {
        val a = HashSelector.roll("2026-11-18", "user-0001", "VERSE")
        val b = HashSelector.roll("2026-11-18", "user-0001", "VERSE")
        assertEquals(a, b)
        assertTrue("roll=$a must be in 0..99", a in 0..99)
        for (seed in 1..50) {
            val v = HashSelector.roll("2026-11-18", "u-$seed", "VERSE", range = 7)
            assertTrue("v=$v must be in 0..6", v in 0..6)
        }
    }

    @Test
    fun roll_nonPositiveRange_returnsZero() {
        assertEquals(0, HashSelector.roll("2026-11-18", "u", "VERSE", range = 0))
        assertEquals(0, HashSelector.roll("2026-11-18", "u", "VERSE", range = -5))
    }
}
