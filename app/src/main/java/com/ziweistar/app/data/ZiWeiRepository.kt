package com.ziweistar.app.data

import android.content.Context
import com.ziweistar.app.data.db.ProfileEntity
import com.ziweistar.app.data.db.StarFigureEntity
import com.ziweistar.app.data.db.ZiWeiDatabase
import com.ziweistar.app.engine.AlmanacEngine
import com.ziweistar.app.engine.EngineInput
import com.ziweistar.app.engine.StarFigure
import com.ziweistar.app.engine.model.AdverseEffect
import com.ziweistar.app.engine.model.AlmanacSnapshot
import com.ziweistar.app.engine.model.ClimaxFemale
import com.ziweistar.app.engine.model.ClimaxMale
import com.ziweistar.app.engine.model.MenstrualPhase
import com.ziweistar.app.engine.model.MethodLibrary
import com.ziweistar.app.engine.model.PrivacyTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * 聚合仓库：把 DB 实体与引擎输入组装起来。
 */
class ZiWeiRepository(
    private val context: Context,
    private val db: ZiWeiDatabase,
) {
    val profileDao get() = db.profileDao()
    val sessionDao get() = db.sessionDao()
    val starDao get() = db.starFigureDao()
    val oracleDao get() = db.oracleDao()
    val reportDao get() = db.reportDao()

    fun observeProfile(): Flow<ProfileEntity?> = profileDao.observe()

    fun observeSessions(): Flow<List<com.ziweistar.app.data.db.SessionEntity>> =
        sessionDao.observeAll()

    fun observeStars(): Flow<List<StarFigureEntity>> = starDao.observeAll()

    /** 首页状态聚合 */
    fun observeHomeState(nowProvider: () -> Long = { System.currentTimeMillis() }): Flow<HomeState?> =
        combine(observeProfile(), observeSessions(), observeStars()) { profile, sessions, stars ->
            if (profile == null) return@combine null
            buildHomeState(profile, sessions, stars, nowProvider())
        }.flowOn(Dispatchers.Default)

    suspend fun buildHomeStateOnce(now: Long = System.currentTimeMillis()): HomeState? {
        val profile = profileDao.get() ?: return null
        val sessions = sessionDao.since(now - 30L * 24 * 3600_000L)
        val stars = starDao.getAll()
        return withContext(Dispatchers.Default) { buildHomeState(profile, sessions, stars, now) }
    }

    private fun buildHomeState(
        profile: ProfileEntity,
        sessions: List<com.ziweistar.app.data.db.SessionEntity>,
        stars: List<StarFigureEntity>,
        now: Long,
    ): HomeState {        val almanac = AlmanacEngine.snapshot(Date(now))
        val todayKey = almanac.solarDate
        // 「谨遵法旨」插入的静养记录不算一次起坛，否则会被自己推进天罡戒律
        val todaySessions = sessions.filter { it.dayKey == todayKey && !it.isRestRecord() }
        val qi = DomainMapper.currentQi(sessions, now)
        val streak = DomainMapper.computeStreak(sessions, now)

        val starFigures = stars.map { e ->
            val related = sessions.filter { it.starId == e.id }
            DomainMapper.starFigureOf(e).copy(
                usageCount = related.size,
                avgRating = if (related.isEmpty()) 0.0
                else related.sumOf { DomainMapper.satisfactionStars(it) } / related.size,
            )
        }

        return HomeState(
            profile = profile,
            almanac = almanac,
            qi = qi,
            streakDays = streak,
            todayCount = todaySessions.size,
            lastSession = sessions.maxByOrNull { it.endedAt },
            todaySessions = todaySessions,
            starFigures = starFigures,
            recentSessions = sessions,
            maxStreak = DomainMapper.maxStreakEver(sessions),
        )
    }

    /**
     * 构造规则引擎输入。
     */
    fun buildEngineInput(state: HomeState, now: Long = System.currentTimeMillis()): EngineInput {
        val p = state.profile
        val isMale = p.isMale
        val recent = state.recentSessions
        val hoursSince = DomainMapper.hoursSinceLast(recent, now)
        val last = recent.maxByOrNull { it.endedAt }
        val lastAdverse = last?.let { DomainMapper.adverseOf(it) } ?: emptySet()

        return EngineInput(
            userId = p.userRef,
            gender = if (isMale) com.ziweistar.app.engine.model.Gender.QIAN
            else com.ziweistar.app.engine.model.Gender.KUN,
            natal = DomainMapper.toNatalChart(p),
            relationship = DomainMapper.relationshipOf(p),
            partnerFrequency = DomainMapper.partnerFrequencyOf(p),
            privacyTier = DomainMapper.privacyTierOf(p),
            heightCm = p.heightCm,
            weightKg = p.weightKg,
            boneType = DomainMapper.boneTypeOf(p),
            flaccidLengthCm = p.flaccidLengthCm,
            flaccidGirthCm = p.flaccidGirthCm,
            erectLengthCm = p.erectLengthCm,
            erectGirthCm = p.erectGirthCm,
            cupSize = DomainMapper.cupSizeOf(p),
            jadeGateShape = DomainMapper.jadeGateOf(p),
            menstrualPhase = DomainMapper.menstrualOf(p),
            periodDiscomfort = false,
            qi = state.qi,
            streakDays = state.streakDays,
            todayCount = state.todayCount,
            yesterdayCount = DomainMapper.countYesterday(recent, now),
            hoursSinceLast = hoursSince,
            lastHadSevereAdverse = lastAdverse.any { it.isSevere },
            lastAdverse = lastAdverse,
            starFigures = state.starFigures,
            almanac = state.almanac,
        )
    }

    // ---------- 写入 ----------

    suspend fun saveProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        profileDao.upsert(profile)
    }

    suspend fun updatePrivacyTier(tier: PrivacyTier) = withContext(Dispatchers.IO) {
        profileDao.updatePrivacyTier(tier.name)
    }

    suspend fun saveSession(session: com.ziweistar.app.data.db.SessionEntity): Long =
        withContext(Dispatchers.IO) { sessionDao.insert(session) }

    suspend fun updateSession(session: com.ziweistar.app.data.db.SessionEntity) =
        withContext(Dispatchers.IO) { sessionDao.update(session) }

    suspend fun saveStarFigure(e: StarFigureEntity) = withContext(Dispatchers.IO) { starDao.upsert(e) }

    suspend fun deleteStarFigure(id: String) = withContext(Dispatchers.IO) { starDao.delete(id) }

    suspend fun saveOracle(e: com.ziweistar.app.data.db.DailyOracleEntity) =
        withContext(Dispatchers.IO) { oracleDao.upsert(e) }

    suspend fun oracleFor(dayKey: String) = withContext(Dispatchers.IO) { oracleDao.get(dayKey) }

    suspend fun saveReport(e: com.ziweistar.app.data.db.PeriodicReportEntity) =
        withContext(Dispatchers.IO) { reportDao.upsert(e) }

    suspend fun reportFor(key: String) = withContext(Dispatchers.IO) { reportDao.get(key) }

    /** 焚毁天机：彻底抹除所有本地数据 */
    suspend fun destroyAll() = withContext(Dispatchers.IO) {
        runCatching { sessionDao.clear() }
        runCatching { starDao.clear() }
        runCatching { oracleDao.clear() }
        runCatching { reportDao.clear() }
        runCatching { profileDao.clear() }
        AvatarStore.wipeAll(context)
    }
}

/**
 * 首页聚合状态
 */
data class HomeState(
    val profile: ProfileEntity,
    val almanac: AlmanacSnapshot,
    val qi: Int,
    val streakDays: Int,
    val todayCount: Int,
    val lastSession: com.ziweistar.app.data.db.SessionEntity?,
    val todaySessions: List<com.ziweistar.app.data.db.SessionEntity>,
    val starFigures: List<StarFigure>,
    val recentSessions: List<com.ziweistar.app.data.db.SessionEntity>,
    val maxStreak: Int,
) {
    val isMale: Boolean get() = profile.isMale

    /** 天罡戒律是否封锁（单日满3次 / 元气 < 20） */
    val isTianggangSealed: Boolean get() = todayCount >= 3 || qi < 20
}

/** 是否为「闭关养神」静养记录（methodCodes == "REST"），这类记录不计入起坛次数 */
fun com.ziweistar.app.data.db.SessionEntity.isRestRecord(): Boolean =
    methodCodes.trim() == "REST"
