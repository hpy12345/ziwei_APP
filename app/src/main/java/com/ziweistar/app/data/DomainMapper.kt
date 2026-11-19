package com.ziweistar.app.data

import com.ziweistar.app.data.db.ProfileEntity
import com.ziweistar.app.data.db.SessionEntity
import com.ziweistar.app.data.db.StarFigureEntity
import com.ziweistar.app.engine.AlmanacEngine
import com.ziweistar.app.engine.ClimaxClass
import com.ziweistar.app.engine.QiContribution
import com.ziweistar.app.engine.QiEngine
import com.ziweistar.app.engine.model.AdverseEffect
import com.ziweistar.app.engine.model.BoneType
import com.ziweistar.app.engine.model.ClimaxFemale
import com.ziweistar.app.engine.model.ClimaxMale
import com.ziweistar.app.engine.model.CupSize
import com.ziweistar.app.engine.model.Gender
import com.ziweistar.app.engine.model.JadeGateShape
import com.ziweistar.app.engine.model.MenstrualPhase
import com.ziweistar.app.engine.model.MethodLibrary
import com.ziweistar.app.engine.model.NatalChart
import com.ziweistar.app.engine.model.PartnerFrequency
import com.ziweistar.app.engine.model.PrivacyTier
import com.ziweistar.app.engine.model.RelationshipStatus
import com.ziweistar.app.engine.model.StarAttribute
import com.ziweistar.app.engine.StarFigure
import java.util.Date

/**
 * 领域映射与统计计算（Repository 的纯函数部分）
 */
object DomainMapper {

    // ---------- 档案 ----------

    fun toNatalChart(p: ProfileEntity): NatalChart = NatalChart(
        birthMillis = p.birthMillis,
        fourPillars = p.fourPillars,
        dayMaster = p.dayMaster,
        dayMasterStrength = p.dayMasterStrength,
        lunarBirthText = p.lunarBirthText,
    )

    fun genderOf(p: ProfileEntity): Gender = if (p.isMale) Gender.QIAN else Gender.KUN

    fun relationshipOf(p: ProfileEntity): RelationshipStatus =
        enumValueOr(p.relationship, RelationshipStatus.SINGLE)

    fun partnerFrequencyOf(p: ProfileEntity): PartnerFrequency =
        enumValueOr(p.partnerFrequency, PartnerFrequency.CHASTE)

    fun privacyTierOf(p: ProfileEntity): PrivacyTier =
        enumValueOr(p.privacyTier, PrivacyTier.LOCAL_ONLY)

    fun boneTypeOf(p: ProfileEntity): BoneType =
        enumValueOr(p.boneType, BoneType.YUNTING)

    fun cupSizeOf(p: ProfileEntity): CupSize? = p.cupSize?.let { enumValueOr<CupSize>(it, CupSize.B) }

    fun jadeGateOf(p: ProfileEntity): JadeGateShape? =
        p.jadeGateShape?.let { enumValueOr<JadeGateShape>(it, JadeGateShape.BUDDING) }

    fun menstrualOf(p: ProfileEntity): MenstrualPhase =
        enumValueOr(p.menstrualPhase, MenstrualPhase.NONE)

    // ---------- 记录 ----------

    fun methodCodes(s: SessionEntity): List<String> =
        s.methodCodes.split(",").map { it.trim() }.filter { it.isNotBlank() }

    fun methodNames(s: SessionEntity): List<String> =
        s.methodNames.split(",").map { it.trim() }.filter { it.isNotBlank() }

    fun adverseOf(s: SessionEntity): Set<AdverseEffect> =
        s.adverse.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { name -> runCatching { AdverseEffect.valueOf(name) }.getOrNull() }
            .toSet()

    fun mediums(s: SessionEntity): List<String> =
        s.mediums.split(",").map { it.trim() }.filter { it.isNotBlank() }

    fun satisfactionStars(s: SessionEntity): Double = s.satisfactionTenths / 2.0

    /** 登顶造化 → 元气基础损耗分类 */
    fun climaxClassOf(s: SessionEntity): ClimaxClass = when {
        s.climax.endsWith("ABORT") -> ClimaxClass.ABORT
        s.climax.endsWith("EDGE") || s.climax.endsWith("BUZZ") -> ClimaxClass.EDGE
        else -> ClimaxClass.FULL
    }

    fun climaxLabel(s: SessionEntity, isMale: Boolean): String {
        if (isMale) {
            runCatching { ClimaxMale.valueOf(s.climax) }.getOrNull()?.let { return it.label }
        } else {
            runCatching { ClimaxFemale.valueOf(s.climax) }.getOrNull()?.let { return it.label }
        }
        return s.climax
    }

    fun starFigureOf(e: StarFigureEntity): StarFigure = StarFigure(
        id = e.id,
        name = e.name,
        attribute = enumValueOr(e.attribute, StarAttribute.PURE_LOVE),
        avatarPath = e.avatarPath,
        usageCount = 0,
        avgRating = 0.0,
    )

    /**
     * 把记录列表转换为元气值计算投影。
     */
    fun toQiContributions(sessions: List<com.ziweistar.app.data.db.SessionEntity>): List<QiContribution> =
        sessions.map { s ->
            val isRest = s.methodCodes.trim() == "REST"
            QiContribution(
                timestampMillis = s.endedAt,
                climaxClass = climaxClassOf(s),
                methodCoefficients = methodNames(s).map { MethodLibrary.coefficientOf(it) },
                durationMinutes = s.durationSeconds / 60,
                adverse = adverseOf(s),
                // 静养记录（谨遵法旨 / 闭关打坐）是回补而非损耗
                qiRestore = if (isRest) REST_QI_GAIN else 0.0,
            )
        }

    /** 「谨遵法旨 · 闭关养神」的元气回补量（设计文档 5.2：+5 元气） */
    const val REST_QI_GAIN = 5.0

    // ---------- 统计 ----------
    // 注意：以下统计一律排除「闭关养神」静养记录（isRestRecord），
    // 否则一次「谨遵法旨」就会凭空造出一段连击/一次观星日。

    /** 当前连击天数（连续观星 Day X） */
    fun computeStreak(sessions: List<SessionEntity>, nowMillis: Long): Int {
        val real = sessions.filter { !it.isRestRecord() }
        if (real.isEmpty()) return 0
        val dayKeys = real.map { it.dayKey }.toSortedSet(compareByDescending { it })
        if (dayKeys.isEmpty()) return 0

        val today = AlmanacEngine.formatDate(Date(nowMillis))
        val yesterday = AlmanacEngine.formatDate(Date(AlmanacEngine.startOfDay(nowMillis) - 86400_000L))

        // 今天或昨天有记录才算连击存活
        val cursor: String = when {
            dayKeys.contains(today) -> today
            dayKeys.contains(yesterday) -> yesterday
            else -> return 0
        }

        var streak = 0
        val set = dayKeys.toHashSet()
        var cursorDate = java.time.LocalDate.parse(cursor)
        while (set.contains(cursorDate.toString())) {
            streak++
            cursorDate = cursorDate.minusDays(1)
        }
        return streak
    }

    /** 昨日次数（用于 P0 矩阵） */
    fun countYesterday(sessions: List<SessionEntity>, nowMillis: Long): Int {
        val yesterday = AlmanacEngine.formatDate(Date(AlmanacEngine.startOfDay(nowMillis) - 86400_000L))
        return sessions.count { it.dayKey == yesterday && !it.isRestRecord() }
    }

    /** 距上次结束的小时数（只看真实起坛，「闭关养神」不应重置这个间隔） */
    fun hoursSinceLast(sessions: List<SessionEntity>, nowMillis: Long): Double? {
        val last = sessions.filter { !it.isRestRecord() }.maxByOrNull { it.endedAt } ?: return null
        return (nowMillis - last.endedAt).toDouble() / 3_600_000.0
    }

    /** 历史最高连击 */
    fun maxStreakEver(sessions: List<SessionEntity>): Int {
        val real = sessions.filter { !it.isRestRecord() }
        if (real.isEmpty()) return 0
        val days = real.map { it.dayKey }.toSortedSet()
        var best = 0
        var current = 0
        var prev: java.time.LocalDate? = null
        for (d in days) {
            val date = java.time.LocalDate.parse(d)
            current = if (prev != null && prev.plusDays(1) == date) current + 1 else 1
            best = maxOf(best, current)
            prev = date
        }
        return best
    }

    /** 当前元气值 */
    fun currentQi(sessions: List<SessionEntity>, nowMillis: Long): Int =
        QiEngine.currentQi(toQiContributions(sessions), nowMillis)

    inline fun <reified T : Enum<T>> enumValueOr(name: String, fallback: T): T =
        runCatching { enumValueOf<T>(name) }.getOrElse { fallback }
}
