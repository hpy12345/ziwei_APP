package com.ziweistar.app.engine

import com.nlf.calendar.EightChar
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import com.ziweistar.app.engine.model.AlmanacSnapshot
import com.ziweistar.app.engine.model.NatalChart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 天文历法引擎（Almanac Engine）
 *
 * 基于客户端内置的开源寿星万年历算法（cn.6tail:lunar），
 * 实现设计文档 3.1 / 3.2 所需的全部本地确定性历法推算：
 *  - 公历 → 农历四柱八字（精确到时分）
 *  - 建除十二神（getZhiXing）
 *  - 二十八星宿（getXiu / 七曜 / 神兽）
 *  - 今日干支、节气、月相、宜忌
 *
 * 全部为纯函数，无网络、无随机，保证跨模型 100% 可复现。
 */
object AlmanacEngine {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    /** 二十八宿七曜所对应的「水/月」「火/日」「木/金/土」五行归组 */
    private val XIU_WATER_MOON = setOf("水", "月")
    private val XIU_FIRE_SUN = setOf("日", "火")
    private val XIU_WOOD_METAL_EARTH = setOf("木", "金", "土")

    fun formatDate(date: Date): String = dateFmt.format(date)

    fun formatIso(date: Date): String = isoFmt.format(date)

    /**
     * 生成今日黄历快照。
     * @param now 目标时刻（默认取当前系统时间）
     */
    fun snapshot(now: Date = Date()): AlmanacSnapshot {
        val solar = Solar.fromDate(now)
        val lunar = solar.lunar

        val prevJieQi = runCatching { lunar.prevJieQi?.name ?: "" }.getOrDefault("")
        val nextJieQi = runCatching { lunar.nextJieQi?.name ?: "" }.getOrDefault("")

        return AlmanacSnapshot(
            solarDate = dateFmt.format(now),
            lunarText = runCatching {
                "${lunar.yearInGanZhi}年${lunar.monthInChinese}月${lunar.dayInChinese}"
            }.getOrDefault(""),
            ganZhiDay = runCatching { "${lunar.dayInGanZhi}日" }.getOrDefault(""),
            dayGan = runCatching { lunar.dayGan }.getOrDefault(""),
            dayZhi = runCatching { lunar.dayZhi }.getOrDefault(""),
            zhiXing = runCatching { lunar.zhiXing }.getOrDefault(""),
            xiu = runCatching { lunar.xiu }.getOrDefault(""),
            xiuLuck = runCatching { lunar.xiuLuck }.getOrDefault(""),
            xiuZheng = runCatching { lunar.zheng }.getOrDefault(""),
            xiuShou = runCatching { lunar.shou }.getOrDefault(""),
            xiuAnimal = runCatching { lunar.animal }.getOrDefault(""),
            jieQi = runCatching { lunar.jieQi ?: "" }.getOrDefault(""),
            prevJieQi = prevJieQi,
            nextJieQi = nextJieQi,
            yueXiang = runCatching { lunar.yueXiang }.getOrDefault(""),
            dayYi = runCatching { lunar.dayYi.take(8).toList() }.getOrDefault(emptyList()),
            dayJi = runCatching { lunar.dayJi.take(8).toList() }.getOrDefault(emptyList()),
        )
    }

    /** 二十八宿的五行归组判定（用于决策矩阵 P1/P2/P3 洞府定局） */
    fun xiuElementGroup(zheng: String): XiuGroup = when (zheng) {
        in XIU_WATER_MOON -> XiuGroup.WATER_MOON
        in XIU_FIRE_SUN -> XiuGroup.FIRE_SUN
        in XIU_WOOD_METAL_EARTH -> XiuGroup.WOOD_METAL_EARTH
        else -> XiuGroup.WOOD_METAL_EARTH
    }

    enum class XiuGroup { WATER_MOON, FIRE_SUN, WOOD_METAL_EARTH }

    /**
     * 由公历出生时刻推算命盘（立命盘 第一步：天命排盘）
     * 八字以「节气换月 + 立春换年」的 exact 口径计算，符合子平术规范。
     */
    fun natalChart(birth: Date): NatalChart {
        val solar = Solar.fromDate(birth)
        val lunar = solar.lunar
        val ec: EightChar = lunar.eightChar

        val year = ec.year
        val month = ec.month
        val day = ec.day
        val time = ec.time
        val dayGan = ec.dayGan

        val strength = dayMasterStrength(dayGan, month)

        return NatalChart(
            birthMillis = birth.time,
            fourPillars = "${year}年 ${month}月 ${day}日 ${time}时",
            yearPillar = year,
            monthPillar = month,
            dayPillar = day,
            timePillar = time,
            dayMaster = dayGan,
            dayMasterStrength = strength,
            lunarBirthText = "${lunar.yearInGanZhi}年${lunar.monthInChinese}月${lunar.dayInChinese} ${lunar.timeZhi}时",
        )
    }

    /**
     * 日主强弱定性（简化的司天式判定：按月令生克 + 日主五行）
     * 输出「偏弱 / 中和 / 偏旺」，用于生成「日主戊土偏弱」这样的命理短句。
     */
    private fun dayMasterStrength(dayGan: String, monthPillar: String): String {
        val ganElement = mapOf(
            "甲" to "木", "乙" to "木", "丙" to "火", "丁" to "火",
            "戊" to "土", "己" to "土", "庚" to "金", "辛" to "金",
            "壬" to "水", "癸" to "水",
        )
        val produce = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")

        val self = ganElement[dayGan] ?: return ""
        val monthGan = monthPillar.dropLast(1)
        val monthZhi = monthPillar.takeLast(1)
        val monthEl = ganElement[monthGan] ?: return ""

        // 月令地支主气（简表）
        val zhiEl = mapOf(
            "寅" to "木", "卯" to "木", "辰" to "土", "巳" to "火", "午" to "火", "未" to "土",
            "申" to "金", "酉" to "金", "戌" to "土", "亥" to "水", "子" to "水", "丑" to "土",
        )[monthZhi] ?: "土"

        var score = 0
        if (monthEl == self) score += 2
        if (zhiEl == self) score += 2
        if (produce[monthEl] == self) score += 1
        if (produce[zhiEl] == self) score += 1
        if (produce[self] == monthEl) score -= 1
        if (produce[self] == zhiEl) score -= 1

        return when {
            score >= 3 -> "偏旺"
            score <= 0 -> "偏弱"
            else -> "中和"
        }
    }

    /** 取某个日期所在日 00:00 的时间戳（用于连击/日切计算） */
    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    /** 某日 23:59:59.999 的时间戳 */
    fun endOfDay(millis: Long): Long {
        // 不能用 startOfDay + 24h - 1：夏令时切换日只有 23 小时 / 有 25 小时，
        // 直接加固定毫秒会越界到次日。改用 Calendar 加一天再减 1 毫秒。
        val c = Calendar.getInstance().apply {
            timeInMillis = startOfDay(millis)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.timeInMillis - 1
    }

    /** 两个时间戳相隔的自然日数（按本地日切） */
    fun daysBetween(fromMillis: Long, toMillis: Long): Int {
        val a = startOfDay(fromMillis)
        val b = startOfDay(toMillis)
        // 不能用 (b - a) / 86400000 直接除：跨夏令时切换的区间不是整 24 小时的倍数，
        // 整数除法会算少一天。改用 Calendar 逐日累加，按日历日计数。
        if (a == b) return 0
        val sign = if (b > a) 1 else -1
        val (lo, hi) = if (a < b) a to b else b to a
        val c = Calendar.getInstance().apply { timeInMillis = lo }
        var days = 0
        while (c.timeInMillis < hi) {
            c.add(Calendar.DAY_OF_MONTH, 1)
            days++
        }
        return days * sign
    }

    /** 用于排盘展示：拨动滚轮时实时预览（不落库） */
    fun previewBirth(year: Int, month: Int, day: Int, hour: Int, minute: Int): NatalChart {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return natalChart(cal.time)
    }
}
