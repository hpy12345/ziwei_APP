package com.ziweistar.app.engine

/**
 * 确定性伪随机哈希选择器（Deterministic Hash Selector）
 *
 * 设计文档 6.2-2：同一天、同一用户，无论刷新多少次或换任何模型，
 * 必须抽中同一个人/同一项。严禁让大模型随机挑选。
 *
 * 采用 FNV-1a 32-bit。
 */
object HashSelector {

    /**
     * FNV-1a 32-bit 哈希。
     * 注意：使用 Long 做无符号传播，保证与 TS 的 Math.imul 语义一致。
     */
    fun fnv1a32(input: String): Int {
        var hash = 2166136261L // FNV offset basis (as unsigned)
        for (ch in input) {
            hash = hash xor (ch.code.toLong() and 0xFF)
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        return hash.toInt()
    }

    /**
     * 从列表中确定性选出唯一一项。
     * @param list 候选列表
     * @param dateStr 日期串（yyyy-MM-dd），保证次日自动轮转
     * @param userId 用户标识
     * @param salt 维度盐值（如 "STAR" / "MEDIUM"），保证同一日不同维度互不共振
     */
    fun <T> select(list: List<T>, dateStr: String, userId: String, salt: String): T? {
        if (list.isEmpty()) return null
        val hashInput = "${dateStr}_${userId}_${salt}"
        val h = fnv1a32(hashInput)
        val index = ((h.toLong() and 0xFFFFFFFFL) % list.size).toInt()
        return list[index]
    }

    /** 不重复选取 n 项（用于功法组合），顺序亦确定性 */
    fun <T> selectDistinct(list: List<T>, n: Int, dateStr: String, userId: String, salt: String): List<T> {
        if (list.isEmpty() || n <= 0) return emptyList()
        val picked = LinkedHashSet<T>()
        var round = 0
        while (picked.size < minOf(n, list.size) && round < 64) {
            val h = fnv1a32("${dateStr}_${userId}_${salt}_$round")
            picked.add(list[((h.toLong() and 0xFFFFFFFFL) % list.size).toInt()])
            round++
        }
        // 兜底：若哈希碰撞导致不足，则顺序补齐
        if (picked.size < minOf(n, list.size)) {
            for (item in list) {
                picked.add(item)
                if (picked.size >= n) break
            }
        }
        return picked.toList()
    }

    /** 确定性 0..100 整数抽样（用于诗句变体等弱随机） */
    fun roll(dateStr: String, userId: String, salt: String, range: Int = 100): Int {
        if (range <= 0) return 0
        val h = fnv1a32("${dateStr}_${userId}_${salt}")
        return ((h.toLong() and 0xFFFFFFFFL) % range).toInt()
    }
}
