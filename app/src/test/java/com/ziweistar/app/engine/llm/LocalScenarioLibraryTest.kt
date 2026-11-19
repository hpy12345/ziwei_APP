package com.ziweistar.app.engine.llm

import com.ziweistar.app.engine.ReportEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 场景兜底文案库的单测。
 *
 * 这些断言锁定的是「卷二不要出现重复句子」这类实测中真实出现的缺陷，
 * 而不是文案本身好不好听——文案可以改，重复不能说。
 */
class LocalScenarioLibraryTest {

    private fun period(): ReportEngine.Period = ReportEngine.Period(
        kind = ReportEngine.Kind.WEEK,
        key = "W20261116",
        label = "2026 年 11 月 16 日 起 · 第 47 周",
        startMillis = 0L,
        endMillis = 1L,
    )

    private fun stats(
        totalCount: Int = 3,
        maxDayCount: Int = 3,
        bestRecoveryDays: Int = 0,
        avgIntervalHours: Double = 0.2,
        avgSatisfaction: Double = 3.5,
        severeCount: Int = 0,
    ): ReportEngine.Stats = ReportEngine.Stats(
        totalCount = totalCount,
        activeDays = 1,
        avgIntervalHours = avgIntervalHours,
        avgDurationMinutes = 0,
        avgSatisfaction = avgSatisfaction,
        bestRecoveryDays = bestRecoveryDays,
        climaxDistribution = emptyList(),
        venueDistribution = emptyList(),
        methodDistribution = emptyList(),
        adverseTop = emptyList(),
        hourHistogram = List(24) { 0 },
        dailyCounts = emptyList(),
        severeCount = severeCount,
        edgeControlCount = 0,
        maxDayCount = maxDayCount,
        streakPeak = 1,
    )

    @Test
    fun periodicScheduleAdvice_doesNotRepeatPostureRule_whenDiagnosisAlreadyMentionsIt() {
        val diagnosis = "本周期有 3 次发生于子丑时。忌盘龙俯卧式床褥压迫。"
        val doc = LocalScenarioLibrary.periodic(
            period = period(),
            stats = stats(),
            isMale = true,
            boneLabel = "匀停骨相",
            diagnosis = diagnosis,
        )
        // 正文已经说过体位守则，节律排程里就不能再说一遍
        val advice = doc.scheduleAdvice.orEmpty()
        assertFalse(
            "节律排程不应重复正文已出现的体位守则，实际：" + advice,
            advice.contains("盘龙俯卧"),
        )
        // 但恢复周期那一段仍要保留
        assertTrue(
            "节律排程仍应包含恢复周期结论，实际：" + advice,
            advice.contains("恢复周期"),
        )
    }

    @Test
    fun periodicScheduleAdvice_keepsPostureRule_whenDiagnosisIsSilent() {
        val doc = LocalScenarioLibrary.periodic(
            period = period(),
            stats = stats(),
            isMale = true,
            boneLabel = "匀停骨相",
            diagnosis = "本周期无特别之处。",
        )
        val advice = doc.scheduleAdvice.orEmpty()
        assertTrue(
            "正文没提体位守则时，节律排程应补上，实际：" + advice,
            advice.contains("盘龙俯卧"),
        )
    }

    @Test
    fun tianggangBody_doesNotDuplicateGenderRule_renderedByOverlay() {
        val doc = LocalScenarioLibrary.tianggang(
            isMale = true,
            qi = 52,
            todayCount = 3,
            boneLabel = "匀停骨相",
            historicalAdverse = listOf("腰膝酸软"),
            weakness = "腰肾气机易因频次过密而暗耗",
        )
        // 黄符 UI 底部固定渲染「乾造之身，宜蓄不宜泄」，正文里不能再写一遍
        assertFalse(
            "天罡批文正文不应重复性别守则（UI 底部已有），实际：" + doc.body,
            doc.body.contains("乾造之身"),
        )
        assertTrue(doc.body.contains("敕令已下"))
    }

    @Test
    fun periodicAlwaysExposesRecoveryOrAccumulationConclusion() {
        val withRecovery = LocalScenarioLibrary.periodic(
            period = period(), stats = stats(bestRecoveryDays = 3),
            isMale = true, boneLabel = "匀停骨相", diagnosis = "略。",
        )
        assertTrue(withRecovery.scheduleAdvice.orEmpty().contains("最佳恢复周期为 3 天"))

        val withoutRecovery = LocalScenarioLibrary.periodic(
            period = period(), stats = stats(bestRecoveryDays = 0),
            isMale = true, boneLabel = "匀停骨相", diagnosis = "略。",
        )
        assertTrue(withoutRecovery.scheduleAdvice.orEmpty().contains("尚不足"))
    }
}
