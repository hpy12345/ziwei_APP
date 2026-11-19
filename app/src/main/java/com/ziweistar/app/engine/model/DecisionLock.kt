package com.ziweistar.app.engine.model

import kotlinx.serialization.Serializable

/**
 * 钦天监定局铁券（Decision Lock JSON）
 *
 * 设计文档 6.2：本地确定性规则引擎在请求大模型之前，先算出不可篡改的 decision_lock。
 * LLM 只做文学渲染，禁止修改其中任何标签。
 */
@Serializable
data class DecisionLock(
    /** 触发规则路径ID，如 P3_ALMANAC_AUSPICIOUS_HIGH_QI */
    val rulePathId: String,
    /** 吉凶朱印 */
    val fortune: String,
    /** 卦象爻辞 */
    val trigram: String,
    /** 洞府定局 */
    val cave: CaveLock,
    /** 功法定局 */
    val method: MethodLock,
    /** 星宿定局 */
    val star: StarLock,
    /** 意境定局 */
    val realm: RealmLock,
)

@Serializable
data class CaveLock(
    val label: String,
    val caveId: String,
    val anchors: List<String>,
)

@Serializable
data class MethodLock(
    val labels: List<String>,
    val sectClass: String,
    val anchors: List<String>,
)

@Serializable
data class StarLock(
    val name: String,
    val attribute: String,
    val anchors: List<String>,
    /** 自定义图鉴对象的本地 UUID；预设虚拟星宿为 null */
    val starId: String? = null,
    /** 头像本地相对路径（仅本地展示，绝不外传） */
    val avatarPath: String? = null,
)

@Serializable
data class RealmLock(
    val theme: String,
    val themeId: String,
    val medium: String,
    val anchors: List<String>,
)

/**
 * 今日排盘的天文历法快照（本地计算，非 LLM 产出）
 */
@Serializable
data class AlmanacSnapshot(
    /** 公历 yyyy-MM-dd */
    val solarDate: String,
    /** 农历文本，如 丙午年十月初九 */
    val lunarText: String,
    /** 今日干支，如 癸酉日 */
    val ganZhiDay: String,
    val dayGan: String,
    val dayZhi: String,
    /** 建除十二神，如 收 */
    val zhiXing: String,
    /** 二十八星宿，如 娄 */
    val xiu: String,
    /** 星宿吉凶，如 吉 */
    val xiuLuck: String,
    /** 星宿七曜，如 金 */
    val xiuZheng: String,
    /** 星宿所属神兽，如 白虎 */
    val xiuShou: String,
    /** 星宿配属动物，如 虎 / 豹 / 貐（构成「壁水貐」这类全称） */
    val xiuAnimal: String = "",
    /** 节气名（当日若为节气则非空） */
    val jieQi: String,
    /** 上一个节气 */
    val prevJieQi: String,
    /** 下一个节气 */
    val nextJieQi: String,
    /** 月相，如 望 */
    val yueXiang: String,
    /** 今日宜 */
    val dayYi: List<String> = emptyList(),
    /** 今日忌 */
    val dayJi: List<String> = emptyList(),
) {
    /** 天干是否属木火（甲/乙/丙/丁） */
    val isWoodFireDay: Boolean
        get() = dayGan in setOf("甲", "乙", "丙", "丁")

    /** 天干是否属土金水（戊/己/庚/辛/壬/癸） */
    val isMetalWaterDay: Boolean
        get() = dayGan in setOf("戊", "己", "庚", "辛", "壬", "癸")
}

/**
 * 用户命盘（立命盘产物）
 */
@Serializable
data class NatalChart(
    /** 公历出生时间戳（毫秒） */
    val birthMillis: Long,
    /** 四柱八字，如 甲辰年 丙寅月 戊戌日 癸亥时 */
    val fourPillars: String,
    val yearPillar: String = "",
    val monthPillar: String = "",
    val dayPillar: String = "",
    val timePillar: String = "",
    /** 日主天干 */
    val dayMaster: String = "",
    /** 日主强弱定性 */
    val dayMasterStrength: String = "",
    /** 农历生日文本 */
    val lunarBirthText: String = "",
) {
    /** 完整八字描述，如 「甲辰年 丙寅月 戊戌日 癸亥时 · 日主戊土偏弱」 */
    val fullDescription: String
        get() = if (dayMasterStrength.isBlank()) fourPillars
        else "$fourPillars · 日主$dayMaster$dayMasterStrength"
}
