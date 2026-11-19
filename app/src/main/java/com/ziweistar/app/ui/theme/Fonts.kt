package com.ziweistar.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ziweistar.app.R

/**
 * 内置衬线字体族（思源宋体 · 简体子集）。
 *
 * 为什么必须自带字体：
 * 系统 serif 家族在中文字符上只挂了一层 NotoSerifCJK **Regular**（单字重），
 * 且没有为 CFF 轮廓提供真实 Bold/Medium 字面。Compose 请求 FontWeight.Bold 时，
 * Android 只能用「伪粗体」（在原始轮廓上做笔画加粗描边）来合成，
 * 结果就是「薇」「紫」这类横画密集的字糊成一团黑块——
 * 用户反馈的「薇字有点糊」正是这个原因，不是字号或渲染参数问题。
 *
 * 这里内置 400 / 700 两个真实字面，并补一个 500 的 Medium
 * （用 Bold 轮廓派生会失真，所以 Medium 直接复用 Regular 字面，
 * 由 Compose 在 400↔500 间就近取用，避免合成粗体再次糊字）。
 *
 * 子集范围：本项目全部 .kt/.xml/.json 源码中出现的字符（约 1950 个），
 * 因此单字重只有约 710 KB，APK 增量可接受。
 */
val ZiWeiSerifFamily = FontFamily(
    Font(R.font.ziwei_serif_regular, FontWeight.Normal),
    Font(R.font.ziwei_serif_regular, FontWeight.Medium),
    Font(R.font.ziwei_serif_bold, FontWeight.SemiBold),
    Font(R.font.ziwei_serif_bold, FontWeight.Bold),
    Font(R.font.ziwei_serif_bold, FontWeight.Black),
)
