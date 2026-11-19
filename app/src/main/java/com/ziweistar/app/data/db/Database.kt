package com.ziweistar.app.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * ============================================================
 * 本地加密数据库（SQLCipher + Room）
 * 设计文档第七章：本地数据库与图鉴沙盒加密
 * ============================================================
 */

/** 用户命盘档案（单行，id 固定为 1） */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    /** 统计用的匿名用户标识（仅本地，用于确定性哈希种子） */
    val userRef: String,
    val isMale: Boolean,
    val birthMillis: Long,
    val fourPillars: String,
    val dayMaster: String,
    val dayMasterStrength: String,
    val lunarBirthText: String,
    val relationship: String,
    val partnerFrequency: String,
    val privacyTier: String,
    // 肉身法相
    val heightCm: Int?,
    val weightKg: Int?,
    val boneType: String,
    val flaccidLengthCm: Double?,
    val flaccidGirthCm: Double?,
    val erectLengthCm: Double?,
    val erectGirthCm: Double?,
    val cupSize: String?,
    val jadeGateShape: String?,
    val menstrualPhase: String,
    // 设置
    val stealthIcon: String,
    val biometricLock: Boolean,
    val llmEndpoint: String,
    val llmApiKey: String,
    val llmModel: String,
    val createdAt: Long,
    val onboardingDone: Boolean,
)

/** 单次观星记录 */
@Entity(
    tableName = "session",
    indices = [Index("startedAt"), Index("dayKey")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 起坛时刻 */
    val startedAt: Long,
    /** 收功时刻 */
    val endedAt: Long,
    /** 自然日键 yyyy-MM-dd（本地日切，用于同日计数与日历渲染） */
    val dayKey: String,
    /** 净耗时秒（扣除暂停） */
    val durationSeconds: Int,
    /** 累计暂停秒 */
    val pausedSeconds: Int,
    /** 所习功法 Method_ID 列表（逗号分隔，标准编码） */
    val methodCodes: String,
    /** 功法名称列表（冗余，便于报表直接展示） */
    val methodNames: String,
    /** 结缘星宿 id（自定义图鉴 UUID / 预设 none / 大凶 为 null） */
    val starId: String?,
    val starName: String,
    val starAttribute: String,
    /** 所借灵媒（逗号分隔） */
    val mediums: String,
    /** 所处道场 */
    val venue: String,
    /** 登顶造化（统一存储为枚举名） */
    val climax: String,
    /** 道心圆满度：1..10（半星精度，5.0 星 = 10） */
    val satisfactionTenths: Int,
    /** 不良反应（逗号分隔） */
    val adverse: String,
    /** 是否逆天强行（天罡戒律解锁后标记） */
    val forced: Boolean = false,
    /** 是否依签归档（一键预填） */
    val followedOracle: Boolean = false,
    /** 所属签文 id（当日灵签） */
    val fortuneLabel: String = "",
)

/** 自定义星宿图鉴 */
@Entity(tableName = "star_figure")
data class StarFigureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val attribute: String,
    /** 本地沙盒相对文件名（加密二进制，绝不外传） */
    val avatarPath: String?,
    val createdAt: Long,
)

/** 灵签缓存（当日唯一，本地模板 or LLM 渲染） */
@Entity(tableName = "daily_oracle")
data class DailyOracleEntity(
    @PrimaryKey val dayKey: String,
    val decisionLockJson: String,
    /** LLM 渲染结果 JSON；为空表示尚未渲染，使用本地模板 */
    val renderedJson: String?,
    /** 渲染来源：local / llm */
    val source: String,
    val createdAt: Long,
)

/** 周期报告缓存（周报 / 月报） */
@Entity(tableName = "periodic_report")
data class PeriodicReportEntity(
    @PrimaryKey val periodKey: String, // 2026-W46 / 2026-11
    val kind: String,                  // WEEK / MONTH
    val payloadJson: String,
    val source: String,
    val createdAt: Long,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clear()

    @Query("UPDATE profile SET privacyTier = :tier WHERE id = 1")
    suspend fun updatePrivacyTier(tier: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM session ORDER BY endedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE endedAt >= :fromMillis ORDER BY endedAt DESC")
    suspend fun since(fromMillis: Long): List<SessionEntity>

    @Query("SELECT * FROM session WHERE dayKey = :dayKey ORDER BY endedAt DESC")
    suspend fun forDay(dayKey: String): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM session WHERE dayKey = :dayKey")
    suspend fun countForDay(dayKey: String): Int

    @Query("SELECT * FROM session ORDER BY endedAt DESC LIMIT 1")
    suspend fun latest(): SessionEntity?

    @Query("SELECT * FROM session WHERE endedAt BETWEEN :from AND :to ORDER BY endedAt ASC")
    suspend fun between(from: Long, to: Long): List<SessionEntity>

    @Query("SELECT DISTINCT dayKey FROM session ORDER BY dayKey DESC")
    suspend fun allDayKeys(): List<String>

    @Query("SELECT * FROM session WHERE starId = :starId")
    suspend fun forStar(starId: String): List<SessionEntity>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("DELETE FROM session")
    suspend fun clear()
}

@Dao
interface StarFigureDao {
    @Query("SELECT * FROM star_figure ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<StarFigureEntity>>

    @Query("SELECT * FROM star_figure ORDER BY createdAt ASC")
    suspend fun getAll(): List<StarFigureEntity>

    @Query("SELECT * FROM star_figure WHERE id = :id")
    suspend fun get(id: String): StarFigureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(figure: StarFigureEntity)

    @Query("DELETE FROM star_figure WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM star_figure")
    suspend fun clear()
}

@Dao
interface OracleDao {
    @Query("SELECT * FROM daily_oracle WHERE dayKey = :dayKey")
    suspend fun get(dayKey: String): DailyOracleEntity?

    @Query("SELECT * FROM daily_oracle WHERE dayKey = :dayKey")
    fun observe(dayKey: String): Flow<DailyOracleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(oracle: DailyOracleEntity)

    @Query("DELETE FROM daily_oracle")
    suspend fun clear()
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM periodic_report WHERE periodKey = :key")
    suspend fun get(key: String): PeriodicReportEntity?

    @Query("SELECT * FROM periodic_report ORDER BY createdAt DESC")
    suspend fun all(): List<PeriodicReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: PeriodicReportEntity)

    @Query("DELETE FROM periodic_report")
    suspend fun clear()
}

@Database(
    entities = [
        ProfileEntity::class,
        SessionEntity::class,
        StarFigureEntity::class,
        DailyOracleEntity::class,
        PeriodicReportEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ZiWeiDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun starFigureDao(): StarFigureDao
    abstract fun oracleDao(): OracleDao
    abstract fun reportDao(): ReportDao

    companion object {
        const val NAME = "ziwei_vault.db"
    }
}
