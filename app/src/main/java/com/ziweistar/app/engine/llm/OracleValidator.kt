package com.ziweistar.app.engine.llm

import com.ziweistar.app.engine.LocalOracleLibrary
import com.ziweistar.app.engine.model.DecisionLock
import com.ziweistar.app.engine.model.OraclePouches
import com.ziweistar.app.engine.model.PouchContent
import com.ziweistar.app.engine.model.RenderedOracle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * ============================================================
 * 第三层：输出校验与本地降级兜底（Validator & Fallback）
 *
 * 设计文档 6.4-1：
 *  ① 正则与枚举强校验：若模型擅自改名，客户端直接用本地 decision_lock
 *     的标准字段覆盖 JSON 中的标签字段，仅保留模型的诗句与批注文本。
 *  ② 校验四句签诗是否同步复制到四个锦囊的「引诗」，UI 直接通过指针引用
 *     七言签诗的对应句，从根源杜绝不一致。
 * ============================================================
 */
object OracleValidator {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    const val HAN_CHAR_PER_LINE = 7

    data class ValidationReport(
        val ok: Boolean,
        val replacedFields: List<String> = emptyList(),
        val issues: List<String> = emptyList(),
        val anchorAudit: String = "",
    )

    /**
     * 校验并「修复」LLM 返回的 JSON，产出可信任的 RenderedOracle。
     * 任何致命问题（无法解析 / 诗句缺失 / 批注缺失）→ 返回 null，由上层降级。
     */
    fun validate(
        raw: String,
        lock: DecisionLock,
        dayKey: String,
    ): Pair<RenderedOracle, ValidationReport>? {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        val replaced = mutableListOf<String>()
        val issues = mutableListOf<String>()

        // ---- 1. 吉凶朱印 / 卦象爻辞 强校验（必须与铁券一致，不一致则以铁券覆盖）----
        val shortExpected = fortuneShort(lock.fortune)
        val fortuneGot = str(root, "吉凶朱印").orEmpty()
        if (!fortuneGot.contains(shortExpected)) {
            replaced.add("吉凶朱印 ← 铁券")
        }
        if (str(root, "卦象爻辞") != lock.trigram) {
            replaced.add("卦象爻辞 ← 铁券")
        }

        // ---- 2. 七言签诗四句 ----
        val poemObj = obj(root, "七言签诗") ?: return null
        val rawLines = listOf(
            str(poemObj, "第一句_应洞府"),
            str(poemObj, "第二句_应功法"),
            str(poemObj, "第三句_应星宿"),
            str(poemObj, "第四句_应意境"),
        )
        if (rawLines.any { it.isNullOrBlank() }) {
            issues.add("七言签诗缺失句")
            return null
        }

        val poem = rawLines.filterNotNull().map { sanitizePoemLine(it) }
        if (poem.size != 4 || poem.any { it.length != HAN_CHAR_PER_LINE }) {
            issues.add("七言签诗不合规格（需4句×7字）")
            return null
        }

        // ---- 3. 命理机缘解曰 ----
        val commentary = str(root, "命理机缘解曰")?.trim().orEmpty()
        if (commentary.isBlank()) return null

        // ---- 4. 四道锦囊：引诗强制指针引用，标签强制覆盖 ----
        val pouchesObj = obj(root, "思维锦囊")

        fun sub(block: String, field: String): String {
            val b = pouchesObj?.let { obj(it, block) } ?: return ""
            return str(b, field)?.trim().orEmpty()
        }

        val caveNote = sub("天时洞府", "玄学批注")
        val methodNote = sub("神通功法", "玄学批注")
        val starNote = sub("结缘星宿", "玄学批注")
        val realmNote = sub("神游意境", "微剧本引导")

        if (caveNote.isBlank() || methodNote.isBlank() || starNote.isBlank() || realmNote.isBlank()) {
            issues.add("锦囊批注缺失")
            return null
        }

        // 引诗一致性审计（不做修补，UI 永远用 poem 数组作为真源）
        val quoteBlocks = listOf("天时洞府", "神通功法", "结缘星宿", "神游意境")
        quoteBlocks.forEachIndexed { i, block ->
            val got = pouchesObj?.let { obj(it, block) }?.let { str(it, "引诗") }
            if (got != null && sanitizePoemLine(got) != poem[i]) {
                replaced.add("$block.引诗 ← 指针引用")
            }
        }
        replaced.add("洞府定局.选定标签 ← 铁券")
        replaced.add("功法定局.选定标签 ← 铁券")
        replaced.add("星宿定局.选定星宿 ← 铁券")
        replaced.add("意境定局.选定灵媒 ← 铁券")

        val receipt = obj(root, "内核一致性回执")
        val anchorAudit = receipt?.let { str(it, "词根命中自检") }.orEmpty()
            .ifBlank { LocalOracleLibrary.auditAnchors(poem, lock) }

        val oracle = RenderedOracle(
            dayKey = dayKey,
            rulePathId = lock.rulePathId,
            fortuneLabel = lock.fortune,
            fortuneShort = shortExpected,
            trigram = lock.trigram,
            poem = poem,
            commentary = commentary,
            pouch = OraclePouches(
                cave = PouchContent(poem[0], listOf(lock.cave.label), caveNote),
                method = PouchContent(poem[1], lock.method.labels, methodNote),
                star = PouchContent(poem[2], listOf(lock.star.name, lock.star.attribute), starNote),
                realm = PouchContent(poem[3], listOf(lock.realm.theme, lock.realm.medium), realmNote),
            ),
            source = "llm",
            anchorAudit = anchorAudit,
        )

        return oracle to ValidationReport(
            ok = issues.isEmpty(),
            replacedFields = replaced,
            issues = issues,
            anchorAudit = anchorAudit,
        )
    }

    // ---- 安全取值辅助 ----

    private fun obj(parent: JsonObject, key: String): JsonObject? {
        val el: JsonElement = parent[key] ?: return null
        return runCatching { el.jsonObject }.getOrNull()
    }

    private fun str(parent: JsonObject?, key: String): String? {
        if (parent == null) return null
        val el = parent[key] ?: return null
        return runCatching { el.jsonPrimitive.content }.getOrNull()
    }

    /** 去除标点与空白，仅保留汉字，用于长度校验 */
    fun sanitizePoemLine(s: String): String = s.filter { it.code in 0x4E00..0x9FFF }

    fun fortuneShort(fullLabel: String): String = when {
        fullLabel.contains("大吉") -> "大吉"
        fullLabel.contains("小吉") -> "小吉"
        fullLabel.contains("大凶") -> "大凶"
        else -> "平"
    }
}
