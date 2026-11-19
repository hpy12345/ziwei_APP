package com.ziweistar.app.engine

import com.ziweistar.app.engine.model.AdverseEffect
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * 先天元气值（Vitality Qi Score, 0~100）动态微分算法
 *
 * 设计文档 6.2-1：
 *   Q_now = Clamp( 100 - Σ(B_i × M_i × D_i + P_i) × e^(-λ·Δt_i), 0, 100 )
 *
 *  - B_i 单次基础损耗：飞升登顶/九天揽月 = 22；锁龙御气/微醺即止 = 10；中途散功 = 4
 *  - M_i 功法强度系数（取第四章表格）
 *  - D_i 时长损耗系数：<15min = 0.9；15~40min = 1.0；>40min = 1.35
 *  - P_i 不良反应惩罚：器物磨损/头昏耳鸣/腰膝酸软 +25；神疲乏力 +15
 *  - λ = 0.035 / 小时（约 24h 恢复 57%，48h 基本充盈）
 */
object QiEngine {

    /** 恢复常数 λ */
    const val LAMBDA = 0.035

    /** 统计窗口：近 7 日（168 小时） */
    const val WINDOW_HOURS = 7 * 24

    /** 单次基础损耗 B_i */
    fun baseLoss(climaxClass: ClimaxClass): Int = when (climaxClass) {
        ClimaxClass.FULL -> 22    // 飞升登顶 / 九天揽月 / 一重飞升
        ClimaxClass.EDGE -> 10    // 锁龙御气 / 微醺即止
        ClimaxClass.ABORT -> 4    // 中途散功
    }

    /** 时长损耗系数 D_i */
    fun durationFactor(durationMinutes: Int): Double = when {
        durationMinutes < 15 -> 0.9
        durationMinutes <= 40 -> 1.0
        else -> 1.35
    }

    /** 不良反应惩罚项 P_i */
    fun adversePenalty(effects: Set<AdverseEffect>): Int {
        var p = 0
        if (effects.any { it.isSevere }) p += 25
        if (AdverseEffect.FATIGUE in effects) p += 15
        return p
    }

    /**
     * 单次记录对元气值的「当前损耗」（已按 Δt 衰减）。
     */
    fun decayedLoss(
        climaxClass: ClimaxClass,
        methodCoefficients: List<Double>,
        durationMinutes: Int,
        adverse: Set<AdverseEffect>,
        hoursAgo: Double,
    ): Double {
        val b = baseLoss(climaxClass).toDouble()
        // M_i 取所选功法的平均强度系数（多选时取均值，与设计文档「所习功法」数组语义一致）
        val m = if (methodCoefficients.isEmpty()) 1.0 else methodCoefficients.average()
        val d = durationFactor(durationMinutes)
        val p = adversePenalty(adverse).toDouble()
        val decay = exp(-LAMBDA * hoursAgo.coerceAtLeast(0.0))
        return (b * m * d + p) * decay
    }

    /**
     * 计算当前元气值。
     * @param entries 近 7 日内的所有记录
     * @param nowMillis 当前时刻
     */
    fun currentQi(entries: List<QiContribution>, nowMillis: Long): Int {
        if (entries.isEmpty()) return 100
        var total = 0.0
        for (e in entries) {
            val hoursAgo = (nowMillis - e.timestampMillis).toDouble() / 3_600_000.0
            if (hoursAgo < 0 || hoursAgo > WINDOW_HOURS) continue
            // 静养记录（谨遵法旨 / 闭关打坐）不是损耗而是回补，直接抵减总额
            if (e.qiRestore > 0.0) {
                total -= e.qiRestore
                continue
            }
            total += decayedLoss(
                climaxClass = e.climaxClass,
                methodCoefficients = e.methodCoefficients,
                durationMinutes = e.durationMinutes,
                adverse = e.adverse,
                hoursAgo = hoursAgo,
            )
        }
        return (100.0 - total).roundToInt().coerceIn(0, 100)
    }

    /** 元气值定性描述（首页半环灵气条文字） */
    fun qiLabel(qi: Int): String = when {
        qi >= 85 -> "充盈"
        qi >= 70 -> "尚足"
        qi >= 55 -> "平稳"
        qi >= 40 -> "微弱"
        qi >= 20 -> "枯竭"
        else -> "将尽"
    }

    /** 元气槽配色分档（青莲 / 泥金 / 朱砂） */
    fun qiBand(qi: Int): QiBand = when {
        qi >= 70 -> QiBand.FULL
        qi >= 35 -> QiBand.MODERATE
        else -> QiBand.DEPLETED
    }

    enum class QiBand { FULL, MODERATE, DEPLETED }

    /**
     * 元气恢复周期推算（周报/月报用）：从完全枯竭恢复到 85 分所需小时数。
     * ln(100/15)/0.035 ≈ 54.3h
     */
    fun recoveryHours(target: Int = 85): Double {
        val remaining = (100 - target).coerceAtLeast(1).toDouble()
        return kotlin.math.ln(100.0 / remaining) / LAMBDA
    }
}

/** 单次基础损耗分类 */
enum class ClimaxClass { FULL, EDGE, ABORT }

/** 计算元气值所需的最小记录投影 */
data class QiContribution(
    val timestampMillis: Long,
    val climaxClass: ClimaxClass,
    val methodCoefficients: List<Double>,
    val durationMinutes: Int,
    val adverse: Set<AdverseEffect>,
    /**
     * 该记录带来的元气回补量（静养记录专用，如「谨遵法旨」+5）。
     * 大于 0 时按回补处理、不再计损耗；普通起坛记录恒为 0。
     */
    val qiRestore: Double = 0.0,
)
