package com.ziweistar.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字体体系：
 *  - 衬线体走**内置**思源宋体简体子集（[ZiWeiSerifFamily]），有真实 400/700 两个字面，
 *    避免系统单字重 CJK serif 在 Bold 时被合成加粗，把「薇」「紫」这类密笔画字糊掉。
 *  - 无衬线 / 等宽仍用系统族（这些地方没有加粗需求，且拉丁数字显示更规整）。
 */
object ZiWeiType {
    /** 古籍标题：宋体加粗，用于「紫薇皇历」「观紫微星」等书法感标题 */
    val SealTitle = TextStyle(
        fontFamily = ZiWeiSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 6.sp,
    )

    val SectionTitle = TextStyle(
        fontFamily = ZiWeiSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        letterSpacing = 2.sp,
    )

    /** 签诗正文：宋体，字距放宽以显古韵 */
    val PoemLine = TextStyle(
        fontFamily = ZiWeiSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 3.sp,
        lineHeight = 30.sp,
    )

    /** 解曰 / 批注：宋体，行高舒展 */
    val Commentary = TextStyle(
        fontFamily = ZiWeiSerifFamily,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
        lineHeight = 24.sp,
    )

    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )

    val Caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
    )

    /** 双轨计时：现代数码轨 */
    val DigitalTimer = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        letterSpacing = 2.sp,
    )

    /** 双轨计时：古风时辰轨 */
    val AncientTimer = TextStyle(
        fontFamily = ZiWeiSerifFamily,
        fontSize = 16.sp,
        letterSpacing = 3.sp,
    )

    /** 数字强调 */
    val MetricLarge = TextStyle(
        fontFamily = ZiWeiSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
    )
}
