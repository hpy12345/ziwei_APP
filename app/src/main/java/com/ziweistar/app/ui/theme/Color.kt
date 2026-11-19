package com.ziweistar.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 赛博新中式（Cyber-Almanac）配色体系
 * Dark-First Architecture —— 全局暗色优先，营造私密沉浸的夜间观星氛围。
 */
object ZiWeiColors {
    /** 紫微深空：全局主背景 */
    val DeepSpace = Color(0xFF140A24)

    /** 玄冥幽紫：次级容器 / 抽屉背景 */
    val XuanMing = Color(0xFF1E1136)

    /** 玄冥幽紫·更浅一档：卡片底座 */
    val XuanMingLight = Color(0xFF271648)

    /** 流光泥金：边框 / 星轨 / 高亮文本 */
    val Gold = Color(0xFFE6C17A)

    /** 泥金·半透明：分隔线与弱边框 */
    val GoldDim = Color(0x59E6C17A)

    /** 朱砂正红：核心操作 / 吉印 / 预警 */
    val Cinnabar = Color(0xFFD94E4E)

    /** 玄青冷翠：休养 / 静心 / 安全态 */
    val Jade = Color(0xFF4E9A8E)

    /** 宣纸玉白：签文卡片局部底色 */
    val RicePaper = Color(0xFFF4EFE6)

    /** 徽墨：宣纸上的正文色 */
    val Ink = Color(0xFF2C221E)

    /** 次级文本：暗底上的说明文字 */
    val TextSecondary = Color(0xFFB8A9CE)

    /** 三级文本：暗底上的弱提示 */
    val TextTertiary = Color(0xFF7C6E93)

    /** 玄铁封印态：寿星被戒律封锁时的灰调 */
    val SealedIron = Color(0xFF4A4550)

    /** 玄墨：天罡戒律页的暗墨背景 */
    val InkBlack = Color(0xFF0B0614)

    // ---- 吉凶四印专用色 ----
    val AuspiciousGreat = Cinnabar          // 大吉
    val AuspiciousSmall = Gold              // 小吉
    val Neutral = Jade                      // 平
    val Ominous = Color(0xFF8C3A3A)         // 大凶
}
