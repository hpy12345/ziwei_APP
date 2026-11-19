package com.ziweistar.app.engine

import com.ziweistar.app.engine.model.*

/**
 * 规则引擎输入快照（已聚合的用户档案 + 节律状态）
 */
data class EngineInput(
    val userId: String,
    val gender: Gender,
    val natal: NatalChart,
    val relationship: RelationshipStatus,
    val partnerFrequency: PartnerFrequency,
    val privacyTier: PrivacyTier,

    // ---- 肉身法相（获授权后才有） ----
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val boneType: BoneType = BoneType.YUNTING,
    // 乾造
    val flaccidLengthCm: Double? = null,
    val flaccidGirthCm: Double? = null,
    val erectLengthCm: Double? = null,
    val erectGirthCm: Double? = null,
    // 坤造
    val cupSize: CupSize? = null,
    val jadeGateShape: JadeGateShape? = null,
    val menstrualPhase: MenstrualPhase = MenstrualPhase.NONE,
    /** 行经期伴不适（触发大凶兜底矩阵） */
    val periodDiscomfort: Boolean = false,

    // ---- 节律状态 ----
    val qi: Int = 100,
    val streakDays: Int = 0,
    val todayCount: Int = 0,
    val yesterdayCount: Int = 0,
    val hoursSinceLast: Double? = null,
    /** 上次记录是否含严重不良反应（磨损/耳鸣/腰酸） */
    val lastHadSevereAdverse: Boolean = false,
    val lastAdverse: Set<AdverseEffect> = emptySet(),

    // ---- 自定义星宿图鉴 ----
    val starFigures: List<StarFigure> = emptyList(),

    // ---- 今日黄历 ----
    val almanac: AlmanacSnapshot,
)

/** 自定义星宿图鉴对象 */
data class StarFigure(
    val id: String,
    val name: String,
    val attribute: StarAttribute,
    /** 本地沙盒相对路径（加密存储，绝不上传） */
    val avatarPath: String? = null,
    val usageCount: Int = 0,
    val avgRating: Double = 0.0,
)

/**
 * ============================================================
 * 本地确定性规则引擎（Pre-computation Engine）
 *
 * 严格实现设计文档 6.2-3 的【完整决策树与边界兜底矩阵】。
 * 所有判定均由代码精确计算，输出不可篡改的 DecisionLock。
 * ============================================================
 */
object DecisionEngine {

    // 建除十二神分组
    private val BREAK_CLOSE = setOf("破", "闭")
    private val AUSPICIOUS = setOf("成", "开", "满", "定")
    private val MILD = setOf("建", "除", "执", "收")
    private val FLAT = setOf("平", "危")

    /**
     * 主入口：计算今日定局铁券。
     */
    fun compute(input: EngineInput): DecisionLock {
        val fortune = decideFortune(input)
        val cave = decideCave(input, fortune)
        val method = decideMethod(input, fortune, cave)
        val star = decideStar(input, fortune)
        val realm = decideRealm(input, fortune)

        return DecisionLock(
            rulePathId = fortune.rulePathId,
            fortune = fortune.fortune.fullLabel,
            trigram = fortune.trigramText,
            cave = cave,
            method = method,
            star = star,
            realm = realm,
        )
    }

    // ============================================================
    // 【吉凶总印】P0 ~ P5
    // ============================================================
    private data class FortuneResult(
        val fortune: Fortune,
        val rulePathId: String,
        val trigramText: String,
    )

    private fun decideFortune(input: EngineInput): FortuneResult {
        val a = input.almanac

        // P0：高危压制
        val p0 = input.todayCount >= 2 ||
                input.yesterdayCount >= 2 ||
                input.streakDays >= 4 ||
                input.qi < 35 ||
                input.lastHadSevereAdverse ||
                (input.gender == Gender.KUN &&
                        input.menstrualPhase == MenstrualPhase.PERIOD &&
                        input.periodDiscomfort)
        if (p0) {
            val path = when {
                input.todayCount >= 2 -> "P0_TODAY_OVERLOAD"
                input.yesterdayCount >= 2 -> "P0_YESTERDAY_OVERLOAD"
                input.streakDays >= 4 -> "P0_STREAK_LIMIT"
                input.qi < 35 -> "P0_QI_DEPLETED"
                input.lastHadSevereAdverse -> "P0_SEVERE_ADVERSE"
                else -> "P0_MENSTRUAL_PROTECTION"
            }
            return FortuneResult(Fortune.OMINOUS, path, "天地否塞 · 潜龙勿用")
        }

        // P1：高频伴侣 + 48h 内
        val hours = input.hoursSinceLast
        if (input.partnerFrequency.isHighIntensity && hours != null && hours < 48.0) {
            return FortuneResult(Fortune.NEUTRAL, "P1_HIGH_FREQ_PARTNER", "阴阳既济 · 顺其自然")
        }

        // P2：破日 / 闭日
        if (a.zhiXing in BREAK_CLOSE) {
            return FortuneResult(Fortune.OMINOUS, "P2_BREAK_CLOSE_DAY", "气机闭塞 · 静守灵台")
        }

        // P3：吉日 + 元气充盈
        if (a.zhiXing in AUSPICIOUS && input.qi >= 70) {
            return FortuneResult(Fortune.GREAT, "P3_ALMANAC_AUSPICIOUS_HIGH_QI", "乾坤交泰 · 飞龙在天")
        }

        // P4：平缓日 或 (吉日但元气中段)
        if (a.zhiXing in MILD || (a.zhiXing in AUSPICIOUS && input.qi in 35..69)) {
            return FortuneResult(Fortune.SMALL, "P4_MILD_DAY_OR_MODERATE_QI", "水天需待 · 温火烹茶")
        }

        // P5：兜底（平日 / 危日 / 其他未命中）
        if (a.zhiXing in FLAT) {
            return FortuneResult(Fortune.NEUTRAL, "P5_FLAT_DAY_FALLBACK", "云淡风轻 · 进退自如")
        }

        return FortuneResult(Fortune.NEUTRAL, "P5_UNMATCHED_FALLBACK", "云淡风轻 · 进退自如")
    }

    // ============================================================
    // 【锦囊一：天时洞府】
    // ============================================================
    private fun decideCave(input: EngineInput, fortune: FortuneResult): CaveLock {
        if (fortune.fortune.isOminous) {
            return CaveLock(
                label = CaveType.RETREAT.label,
                caveId = CaveType.RETREAT.caveId,
                anchors = listOf("素榻", "清枕", "早安眠", "熄烛火"),
            )
        }
        return when (AlmanacEngine.xiuElementGroup(input.almanac.xiuZheng)) {
            AlmanacEngine.XiuGroup.WATER_MOON -> CaveLock(
                label = CaveType.BATH.label,
                caveId = CaveType.BATH.caveId,
                anchors = listOf("温汤", "水雾", "兰汤浴", "净室烟"),
            )
            AlmanacEngine.XiuGroup.FIRE_SUN -> CaveLock(
                label = CaveType.BED.label,
                caveId = CaveType.BED.caveId,
                anchors = listOf("锦衾", "暖帐", "芙蓉榻", "红罗帷"),
            )
            AlmanacEngine.XiuGroup.WOOD_METAL_EARTH -> CaveLock(
                label = CaveType.DARK.label,
                caveId = CaveType.DARK.caveId,
                anchors = listOf("子夜", "幽窗", "暗香浮", "静室深"),
            )
        }
    }

    // ============================================================
    // 【锦囊二：神通功法】
    // ============================================================
    private fun decideMethod(input: EngineInput, fortune: FortuneResult, cave: CaveLock): MethodLock {
        if (fortune.fortune.isOminous) {
            return MethodLock(
                labels = MethodLibrary.RETREAT_METHODS,
                sectClass = "SECRET",
                anchors = if (input.gender.isMale)
                    listOf("盘膝", "吐纳", "锁心猿", "定意马")
                else
                    listOf("盘膝", "吐纳", "敛芳华", "守玉台"),
            )
        }

        val caveId = cave.caveId
        val remaining = fortune.fortune == Fortune.GREAT ||
                fortune.fortune == Fortune.NEUTRAL

        return if (input.gender.isMale) decideMaleMethod(input, fortune, caveId, remaining)
        else decideFemaleMethod(input, fortune, caveId, remaining)
    }

    private fun decideMaleMethod(
        input: EngineInput,
        fortune: FortuneResult,
        caveId: String,
        remaining: Boolean,
    ): MethodLock {
        val erectLen = input.erectLengthCm ?: 0.0
        val erectGirth = input.erectGirthCm ?: 0.0
        val tier = PhallusTierResolver.resolve(erectLen, erectGirth)

        val (labels, anchors) = when (tier) {
            PhallusTier.MAJESTIC -> {
                if (remaining) Pair(
                    listOf("甘露沐身", "玉杵吞吐"),
                    listOf("甘露", "玉杵", "锁潜龙", "润金茎"),
                ) else Pair(
                    listOf("甘露沐身", "锁龙御气"),
                    listOf("甘露", "御气", "锁潜龙", "润金茎"),
                )
            }
            PhallusTier.REFINED -> {
                if (remaining) Pair(
                    listOf("灵犀点穴", "天机电动"),
                    listOf("灵犀", "点穴", "天机转", "叩玄关"),
                ) else Pair(
                    listOf("灵犀点穴", "锁龙御气"),
                    listOf("灵犀", "点穴", "御长气", "叩玄关"),
                )
            }
            PhallusTier.BALANCED -> {
                if (remaining) Pair(
                    listOf("太极换手", "玉杵吞吐"),
                    listOf("太极", "阴阳手", "玉杵", "掌中旋"),
                ) else Pair(
                    listOf("五指正宗", "锁龙御气"),
                    listOf("太极", "阴阳手", "御长气", "掌中旋"),
                )
            }
        }

        val finalLabels = labels.toMutableList()
        val finalAnchors = anchors.toMutableList()

        // 叠加修正：丰隆骨相 + 沐浴净室 → 追加「走水淋漓」，永久禁选「盘龙俯卧」
        if (input.boneType == BoneType.FENGLONG && caveId == CaveType.BATH.caveId) {
            if ("走水淋漓" !in finalLabels) finalLabels.add("走水淋漓")
            finalAnchors.add("走水")
            finalAnchors.add("飞泉")
            finalAnchors.add("洗凝脂")
        }
        // 丰隆骨相永久禁用 盘龙俯卧（忌俯卧压迫心肺）
        if (input.boneType == BoneType.FENGLONG) {
            finalLabels.remove("盘龙俯卧")
        }

        return MethodLock(
            labels = finalLabels.distinct(),
            sectClass = sectClassName(finalLabels),
            anchors = finalAnchors.distinct().take(6),
        )
    }

    private fun decideFemaleMethod(
        input: EngineInput,
        fortune: FortuneResult,
        caveId: String,
        remaining: Boolean,
    ): MethodLock {
        // P1：行经期（癸水至）且非大凶 → 强制外部轻柔，严禁纳入与水冲
        if (input.menstrualPhase == MenstrualPhase.PERIOD) {
            return MethodLock(
                labels = listOf("隔纱戏水", "抚雪寻梅"),
                sectClass = "HAND",
                anchors = listOf("隔纱", "护娇蕊", "轻拢慢捻"),
            )
        }

        val (labels, anchors) = when (input.jadeGateShape) {
            JadeGateShape.ONE_LINE, JadeGateShape.BUDDING -> {
                if (remaining) Pair(
                    if (input.jadeGateShape == JadeGateShape.BUDDING)
                        listOf("凌波吸吮", "拈花揉珠")
                    else listOf("凌波吸吮", "隔纱戏水"),
                    listOf("凌波", "揉明珠", "隔轻纱", "吸甘露"),
                ) else Pair(
                    listOf("隔纱戏水", "拈花揉珠"),
                    listOf("凌波", "揉明珠", "隔轻纱", "吸甘露"),
                )
            }
            JadeGateShape.BUTTERFLY, JadeGateShape.ASYMMETRIC -> {
                if (remaining) Pair(
                    listOf("双莲并蒂", "清泉濯玉"),
                    listOf("双莲", "濯清泉", "拨云霞", "九浅深"),
                ) else Pair(
                    listOf("拨云抚翼", "九浅一深"),
                    listOf("双莲", "濯清泉", "拨云霞", "九浅深"),
                )
            }
            JadeGateShape.PEARL -> {
                if (remaining) Pair(
                    listOf("双修法器", "雷音震颤"),
                    listOf("雷音", "双修器", "捻玉珠", "颤琼枝"),
                ) else Pair(
                    listOf("拈花揉珠", "九浅一深"),
                    listOf("雷音", "双修器", "捻玉珠", "颤琼枝"),
                )
            }
            null -> {
                // 未采集玉门形制（婉拒授权）→ 通用匀停组合
                if (remaining) Pair(
                    listOf("拈花揉珠", "拨云抚翼"),
                    listOf("灵珠", "抚云霞", "轻拢慢捻", "绕指柔"),
                ) else Pair(
                    listOf("隔纱戏水", "拈花揉珠"),
                    listOf("灵珠", "隔轻纱", "轻拢慢捻", "绕指柔"),
                )
            }
        }

        val finalLabels = labels.toMutableList()
        val finalAnchors = anchors.toMutableList()

        // 叠加修正：云峰罩杯 in [C,D,E,F及以上] → 数组末尾固定追加「抚雪寻梅」
        if (input.cupSize?.isVoluptuous == true && "抚雪寻梅" !in finalLabels) {
            finalLabels.add("抚雪寻梅")
            finalAnchors.add("抚雪")
            finalAnchors.add("寻寒梅")
            finalAnchors.add("酥山暖")
        }

        return MethodLock(
            labels = finalLabels.distinct().take(3),
            sectClass = sectClassName(finalLabels),
            anchors = finalAnchors.distinct().take(6),
        )
    }

    /** 把功法名数组映射为「功法大类」枚举（用于本地签诗模板键） */
    private fun sectClassName(labels: List<String>): String {
        val sects = labels.mapNotNull { MethodLibrary.sectOf(it) }
        val dominant = sects.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: MethodSect.SECRET
        return when (dominant) {
            MethodSect.UNARMED -> "HAND"
            MethodSect.BODY -> "BODY"
            MethodSect.TOOL -> "TOOL"
            MethodSect.SECRET -> "SECRET"
        }
    }

    // ============================================================
    // 【锦囊三：结缘星宿】
    // ============================================================
    private fun decideStar(input: EngineInput, fortune: FortuneResult): StarLock {
        // P0：大凶
        if (fortune.fortune.isOminous) {
            return StarLock(
                name = PresetStars.QINGXIN_MINGYUE.name,
                attribute = "独坐观心",
                anchors = listOf("孤月", "清风", "莫思量"),
                starId = null,
                avatarPath = null,
            )
        }

        val dateStr = input.almanac.solarDate

        // P1：自定义图鉴为空 → 系统预设虚拟星宿
        if (input.starFigures.isEmpty()) {
            val preset = if (input.almanac.isWoodFireDay) PresetStars.YAOCHI_CHILIAN
            else PresetStars.GUANGHAN_BAIYUE
            return StarLock(
                name = preset.name,
                attribute = preset.attribute.label,
                anchors = listOf("仙姿", "瑶池客", "月下人"),
                starId = null,
                avatarPath = null,
            )
        }

        // P2 / P3：按天干五行属性分组后哈希锁定
        val preferred = if (input.almanac.isWoodFireDay) {
            input.starFigures.filter { it.attribute.woodFireAligned }
        } else {
            input.starFigures.filter { it.attribute.metalWaterAligned }
        }

        val candidates = preferred.ifEmpty { input.starFigures }
        // 组内再按名称排序保证顺序稳定（避免 DB 返回顺序抖动影响哈希结果）
        val sorted = candidates.sortedBy { it.id }
        val picked = HashSelector.select(sorted, dateStr, input.userId, "STAR")
            ?: sorted.first()

        val anchors = if (input.almanac.isWoodFireDay) {
            listOf("红颜", "烈火", "惊鸿影", "越雷池")
        } else {
            listOf("故人", "旧梦", "白月光", "仙袂飘")
        }

        return StarLock(
            name = picked.name,
            attribute = picked.attribute.label,
            anchors = anchors,
            starId = picked.id,
            avatarPath = picked.avatarPath,
        )
    }

    // ============================================================
    // 【锦囊四：神游意境】
    // ============================================================
    private fun decideRealm(input: EngineInput, fortune: FortuneResult): RealmLock {
        if (fortune.fortune.isOminous) {
            return RealmLock(
                theme = RealmTheme.STILL_WATER.label,
                themeId = RealmTheme.STILL_WATER.id,
                medium = "静心白噪音/古琴曲",
                anchors = listOf("听松", "古琴冷", "一炉香", "万虑空"),
            )
        }

        return if (input.almanac.isWoodFireDay) {
            RealmLock(
                theme = RealmTheme.FORBIDDEN_CROSSING.label,
                themeId = RealmTheme.FORBIDDEN_CROSSING.id,
                medium = MediumKind.VIDEO.classicLabel,
                anchors = listOf("画卷", "秘戏", "春潮涌", "烛影摇"),
            )
        } else {
            RealmLock(
                theme = RealmTheme.REUNION.label,
                themeId = RealmTheme.REUNION.id,
                medium = MediumKind.ASMR.classicLabel,
                anchors = listOf("传音", "耳畔语", "旧诗篇", "梦魂萦"),
            )
        }
    }
}

/** 乾造龙根定局判定 */
object PhallusTierResolver {
    fun resolve(erectLengthCm: Double, erectGirthCm: Double): PhallusTier = when {
        erectLengthCm >= 15.0 || erectGirthCm >= 13.0 -> PhallusTier.MAJESTIC
        erectLengthCm in 0.01..11.99 -> PhallusTier.REFINED
        else -> PhallusTier.BALANCED
    }

    /** 潜飞膨胀比 → 血肉龙 / 筋骨龙 */
    fun dragonShape(flaccidLengthCm: Double, erectLengthCm: Double): DragonShape? {
        if (flaccidLengthCm <= 0.0 || erectLengthCm <= 0.0) return null
        val ratio = erectLengthCm / flaccidLengthCm
        return if (ratio >= 1.6) DragonShape.BLOOD else DragonShape.BONE
    }
}
