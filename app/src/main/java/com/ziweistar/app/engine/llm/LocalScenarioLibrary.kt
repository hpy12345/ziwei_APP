package com.ziweistar.app.engine.llm

import com.ziweistar.app.engine.EngineInput
import com.ziweistar.app.engine.ReportEngine

/**
 * ============================================================
 * 场景批文·本地兜底库（0ms 离线可用）
 *
 * 设计文档 5.1 / 5.2 / 5.3：LLM 未配置、超时、隐私模式 DECLINE 时，
 * 梅开二度贺卡、天罡戒律黄符、周月报批文仍然必须有完整可读的古风文案。
 * 本库用确定性字符串拼装（无随机、无网络），文案与 LLM 分支风格一致。
 * ============================================================
 */
object LocalScenarioLibrary {

    /** 梅开二度兜底贺词 */
    fun secondBloom(
        isMale: Boolean,
        qi: Int,
        todayCount: Int,
        hoursSinceLast: Double?,
        secondStarName: String,
        secondMethods: List<String>,
    ): ScenarioDoc {
        val gap = gapLabel(hoursSinceLast)
        // 上一次未录功法时，不要写「以 未录功法 收功」这种病句，改用「未录功法」独立成句
        val hasMethods = secondMethods.isNotEmpty()
        val star = secondStarName.ifBlank { "无相神游" }
        val body = buildString {
            append("阁下今日炉鼎再燃，双星连珠，气贯周天。")
            if (hasMethods) {
                append("此番结缘「$star」，以 ${secondMethods.joinToString("、")} 收功；")
            } else {
                append("此番结缘「$star」，功法未及录入；")
            }
            append("前后相隔$gap，较之寻常一夜，气血之沛已属少见。")
            append(if (isMale) "然乾造之身，一鼓作气，再而衰，三而竭。" else "然坤造之身，宜养不宜耗，再而衰，三而竭。")
            append("事不过三，今日炉鼎已满，切勿再犯第三次。")
        }
        val extra = when {
            qi < 30 -> "即刻温汤一盏，静卧半刻，今夜务必早眠。"
            qi < 60 -> "饮温水一盏，忌冷饮，揉腰眼三十六下。"
            else -> "温水一盏，稍事静坐，勿即视屏。"
        }
        return ScenarioDoc(
            sceneCode = SceneType.SECOND_BLOOM.code,
            title = "【批曰：双星拱照 · 梅开二度】",
            body = body,
            extra = extra,
            source = "local",
        )
    }

    /** 天罡戒律兜底劝阻 */
    fun tianggang(
        isMale: Boolean,
        qi: Int,
        todayCount: Int,
        boneLabel: String,
        historicalAdverse: List<String>,
        weakness: String,
    ): ScenarioDoc {
        val adverse = if (historicalAdverse.isEmpty()) "腰膝酸软、头昏耳鸣" else historicalAdverse.joinToString("、")
        val body = buildString {
            append("敕令已下：今日起坛 $todayCount 次，先天元气仅余 $qi%。")
            append("观阁下$boneLabel，$weakness；历次收功，已见 $adverse 之征。")
            append("心火虚亢而肾水已竭，若强行再起，必致走火入魔。")
            append("精为命根，气为命主。速速止步，闭关养神，方是全生之道。")
            // 注意：不要在这里补「乾造之身，宜蓄不宜泄」之类的性别守则，
            // 黄符 UI 底部固定会渲染那一行，写在这里会导致同一句话出现两次。
        }
        return ScenarioDoc(
            sceneCode = SceneType.TIANGANG_WARNING.code,
            title = "【警世批言：精竭神枯 · 速速止步！】",
            body = body,
            extra = "心息相依，气归丹田，虚极静笃，万念俱寂。",
            source = "local",
        )
    }

    /** 周报 / 月报兜底批文（在 ReportEngine.diagnosisText 基础上补足分栏结构） */
    fun periodic(
        period: ReportEngine.Period,
        stats: ReportEngine.Stats,
        isMale: Boolean,
        boneLabel: String,
        diagnosis: String,
    ): ScenarioDoc {
        val open = when {
            !stats.hasData -> "天机沉寂"
            stats.maxDayCount >= 3 -> "精竭宜收"
            stats.severeCount > 0 -> "损中求养"
            stats.avgSatisfaction >= 4.5 -> "道心圆满"
            stats.totalCount >= 8 -> "过犹不及"
            stats.totalCount <= 2 -> "静养生机"
            else -> "循节律行"
        }
        val pulse = when {
            !stats.hasData -> "本周期无记录，精气未见耗散，亦未见流转。宜先建立记录，方可诊脉。"
            stats.severeCount > 0 ->
                "观阁下$boneLabel：本周期携严重不良反应 ${stats.severeCount} 次收功，" +
                    "无论诸项指标如何，此一项已足证元神过耗。器物与强震之耗，非加时静养可速复。"

            stats.maxDayCount >= 3 ->
                "观阁下$boneLabel：单日峰值 ${stats.maxDayCount} 次，已越天罡戒律。" +
                    "此非体魄强弱之别，而在于肾精不可频夺。元气一泄难回，务在节其频。"

            stats.avgSatisfaction >= 4.5 ->
                "观阁下$boneLabel：本周期道心圆满均分 ${"%.1f".format(stats.avgSatisfaction)} 星，" +
                    "且未见重伤，是气机与心神俱得其养之象。循此节律，可徐图长久。"

            else ->
                "观阁下$boneLabel：本周期起坛 ${stats.totalCount} 次，平均间隔 " +
                    "${"%.1f".format(stats.avgIntervalHours)} 时。气机尚可支撑，然未见明显增益，" +
                    "宜在功法与时辰上求质，而非求量。"
        }
        val recommend = when {
            !stats.hasData -> if (isMale) "吐纳静心" else "吐纳静心"
            stats.edgeControlCount * 4 >= stats.totalCount && stats.totalCount > 0 ->
                if (isMale) "锁龙御气" else "九浅一深"

            stats.severeCount > 0 || stats.maxDayCount >= 3 -> "吐纳静心"
            else -> stats.methodDistribution.firstOrNull()?.first ?: if (isMale) "锁龙御气" else "隔纱戏水"
        }
        val advice = buildString {
            if (stats.bestRecoveryDays > 0) {
                append("据本周期合格记录推算，阁下最佳恢复周期为 ${stats.bestRecoveryDays} 天。")
                append("下周期宜循此间隔起坛，不早于此数。")
            } else {
                append("本周期合格记录（无不良反应 + 道心圆满 ≥ 4.5 星）尚不足，暂无法推算黄金恢复周期。")
                append("宜先积累七日如实记录，再定节律。")
            }
            // 体位/时机守则只在正文（ReportEngine.diagnosisText）**没有**提到时才补，
            // 否则卷二会出现「忌盘龙俯卧…」连着说两遍的重复感。
            val posture = if (isMale) "忌盘龙俯卧式床褥压迫，宜以甘露沐身减少干摩擦。" else "行经期禁纳入式功法，宜改隔纱戏水。"
            val alreadySaid = diagnosis.contains("盘龙俯卧") || diagnosis.contains("隔纱戏水")
            if (!alreadySaid) append(posture)
        }
        return ScenarioDoc(
            sceneCode = SceneType.PERIODIC_REPORT.code,
            title = open,
            scrollOpen = open,
            body = diagnosis,
            pulse = pulse,
            recommendMethod = recommend,
            scheduleAdvice = advice,
            source = "local",
        )
    }

    /**
     * 双星间隔的人话表述。
     *
     * 为什么不能直接 "%.1f".format(小时)：间隔不足 1 小时会渲染成「0.0 时」，
     * 读起来像 bug。按量级切换单位：< 1 小时说「不足一时」或「约 N 刻」，
     * 1 小时以上仍按「X.X 时」。
     */
    private fun gapLabel(hours: Double?): String {
        if (hours == null) return "（前次未录，无从相较）"
        if (hours < 0) return "不足一时"
        if (hours < 1.0) {
            // 1 刻 = 15 分钟（设计文档 3.3 的双轨计时）
            val quarters = (hours * 4).toInt().coerceAtLeast(1)
            return if (quarters <= 1) "不足一时" else "约 $quarters 刻"
        }
        return "${"%.1f".format(hours)} 时"
    }
}
