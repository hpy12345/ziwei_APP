package com.ziweistar.app.engine

import com.ziweistar.app.data.db.SessionEntity
import com.ziweistar.app.data.DomainMapper
import com.ziweistar.app.data.db.ProfileEntity
import com.ziweistar.app.data.isRestRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 钦天监周期结算引擎（周报 / 月报）
 *
 * 设计文档 5.3：
 *  卷一·星轨总览（本地确定性统计 + 图表）
 *  卷二·钦天监诊脉（AI 深度批文 / 本地兜底文案）
 */
object ReportEngine {

    enum class Kind { WEEK, MONTH }

    /** 周期区间 */
    data class Period(
        val kind: Kind,
        val key: String,
        val label: String,
        val startMillis: Long,
        val endMillis: Long,
    )

    /** 星轨总览统计 */
    data class Stats(
        val totalCount: Int,
        val activeDays: Int,
        val avgIntervalHours: Double,
        val avgDurationMinutes: Int,
        val avgSatisfaction: Double,
        val bestRecoveryDays: Int,
        val climaxDistribution: List<Pair<String, Int>>,
        val venueDistribution: List<Pair<String, Int>>,
        val methodDistribution: List<Pair<String, Int>>,
        val adverseTop: List<Pair<String, Int>>,
        val hourHistogram: List<Int>,
        val dailyCounts: List<Pair<String, Int>>,
        val severeCount: Int,
        val edgeControlCount: Int,
        val maxDayCount: Int,
        val streakPeak: Int,
    ) {
        val hasData: Boolean get() = totalCount > 0
    }

    fun weekOf(millis: Long): Period {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 归到本周一
        val dow = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1
        val delta = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
        cal.add(Calendar.DAY_OF_MONTH, delta)
        val start = cal.timeInMillis
        // 不能用 start + 7*24h - 1：跨夏令时切换的周只有 167 或 169 小时，
        // 固定毫秒会让周期末端越界到下一周的第一天（或提前截止）。
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val end = cal.timeInMillis - 1
        // 复位到周一，供下面取周号 / 年月日使用
        cal.add(Calendar.DAY_OF_MONTH, -7)

        val woy = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        // 用周一日期做唯一键，避免跨年周号冲突
        val key = "W" + SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date(start))
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return Period(
            kind = Kind.WEEK,
            key = key,
            label = "$year 年 $month 月 $day 日 起 · 第 $woy 周",
            startMillis = start,
            endMillis = end,
        )
    }

    fun monthOf(millis: Long): Period {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1
        return Period(
            kind = Kind.MONTH,
            key = "M" + SimpleDateFormat("yyyyMM", Locale.CHINA).format(Date(start)),
            label = "$year 年 $month 月",
            startMillis = start,
            endMillis = end,
        )
    }

    /**
     * 计算周期统计。
     */
    fun compute(
        period: Period,
        sessions: List<SessionEntity>,
        isMale: Boolean,
    ): Stats {
        val inRange = sessions
            // 「谨遵法旨」插入的静养记录不是一次起坛，统计时必须排除，
            // 否则周报会把「闭关养神」也算成一次观星（次数虚高、间隔失真）。
            .filter { it.endedAt in period.startMillis..period.endMillis && !it.isRestRecord() }
            .sortedBy { it.endedAt }

        if (inRange.isEmpty()) {
            val days = ((period.endMillis - period.startMillis) / 86400_000L).toInt() + 1
            return Stats(
                totalCount = 0, activeDays = 0, avgIntervalHours = 0.0,
                avgDurationMinutes = 0, avgSatisfaction = 0.0, bestRecoveryDays = 0,
                climaxDistribution = emptyList(), venueDistribution = emptyList(),
                methodDistribution = emptyList(), adverseTop = emptyList(),
                hourHistogram = List(24) { 0 },
                dailyCounts = List(days) { 0 }.mapIndexed { i, _ ->
                    val d = SimpleDateFormat("MM-dd", Locale.CHINA)
                        .format(Date(period.startMillis + i * 86400_000L))
                    d to 0
                },
                severeCount = 0, edgeControlCount = 0, maxDayCount = 0, streakPeak = 0,
            )
        }

        val total = inRange.size
        val byDay = inRange.groupBy { it.dayKey }
        val activeDays = byDay.size

        // 平均间隔
        val intervals = inRange.zipWithNext { a, b ->
            (b.endedAt - a.endedAt).toDouble() / 3_600_000.0
        }.filter { it > 0.0 }
        val avgInterval = if (intervals.isEmpty()) 0.0 else intervals.average()

        val avgDuration = inRange.map { it.durationSeconds / 60 }.average().toInt()
        val avgSat = inRange.map { DomainMapper.satisfactionStars(it) }.average()

        // 最佳恢复周期：无不良反应且满意度 >= 4.5 星的日均间隔
        val good = inRange.filter {
            it.adverse.isBlank() && DomainMapper.satisfactionStars(it) >= 4.5
        }
        val bestRecovery = if (good.size < 2) 0 else {
            val gaps = good.zipWithNext { a, b ->
                (b.endedAt - a.endedAt).toDouble() / 86_400_000.0
            }.filter { it > 0 }
            if (gaps.isEmpty()) 0 else Math.round(gaps.average()).toInt().coerceAtLeast(1)
        }

        fun topN(items: List<String>, n: Int): List<Pair<String, Int>> =
            items.filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(n)
                .map { it.key to it.value }

        val climaxDist = topN(inRange.map { DomainMapper.climaxLabel(it, isMale) }, 6)
        val venueDist = topN(inRange.map { it.venue }, 5)
        val methodDist = topN(inRange.flatMap { DomainMapper.methodNames(it) }, 8)
        val adverseTop = topN(inRange.flatMap { DomainMapper.adverseOf(it) }
            .filter { it.penalty > 0 || it.name == "GUILT" || it.name == "DISTRACT" }
            .map { it.classicLabel }, 6)

        val hours = IntArray(24)
        for (s in inRange) {
            val cal = Calendar.getInstance().apply { timeInMillis = s.startedAt }
            hours[cal.get(Calendar.HOUR_OF_DAY)]++
        }

        val days = ((period.endMillis - period.startMillis) / 86400_000L).toInt() + 1
        val dailyCounts = (0 until days).map { i ->
            val millis = period.startMillis + i * 86400_000L
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(millis))
            SimpleDateFormat("MM-dd", Locale.CHINA).format(Date(millis)) to (byDay[key]?.size ?: 0)
        }

        val severe = inRange.count { s -> DomainMapper.adverseOf(s).any { it.isSevere } }
        val edge = inRange.count {
            DomainMapper.climaxClassOf(it) == ClimaxClass.EDGE
        }

        // 周期内最高连击
        val keys = byDay.keys.sorted()
        var bestStreak = 0
        var cur = 0
        var prev: java.time.LocalDate? = null
        for (k in keys) {
            val d = runCatching { java.time.LocalDate.parse(k) }.getOrNull() ?: continue
            cur = if (prev != null && prev.plusDays(1) == d) cur + 1 else 1
            bestStreak = maxOf(bestStreak, cur)
            prev = d
        }

        return Stats(
            totalCount = total,
            activeDays = activeDays,
            avgIntervalHours = avgInterval,
            avgDurationMinutes = avgDuration,
            avgSatisfaction = avgSat,
            bestRecoveryDays = bestRecovery,
            climaxDistribution = climaxDist,
            venueDistribution = venueDist,
            methodDistribution = methodDist,
            adverseTop = adverseTop,
            hourHistogram = hours.toList(),
            dailyCounts = dailyCounts,
            severeCount = severe,
            edgeControlCount = edge,
            maxDayCount = byDay.values.maxOfOrNull { it.size } ?: 0,
            streakPeak = bestStreak,
        )
    }

    /**
     * 卷二·钦天监诊脉（本地兜底批文）
     * 设计文档要求在 LLM 不可用时仍有可读的诊断文本。
     */
    fun diagnosisText(
        period: Period,
        stats: Stats,
        profile: ProfileEntity,
        isMale: Boolean,
    ): String {
        if (!stats.hasData) {
            return "本周期内钦天监未见起坛记录。天机沉寂，宜静养元神、调息气血，待元气充盈再行观星。"
        }
        val sb = StringBuilder()
        val genderLabel = if (isMale) "乾造" else "坤造"
        val bone = runCatching {
            com.ziweistar.app.engine.model.BoneType.valueOf(profile.boneType)
        }.getOrNull()

        // 高压预警
        if (stats.maxDayCount >= 3) {
            sb.append("本周期最高单日 ${stats.maxDayCount} 次，已越天罡戒律之限，")
            sb.append("有此等频次者，多因心神难安、以耗散求解，宜先治其心而后治其身。")
        } else if (stats.severeCount > 0) {
            sb.append("本周期有 ${stats.severeCount} 次携严重不良反应收功（腰膝酸软 / 头昏耳鸣 / 器物磨损），")
            sb.append("此乃元神过耗之兆，非小事也。")
        } else if (stats.totalCount >= 8) {
            sb.append("本周期起坛 ${stats.totalCount} 次，频次偏稠，虽暂未见重伤，然气机已属透支。")
        } else {
            sb.append("本周期起坛 ${stats.totalCount} 次，节律尚在常度之内。")
        }
        sb.append("\n\n")

        // 骨相交叉诊断
        if (bone != null) {
            sb.append("观阁下${genderLabel}·${bone.classicLabel}：${bone.physiqueHint}")
            sb.append("\n\n")
        }

        // 黄金恢复周期
        if (stats.bestRecoveryDays > 0) {
            sb.append("据本周期「无不良反应且道心圆满 ≥ 4.5 星」之 ${stats.activeDays} 日记录推算，")
            sb.append("阁下最佳恢复周期为 ${stats.bestRecoveryDays} 天。")
            sb.append("若能循此节律起坛，则气血不受其损，反得其养。")
        } else {
            sb.append("本周期合格记录（无不良反应 + 高满意度）尚不足，暂无法推算黄金恢复周期。")
            sb.append("建议先减少器械与高频强刺激，收功时如实勾选不良反应，积累七日以上再作诊断。")
        }
        sb.append("\n\n")

        // 时辰归因
        val hourHist = stats.hourHistogram
        val peakHour = hourHist.indices.maxByOrNull { hourHist[it] } ?: -1
        val peakCount = if (peakHour >= 0) hourHist[peakHour] else 0
        val lateNight = hourHist[23] + hourHist[0] + hourHist[1] + hourHist[2]
        if (lateNight >= 2 && lateNight * 3 >= stats.totalCount) {
            sb.append("本周期有 $lateNight 次发生于子丑时（23:00–03:00）之后，")
            sb.append("此为胆经当令、宜眠之时。深夜高频起坛，多映射日间焦虑未解或入睡困难，")
            sb.append("宜先调其睡眠节律，而非以观星助眠。")
        } else if (peakHour >= 0 && peakCount > 0) {
            sb.append("本周期起坛高峰在 $peakHour 时前后（$peakCount 次），")
            sb.append("此时段心神最易起念，宜预先安排他事以转移其机。")
        }
        sb.append("\n\n")

        // 下调令
        sb.append("【下周期调理建议】")
        sb.append(if (isMale) {
            "宜以【锁龙御气】为主，配合【甘露沐身】减少干摩擦；忌【盘龙俯卧】式床褥压迫。"
        } else {
            "宜以【隔纱戏水】或【拈花揉珠】为主，避开高频强震；行经期禁纳入式功法。"
        })
        return sb.toString().trim()
    }
}
