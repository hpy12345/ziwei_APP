package com.ziweistar.app.engine.llm

import com.ziweistar.app.engine.model.AdverseEffect
import com.ziweistar.app.engine.model.AlmanacSnapshot
import com.ziweistar.app.engine.model.BoneType
import com.ziweistar.app.engine.model.CupSize
import com.ziweistar.app.engine.model.DecisionLock
import com.ziweistar.app.engine.model.Gender
import com.ziweistar.app.engine.model.JadeGateShape
import com.ziweistar.app.engine.model.NatalChart
import com.ziweistar.app.engine.model.PartnerFrequency
import com.ziweistar.app.engine.model.RelationshipStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 把用户档案 + 定局铁券 序列化为设计文档 6.3.1 规定的 Input Data JSON。
 * 只传送「玄门定性代号」，绝不上传原始尺寸数值（隐私铁律）。
 */
object PromptPayloadBuilder {

    private val json = Json { prettyPrint = false }

    fun buildDailyPayload(
        gender: Gender,
        natal: NatalChart,
        relationship: RelationshipStatus,
        partnerFrequency: PartnerFrequency,
        boneType: BoneType,
        physiqueFeature: String,
        qi: Int,
        streakDays: Int,
        todayCount: Int,
        hoursSinceLast: Double?,
        recentAdverse: List<AdverseEffect>,
        almanac: AlmanacSnapshot,
        lock: DecisionLock,
    ): String {
        val root = buildJsonObject {
            putJsonObject("用户档案摘要") {
                put("性别", gender.classicLabel)
                put("农历八字", natal.fullDescription)
                put("红尘羁绊", relationship.classicLabel)
                put("双修频次", partnerFrequency.classicLabel)
                put("骨相定性", boneType.classicLabel)
                put("肉身禀赋特征", physiqueFeature)
            }
            putJsonObject("近期节律状态") {
                put("先天元气值_Qi", qi)
                put("当前连击天数_Streak", streakDays)
                put("今日已观星次数", todayCount)
                if (hoursSinceLast != null) {
                    put("距上次观星间隔小时", (hoursSinceLast * 10).toInt() / 10.0)
                } else {
                    put("距上次观星间隔小时", -1)
                }
                putJsonArray("近期不良反应记录") {
                    recentAdverse.filter { it != AdverseEffect.NONE }
                        .forEach { add(it.classicLabel) }
                }
            }
            putJsonObject("今日黄历天时") {
                put("今日干支与五行", "${almanac.ganZhiDay} · ${almanac.xiuZheng}旺")
                put("今日建除十二神", almanac.zhiXing)
                put("今日二十八星宿", "${almanac.xiu}宿")
            }
            putJsonObject("钦天监定局铁券") {
                put("触发规则路径ID", lock.rulePathId)
                put("吉凶朱印", lock.fortune)
                put("卦象爻辞", lock.trigram)
                putJsonObject("洞府定局") {
                    put("选定标签", lock.cave.label)
                    putJsonArray("诗句必含词根") { lock.cave.anchors.forEach { add(it) } }
                }
                putJsonObject("功法定局") {
                    putJsonArray("选定标签") { lock.method.labels.forEach { add(it) } }
                    putJsonArray("诗句必含词根") { lock.method.anchors.forEach { add(it) } }
                }
                putJsonObject("星宿定局") {
                    put("选定星宿", lock.star.name)
                    put("星宿属性", lock.star.attribute)
                    putJsonArray("诗句必含词根") { lock.star.anchors.forEach { add(it) } }
                }
                putJsonObject("意境定局") {
                    put("意境主题", lock.realm.theme)
                    put("选定灵媒", lock.realm.medium)
                    putJsonArray("诗句必含词根") { lock.realm.anchors.forEach { add(it) } }
                }
            }
        }
        return root.toString()
    }

    /**
     * 肉身禀赋特征的「定性代号」渲染（用于 Prompt 与本地模板的 {{physique}} 占位符）。
     * 绝不含原始尺寸数字。
     */
    fun physiqueFeature(
        gender: Gender,
        boneType: BoneType,
        heightCm: Int?,
        weightKg: Int?,
        erectLengthCm: Double?,
        erectGirthCm: Double?,
        flaccidLengthCm: Double?,
        cupSize: CupSize?,
        jadeGateShape: JadeGateShape?,
        privacyTierSendsPhysique: Boolean,
    ): String {
        if (!privacyTierSendsPhysique) {
            return "${gender.classicLabel}·匀停资质（未采集肉身法相，以通用模板推演）"
        }
        val prefix = "${gender.classicLabel}·${boneType.classicLabel}"
        return if (gender.isMale) {
            if (erectLengthCm == null) {
                "$prefix·体魄匀停（未录龙根法相）"
            } else {
                val tier = com.ziweistar.app.engine.PhallusTierResolver.resolve(
                    erectLengthCm,
                    erectGirthCm ?: 0.0,
                )
                val shape = com.ziweistar.app.engine.PhallusTierResolver.dragonShape(
                    flaccidLengthCm ?: 0.0,
                    erectLengthCm,
                )
                val shapeLabel = shape?.label ?: "血肉龙"
                val bmi = if (heightCm != null && weightKg != null && heightCm > 0) {
                    weightKg.toDouble() / ((heightCm / 100.0) * (heightCm / 100.0))
                } else 0.0
                val bmiLabel = if (bmi > 0) "·BMI${"%.1f".format(bmi)}" else ""
                // 形制已含「龙」字，不再附加「充血」说明，避免「血肉龙(血肉龙充血)」这类重复
                "$prefix · ${tier.label}$shapeLabel$bmiLabel"
            }
        } else {
            val cup = cupSize?.label ?: "未录"
            val gate = jadeGateShape?.label ?: "未录"
            if (cupSize == null && jadeGateShape == null) {
                "$prefix·玉女法相未录（以匀停模板推演）"
            } else {
                "$prefix·${cup}杯云峰+${gate}"
            }
        }
    }
}
