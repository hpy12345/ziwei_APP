package com.ziweistar.app.engine

import com.ziweistar.app.engine.model.AlmanacSnapshot
import com.ziweistar.app.engine.model.BoneType
import com.ziweistar.app.engine.model.CaveType
import com.ziweistar.app.engine.model.CupSize
import com.ziweistar.app.engine.model.DragonShape
import com.ziweistar.app.engine.model.Fortune
import com.ziweistar.app.engine.model.Gender
import com.ziweistar.app.engine.model.JadeGateShape
import com.ziweistar.app.engine.model.MenstrualPhase
import com.ziweistar.app.engine.model.MethodLibrary
import com.ziweistar.app.engine.model.NatalChart
import com.ziweistar.app.engine.model.PartnerFrequency
import com.ziweistar.app.engine.model.PhallusTier
import com.ziweistar.app.engine.model.PrivacyTier
import com.ziweistar.app.engine.model.RelationshipStatus
import com.ziweistar.app.engine.model.StarAttribute
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * DecisionEngine 本地确定性规则引擎测试。
 *
 * 设计文档 6.2 要求「同一天、同一用户、同一状态」必须得到完全相同的 DecisionLock，
 * 因此这里对同一 EngineInput 连续调用两次，逐字段断言结果一致。
 */
class DecisionEngineTest {

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

    private fun fixedDate(): Date = Date(
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 18, 12, 0, 0)
        }.timeInMillis,
    )

    private fun almanac(): AlmanacSnapshot = AlmanacEngine.snapshot(fixedDate())

    private fun natalChart(): NatalChart = AlmanacEngine.natalChart(
        Date(
            Calendar.getInstance().apply {
                clear()
                set(1996, Calendar.MARCH, 21, 8, 15, 0)
            }.timeInMillis,
        ),
    )

    /** 乾造：授权完整、状态正常、图鉴为空 */
    private fun maleInput(): EngineInput = EngineInput(
        userId = "user-0001",
        gender = Gender.QIAN,
        natal = natalChart(),
        relationship = RelationshipStatus.PARTNERED,
        partnerFrequency = PartnerFrequency.WEEKLY,
        privacyTier = PrivacyTier.FULL,
        heightCm = 178,
        weightKg = 70,
        boneType = BoneType.YUNTING,
        flaccidLengthCm = 9.0,
        erectLengthCm = 16.0,
        erectGirthCm = 12.5,
        qi = 88,
        streakDays = 0,
        todayCount = 0,
        yesterdayCount = 0,
        hoursSinceLast = 30.0,
        almanac = almanac(),
    )

    /** 坤造：授权完整、行经期平常、图鉴为空 */
    private fun femaleInput(): EngineInput = EngineInput(
        userId = "user-0002",
        gender = Gender.KUN,
        natal = natalChart(),
        relationship = RelationshipStatus.SINGLE,
        partnerFrequency = PartnerFrequency.CHASTE,
        privacyTier = PrivacyTier.LOCAL_ONLY,
        heightCm = 165,
        weightKg = 52,
        boneType = BoneType.QINGQI,
        cupSize = CupSize.C,
        jadeGateShape = JadeGateShape.BUTTERFLY,
        menstrualPhase = MenstrualPhase.NONE,
        qi = 92,
        almanac = almanac(),
    )

    private fun starFigures() = listOf(
        StarFigure(id = "b-uuid", name = "洛水惊鸿", attribute = StarAttribute.FAIRY),
        StarFigure(id = "a-uuid", name = "瑶池赤练", attribute = StarAttribute.PASSION),
        StarFigure(id = "c-uuid", name = "旧梦故人", attribute = StarAttribute.PURE_LOVE),
    )

    @Test
    fun compute_maleInputTwice_isFullyDeterministic() {
        val input = maleInput()
        val first = DecisionEngine.compute(input)
        val second = DecisionEngine.compute(input)

        assertEquals(first.rulePathId, second.rulePathId)
        assertEquals(first.fortune, second.fortune)
        assertEquals(first.trigram, second.trigram)
        assertEquals(first.cave.label, second.cave.label)
        assertEquals(first.cave.caveId, second.cave.caveId)
        assertEquals(first.cave.anchors, second.cave.anchors)
        assertEquals(first.star.name, second.star.name)
        assertEquals(first.star.attribute, second.star.attribute)
        assertEquals(first.star.starId, second.star.starId)
        assertEquals(first.method.labels, second.method.labels)
        assertEquals(first.method.sectClass, second.method.sectClass)
        assertEquals(first.method.anchors, second.method.anchors)
        assertEquals(first.realm.themeId, second.realm.themeId)
        assertEquals(first.realm.anchors, second.realm.anchors)
        // data class 整体相等
        assertEquals(first, second)
    }

    @Test
    fun compute_femaleInputTwice_isFullyDeterministic() {
        val input = femaleInput()
        val first = DecisionEngine.compute(input)
        val second = DecisionEngine.compute(input)
        assertEquals(first.rulePathId, second.rulePathId)
        assertEquals(first.fortune, second.fortune)
        assertEquals(first.cave.label, second.cave.label)
        assertEquals(first.star.name, second.star.name)
        assertEquals(first.method.labels, second.method.labels)
        assertEquals(first, second)
    }

    @Test
    fun compute_withStarFigures_starPickIsDeterministic() {
        val input = maleInput().copy(starFigures = starFigures())
        val first = DecisionEngine.compute(input)
        val second = DecisionEngine.compute(input)
        assertEquals(first.star, second.star)
        assertEquals(first.star.name, second.star.name)
        // 星宿必须来自图鉴
        assertTrue(
            "star.name=${first.star.name} 应来自图鉴",
            starFigures().any { it.name == first.star.name },
        )
        assertNotNull(first.star.starId)
    }

    @Test
    fun compute_outputFieldsAreNonBlank() {
        val lock = DecisionEngine.compute(maleInput())
        assertTrue(lock.rulePathId.isNotBlank())
        assertTrue(lock.fortune.isNotBlank())
        assertTrue(lock.trigram.isNotBlank())
        assertTrue(lock.cave.label.isNotBlank())
        assertTrue(lock.cave.caveId.isNotBlank())
        assertTrue(lock.cave.anchors.isNotEmpty())
        assertTrue(lock.method.labels.isNotEmpty())
        assertTrue(lock.method.sectClass.isNotBlank())
        assertTrue(lock.method.anchors.isNotEmpty())
        assertTrue(lock.star.name.isNotBlank())
        assertTrue(lock.star.attribute.isNotBlank())
        assertTrue(lock.star.anchors.isNotEmpty())
        assertTrue(lock.realm.theme.isNotBlank())
        assertTrue(lock.realm.themeId.isNotBlank())
        assertTrue(lock.realm.anchors.isNotEmpty())
    }

    @Test
    fun compute_qiDepleted_hitsP0QiPath() {
        // qi < 35 → P0_QI_DEPLETED（P0 最高优先级，先于 todayCount 等判定）
        val lock = DecisionEngine.compute(maleInput().copy(qi = 20))
        assertEquals("P0_QI_DEPLETED", lock.rulePathId)
        assertEquals(Fortune.OMINOUS.fullLabel, lock.fortune)
        // 大凶 → 洞府退回清心卧榻、功法走闭关静心
        assertEquals(CaveType.RETREAT.label, lock.cave.label)
        assertEquals(MethodLibrary.RETREAT_METHODS, lock.method.labels)
    }

    @Test
    fun compute_todayOverload_hasHigherPriorityThanQi() {
        val lock = DecisionEngine.compute(maleInput().copy(todayCount = 2, qi = 20))
        assertEquals("P0_TODAY_OVERLOAD", lock.rulePathId)
    }

    @Test
    fun compute_highFreqPartnerWithin48h_hitsP1() {
        val lock = DecisionEngine.compute(
            maleInput().copy(
                partnerFrequency = PartnerFrequency.NIGHTLY,
                hoursSinceLast = 12.0,
            ),
        )
        assertEquals("P1_HIGH_FREQ_PARTNER", lock.rulePathId)
        assertEquals(Fortune.NEUTRAL.fullLabel, lock.fortune)
    }

    @Test
    fun compute_menstrualProtection_forFemale() {
        val lock = DecisionEngine.compute(
            femaleInput().copy(
                menstrualPhase = MenstrualPhase.PERIOD,
                periodDiscomfort = true,
            ),
        )
        assertEquals("P0_MENSTRUAL_PROTECTION", lock.rulePathId)
        assertEquals(Fortune.OMINOUS.fullLabel, lock.fortune)
    }

    @Test
    fun compute_rulePathIdMatchesFortuneConsistency() {
        // 大凶路径必为大凶印；非大凶路径不该出现 OMINOUS 标签
        val ominousPaths = listOf(
            maleInput().copy(qi = 10),
            maleInput().copy(streakDays = 5),
            maleInput().copy(lastHadSevereAdverse = true),
        )
        for (input in ominousPaths) {
            val lock = DecisionEngine.compute(input)
            assertTrue("path=${lock.rulePathId}", lock.rulePathId.startsWith("P0"))
            assertEquals(Fortune.OMINOUS.fullLabel, lock.fortune)
            assertEquals(Fortune.OMINOUS.trigram, lock.trigram)
        }
    }

    @Test
    fun compute_sameInputExceptUserId_isStablePerUser() {
        // 不同 userId 允许结果不同，但各自必须稳定
        val a = maleInput().copy(userId = "user-A", starFigures = starFigures())
        val b = maleInput().copy(userId = "user-B", starFigures = starFigures())
        assertEquals(DecisionEngine.compute(a), DecisionEngine.compute(a))
        assertEquals(DecisionEngine.compute(b), DecisionEngine.compute(b))
    }

    @Test
    fun phallusTierResolver_boundaries() {
        assertEquals(PhallusTier.MAJESTIC, PhallusTierResolver.resolve(15.0, 0.0))
        assertEquals(PhallusTier.MAJESTIC, PhallusTierResolver.resolve(0.0, 13.0))
        assertEquals(PhallusTier.REFINED, PhallusTierResolver.resolve(11.99, 10.0))
        assertEquals(PhallusTier.BALANCED, PhallusTierResolver.resolve(12.0, 11.0))
        assertEquals(PhallusTier.BALANCED, PhallusTierResolver.resolve(0.0, 0.0))
    }

    @Test
    fun dragonShapeResolver_ratio() {
        assertEquals(DragonShape.BLOOD, PhallusTierResolver.dragonShape(8.0, 14.0))
        assertEquals(DragonShape.BONE, PhallusTierResolver.dragonShape(10.0, 14.0))
        assertNull(PhallusTierResolver.dragonShape(0.0, 14.0))
        assertNull(PhallusTierResolver.dragonShape(8.0, 0.0))
    }
}
