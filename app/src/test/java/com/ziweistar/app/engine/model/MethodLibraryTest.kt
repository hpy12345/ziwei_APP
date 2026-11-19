package com.ziweistar.app.engine.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MethodLibrary 标准化功法全集测试。
 *
 * 设计文档第四章要求功法只能从系统内置全集选取（禁止手打），
 * 因此这里校验数量下限、编码/名称唯一性、按名查表与宗派/性别映射。
 */
class MethodLibraryTest {

    @Test
    fun library_containsAtLeast29Methods() {
        val all = MethodLibrary.all()
        assertTrue("功法总数 ${all.size} 应 >= 29", all.size >= 29)
        assertEquals(MethodLibrary.MALE.size + MethodLibrary.FEMALE.size, all.size)
        assertTrue("乾造功法 ${MethodLibrary.MALE.size} 应 > 0", MethodLibrary.MALE.isNotEmpty())
        assertTrue("坤造功法 ${MethodLibrary.FEMALE.size} 应 > 0", MethodLibrary.FEMALE.isNotEmpty())
    }

    @Test
    fun byName_firstMethodName_resolvesToMethod() {
        val first = MethodLibrary.MALE.first()
        val found = MethodLibrary.byName(first.name)
        assertNotNull("byName(${first.name}) 不应为 null", found)
        assertEquals(first, found)
        // 坤造首个功法同样可查回
        val firstFemale = MethodLibrary.FEMALE.first()
        assertEquals(firstFemale, MethodLibrary.byName(firstFemale.name))
    }

    @Test
    fun byName_unknownName_returnsNull() {
        assertNull(MethodLibrary.byName("不存在的功法"))
        assertNull(MethodLibrary.byName(""))
        // 大凶专用功法不在统计库中
        for (retreat in MethodLibrary.RETREAT_METHODS) {
            assertNull("$retreat 不应在统计库中", MethodLibrary.byName(retreat))
        }
    }

    @Test
    fun methodCodesAndNames_areUnique() {
        val all = MethodLibrary.all()
        assertEquals("code 应唯一", all.size, all.map { it.code }.distinct().size)
        assertEquals("name 应唯一", all.size, all.map { it.name }.distinct().size)
    }

    @Test
    fun forGender_returnsGenderSpecificLists() {
        assertEquals(MethodLibrary.MALE, MethodLibrary.forGender(true))
        assertEquals(MethodLibrary.FEMALE, MethodLibrary.forGender(false))
        assertTrue("乾造功法应全部 forMale", MethodLibrary.MALE.all { it.forMale })
        assertTrue("坤造功法应全部 !forMale", MethodLibrary.FEMALE.all { !it.forMale })
    }

    @Test
    fun coefficients_arePositiveAndBounded() {
        for (m in MethodLibrary.all()) {
            assertTrue("${m.name} 系数 ${m.coefficient} 应 > 0", m.coefficient > 0.0)
            assertTrue("${m.name} 系数 ${m.coefficient} 应在合理区间", m.coefficient in 0.1..3.0)
        }
    }

    @Test
    fun coefficientOf_matchesLookup_andDefaultsToOne() {
        val m = MethodLibrary.MALE.first { it.code == "M_S01" } // 锁龙御气 0.6
        assertEquals(m.coefficient, MethodLibrary.coefficientOf(m.name), 1e-9)
        assertEquals(1.0, MethodLibrary.coefficientOf("不存在的功法"), 1e-9)
    }

    @Test
    fun sectOf_resolvesFourSects() {
        assertEquals(MethodSect.SECRET, MethodLibrary.sectOf("锁龙御气"))
        assertEquals(MethodSect.UNARMED, MethodLibrary.sectOf("五指正宗"))
        assertEquals(MethodSect.BODY, MethodLibrary.sectOf("盘龙俯卧"))
        assertEquals(MethodSect.TOOL, MethodLibrary.sectOf("天机电动"))
        assertNull(MethodLibrary.sectOf("不存在的功法"))
        assertTrue(
            "四大宗派都应至少有一个功法",
            MethodSect.entries.all { sect -> MethodLibrary.all().any { it.sect == sect } },
        )
    }

    @Test
    fun highRiskMethod_isFlagged() {
        val longWo = MethodLibrary.byName("盘龙俯卧")
        assertNotNull(longWo)
        assertTrue("盘龙俯卧应标记高危", longWo!!.highRisk)
        assertTrue("其余功法不应误标高危", MethodLibrary.all().count { it.highRisk } < 5)
    }

    @Test
    fun retreatMethods_areTheTwoDetoxMethods() {
        assertEquals(listOf("闭关打坐", "吐纳静心"), MethodLibrary.RETREAT_METHODS)
    }

    @Test
    fun lookingUpByName_isStable() {
        for (m in MethodLibrary.all()) {
            assertEquals(m, MethodLibrary.byName(m.name))
            assertEquals(MethodLibrary.byName(m.name), MethodLibrary.byName(m.name))
        }
    }
}
