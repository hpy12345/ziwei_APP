package com.ziweistar.app.engine

/**
 * 锦囊正文清洗器（用户要求：思维锦囊中不出现个人身体信息）
 *
 * 本地古籍模板与 LLM 批文中，常出现以骨相、肉身尺寸作比拟的短句
 * （例如「正合中正之骨相」「阁下雄伟型，故宜由浅入深」）。
 * 这些内容属于个人体征，不应出现在每日灵签的锦囊里。
 *
 * 做法：按中文标点切分短句，丢弃任何提及骨相 / 肉身 / 体量等级 / 尺寸单位的短句，
 * 并连带丢弃紧随其后的「亦 / 并 / 且」类续句，最后清理多余标点。
 */
object PouchSanitizer {

    /** 命中即整句丢弃的体征关键词 */
    private val bodyKeys = listOf(
        // 结构类
        "骨相", "骨格", "骨架", "肉身", "体征", "体质资质",
        // 占位符（未替换时兜底）
        "{{bone}}", "{{physique}}",
        // 骨相 / 体量等级名（Domain.BoneType / PhallusTierResolver 的定性标签）
        "匀停型", "雄伟型", "雄壮型", "中正型", "清奇型", "清瘦型", "丰隆型",
        "血肉龙", "玉杵", "玉茎", "龙根",
        // 数值与尺码
        "BMI", "尺寸", "尺码", "长度", "围度", "直径", "厘米", "cm",
    )

    /** 续句标记：上一句被丢弃时，这类开头的短句也一并丢弃（多为承接被删主语的残句） */
    private val continuationHeads = listOf("亦", "并", "且", "而", "又", "本")

    private val separators = charArrayOf('，', '。', '；', '、', '！', '？', '：')

    fun clean(note: String): String {
        if (note.isBlank()) return note
        if (bodyKeys.none { note.contains(it) }) return note

        // 保留分隔符信息，逐句判定
        val kept = StringBuilder()
        var droppedPrev = false

        var i = 0
        while (i < note.length) {
            var j = i
            while (j < note.length && note[j] !in separators) j++
            val clause = note.substring(i, j)
            val sep = if (j < note.length) note[j] else ' '

            val hasBody = bodyKeys.any { clause.contains(it) }
            // 上一句被丢弃后，紧随的承接性短句（缺主语）也一并丢弃，
            // 避免留下「…。本不耐久持；」这种病句。
            val isContinuation = continuationHeads.any { clause.trimStart().startsWith(it) }

            val drop = hasBody || (droppedPrev && isContinuation)
            if (!drop && clause.isNotBlank()) {
                kept.append(clause.trim())
                if (sep != ' ') kept.append(sep)
            } else if (drop && kept.isNotEmpty() && kept.last() == '，') {
                // 被丢弃句之前的逗号悬空，改为句号收尾
                kept.setLength(kept.length - 1)
                kept.append('。')
            }
            droppedPrev = drop
            i = j + 1
        }

        var out = kept.toString().trim()
        // 清理开头多余的连接词与重复标点
        out = out.trimStart('，', '。', '；', '、', '：')
        out = Regex("([，。；、])\\1+").replace(out, "$1")
        out = out.replace("，。", "。").replace("；。", "。").replace("、。", "。")
        if (out.isEmpty()) return note
        if (out.last() !in charArrayOf('。', '！', '？')) {
            out = out.trimEnd('，', '；', '、') + "。"
        }
        return out
    }
}
