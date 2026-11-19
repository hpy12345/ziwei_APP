package com.ziweistar.app.engine.llm

import com.ziweistar.app.engine.EngineInput
import com.ziweistar.app.engine.ReportEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * ============================================================
 * 场景批文（Scene Oracle）
 *
 * 设计文档 5.1 / 5.2 / 5.3 与 6.3.2：
 *   - SECOND_BLOOM      梅开二度烫金贺卡
 *   - TIANGANG_WARNING  天罡戒律黄符劝阻
 *   - PERIODIC_REPORT   钦天监·紫薇内丹录 周报 / 月报
 *
 * 与每日灵签同为「双擎」结构：
 *   本地确定性统计 → LLM 受控渲染 → 失败 / 未配置 / 纯本地隐私模式
 *   → LocalScenarioLibrary 0ms 兜底（文案仍为古风，绝不出现空卡）
 * ============================================================
 */

/** 场景类型（与 Prompts.SCENARIO_SYSTEM 中的 scene_type 一一对应） */
enum class SceneType(val code: String, val label: String) {
    SECOND_BLOOM("SECOND_BLOOM", "梅开二度"),
    TIANGANG_WARNING("TIANGANG_WARNING", "天罡戒律"),
    PERIODIC_REPORT("PERIODIC_REPORT", "钦天监诊脉"),
    ;

    companion object {
        fun of(code: String): SceneType =
            entries.firstOrNull { it.code == code } ?: SECOND_BLOOM
    }
}

/**
 * 场景批文成品。字段按场景按需填充，UI 直接读取即可，无需判空拼接。
 */
@Serializable
data class ScenarioDoc(
    /** 场景类型编码 */
    val sceneCode: String,
    /** 印章 / 符箓 / 卷首标题，如 【批曰：双星拱照 · 梅开二度】 */
    val title: String = "",
    /** 正文（贺词 / 劝阻正文 / 周报·流年心境洞察） */
    val body: String = "",
    /** 附加：养生即刻处方（梅开二度） / 静心咒文（天罡） / 空（周报） */
    val extra: String = "",
    /** 周报·卷首语（四字古风总评） */
    val scrollOpen: String = "",
    /** 周报·肉身精气诊脉 */
    val pulse: String = "",
    /** 周报·推荐调理功法（唯一 1 项） */
    val recommendMethod: String = "",
    /** 周报·节律排程建议 */
    val scheduleAdvice: String = "",
    /** local / llm */
    val source: String = "local",
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * 文案内容版本号。
     *
     * 为什么需要：周报 / 月报会按月键（periodKey）缓存到数据库里，缓存是没有失效机制的。
     * 一旦兜底文案或分栏结构改了，老用户永远读不到新文案，只有卸载重装才 "修好"。
     *
     * 约定：**默认值 0 表示「旧版本写入、未标记」**，读到 0 一律视为过期。
     * 落库时由 MainViewModel 统一盖成 CONTENT_VERSION；提升 CONTENT_VERSION 即可作废全部旧缓存。
     */
    val contentVersion: Int = 0,
) {
    val scene: SceneType get() = SceneType.of(sceneCode)
    val fromLlm: Boolean get() = source == "llm"

    companion object {
        /** 文案结构性改动时 +1（旧缓存随之作废） */
        const val CONTENT_VERSION = 2
    }
}

/**
 * 将客户端确定性统计封装为 LLM 的 Input Data JSON（严格按 6.3.2 字段命名）。
 */
object ScenarioPayloadBuilder {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    /** 梅开二度：需点明两次所用功法 / 星宿 */
    fun buildSecondBloom(
        input: EngineInput,
        physiqueFeature: String,
        firstStarName: String,
        secondStarName: String,
        firstMethods: List<String>,
        secondMethods: List<String>,
        hoursSinceLast: Double?,
        todayCount: Int,
    ): String = buildJsonObject {
        put("scene_type", SceneType.SECOND_BLOOM.code)
        putJsonObject("用户档案摘要") {
            put("性别", input.gender.classicLabel)
            put("农历八字", input.natal.fullDescription)
            put("红尘羁绊", input.relationship.classicLabel)
            put("骨相定性", input.boneType.classicLabel)
            put("肉身禀赋特征", physiqueFeature)
        }
        putJsonObject("近期节律状态") {
            put("先天元气值_Qi", input.qi)
            put("今日已观星次数", todayCount)
            put("当前连击天数_Streak", input.streakDays)
            if (hoursSinceLast != null) {
                put("距上次观星间隔小时", (hoursSinceLast * 10).toInt() / 10.0)
            } else {
                put("距上次观星间隔小时", -1)
            }
        }
        putJsonObject("本次双星连珠明细") {
            put("第一次_结缘星宿", firstStarName)
            putJsonArray("第一次_所习功法") { firstMethods.forEach { add(it) } }
            put("第二次_结缘星宿", secondStarName)
            putJsonArray("第二次_所习功法") { secondMethods.forEach { add(it) } }
        }
        putJsonObject("今日黄历天时") {
            put("今日干支与五行", "${input.almanac.ganZhiDay} · ${input.almanac.xiuZheng}旺")
            put("今日二十八星宿", "${input.almanac.xiu}宿")
        }
    }.toString()

    /** 天罡戒律：需点名历史高频不良反应与肉身弱点 */
    fun buildTianggang(
        input: EngineInput,
        physiqueFeature: String,
        todayCount: Int,
        historicalAdverse: List<String>,
    ): String = buildJsonObject {
        put("scene_type", SceneType.TIANGANG_WARNING.code)
        putJsonObject("用户档案摘要") {
            put("性别", input.gender.classicLabel)
            put("骨相定性", input.boneType.classicLabel)
            put("肉身禀赋特征", physiqueFeature)
            put("肉身禀赋弱点", weaknessOf(input.gender))
        }
        putJsonObject("近期节律状态") {
            put("先天元气值_Qi", input.qi)
            put("今日已观星次数", todayCount)
            put("当前连击天数_Streak", input.streakDays)
            putJsonArray("历史高频不良反应") {
                if (historicalAdverse.isEmpty()) add("腰膝酸软") else historicalAdverse.forEach { add(it) }
            }
            putJsonArray("最近一次不良反应") { input.lastAdverse.forEach { add(it.classicLabel) } }
        }
        putJsonObject("今日黄历天时") {
            put("今日干支与五行", "${input.almanac.ganZhiDay} · ${input.almanac.xiuZheng}旺")
            put("今日二十八星宿", "${input.almanac.xiu}宿")
        }
    }.toString()

    /** 周报 / 月报：必须原样引用高危归因预警与最佳恢复周期 */
    fun buildPeriodic(
        input: EngineInput,
        physiqueFeature: String,
        period: ReportEngine.Period,
        stats: ReportEngine.Stats,
    ): String = buildJsonObject {
        put("scene_type", SceneType.PERIODIC_REPORT.code)
        putJsonObject("周期信息") {
            put("周期类型", if (period.kind == ReportEngine.Kind.WEEK) "周报" else "月报")
            put("周期区间", period.label)
            put("周期标识", period.key)
        }
        putJsonObject("用户档案摘要") {
            put("性别", input.gender.classicLabel)
            put("骨相定性", input.boneType.classicLabel)
            put("肉身禀赋特征", physiqueFeature)
        }
        putJsonObject("生理统计指标") {
            put("观星总次数", stats.totalCount)
            put("活跃天数", stats.activeDays)
            put("平均间隔小时", (stats.avgIntervalHours * 10).toInt() / 10.0)
            put("平均时长分钟", stats.avgDurationMinutes)
            put("道心圆满均分", (stats.avgSatisfaction * 10).toInt() / 10.0)
            put("单日最高次数", stats.maxDayCount)
            put("连续峰值天数", stats.streakPeak)
            put("严重不良反应次数", stats.severeCount)
            put("边缘控制次数", stats.edgeControlCount)
            put("最佳恢复周期_天", stats.bestRecoveryDays)
        }
        putJsonObject("高危归因预警") {
            putJsonArray("不良反应排行") {
                stats.adverseTop.forEach { (label, cnt) -> add("$label×$cnt") }
            }
            putJsonArray("所习功法排行") {
                stats.methodDistribution.forEach { (label, cnt) -> add("$label×$cnt") }
            }
            put("高发时段", peakHourLabel(stats.hourHistogram))
            put("预警结论", riskConclusion(stats))
        }
        putJsonObject("当前节律") {
            put("先天元气值_Qi", input.qi)
            put("今日已观星次数", input.todayCount)
        }
    }.toString()

    /** 肉身禀赋弱点（用于天罡劝阻点名） */
    fun weaknessOf(gender: com.ziweistar.app.engine.model.Gender): String =
        if (gender.isMale) "腰肾气机易因频次过密而暗耗，龙根血络易摩擦受损" else "冲任二脉易受寒凉，玉门黏膜娇嫩不耐高频强震"

    private fun peakHourLabel(hist: List<Int>): String {
        if (hist.isEmpty()) return "无记录"
        val peak = hist.indices.maxByOrNull { hist[it] } ?: return "无记录"
        return "$peak 时前后（${hist[peak]} 次）"
    }

    private fun riskConclusion(stats: ReportEngine.Stats): String = when {
        stats.maxDayCount >= 3 -> "单日峰值 ${stats.maxDayCount} 次，已越天罡戒律之限"
        stats.severeCount > 0 -> "携严重不良反应 ${stats.severeCount} 次，属元神过耗"
        stats.totalCount >= 8 -> "频次偏稠，气机已属透支"
        stats.totalCount == 0 -> "本周期未见起坛记录，天机沉寂"
        else -> "节律尚在常度之内"
    }

    /** 解析用 Json（不可变、可复用：避免每次调用都重建 format 实例） */
    private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 解析 LLM 返回的 JSON（键名按场景不同），容忍缺字段 */
    fun parse(scene: SceneType, raw: String): ScenarioDoc? = runCatching {
        val obj = lenient
            .parseToJsonElement(raw).let { it as? JsonObject ?: return null }
        when (scene) {
            SceneType.SECOND_BLOOM -> ScenarioDoc(
                sceneCode = scene.code,
                title = obj.str("印章标题"),
                body = obj.str("贺词正文"),
                extra = obj.str("养生即刻处方"),
                source = "llm",
            )

            SceneType.TIANGANG_WARNING -> ScenarioDoc(
                sceneCode = scene.code,
                title = obj.str("符箓标题"),
                body = obj.str("劝阻正文"),
                extra = obj.str("静心咒文"),
                source = "llm",
            )

            SceneType.PERIODIC_REPORT -> {
                val next = obj["下期天机指引"] as? JsonObject
                ScenarioDoc(
                    sceneCode = scene.code,
                    title = obj.str("报告卷首语"),
                    scrollOpen = obj.str("报告卷首语"),
                    body = obj.str("流年心境洞察"),
                    pulse = obj.str("肉身精气诊脉"),
                    recommendMethod = next?.str("推荐调理功法") ?: "",
                    scheduleAdvice = next?.str("节律排程建议") ?: "",
                    source = "llm",
                )
            }
        }
    }.getOrNull()

    internal fun JsonObject.str(key: String): String =
        runCatching { this[key]?.jsonPrimitive?.content ?: "" }.getOrDefault("")
}
