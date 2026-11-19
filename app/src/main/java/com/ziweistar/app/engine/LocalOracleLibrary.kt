package com.ziweistar.app.engine

import android.content.Context
import com.ziweistar.app.engine.model.*
import kotlinx.serialization.json.Json

/**
 * 本地《钦天监六十四签古籍模板库》
 *
 * 设计文档 6.4-2：客户端内置本地 JSON 模板，按
 * 4种吉凶 × 4种洞府 × 4种功法大类 预置 64 首签诗与批注模板，
 * 支持 {{星宿名}} / {{功法名}} 动态占位符替换。
 *
 * 网络断连 / API 超时 / 纯本地隐私模式 → 0ms 离线出签。
 */
object LocalOracleLibrary {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Volatile
    private var loaded = false

    /** key = "$fortune|$cave|$methodClass" */
    private val templates = HashMap<String, OracleTemplate>()

    /** 大凶兜底专用：若模板缺失，使用此内置极简模板 */
    private val emergency = OracleTemplate(
        id = "EMERGENCY",
        fortune = "大凶",
        cave = "RETREAT",
        methodClass = "SECRET",
        trigram = "天地否塞 · 潜龙勿用",
        poem = listOf("素榻清枕早安眠", "吐纳敛息锁心猿", "孤月清风莫思量", "焚香听雨万虑空"),
        commentary = "值{{ganzhi}}，{{xiu}}临照，先天元气仅余{{qi}}，连观{{streak}}日而神已疲，气机闭塞，故天心示警，宜闭关养神。",
        pouch = PouchTemplates(
            caveNote = "戌时阴气渐生，宜早卧以养元神。素榻清枕，熄烛而眠，较之一切施法皆更契天道。",
            methodNote = "观阁下{{bone}}之质，{{physique}}。今日不宜再耗，宜盘膝吐纳、敛息静心，使气血归元。",
            starNote = "今日天干五行闭藏，不宜召请{{starAttr}}之{{starName}}入梦。宜独坐观心，与孤月相对。",
            realmNote = "阁下且焚一炉沉香，听窗外雨声渐密。不必思量何事，只将呼吸放缓，随雨声沉入无梦之境，待东方既白，元气自复。",
        ),
    )

    /** 预置占位符替换表 */
    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val files = listOf("oracle_templates.json", "oracle_templates_ping.json")
            for (f in files) {
                runCatching {
                    val text = context.assets.open(f).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = json.decodeFromString<OracleTemplateFile>(text)
                    for (t in parsed.templates) {
                        templates[key(t.fortune, t.cave, t.methodClass)] = t
                    }
                }
            }
            loaded = true
        }
    }

    private fun key(fortune: String, cave: String, methodClass: String) = "$fortune|$cave|$methodClass"

    /** 模板总量（健康检查用） */
    fun count(): Int = templates.size

    /** 键映射：吉凶全名 → 短名 */
    fun shortFortune(fullLabel: String): String = when {
        fullLabel.contains("大吉") -> "大吉"
        fullLabel.contains("小吉") -> "小吉"
        fullLabel.contains("大凶") -> "大凶"
        else -> "平"
    }

    /**
     * 从本地模板库套出一张签。
     * @param lock 定局铁券
     * @param vars 占位符替换表（ganzhi / xiu / qi / streak / bone / physique / starName / starAttr）
     */
    fun render(
        lock: DecisionLock,
        dayKey: String,
        vars: Map<String, String>,
    ): RenderedOracle {
        val short = shortFortune(lock.fortune)
        val tpl = templates[key(short, lock.cave.caveId, lock.method.sectClass)]
            ?: templates[key(short, lock.cave.caveId, "HAND")]
            ?: templates[key(short, "RETREAT", "SECRET")]
            ?: emergency

        val poem = tpl.poem.take(4).let {
            if (it.size == 4) it else emergency.poem
        }

        return RenderedOracle(
            dayKey = dayKey,
            rulePathId = lock.rulePathId,
            fortuneLabel = lock.fortune,
            fortuneShort = short,
            trigram = lock.trigram,
            poem = poem,
            commentary = substitute(tpl.commentary, vars),
            pouch = OraclePouches(
                cave = PouchContent(
                    quote = poem[0],
                    labels = listOf(lock.cave.label),
                    note = PouchSanitizer.clean(substitute(tpl.pouch.caveNote, vars)),
                ),
                method = PouchContent(
                    quote = poem[1],
                    labels = lock.method.labels,
                    note = PouchSanitizer.clean(substitute(tpl.pouch.methodNote, vars)),
                ),
                star = PouchContent(
                    quote = poem[2],
                    labels = listOf(lock.star.name, lock.star.attribute),
                    note = PouchSanitizer.clean(substitute(tpl.pouch.starNote, vars)),
                ),
                realm = PouchContent(
                    quote = poem[3],
                    labels = listOf(lock.realm.theme, lock.realm.medium),
                    note = PouchSanitizer.clean(substitute(tpl.pouch.realmNote, vars)),
                ),
            ),
            source = "local",
            anchorAudit = auditAnchors(poem, lock),
        )
    }

    private fun substitute(text: String, vars: Map<String, String>): String {
        var out = text
        for ((k, v) in vars) out = out.replace("{{$k}}", v)
        // 清理残留占位符（Android 的 ICU 正则要求花括号成对转义）
        out = Regex("\\{\\{[^}]*\\}\\}").replace(out, "")
        return out.trim()
    }

    /**
     * 词根命中自检：核查四句诗是否分别嵌入了铁券要求的意象词根。
     * 本地模板可能不 100% 命中（模板是通用版），此处输出审计字符串供 UI 展示。
     */
    fun auditAnchors(poem: List<String>, lock: DecisionLock): String {
        val pairs = listOf(
            "洞府" to (poem.getOrNull(0) to lock.cave.anchors),
            "功法" to (poem.getOrNull(1) to lock.method.anchors),
            "星宿" to (poem.getOrNull(2) to lock.star.anchors),
            "意境" to (poem.getOrNull(3) to lock.realm.anchors),
        )
        val hits = pairs.map { (name, pair) ->
            val line = pair.first ?: ""
            val matched = pair.second.firstOrNull { line.contains(it) || line.contains(it.take(1)) }
            if (matched != null) "第${pairs.indexOfFirst { it.first == name } + 1}句[$name]命中「$matched」" else null
        }.filterNotNull()
        return if (hits.isEmpty()) "本地古籍模板（通用意象）" else hits.joinToString("；")
    }
}
