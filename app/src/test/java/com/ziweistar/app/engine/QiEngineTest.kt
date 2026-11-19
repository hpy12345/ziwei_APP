package com.ziweistar.app.engine

import com.ziweistar.app.engine.model.AdverseEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QiEngine 元气值算法测试。
 *
 * 语义（见 QiEngine 源码注释）：
 *   Q = Clamp(100 - Σ(B_i × M_i × D_i + P_i) × e^(-λΔt_i), 0, 100)
 * 空记录列表 → 无任何损耗 → 元气满值 100。
 */
class QiEngineTest {

    private val now = 1_800_000_000_000L

    private fun entry(
        climax: ClimaxClass = ClimaxClass.FULL,
        coefficients: List<Double> = listOf(1.0),
        durationMinutes: Int = 30,
        adverse: Set<AdverseEffect> = emptySet(),
        hoursAgo: Double = 1.0,
    ) = QiContribution(
        timestampMillis = now - (hoursAgo * 3_600_000.0).toLong(),
        climaxClass = climax,
        methodCoefficients = coefficients,
        durationMinutes = durationMinutes,
        adverse = adverse,
    )

    @Test
    fun currentQi_emptyEntries_isFullValue() {
        assertEquals(100, QiEngine.currentQi(emptyList(), now))
    }

    @Test
    fun currentQi_singleEntry_dropsQi() {
        val withOne = QiEngine.currentQi(listOf(entry()), now)
        assertTrue("元气应低于满值 100，实际 $withOne", withOne < 100)
        assertTrue("元气不应低于 0，实际 $withOne", withOne >= 0)
    }

    @Test
    fun currentQi_isMonotonicInEntryCount() {
        val one = QiEngine.currentQi(listOf(entry(hoursAgo = 1.0)), now)
        val two = QiEngine.currentQi(
            listOf(entry(hoursAgo = 1.0), entry(hoursAgo = 2.0)),
            now,
        )
        val three = QiEngine.currentQi(
            listOf(entry(hoursAgo = 1.0), entry(hoursAgo = 2.0), entry(hoursAgo = 3.0)),
            now,
        )
        assertTrue("一条 $one 应低于满值", one < 100)
        assertTrue("两条 $two 应不高于一条 $one", two <= one)
        assertTrue("三条 $three 应不高于两条 $two", three <= two)
    }

    @Test
    fun currentQi_sameInputTwice_isDeterministic() {
        val entries = listOf(entry(hoursAgo = 1.0), entry(climax = ClimaxClass.EDGE, hoursAgo = 20.0))
        assertEquals(
            QiEngine.currentQi(entries, now),
            QiEngine.currentQi(entries, now),
        )
    }

    @Test
    fun currentQi_ignoresEntriesOutsideWindowOrInFuture() {
        val tooOld = entry(hoursAgo = QiEngine.WINDOW_HOURS + 1.0)
        val inFuture = entry(hoursAgo = -2.0)
        // 过期记录与未来记录都不计入损耗 → 元气满值
        assertEquals(100, QiEngine.currentQi(listOf(tooOld, inFuture), now))
        // 对照：窗口内的同类记录确实会掉元气
        assertTrue(QiEngine.currentQi(listOf(entry(hoursAgo = 1.0)), now) < 100)
    }

    @Test
    fun decayedLoss_decaysWithElapsedHours() {
        val fresh = QiEngine.decayedLoss(ClimaxClass.FULL, listOf(1.0), 30, emptySet(), 0.0)
        val older = QiEngine.decayedLoss(ClimaxClass.FULL, listOf(1.0), 30, emptySet(), 24.0)
        assertTrue("fresh=$fresh older=$older", older < fresh)
        assertTrue(fresh > 0.0)
    }

    // ---------- 静养回补（谨遵法旨 +5 元气） ----------
    // 实测缺陷：曾把「闭关养神」当普通记录插入，climax=ABORT 被算成 4 点损耗，
    // 于是点「谨遵法旨（+5 元气）」元气反而下降。以下用例锁定该回归。

    private fun restEntry(hoursAgo: Double = 1.0, restore: Double = 5.0) = QiContribution(
        timestampMillis = now - (hoursAgo * 3_600_000.0).toLong(),
        climaxClass = ClimaxClass.ABORT,
        methodCoefficients = listOf(1.0),
        durationMinutes = 1,
        adverse = emptySet(),
        qiRestore = restore,
    )

    @Test
    fun currentQi_restEntry_restoresInsteadOfDraining() {
        // 一条真实起坛把元气打下去
        val drained = QiEngine.currentQi(listOf(entry(climax = ClimaxClass.FULL, hoursAgo = 1.0)), now)
        assertTrue("起坛后元气应下降，实际 $drained", drained < 100)

        // 追加一条静养记录后，元气必须回升（而不是被 ABORT 再扣一次）
        val restored = QiEngine.currentQi(
            listOf(entry(climax = ClimaxClass.FULL, hoursAgo = 1.0), restEntry(hoursAgo = 0.5)),
            now,
        )
        assertTrue(
            "静养记录应回升元气：before=$drained after=$restored",
            restored > drained,
        )
    }

    @Test
    fun currentQi_restEntry_neverExceedsHundred() {
        val qi = QiEngine.currentQi(listOf(restEntry(hoursAgo = 1.0, restore = 999.0)), now)
        assertEquals("元气上限必须封顶 100", 100, qi)
    }

    @Test
    fun currentQi_zeroRestore_keepsOriginalLossSemantics() {
        // qiRestore 默认为 0，普通记录必须仍按损耗计算（不能因为新字段改变旧行为）
        val withDefault = QiContribution(
            timestampMillis = now - 3_600_000L,
            climaxClass = ClimaxClass.FULL,
            methodCoefficients = listOf(1.0),
            durationMinutes = 30,
            adverse = emptySet(),
        )
        val qi = QiEngine.currentQi(listOf(withDefault), now)
        assertTrue("普通记录 qiRestore=0 时仍应扣元气，实际 $qi", qi < 100)
    }

    @Test
    fun baseLoss_matchesDesignDocument() {
        assertEquals(22, QiEngine.baseLoss(ClimaxClass.FULL))
        assertEquals(10, QiEngine.baseLoss(ClimaxClass.EDGE))
        assertEquals(4, QiEngine.baseLoss(ClimaxClass.ABORT))
    }

    @Test
    fun durationFactor_boundaries() {
        assertEquals(0.9, QiEngine.durationFactor(0), 1e-9)
        assertEquals(0.9, QiEngine.durationFactor(14), 1e-9)
        assertEquals(1.0, QiEngine.durationFactor(15), 1e-9)
        assertEquals(1.0, QiEngine.durationFactor(40), 1e-9)
        assertEquals(1.35, QiEngine.durationFactor(41), 1e-9)
    }

    @Test
    fun adversePenalty_severeAndFatigue() {
        assertEquals(0, QiEngine.adversePenalty(emptySet()))
        assertEquals(25, QiEngine.adversePenalty(setOf(AdverseEffect.WAIST)))
        assertEquals(25, QiEngine.adversePenalty(setOf(AdverseEffect.DIZZY)))
        assertEquals(25, QiEngine.adversePenalty(setOf(AdverseEffect.WEAR)))
        assertEquals(15, QiEngine.adversePenalty(setOf(AdverseEffect.FATIGUE)))
        // 严重项只计一次 + 疲劳独立累加
        assertEquals(
            40,
            QiEngine.adversePenalty(setOf(AdverseEffect.WAIST, AdverseEffect.DIZZY, AdverseEffect.FATIGUE)),
        )
        // NONE / GUILT / DISTRACT 无惩罚
        assertEquals(0, QiEngine.adversePenalty(setOf(AdverseEffect.NONE, AdverseEffect.GUILT, AdverseEffect.DISTRACT)))
    }

    @Test
    fun qiLabel_and_qiBand_areStepwise() {
        // 阈值：>=85 充盈 / >=70 尚足 / >=55 平稳 / >=40 微弱 / >=20 枯竭 / else 将尽
        assertEquals("充盈", QiEngine.qiLabel(100))
        assertEquals("充盈", QiEngine.qiLabel(85))
        assertEquals("尚足", QiEngine.qiLabel(84))
        assertEquals("尚足", QiEngine.qiLabel(70))
        assertEquals("平稳", QiEngine.qiLabel(69))
        assertEquals("平稳", QiEngine.qiLabel(55))
        assertEquals("微弱", QiEngine.qiLabel(54))
        assertEquals("微弱", QiEngine.qiLabel(40))
        assertEquals("枯竭", QiEngine.qiLabel(39))
        assertEquals("枯竭", QiEngine.qiLabel(20))
        assertEquals("将尽", QiEngine.qiLabel(19))
        assertEquals("将尽", QiEngine.qiLabel(0))

        // 配色分档：>=70 FULL / >=35 MODERATE / else DEPLETED
        assertEquals(QiEngine.QiBand.FULL, QiEngine.qiBand(100))
        assertEquals(QiEngine.QiBand.FULL, QiEngine.qiBand(70))
        assertEquals(QiEngine.QiBand.MODERATE, QiEngine.qiBand(69))
        assertEquals(QiEngine.QiBand.MODERATE, QiEngine.qiBand(35))
        assertEquals(QiEngine.QiBand.DEPLETED, QiEngine.qiBand(34))
    }

    @Test
    fun recoveryHours_matchesDocumentedApproximation() {
        // 注释给出 ln(100/15)/0.035 ≈ 54.3h
        assertEquals(54.3, QiEngine.recoveryHours(85), 0.2)
        assertTrue(QiEngine.recoveryHours(85) > 0.0)
        // 目标值越高（越接近 100），可恢复的剩余量越小，所需恢复时长越长
        assertTrue(QiEngine.recoveryHours(85) < QiEngine.recoveryHours(95))
    }
}
