package com.ziweistar.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ziweistar.app.ZiWeiApp
import com.ziweistar.app.data.AvatarStore
import com.ziweistar.app.data.DomainMapper
import com.ziweistar.app.data.HomeState
import com.ziweistar.app.data.isRestRecord
import com.ziweistar.app.data.db.DailyOracleEntity
import com.ziweistar.app.data.db.ProfileEntity
import com.ziweistar.app.data.db.SessionEntity
import com.ziweistar.app.data.db.StarFigureEntity
import com.ziweistar.app.engine.AlmanacEngine
import com.ziweistar.app.engine.GeneratedOracle
import com.ziweistar.app.engine.OraclePipeline
import com.ziweistar.app.engine.ReportEngine
import com.ziweistar.app.engine.ScenarioPipeline
import com.ziweistar.app.engine.llm.LlmRenderer
import com.ziweistar.app.engine.llm.PromptPayloadBuilder
import com.ziweistar.app.engine.llm.ScenarioDoc
import com.ziweistar.app.engine.model.AdverseEffect
import com.ziweistar.app.engine.model.BoneType
import com.ziweistar.app.engine.model.ClimaxFemale
import com.ziweistar.app.engine.model.ClimaxMale
import com.ziweistar.app.engine.model.CupSize
import com.ziweistar.app.engine.model.DecisionLock
import com.ziweistar.app.engine.model.Fortune
import com.ziweistar.app.engine.model.Gender
import com.ziweistar.app.engine.model.JadeGateShape
import com.ziweistar.app.engine.model.MenstrualPhase
import com.ziweistar.app.engine.model.NatalChart
import com.ziweistar.app.engine.model.PartnerFrequency
import com.ziweistar.app.engine.model.PrivacyTier
import com.ziweistar.app.engine.model.RelationshipStatus
import com.ziweistar.app.engine.model.RenderedOracle
import com.ziweistar.app.engine.model.StarAttribute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * 全局 ViewModel：承载立命盘草稿、首页状态、灵签、计时与归档。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ZiWeiApp.repo(app)

    // ---------------- 首页 ----------------

    private val _home = MutableStateFlow<HomeState?>(null)
    val home: StateFlow<HomeState?> = _home.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** 今日灵签（已生成） */
    private val _oracle = MutableStateFlow<GeneratedOracle?>(null)
    val oracle: StateFlow<GeneratedOracle?> = _oracle.asStateFlow()

    private val _oracleLoading = MutableStateFlow(false)
    val oracleLoading: StateFlow<Boolean> = _oracleLoading.asStateFlow()

    /** 灵签是否已展开（求签动作后为 true） */
    private val _oracleRevealed = MutableStateFlow(false)
    val oracleRevealed: StateFlow<Boolean> = _oracleRevealed.asStateFlow()

    /** 天罡戒律是否已通过静心咒解锁 */
    private val _tianggangReleased = MutableStateFlow(false)
    val tianggangReleased: StateFlow<Boolean> = _tianggangReleased.asStateFlow()

    private val _profile = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeHomeState().collect { state ->
                _home.value = state
                _loading.value = false
            }
        }
        viewModelScope.launch {
            repo.observeProfile().collect { p ->
                _profile.value = p
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val state = repo.buildHomeStateOnce()
            _home.value = state
            _loading.value = false
        }
    }

    // ---------------- 立命盘草稿 ----------------

    data class OnboardingDraft(
        val isMale: Boolean = true,
        val birthYear: Int = 1995,
        val birthMonth: Int = 6,
        val birthDay: Int = 15,
        val birthHour: Int = 12,
        val birthMinute: Int = 0,
        /** 时辰不详：勾选后统一按子时（00:00）推演，年/月/日三柱仍为实际值 */
        val birthHourUnknown: Boolean = false,
        val relationship: RelationshipStatus = RelationshipStatus.SINGLE,
        val privacyTier: PrivacyTier = PrivacyTier.FULL,
        val heightCm: Int = 172,
        val weightKg: Int = 65,
        val partnerFrequency: PartnerFrequency = PartnerFrequency.CHASTE,
        val flaccidLengthCm: Float = 8.0f,
        val flaccidGirthCm: Float = 9.0f,
        val erectLengthCm: Float = 14.0f,
        val erectGirthCm: Float = 12.0f,
        val cupSize: CupSize = CupSize.B,
        val jadeGateShape: JadeGateShape = JadeGateShape.BUDDING,
        val menstrualPhase: MenstrualPhase = MenstrualPhase.NONE,
    ) {
        val bmi: Double
            get() {
                val h = heightCm / 100.0
                return if (h <= 0) 0.0 else weightKg / (h * h)
            }

        val boneType: BoneType get() = BoneType.fromBmi(bmi)

        val natalPreview: NatalChart
            get() = AlmanacEngine.previewBirth(
                birthYear, birthMonth, birthDay, birthHour, birthMinute,
            )
    }

    private val _draft = MutableStateFlow(OnboardingDraft())
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    fun updateDraft(block: (OnboardingDraft) -> OnboardingDraft) {
        _draft.value = block(_draft.value)
    }

    fun isOnboarded(): Boolean = _profile.value?.onboardingDone == true

    fun completeOnboarding(onDone: () -> Unit) {
        val d = _draft.value
        viewModelScope.launch {
            val natal = withContext(Dispatchers.Default) { d.natalPreview }
            val cal = Calendar.getInstance().apply {
                clear()
                set(d.birthYear, d.birthMonth - 1, d.birthDay, d.birthHour, d.birthMinute, 0)
            }
            val entity = ProfileEntity(
                id = 1,
                userRef = UUID.randomUUID().toString().take(12),
                isMale = d.isMale,
                birthMillis = cal.timeInMillis,
                fourPillars = natal.fourPillars,
                dayMaster = natal.dayMaster,
                dayMasterStrength = natal.dayMasterStrength,
                lunarBirthText = natal.lunarBirthText,
                relationship = d.relationship.name,
                partnerFrequency = d.partnerFrequency.name,
                privacyTier = d.privacyTier.name,
                heightCm = d.heightCm,
                weightKg = d.weightKg,
                boneType = d.boneType.name,
                flaccidLengthCm = if (d.isMale) d.flaccidLengthCm.toDouble() else null,
                flaccidGirthCm = if (d.isMale) d.flaccidGirthCm.toDouble() else null,
                erectLengthCm = if (d.isMale) d.erectLengthCm.toDouble() else null,
                erectGirthCm = if (d.isMale) d.erectGirthCm.toDouble() else null,
                cupSize = if (d.isMale) null else d.cupSize.name,
                jadeGateShape = if (d.isMale) null else d.jadeGateShape.name,
                menstrualPhase = if (d.isMale) MenstrualPhase.NONE.name else d.menstrualPhase.name,
                stealthIcon = "REAL",
                biometricLock = false,
                llmEndpoint = "",
                llmApiKey = "",
                llmModel = "",
                createdAt = System.currentTimeMillis(),
                onboardingDone = true,
            )
            repo.saveProfile(entity)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    // ---------------- 灵签 ----------------

    /**
     * 今日是否已开签。
     *
     * 用户反馈：一天只能摇一签，且摇过之后结果要一直展示。
     * 因此这里不再依赖内存里的 [_oracleRevealed]，而是以「当天是否已有落库灵签」为准，
     * 冷启动 / 切后台回来都能恢复到已开签状态。
     */
    private val _oracleDrawnToday = MutableStateFlow(false)
    val oracleDrawnToday: StateFlow<Boolean> = _oracleDrawnToday.asStateFlow()

    fun ensureOracle() {
        val state = _home.value ?: return
        val dayKey = state.almanac.solarDate
        if (_oracle.value != null && _oracle.value?.oracle?.dayKey == dayKey) {
            // 内存里已经是今天的签，直接确保解锁状态与之一致
            if (_oracleDrawnToday.value) _oracleRevealed.value = true
            return
        }
        viewModelScope.launch {
            // 先查缓存
            val cached = repo.oracleFor(dayKey)
            if (cached != null) {
                val lock = OraclePipeline.lockFromJson(cached.decisionLockJson)
                val rendered = cached.renderedJson?.let { OraclePipeline.oracleFromJson(it) }
                if (lock != null && rendered != null) {
                    _oracle.value = GeneratedOracle(
                        lock = lock,
                        oracle = rendered,
                        report = com.ziweistar.app.engine.llm.OracleValidator.ValidationReport(
                            ok = true, anchorAudit = rendered.anchorAudit,
                        ),
                        degraded = cached.source == "local",
                        failureReason = null,
                    )
                    // 缓存命中 = 今天已经求过签
                    _oracleDrawnToday.value = true
                    _oracleRevealed.value = true
                    return@launch
                }
            }
            // 今天还没求过：静默备签（不算开签，等用户点/摇）
            _oracleDrawnToday.value = false
            drawOracle(state, persist = false)
        }
    }

    /**
     * 求签（摇一摇 / 点击）：一天只允许一次。
     *
     * - 当天已开签：[force] 为 false 时直接展开既有结果，不重新抽取；
     * - 当天未开签：抽取并落库，后续冷启由 [ensureOracle] 恢复。
     */
    fun drawNewOracle(force: Boolean = false) {
        val state = _home.value ?: return
        val dayKey = state.almanac.solarDate
        if (!force && _oracleDrawnToday.value) {
            // 今日已开签，仅展开既有结果
            _oracleRevealed.value = true
            return
        }
        _oracleRevealed.value = true
        _oracleDrawnToday.value = true
        viewModelScope.launch {
            val current = _oracle.value
            if (current != null && current.oracle.dayKey == dayKey && !force) {
                // 已有今日签，直接落库并返回，避免重复消耗 LLM 额度
                persistOracle(current)
            } else {
                drawOracle(state, persist = true)
            }
        }
    }

    /**
     * 摇一摇专用入口：今日已开签时不响应（由 UI 侧禁用，这里再兜一层）。
     */
    fun shakeForOracle() {
        if (_oracleDrawnToday.value) {
            _oracleRevealed.value = true
            return
        }
        drawNewOracle(force = false)
    }

    fun revealOracle() {
        _oracleRevealed.value = true
        if (_oracle.value == null) ensureOracle()
    }

    private suspend fun persistOracle(generated: GeneratedOracle) {
        repo.saveOracle(
            DailyOracleEntity(
                dayKey = generated.oracle.dayKey,
                decisionLockJson = OraclePipeline.lockToJson(generated.lock),
                renderedJson = OraclePipeline.oracleToJson(generated.oracle),
                source = generated.oracle.source,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun drawOracle(state: HomeState, persist: Boolean) {
        _oracleLoading.value = true
        try {
            val input = repo.buildEngineInput(state)
            val renderer = rendererOf(state.profile)
            val allowLlm = DomainMapper.privacyTierOf(state.profile) != PrivacyTier.DECLINE &&
                state.profile.llmEndpoint.isNotBlank()

            val generated = withContext(Dispatchers.Default) {
                OraclePipeline.generate(
                    input = input,
                    renderer = renderer,
                    allowLlm = allowLlm,
                    dayKey = state.almanac.solarDate,
                )
            }
            _oracle.value = generated

            if (persist) persistOracle(generated)
        } finally {
            _oracleLoading.value = false
        }
    }

    /** 统一的 LLM 渲染器构造（未配置则返回 null，全链路自动降级） */
    private fun rendererOf(p: ProfileEntity): LlmRenderer? =
        if (p.llmEndpoint.isNotBlank() && p.llmApiKey.isNotBlank()) {
            LlmRenderer(
                endpoint = p.llmEndpoint,
                apiKey = p.llmApiKey,
                model = p.llmModel.ifBlank { "gpt-4o-mini" },
                timeoutSeconds = 20,
            )
        } else null

    /** 肉身禀赋定性（场景批文共用，绝不含原始尺寸数字） */
    private fun physiqueFeatureOf(state: HomeState): String {
        val p = state.profile
        return PromptPayloadBuilder.physiqueFeature(
            gender = if (p.isMale) Gender.QIAN else Gender.KUN,
            boneType = DomainMapper.boneTypeOf(p),
            heightCm = p.heightCm,
            weightKg = p.weightKg,
            erectLengthCm = p.erectLengthCm,
            erectGirthCm = p.erectGirthCm,
            flaccidLengthCm = p.flaccidLengthCm,
            cupSize = DomainMapper.cupSizeOf(p),
            jadeGateShape = DomainMapper.jadeGateOf(p),
            privacyTierSendsPhysique = DomainMapper.privacyTierOf(p).sendsPhysiqueToAi,
        )
    }

    // ---------------- 计时 ----------------

    private val _timerStart = MutableStateFlow<Long?>(null)
    val timerStart: StateFlow<Long?> = _timerStart.asStateFlow()

    fun beginSession() {
        _timerStart.value = System.currentTimeMillis()
    }

    fun clearTimer() {
        _timerStart.value = null
    }

    // ---------------- 归档 ----------------

    data class ArchiveDraft(
        val methodNames: List<String> = emptyList(),
        val starId: String? = null,
        val starName: String = "",
        val starAttribute: StarAttribute = StarAttribute.PURE_LOVE,
        val mediums: List<String> = emptyList(),
        val venue: String = "",
        val climax: String = "",
        val satisfaction: Float = 3.5f,
        val adverse: Set<AdverseEffect> = setOf(AdverseEffect.NONE),
        val forced: Boolean = false,
        val followedOracle: Boolean = false,
    )

    private val _archive = MutableStateFlow(ArchiveDraft())
    val archive: StateFlow<ArchiveDraft> = _archive.asStateFlow()

    fun resetArchive() {
        _archive.value = ArchiveDraft()
    }

    fun updateArchive(block: (ArchiveDraft) -> ArchiveDraft) {
        _archive.value = block(_archive.value)
    }

    /** 依签归档：用今日灵签的锦囊一键预填 */
    fun prefillFromOracle() {
        val state = _home.value ?: return
        val generated = _oracle.value ?: return
        val lock = generated.lock
        val isMale = state.isMale

        // 功法：取铁券中的功法名，过滤掉「闭关打坐/吐纳静心」这种非记录项
        val methods = lock.method.labels.filter { name ->
            com.ziweistar.app.engine.model.MethodLibrary.byName(name) != null
        }.ifEmpty {
            com.ziweistar.app.engine.model.MethodLibrary.forGender(isMale)
                .take(1).map { it.name }
        }
        // 星宿
        val starAttr = runCatching { StarAttribute.valueOf(lock.star.attribute) }
            .getOrDefault(StarAttribute.PURE_LOVE)
        // 灵媒
        val medium = lock.realm.medium
        val venueLabel = lock.cave.label.substringAfter("· ").substringBefore("（")

        val climax = if (isMale) {
            when (lock.fortune) {
                Fortune.OMINOUS.fullLabel -> ClimaxMale.ABORT.name
                else -> ClimaxMale.FLY.name
            }
        } else {
            when (lock.fortune) {
                Fortune.OMINOUS.fullLabel -> ClimaxFemale.ABORT.name
                else -> ClimaxFemale.SINGLE_PEAK.name
            }
        }

        _archive.value = _archive.value.copy(
            methodNames = methods,
            starId = lock.star.starId,
            starName = lock.star.name,
            starAttribute = starAttr,
            mediums = listOfNotNull(
                com.ziweistar.app.engine.model.MediumKind.entries
                    .firstOrNull { medium.contains(it.classicLabel) }?.label,
            ),
            venue = com.ziweistar.app.engine.model.VenueKind.entries
                .firstOrNull { venueLabel.contains(it.classicLabel) }?.label
                ?: com.ziweistar.app.engine.model.VenueKind.BEDROOM.label,
            climax = climax,
            followedOracle = true,
        )
    }

    /** 保存归档，返回判定结果 */
    data class ArchiveResult(
        val meikai: Boolean,
        val sessionId: Long,
        val qi: Int,
        /** 归档后是否已触发天罡戒律（触发时应由天罡黄符接管，而非入定引导） */
        val tianggang: Boolean = false,
    )

    // ---------------- 场景批文 ----------------

    /** 梅开二度贺卡批文（LLM 渲染失败时自动落到 LocalScenarioLibrary） */
    private val _meikaiDoc = MutableStateFlow<ScenarioDoc?>(null)
    val meikaiDoc: StateFlow<ScenarioDoc?> = _meikaiDoc.asStateFlow()

    /** 天罡戒律黄符批文 */
    private val _tianggangDoc = MutableStateFlow<ScenarioDoc?>(null)
    val tianggangDoc: StateFlow<ScenarioDoc?> = _tianggangDoc.asStateFlow()

    /**
     * 触发生成梅开二度批文。
     * 取最近两次record：倒序第一条为本次、第二条为上一次。
     */
    fun loadMeikaiDoc() {
        val state = _home.value ?: return
        viewModelScope.launch {
            // 先清空：避免上一次的贺词残留，让卡片先显示预设文案、批文到了再替换
            _meikaiDoc.value = null
            val sessions = state.recentSessions.sortedByDescending { it.endedAt }
            val thisOne = sessions.getOrNull(0)
            val prevOne = sessions.getOrNull(1)
            // 双星间隔：本次结束时刻 − 上一次结束时刻（小时）
            val gapHours = if (thisOne != null && prevOne != null) {
                (thisOne.endedAt - prevOne.endedAt).toDouble() / 3_600_000.0
            } else null
            val input = repo.buildEngineInput(state)
            val doc = withContext(Dispatchers.Default) {
                ScenarioPipeline.secondBloom(
                    input = input,
                    renderer = rendererOf(state.profile),
                    privacyTier = DomainMapper.privacyTierOf(state.profile),
                    physiqueFeature = physiqueFeatureOf(state),
                    firstStarName = prevOne?.starName ?: "前尘未录",
                    secondStarName = thisOne?.starName ?: "无相神游",
                    firstMethods = prevOne?.methodNames?.split(",")?.filter { it.isNotBlank() }
                        ?: emptyList(),
                    secondMethods = thisOne?.methodNames?.split(",")?.filter { it.isNotBlank() }
                        ?: emptyList(),
                    hoursSinceLast = gapHours,
                    todayCount = state.todayCount,
                    dayKey = state.almanac.solarDate,
                )
            }
            _meikaiDoc.value = doc
        }
    }

    /** 触发生成天罡戒律批文 */
    fun loadTianggangDoc() {
        val state = _home.value ?: return
        viewModelScope.launch {
            _tianggangDoc.value = null
            val input = repo.buildEngineInput(state)
            // 历史高频不良反应：全量记录统计 top3
            val historical = state.recentSessions
                .flatMap { DomainMapper.adverseOf(it) }
                .filter { it != AdverseEffect.NONE }
                .groupingBy { it.classicLabel }
                .eachCount()
                .entries.sortedByDescending { it.value }
                .take(3)
                .map { "${it.key}×${it.value}" }
            val doc = withContext(Dispatchers.Default) {
                ScenarioPipeline.tianggang(
                    input = input,
                    renderer = rendererOf(state.profile),
                    privacyTier = DomainMapper.privacyTierOf(state.profile),
                    physiqueFeature = physiqueFeatureOf(state),
                    todayCount = state.todayCount,
                    historicalAdverse = historical,
                    dayKey = state.almanac.solarDate,
                )
            }
            _tianggangDoc.value = doc
        }
    }

    fun saveArchive(
        startedAt: Long,
        endedAt: Long,
        durationSeconds: Int,
        pausedSeconds: Int,
        onDone: (ArchiveResult) -> Unit,
    ) {
        val d = _archive.value
        val state = _home.value ?: return
        viewModelScope.launch {
            val dayKey = AlmanacEngine.formatDate(Date(endedAt))
            val codes = d.methodNames.mapNotNull {
                com.ziweistar.app.engine.model.MethodLibrary.byName(it)?.code
            }
            val entity = SessionEntity(
                startedAt = startedAt,
                endedAt = endedAt,
                dayKey = dayKey,
                durationSeconds = durationSeconds,
                pausedSeconds = pausedSeconds,
                methodCodes = codes.joinToString(","),
                methodNames = d.methodNames.joinToString(","),
                starId = d.starId,
                starName = d.starName.ifBlank { "无相神游" },
                starAttribute = d.starAttribute.name,
                mediums = d.mediums.joinToString(","),
                venue = d.venue,
                climax = d.climax.ifBlank {
                    if (state.isMale) ClimaxMale.FLY.name else ClimaxFemale.SINGLE_PEAK.name
                },
                satisfactionTenths = Math.round(d.satisfaction * 2f).toInt(),
                adverse = d.adverse.filter { it != AdverseEffect.NONE }
                    .joinToString(",") { it.name },
                forced = d.forced,
                followedOracle = d.followedOracle,
                fortuneLabel = _oracle.value?.oracle?.fortuneShort ?: "",
            )
            val id = repo.saveSession(entity)

            // 判定梅开二度：距上次不足 6 小时，或同日第 2 次。
            // 但当日第 3 次、第 4 次起（state.todayCount >= 2）不再重复弹出。
            // state.todayCount 是保存前的今日次数，故 <2 表示本次至多是当日第 2 次。
            val last = state.recentSessions.maxByOrNull { it.endedAt }
            val within6h = last != null && (endedAt - last.endedAt) < 6L * 3_600_000L
            val secondToday = state.todayCount >= 1
            val noSevere = d.adverse.none { it.isSevere }
            val meikai = (within6h || secondToday) && noSevere && state.todayCount < 2

            val newState = repo.buildHomeStateOnce()
            _home.value = newState
            _timerStart.value = null
            _tianggangReleased.value = false
            resetArchive()

            withContext(Dispatchers.Main) {
                onDone(
                    ArchiveResult(
                        meikai = meikai,
                        sessionId = id,
                        qi = newState?.qi ?: 100,
                        tianggang = newState?.isTianggangSealed == true,
                    ),
                )
            }
        }
    }

    /** 谨遵法旨，闭关养神：+5 元气奖励（通过插入一条「吐纳静心」的低耗记录实现） */
    fun acceptRetreat() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val dayKey = AlmanacEngine.formatDate(Date(now))
            repo.saveSession(
                SessionEntity(
                    startedAt = now - 60_000L,
                    endedAt = now,
                    dayKey = dayKey,
                    durationSeconds = 60,
                    pausedSeconds = 0,
                    methodCodes = "REST",
                    methodNames = "吐纳静心",
                    starId = null,
                    starName = "清心明月",
                    starAttribute = StarAttribute.PURE_LOVE.name,
                    mediums = "",
                    venue = com.ziweistar.app.engine.model.VenueKind.BEDROOM.label,
                    climax = ClimaxMale.ABORT.name,
                    satisfactionTenths = 6,
                    adverse = "",
                    forced = false,
                    followedOracle = true,
                    fortuneLabel = "大凶",
                ),
            )
            _home.value = repo.buildHomeStateOnce()
        }
    }

    fun releaseTianggang() {
        _tianggangReleased.value = true
    }

    // ---------------- 星宿图鉴 ----------------

    private val _starBitmaps = MutableStateFlow<Map<String, android.graphics.Bitmap?>>(emptyMap())
    val starBitmaps: StateFlow<Map<String, android.graphics.Bitmap?>> = _starBitmaps.asStateFlow()

    fun loadStarBitmaps() {
        val stars = _home.value?.starFigures ?: return
        viewModelScope.launch {
            val map = withContext(Dispatchers.IO) {
                stars.associate { it.id to AvatarStore.load(getApplication(), it.avatarPath) }
            }
            _starBitmaps.value = map
        }
    }

    fun addStar(
        name: String,
        attribute: StarAttribute,
        uri: android.net.Uri?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val path = if (uri != null) {
                withContext(Dispatchers.IO) { AvatarStore.importAndStore(getApplication(), uri) }
            } else null
            repo.saveStarFigure(
                StarFigureEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    attribute = attribute.name,
                    avatarPath = path,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            _home.value = repo.buildHomeStateOnce()
            loadStarBitmaps()
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun deleteStar(id: String) {
        viewModelScope.launch {
            val star = _home.value?.starFigures?.firstOrNull { it.id == id }
            repo.deleteStarFigure(id)
            star?.avatarPath?.let { AvatarStore.delete(getApplication(), it) }
            _home.value = repo.buildHomeStateOnce()
            loadStarBitmaps()
        }
    }

    // ---------------- 设置 ----------------

    fun updatePrivacyTier(tier: PrivacyTier) {
        viewModelScope.launch { repo.updatePrivacyTier(tier) }
    }

    fun updateLlm(endpoint: String, apiKey: String, model: String) {
        val p = _profile.value ?: return
        viewModelScope.launch {
            repo.saveProfile(
                p.copy(llmEndpoint = endpoint, llmApiKey = apiKey, llmModel = model),
            )
        }
    }

    fun updateStealthIcon(alias: String) {
        val p = _profile.value ?: return
        viewModelScope.launch {
            repo.saveProfile(p.copy(stealthIcon = alias))
            com.ziweistar.app.ui.alias.AliasManager.apply(
                getApplication(),
                com.ziweistar.app.ui.alias.IconAlias.of(alias),
            )
        }
    }

    fun updateBiometric(enabled: Boolean) {
        val p = _profile.value ?: return
        viewModelScope.launch { repo.saveProfile(p.copy(biometricLock = enabled)) }
    }

    fun burnEverything(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.destroyAll()
            com.ziweistar.app.ui.alias.AliasManager.resetToReal(getApplication())
            _home.value = null
            _oracle.value = null
            _oracleRevealed.value = false
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    // ---------------- 报告 ----------------

    private val _reportStats = MutableStateFlow<Pair<ReportEngine.Period, ReportEngine.Stats>?>(null)
    val reportStats: StateFlow<Pair<ReportEngine.Period, ReportEngine.Stats>?> =
        _reportStats.asStateFlow()

    private val _reportText = MutableStateFlow("")
    val reportText: StateFlow<String> = _reportText.asStateFlow()

    /** 卷二·钦天监诊脉的分栏批文（卷首语 / 心境洞察 / 精气诊脉 / 下期指引） */
    private val _reportDoc = MutableStateFlow<ScenarioDoc?>(null)
    val reportDoc: StateFlow<ScenarioDoc?> = _reportDoc.asStateFlow()

    fun loadReport(kind: ReportEngine.Kind, anchorMillis: Long = System.currentTimeMillis()) {
        val state = _home.value ?: return
        viewModelScope.launch {
            val period = if (kind == ReportEngine.Kind.WEEK) ReportEngine.weekOf(anchorMillis)
            else ReportEngine.monthOf(anchorMillis)
            val sessions = state.recentSessions
            val stats = withContext(Dispatchers.Default) {
                ReportEngine.compute(period, sessions, state.isMale)
            }
            _reportStats.value = period to stats

            // 本地兜底批文先算出来（无论走不走 LLM 都需要）
            val localDiagnosis = withContext(Dispatchers.Default) {
                ReportEngine.diagnosisText(period, stats, state.profile, state.isMale)
            }
            _reportText.value = localDiagnosis

            // 缓存命中则直接使用（含本地兜底与 LLM 两种来源）
            // 但只有内容版本号一致的缓存才算数：文案改过就作废重算，否则用户永远读不到新文案。
            val cached = repo.reportFor(period.key)
            if (cached != null && cached.payloadJson.isNotBlank()) {
                val doc = runCatching {
                    ScenarioJson.decodeFromString(ScenarioDoc.serializer(), cached.payloadJson)
                }.getOrNull()
                if (doc != null && doc.contentVersion == ScenarioDoc.CONTENT_VERSION) {
                    _reportDoc.value = doc
                    return@launch
                }
            }

            val input = repo.buildEngineInput(state)
            val doc = withContext(Dispatchers.Default) {
                ScenarioPipeline.periodic(
                    input = input,
                    renderer = rendererOf(state.profile),
                    privacyTier = DomainMapper.privacyTierOf(state.profile),
                    physiqueFeature = physiqueFeatureOf(state),
                    period = period,
                    stats = stats,
                    localDiagnosis = localDiagnosis,
                    dayKey = state.almanac.solarDate,
                )
            }
            _reportDoc.value = doc
            if (doc.body.isNotBlank()) _reportText.value = doc.body
            runCatching {
                // 落库前盖上当前内容版本号，下次读到旧版本缓存就会自动重算
                val stamped = doc.copy(contentVersion = ScenarioDoc.CONTENT_VERSION)
                repo.saveReport(
                    com.ziweistar.app.data.db.PeriodicReportEntity(
                        periodKey = period.key,
                        kind = kind.name,
                        payloadJson = ScenarioJson.encodeToString(ScenarioDoc.serializer(), stamped),
                        source = doc.source,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    /** 星历数据：最近 N 天的每日统计 */
    data class DayCell(
        val dayKey: String,
        val dayOfMonth: Int,
        val count: Int,
        val qiDelta: Int,
        val isToday: Boolean,
    )

    fun buildCalendarCells(year: Int, month: Int): List<DayCell?> {
        val sessions = _home.value?.recentSessions ?: emptyList()
        // 静养记录（谨遵法旨）不是起坛，不能占日历格子，否则「戒字印」会被自己刷出来
        val byDay = sessions.filter { !it.isRestRecord() }.groupBy { it.dayKey }
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday
        val todayKey = AlmanacEngine.formatDate(Date())

        val cells = mutableListOf<DayCell?>()
        // 周一开头：Sunday(1) → 6 个空格
        val leading = if (firstDow == Calendar.SUNDAY) 6 else firstDow - Calendar.MONDAY
        repeat(leading) { cells.add(null) }
        for (d in 1..maxDay) {
            val key = String.format("%04d-%02d-%02d", year, month, d)
            val list = byDay[key] ?: emptyList()
            cells.add(
                DayCell(
                    dayKey = key,
                    dayOfMonth = d,
                    count = list.size,
                    qiDelta = list.size,
                    isToday = key == todayKey,
                ),
            )
        }
        return cells
    }

    companion object {
        /**
         * 场景批文序列化用 Json（不可变，全类共享一个实例）。
         * 提成常量避免每次 loadReport 都重建 format 实例（Kotlin 编译警告 Redundant creation of Json format）。
         */
        private val ScenarioJson = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
}
