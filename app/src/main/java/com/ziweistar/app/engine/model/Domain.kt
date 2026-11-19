package com.ziweistar.app.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ============================================================
 * 紫薇星 · 领域模型（Domain Model）
 * 严格对应《紫薇星_APP完整产品设计方案_优化版v2.md》第一章黑话释义表
 * ============================================================
 */

/** 乾坤定局（性别） */
enum class Gender(val label: String, val classicLabel: String) {
    QIAN("男", "乾造"),
    KUN("女", "坤造");

    val isMale get() = this == QIAN
}

/** 红尘羁绊（感情状态） */
enum class RelationshipStatus(val label: String, val classicLabel: String) {
    SINGLE("单身", "孤星独耀"),
    PARTNERED("稳定伴侣", "双星伴月"),
    LONG_DISTANCE("异地恋", "银河相隔"),
}

/** 双修频次（伴侣性爱频率） */
enum class PartnerFrequency(val label: String, val classicLabel: String, val isHighIntensity: Boolean) {
    CHASTE("无伴侣性生活", "清心寡欲", false),
    RARE("每月1~2次", "久旱逢甘", false),
    WEEKLY("每周1~2次", "小别胜新婚", false),
    OFTEN("每周3~5次", "如胶似漆", true),
    NIGHTLY("几乎每日", "夜夜春宵", true),
}

/** 骨相（BMI 自动换算） */
enum class BoneType(val label: String, val classicLabel: String) {
    QINGQI("清奇骨相", "清奇骨相"),
    YUNTING("匀停骨相", "匀停骨相"),
    FENGLONG("丰隆骨相", "丰隆骨相");

    companion object {
        fun fromBmi(bmi: Double): BoneType = when {
            bmi < 18.5 -> QINGQI
            bmi < 24.0 -> YUNTING
            else -> FENGLONG
        }
    }

    /** 骨相定性批注（引擎与 Prompt 共用） */
    val physiqueHint: String
        get() = when (this) {
            QINGQI -> "气血偏虚，忌久战耗神，宜省力法器或轻柔指法"
            YUNTING -> "阴阳均衡，百无禁忌"
            FENGLONG -> "阳气浑厚但易燥热出汗，忌俯卧压迫心肺，极宜温水沐浴或慢速御气"
        }
}

/** 潜飞膨胀比之龙型（乾造专属） */
enum class DragonShape(val label: String) {
    BLOOD("血肉龙"),  // Grower: 比值 >= 1.6
    BONE("筋骨龙");   // Shower: 比值 < 1.6
}

/** 乾造龙根尺寸定局 */
enum class PhallusTier(val label: String) {
    MAJESTIC("雄伟型"),
    REFINED("精巧敏锐型"),
    BALANCED("匀停型");
}

/** 坤造玉门形制 */
enum class JadeGateShape(val label: String, val hint: String) {
    ONE_LINE("一线天", "闭合型：小阴唇完全包拢于大阴唇内，宜负压吸吮或隔层轻抚"),
    BUDDING("含苞待放", "微露型：小阴唇微露，娇嫩敏感，宜指腹精准点按"),
    BUTTERFLY("蝴蝶展翼", "舒展型：小阴唇丰满外展，包裹摩擦感强，需充沛润滑或水流"),
    ASYMMETRIC("不对称美", "单侧舒展型，自然灵动，宜多角度律动探索"),
    PEARL("珠玑显露", "阴蒂包皮较薄、阴蒂头易直接触碰型，极易登顶，忌干涩强震"),
}

/** 坤造云峰规模（罩杯） */
enum class CupSize(val label: String) {
    AA("AA"), A("A"), B("B"), C("C"), D("D"), E("E"), F("F及以上");

    val isVoluptuous get() = this >= C
}

/** 坤造月信潮汐 */
enum class MenstrualPhase(val label: String) {
    NONE("平常"),
    PERIOD("行经期（癸水至）"),
    OVULATION("排卵期（氤氲期）");
}

/**
 * 功法宗派（四大宗派）
 */
enum class MethodSect(val label: String, val desc: String) {
    UNARMED("徒手宗", "纯手部技巧"),
    BODY("体术宗", "躯干姿势挤压"),
    TOOL("法器宗", "外用器具辅助"),
    SECRET("秘传宗", "进阶与多点开发"),
}

/**
 * 标准化功法（系统内置全集，禁止手打）
 * @param code 唯一标准编码 Method_ID
 * @param name 功法名称（主标签）
 * @param subtitle 现代生理操作释义（副标题小字）
 * @param coefficient 引擎强度系数 M_i
 * @param forMale 适用性别（true = 乾造专用）
 */
data class Method(
    val code: String,
    val name: String,
    val subtitle: String,
    val coefficient: Double,
    val sect: MethodSect,
    val forMale: Boolean,
    /** 高危：高频勾选触发预警（如 盘龙俯卧） */
    val highRisk: Boolean = false,
)

/**
 * 男女标准化功法全集（对应设计文档第四章 4.1 / 4.2）
 */
object MethodLibrary {

    /** 坤造【徒手宗】的编码前缀是 F_Hxx，归属 sect 映射时统一归入四大宗派 */
    val MALE: List<Method> = listOf(
        // 【徒手宗】
        Method("M_H01", "五指正宗", "惯用手常规正握上下套弄", 1.0, MethodSect.UNARMED, true),
        Method("M_H02", "太极换手", "非惯用手操作，或左右双手交替接力新奇刺激", 1.0, MethodSect.UNARMED, true),
        Method("M_H03", "反手倒拔", "虎口朝下（反握式）套弄，改变系带与冠状沟受力角度", 1.2, MethodSect.UNARMED, true),
        Method("M_H04", "灵犀点穴", "仅用指腹配合润滑液，精准打圈摩擦龟头冠与系带敏感区", 0.7, MethodSect.UNARMED, true),
        // 【体术宗】
        Method("M_B01", "盘龙俯卧", "俯卧趴床，借助枕头或床垫推腰挤压摩擦（高频勾选触发高危预警）", 1.8, MethodSect.BODY, true, highRisk = true),
        Method("M_B02", "双腿夹山", "大腿并拢夹紧阴茎，通过腿部肌群收缩与滑动刺激", 1.3, MethodSect.BODY, true),
        // 【法器宗】
        Method("M_T01", "玉杵吞吐", "手动飞机杯 / 慢玩胶体通道包裹抽插", 1.3, MethodSect.TOOL, true),
        Method("M_T02", "天机电动", "电动伸缩 / 旋转 / 脉冲吸吮类智能飞机杯", 1.5, MethodSect.TOOL, true),
        Method("M_T03", "金环锁阳", "佩戴锁精环 / 震动延时环辅助充血", 1.3, MethodSect.TOOL, true),
        Method("M_T04", "甘露沐身", "全程配合充沛水溶性/硅基润滑液进行丝滑游走（防磨损护盾）", 0.8, MethodSect.TOOL, true),
        // 【秘传宗】
        Method("M_S01", "锁龙御气", "边缘控制（Edging，多次逼近射精临界点后停手冷却）", 0.6, MethodSect.SECRET, true),
        Method("M_S02", "走水淋漓", "浴室莲蓬头温水柱冲击敏感区", 0.8, MethodSect.SECRET, true),
        Method("M_S03", "后庭寻龙", "前列腺按摩器 / 手指后庭探索（开发男性G点与干性高潮）", 1.4, MethodSect.SECRET, true),
        Method("M_S04", "双穴同修", "揉捏乳首 / 轻托睾丸与会阴部，配合前端同步刺激", 1.1, MethodSect.SECRET, true),
    )

    val FEMALE: List<Method> = listOf(
        // 【指柔宗】
        Method("F_H01", "拈花揉珠", "指腹直接（或隔着阴蒂包皮）轻柔画圈、点按阴蒂头", 0.8, MethodSect.UNARMED, false),
        Method("F_H02", "拨云抚翼", "手指上下滑动，整体抚弄大小阴唇与外阴前庭", 0.8, MethodSect.UNARMED, false),
        Method("F_H03", "隔纱戏水", "隔着内裤布料进行外部按压与摩擦（温和缓冲防过度刺激）", 0.6, MethodSect.UNARMED, false),
        Method("F_H04", "灵指探幽", "单指或多指探入阴道内壁，勾动探索前壁（G点）或深处", 1.1, MethodSect.UNARMED, false),
        // 【体术宗】
        Method("F_B01", "双莲并蒂", "夹腿自慰，双腿交叉并拢绷紧，利用大腿根部肌群律动挤压外阴", 1.1, MethodSect.BODY, false),
        Method("F_B02", "玉枕骑乘", "骑跨于枕头、被角、玩偶或床沿进行磨蹭施压", 1.2, MethodSect.BODY, false),
        Method("F_B03", "提肛引气", "配合盆底肌（凯格尔/PC肌）的自主高频收缩产生内部快感", 0.5, MethodSect.BODY, false),
        // 【法器宗】
        Method("F_T01", "凌波吸吮", "气压脉冲吸吮类玩具（如小章鱼/玫瑰花，无接触包裹阴蒂）", 1.3, MethodSect.TOOL, false),
        Method("F_T02", "雷音震颤", "跳蛋 / 仙女棒（AV棒）紧贴外阴进行高频震动", 1.5, MethodSect.TOOL, false),
        Method("F_T03", "玉龙入海", "仿真阳具 / 按摩棒 / 玻璃棒纳入阴道抽插或旋转", 1.3, MethodSect.TOOL, false),
        Method("F_T04", "双修法器", "C型/兔耳型震动棒（内部纳入填满的同时，外端同步震动阴蒂）", 1.6, MethodSect.TOOL, false),
        // 【秘传宗】
        Method("F_S01", "抚雪寻梅", "重点揉捏、轻捻胸部与乳首，与下身刺激联动", 0.7, MethodSect.SECRET, false),
        Method("F_S02", "清泉濯玉", "浴室花洒水流精准冲击阴蒂与外阴", 0.9, MethodSect.SECRET, false),
        Method("F_S03", "九浅一深", "女性边缘控制，在即将登顶瞬间撤离刺激，积蓄多重高潮势能", 1.0, MethodSect.SECRET, false),
        Method("F_S04", "后庭秘境", "后庭拉珠 / 尾部塞 / 肛周抚触联合刺激", 1.4, MethodSect.SECRET, false),
    )

    /** 大凶专用：闭关静心功法（不在统计库中，仅作推荐） */
    val RETREAT_METHODS = listOf("闭关打坐", "吐纳静心")

    private val byName: Map<String, Method> =
        (MALE + FEMALE).associateBy { it.name }

    fun forGender(isMale: Boolean): List<Method> = if (isMale) MALE else FEMALE

    fun byName(name: String): Method? = byName[name]

    fun coefficientOf(name: String): Double = byName[name]?.coefficient ?: 1.0

    fun sectOf(name: String): MethodSect? = byName[name]?.sect

    fun all(): List<Method> = MALE + FEMALE
}

/** 所借灵媒（内容形式） */
enum class MediumKind(val label: String, val classicLabel: String) {
    FANTASY("纯凭神游（脑补）", "纯凭神游"),
    VIDEO("春宫影像（真人视频）", "春宫影像"),
    ASMR("天籁传音（有声ASMR）", "天籁传音"),
    COMIC("秘戏图册（漫画/本子）", "秘戏图册"),
    NOVEL("香艳话本（文字小说）", "香艳话本"),
    INTERACTIVE("双人连麦/图文互动", "双人互动"),
}

/** 所处道场（自慰地点） */
enum class VenueKind(val label: String, val classicLabel: String) {
    BEDROOM("自家龙榻（卧室床铺）", "自家龙榻"),
    BATHROOM("沐浴净室（浴室/卫生间）", "沐浴净室"),
    HOTEL("客栈行宫（酒店）", "客栈行宫"),
    DESK("书房案前（电脑桌前）", "书房案前"),
    SOFA("客厅软榻（沙发）", "客厅软榻"),
}

/** 登顶造化（乾造） */
enum class ClimaxMale(val label: String, val classicLabel: String) {
    FLY("飞升登顶（射精）", "飞升登顶"),
    EDGE("锁龙御气（边缘未射）", "锁龙御气"),
    ABORT("中途散功（放弃）", "中途散功"),
}

/** 登顶造化（坤造） */
enum class ClimaxFemale(val label: String, val classicLabel: String) {
    SINGLE_PEAK("一重飞升（单次高潮）", "一重飞升"),
    MULTI("九天揽月（多重高潮）", "九天揽月"),
    BUZZ("微醺即止（未高潮但舒适）", "微醺即止"),
    ABORT("中途散功（放弃）", "中途散功"),
}

/** 不良反应 */
enum class AdverseEffect(val label: String, val classicLabel: String, val penalty: Int) {
    NONE("气息平稳（神清气爽，无不良反应）", "气息平稳", 0),
    WAIST("腰膝酸软", "腰膝酸软", 25),
    DIZZY("头昏目眩/耳鸣", "头昏耳鸣", 25),
    FATIGUE("神疲乏力/嗜睡", "神疲乏力", 15),
    WEAR("器物磨损（局部摩擦痛/充血红肿）", "器物磨损", 25),
    GUILT("道心不稳（空虚感/负罪感）", "道心不稳", 0),
    DISTRACT("心神涣散（注意力难以集中）", "心神涣散", 0);

    val isSevere get() = this in setOf(WAIST, DIZZY, WEAR)
}

/** 星宿属性标签（自定义图鉴四大标准属性） */
enum class StarAttribute(val label: String) {
    PURE_LOVE("纯爱故人"),
    PASSION("热烈御姐"),
    FORBIDDEN("禁忌幻想"),
    FAIRY("虚拟仙子");

    /** 木火日优先匹配的类别 */
    val woodFireAligned get() = this in setOf(PASSION, FORBIDDEN)

    /** 土金水日优先匹配的类别 */
    val metalWaterAligned get() = this in setOf(PURE_LOVE, FAIRY)
}

/** 吉凶四印 */
enum class Fortune(val label: String, val fullLabel: String, val trigram: String) {
    GREAT("大吉", "【大吉 · 宜观星】", "乾坤交泰 · 飞龙在天"),
    SMALL("小吉", "【小吉 · 宜浅尝】", "水天需待 · 温火烹茶"),
    NEUTRAL("平", "【平 · 顺应本心】", "云淡风轻 · 进退自如"),
    OMINOUS("大凶", "【大凶 · 忌妄动】", "天地否塞 · 潜龙勿用");

    val isAuspicious get() = this == GREAT || this == SMALL
    val isOminous get() = this == OMINOUS
}

/** 洞府（时辰 + 空间） */
enum class CaveType(val label: String, val caveId: String) {
    BATH("亥时(21-23点) · 沐浴净室（浴室温水）", "BATH"),
    BED("戌时(19-21点) · 自家龙榻（温暖被窝）", "BED"),
    DARK("子时(23-01点) · 幽暗静室（私密暗室）", "DARK"),
    RETREAT("戌时(19-21点) · 清心卧榻（只眠不施法）", "RETREAT");

    /** 对应 VenueKind 推荐（结算抽屉预填） */
    val venue: VenueKind
        get() = when (this) {
            BATH -> VenueKind.BATHROOM
            BED -> VenueKind.BEDROOM
            DARK -> VenueKind.DESK
            RETREAT -> VenueKind.BEDROOM
        }
}

/** 意境主题 */
enum class RealmTheme(val label: String, val id: String) {
    FORBIDDEN_CROSSING("【禁忌越界 · 感官张力】", "FORBIDDEN"),
    REUNION("【旧日重逢 · 耳鬓厮磨】", "REUNION"),
    STILL_WATER("【澄心止水 · 归根复命】", "STILL");

    companion object {
        fun fromId(id: String): RealmTheme = entries.firstOrNull { it.id == id } ?: STILL_WATER
    }
}

/** 默认预设星宿（图鉴为空时的系统虚拟星宿） */
data class PresetStar(val name: String, val attribute: StarAttribute)

object PresetStars {
    val YAOCHI_CHILIAN = PresetStar("瑶池赤练仙", StarAttribute.PASSION)
    val GUANGHAN_BAIYUE = PresetStar("广寒白月光", StarAttribute.PURE_LOVE)
    /** 大凶专用 */
    val QINGXIN_MINGYUE = PresetStar("清心明月", StarAttribute.PURE_LOVE)
}

/** 档案的隐私授权级别（天机盟约三级） */
enum class PrivacyTier(val label: String, val desc: String) {
    FULL("允准，开启深度定制", "批文贴合本人禀赋，只把笼统说法发出去"),
    LOCAL_ONLY("仅本地保存，不传 AI", "数值只用于本机统计，远程批文按通用模板写"),
    DECLINE("婉拒，仅凭生辰推演", "跳过身体尺码，直接按生辰八字推演");

    val sendsPhysiqueToAi get() = this == FULL
}
