package com.ziweistar.app.engine

import com.ziweistar.app.engine.llm.LlmRenderer
import com.ziweistar.app.engine.llm.LocalScenarioLibrary
import com.ziweistar.app.engine.llm.Prompts
import com.ziweistar.app.engine.llm.ScenarioDoc
import com.ziweistar.app.engine.llm.ScenarioPayloadBuilder
import com.ziweistar.app.engine.llm.SceneType
import com.ziweistar.app.engine.model.Gender
import com.ziweistar.app.engine.model.PrivacyTier

/**
 * ============================================================
 * 场景批文·双擎流水线
 *
 *  本地确定性统计（ScenarioPayloadBuilder）
 *        ↓
 *  LLM 受控渲染（temperature=0 / seed 锁定 / response_format json_object）
 *        ↓
 *  字段校验（缺字段即视为失败）
 *        ↓
 *  失败 / 未配置 / DECLINE 隐私模式 → LocalScenarioLibrary 0ms 兜底
 *
 * 设计文档 6.3.2：场景批文与每日灵签共用同一套「零偏移」哲学——
 * LLM 只做文学润色，所有统计数字与结论均由本地算定。
 * ============================================================
 */
object ScenarioPipeline {

    /** 场景调用是否需要走远程通路 */
    private fun allowLlm(renderer: LlmRenderer?, privacyTier: PrivacyTier): Boolean =
        renderer != null && renderer.isConfigured && privacyTier != PrivacyTier.DECLINE

    /** 字段非空校验：任一必备字段为空即降级 */
    private fun ScenarioDoc.isUsable(): Boolean = when (scene) {
        SceneType.SECOND_BLOOM -> body.isNotBlank()
        SceneType.TIANGANG_WARNING -> body.isNotBlank() && title.isNotBlank()
        SceneType.PERIODIC_REPORT -> body.isNotBlank() && pulse.isNotBlank()
    }

    // ---------------- 梅开二度 ----------------

    fun secondBloom(
        input: EngineInput,
        renderer: LlmRenderer?,
        privacyTier: PrivacyTier,
        physiqueFeature: String,
        firstStarName: String,
        secondStarName: String,
        firstMethods: List<String>,
        secondMethods: List<String>,
        hoursSinceLast: Double?,
        todayCount: Int,
        dayKey: String,
    ): ScenarioDoc {
        if (allowLlm(renderer, privacyTier)) {
            val payload = ScenarioPayloadBuilder.buildSecondBloom(
                input = input,
                physiqueFeature = physiqueFeature,
                firstStarName = firstStarName,
                secondStarName = secondStarName,
                firstMethods = firstMethods,
                secondMethods = secondMethods,
                hoursSinceLast = hoursSinceLast,
                todayCount = todayCount,
            )
            val doc = runCatching {
                val raw = renderer!!.completeJson(
                    Prompts.SCENARIO_SYSTEM,
                    Prompts.scenarioUser(payload),
                    seedOf(dayKey),
                )
                ScenarioPayloadBuilder.parse(SceneType.SECOND_BLOOM, raw)
            }.getOrNull()
            if (doc != null && doc.isUsable()) return doc
        }
        return LocalScenarioLibrary.secondBloom(
            isMale = input.gender.isMale,
            qi = input.qi,
            todayCount = todayCount,
            hoursSinceLast = hoursSinceLast,
            secondStarName = secondStarName,
            secondMethods = secondMethods,
        )
    }

    // ---------------- 天罡戒律 ----------------

    fun tianggang(
        input: EngineInput,
        renderer: LlmRenderer?,
        privacyTier: PrivacyTier,
        physiqueFeature: String,
        todayCount: Int,
        historicalAdverse: List<String>,
        dayKey: String,
    ): ScenarioDoc {
        if (allowLlm(renderer, privacyTier)) {
            val payload = ScenarioPayloadBuilder.buildTianggang(
                input = input,
                physiqueFeature = physiqueFeature,
                todayCount = todayCount,
                historicalAdverse = historicalAdverse,
            )
            val doc = runCatching {
                val raw = renderer!!.completeJson(
                    Prompts.SCENARIO_SYSTEM,
                    Prompts.scenarioUser(payload),
                    seedOf(dayKey),
                )
                ScenarioPayloadBuilder.parse(SceneType.TIANGANG_WARNING, raw)
            }.getOrNull()
            if (doc != null && doc.isUsable()) return doc
        }
        return LocalScenarioLibrary.tianggang(
            isMale = input.gender.isMale,
            qi = input.qi,
            todayCount = todayCount,
            boneLabel = input.boneType.classicLabel,
            historicalAdverse = historicalAdverse,
            weakness = ScenarioPayloadBuilder.weaknessOf(input.gender),
        )
    }

    // ---------------- 周报 / 月报 ----------------

    fun periodic(
        input: EngineInput,
        renderer: LlmRenderer?,
        privacyTier: PrivacyTier,
        physiqueFeature: String,
        period: ReportEngine.Period,
        stats: ReportEngine.Stats,
        localDiagnosis: String,
        dayKey: String,
    ): ScenarioDoc {
        if (allowLlm(renderer, privacyTier) && stats.hasData) {
            val payload = ScenarioPayloadBuilder.buildPeriodic(
                input = input,
                physiqueFeature = physiqueFeature,
                period = period,
                stats = stats,
            )
            val doc = runCatching {
                val raw = renderer!!.completeJson(
                    Prompts.SCENARIO_SYSTEM,
                    Prompts.scenarioUser(payload),
                    seedOf(dayKey),
                )
                ScenarioPayloadBuilder.parse(SceneType.PERIODIC_REPORT, raw)
            }.getOrNull()
            if (doc != null && doc.isUsable()) {
                // 推荐功法强制落到本地功法库，防止 LLM 编造不存在的功法名
                val safe = doc.recommendMethod
                val fixed = if (com.ziweistar.app.engine.model.MethodLibrary.byName(safe) != null) {
                    doc
                } else {
                    doc.copy(
                        recommendMethod = com.ziweistar.app.engine.model.MethodLibrary
                            .forGender(input.gender.isMale).firstOrNull()?.name.orEmpty(),
                    )
                }
                return fixed
            }
        }
        return LocalScenarioLibrary.periodic(
            period = period,
            stats = stats,
            isMale = input.gender.isMale,
            boneLabel = input.boneType.classicLabel,
            diagnosis = localDiagnosis,
        )
    }

    /** seed = YYYYMMDD（与每日灵签一致），保证同日同场景可复现 */
    private fun seedOf(dayKey: String): Int =
        dayKey.replace("-", "").toIntOrNull() ?: 20260101

    /** 供 UI 判断性别标签（避免各页面重复写三元） */
    fun genderLabel(gender: Gender): String = gender.classicLabel
}
