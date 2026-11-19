package com.ziweistar.app.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ============================================================
 * 灵签渲染结果（RenderedOracle）
 * 第二层 LLM 文学渲染器 / 第三层本地古籍模板库 的共同输出契约。
 * ============================================================
 */

/** 本地古籍模板（assets/oracle_templates*.json） */
@Serializable
data class OracleTemplate(
    val id: String,
    /** 大吉 / 小吉 / 平 / 大凶 */
    val fortune: String,
    /** BATH / BED / DARK / RETREAT */
    val cave: String,
    /** HAND / BODY / TOOL / SECRET */
    val methodClass: String,
    val trigram: String,
    val poem: List<String>,
    val commentary: String,
    val pouch: PouchTemplates,
)

@Serializable
data class PouchTemplates(
    val caveNote: String = "",
    val methodNote: String = "",
    val starNote: String = "",
    val realmNote: String = "",
)

@Serializable
data class OracleTemplateFile(
    val version: Int = 1,
    val templates: List<OracleTemplate> = emptyList(),
)

/**
 * 最终渲染产物：一张完整的今日灵签。
 */
@Serializable
data class RenderedOracle(
    val dayKey: String,
    /** 规则路径ID（一致性回执） */
    val rulePathId: String,
    /** 吉凶朱印 */
    val fortuneLabel: String,
    /** 印文短标签：大吉 / 小吉 / 平 / 大凶 */
    val fortuneShort: String,
    val trigram: String,
    /** 七言签诗四句 */
    val poem: List<String>,
    /** 命理机缘解曰 */
    val commentary: String,
    val pouch: OraclePouches,
    /** local / llm */
    val source: String = "local",
    /** 词根自检（LLM 回执；本地模板为自动审计结果） */
    val anchorAudit: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class OraclePouches(
    val cave: PouchContent,
    val method: PouchContent,
    val star: PouchContent,
    val realm: PouchContent,
)

@Serializable
data class PouchContent(
    /** 引诗（100% 逐字复用签诗对应句，由 UI 通过指针引用，不信任 LLM 副本） */
    val quote: String,
    /** 选定标签 */
    val labels: List<String>,
    /** 玄学批注 / 微剧本 */
    val note: String,
)
