package com.ziweistar.app.engine

import com.ziweistar.app.engine.llm.LlmRenderer
import com.ziweistar.app.engine.llm.OracleValidator
import com.ziweistar.app.engine.llm.PromptPayloadBuilder
import com.ziweistar.app.engine.llm.Prompts
import com.ziweistar.app.engine.model.AdverseEffect
import com.ziweistar.app.engine.model.AlmanacSnapshot
import com.ziweistar.app.engine.model.BoneType
import com.ziweistar.app.engine.model.CupSize
import com.ziweistar.app.engine.model.DecisionLock
import com.ziweistar.app.engine.model.Gender
import com.ziweistar.app.engine.model.JadeGateShape
import com.ziweistar.app.engine.model.NatalChart
import com.ziweistar.app.engine.model.PartnerFrequency
import com.ziweistar.app.engine.model.RelationshipStatus
import com.ziweistar.app.engine.model.RenderedOracle
import kotlinx.serialization.json.Json

/**
 * 双擎确定性流水线门面（Dual-Engine Deterministic Pipeline）
 *
 *  本地规则引擎 → DecisionLock（100% 确定性）
 *        ↓
 *  LLM 受控渲染（失败/超时 → 本地古籍模板库 0ms 兜底）
 *        ↓
 *  输出校验器（标签强制以铁券覆盖）
 */
object OraclePipeline {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    /** 占位符替换表 */
    fun templateVars(
        input: EngineInput,
        physiqueFeature: String,
    ): Map<String, String> = mapOf(
        "ganzhi" to input.almanac.ganZhiDay,
        "xiu" to "${input.almanac.xiu}宿",
        "qi" to input.qi.toString(),
        "streak" to input.streakDays.toString(),
        "bone" to input.boneType.classicLabel,
        "physique" to physiqueFeature,
        "starName" to input.almanac.let { "" }, // 由调用方覆盖
        "starAttr" to "",
    )

    /**
     * 完整生成今日灵签。
     *
     * @param renderer 可空；为 null 或未配置时直接走本地模板库
     * @param allowLlm 纯本地隐私模式下传 false，强制离线
     */
    fun generate(
        input: EngineInput,
        renderer: LlmRenderer?,
        allowLlm: Boolean,
        dayKey: String,
    ): GeneratedOracle {
        // ---- 第一层：本地确定性规则引擎 ----
        val lock = DecisionEngine.compute(input)

        val physique = PromptPayloadBuilder.physiqueFeature(
            gender = input.gender,
            boneType = input.boneType,
            heightCm = input.heightCm,
            weightKg = input.weightKg,
            erectLengthCm = input.erectLengthCm,
            erectGirthCm = input.erectGirthCm,
            flaccidLengthCm = input.flaccidLengthCm,
            cupSize = input.cupSize,
            jadeGateShape = input.jadeGateShape,
            privacyTierSendsPhysique = input.privacyTier.sendsPhysiqueToAi,
        )

        val vars = mapOf(
            "ganzhi" to input.almanac.ganZhiDay,
            "xiu" to "${input.almanac.xiu}宿",
            "qi" to input.qi.toString(),
            "streak" to input.streakDays.toString(),
            "bone" to input.boneType.classicLabel,
            "physique" to physique,
            "starName" to lock.star.name,
            "starAttr" to lock.star.attribute,
            "cave" to lock.cave.label,
            "method" to lock.method.labels.joinToString("、"),
            "medium" to lock.realm.medium,
            "realm" to lock.realm.theme,
        )

        // ---- 第二层：LLM 渲染（可选，失败即降级）----
        if (allowLlm && renderer != null && renderer.isConfigured) {
            val payload = PromptPayloadBuilder.buildDailyPayload(
                gender = input.gender,
                natal = input.natal,
                relationship = input.relationship,
                partnerFrequency = input.partnerFrequency,
                boneType = input.boneType,
                physiqueFeature = physique,
                qi = input.qi,
                streakDays = input.streakDays,
                todayCount = input.todayCount,
                hoursSinceLast = input.hoursSinceLast,
                recentAdverse = input.lastAdverse.toList(),
                almanac = input.almanac,
                lock = lock,
            )
            val seed = input.almanac.solarDate.replace("-", "").toIntOrNull() ?: 20260101
            val llmResult = runCatching {
                val raw = renderer.completeJson(
                    Prompts.DAILY_ORACLE_SYSTEM,
                    Prompts.dailyOracleUser(payload),
                    seed,
                )
                OracleValidator.validate(raw, lock, dayKey)
            }.getOrNull()

            if (llmResult != null) {
                val sanitized = llmResult.first.copy(
                    pouch = llmResult.first.pouch.let { p ->
                        p.copy(
                            cave = p.cave.copy(note = PouchSanitizer.clean(p.cave.note)),
                            method = p.method.copy(note = PouchSanitizer.clean(p.method.note)),
                            star = p.star.copy(note = PouchSanitizer.clean(p.star.note)),
                            realm = p.realm.copy(note = PouchSanitizer.clean(p.realm.note)),
                        )
                    },
                )
                return GeneratedOracle(
                    lock = lock,
                    oracle = sanitized,
                    report = llmResult.second,
                    degraded = false,
                    failureReason = null,
                )
            }
            // 降级
            val local = LocalOracleLibrary.render(lock, dayKey, vars)
            return GeneratedOracle(
                lock = lock,
                oracle = local,
                report = OracleValidator.ValidationReport(
                    ok = true,
                    issues = listOf("LLM 不可用，已降级至本地古籍模板库"),
                    anchorAudit = local.anchorAudit,
                ),
                degraded = true,
                failureReason = "LLM 渲染失败或超时",
            )
        }

        // ---- 第三层：本地古籍模板库 0ms 兜底 ----
        val local = LocalOracleLibrary.render(lock, dayKey, vars)
        return GeneratedOracle(
            lock = lock,
            oracle = local,
            report = OracleValidator.ValidationReport(
                ok = true,
                issues = emptyList(),
                anchorAudit = local.anchorAudit,
            ),
            degraded = false,
            failureReason = null,
        )
    }

    fun lockToJson(lock: DecisionLock): String = json.encodeToString(DecisionLock.serializer(), lock)

    fun lockFromJson(text: String): DecisionLock? =
        runCatching { json.decodeFromString(DecisionLock.serializer(), text) }.getOrNull()

    fun oracleToJson(oracle: RenderedOracle): String =
        json.encodeToString(RenderedOracle.serializer(), oracle)

    fun oracleFromJson(text: String): RenderedOracle? =
        runCatching { json.decodeFromString(RenderedOracle.serializer(), text) }.getOrNull()
}

data class GeneratedOracle(
    val lock: DecisionLock,
    val oracle: RenderedOracle,
    val report: OracleValidator.ValidationReport,
    val degraded: Boolean,
    val failureReason: String?,
)
