package com.ziweistar.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.AlmanacEngine
import com.ziweistar.app.engine.HashSelector
import com.ziweistar.app.engine.ReportEngine
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.AlmanacCard
import com.ziweistar.app.ui.components.SectionHeader
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 钦天监 · 星历
 *
 * 设计文档 5.2：以热力图形式回顾一个月/一周的星轨，
 * 每一格按当日次数与吉凶渲染不同印记：
 *  - 普通日：紫薇星
 *  - 5 星：耀金帝星
 *  - 当日 2 次：双梅印
 *  - 当日 ≥3 次：戒字印
 *  - 休养日（元气恢复中）：青莲静心印
 */
@Composable
fun CalendarScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenReport: (ReportEngine.Kind) -> Unit,
) {
    val haptics = LocalHaptics.current
    val home by vm.home.collectAsState()

    val today = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(today.get(Calendar.MONTH) + 1) }

    val cells = remember(year, month, home) { vm.buildCalendarCells(year, month) }

    val monthTotal = cells.count { (it?.count ?: 0) > 0 }
    val monthTimes = cells.sumOf { it?.count ?: 0 }
    val maxDay = cells.maxOfOrNull { it?.count ?: 0 } ?: 0

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground(animated = true, starCount = 60, seed = 23)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(30.dp))

            // 标题栏
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹ 返回",
                    fontSize = 12.sp,
                    color = ZiWeiColors.TextTertiary,
                    modifier = Modifier
                        .tapNoRipple {
                            haptics.light()
                            onBack()
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "钦 天 监 · 星 历",
                    fontSize = 15.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = ZiWeiColors.Gold,
                    letterSpacing = 4.sp,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(18.dp))

            // 月份切换
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                MonthArrow("‹") {
                    haptics.tick()
                    if (month == 1) {
                        month = 12; year -= 1
                    } else month -= 1
                }
                Spacer(Modifier.width(22.dp))
                Text(
                    text = "$year 年 $month 月",
                    fontSize = 18.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = ZiWeiColors.RicePaper,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.width(22.dp))
                MonthArrow("›") {
                    haptics.tick()
                    if (month == 12) {
                        month = 1; year += 1
                    } else month += 1
                }
            }

            Spacer(Modifier.height(16.dp))

            // 月度概览
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                StatBox(Modifier.weight(1f), "观星日", "$monthTotal", "日", ZiWeiColors.Gold)
                StatBox(Modifier.weight(1f), "累计", "$monthTimes", "次", ZiWeiColors.Cinnabar)
                StatBox(Modifier.weight(1f), "单日峰值", "$maxDay", "次", ZiWeiColors.Jade)
            }

            Spacer(Modifier.height(18.dp))

            // 星期表头
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, w ->
                    Text(
                        text = w,
                        fontSize = 10.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = if (i >= 5) ZiWeiColors.Cinnabar.copy(alpha = 0.7f)
                        else ZiWeiColors.TextTertiary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 热力网格
            val rows = cells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { week ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        week.forEach { cell ->
                            Box(Modifier.weight(1f)) {
                                if (cell == null) {
                                    Spacer(Modifier.aspectRatio(1f))
                                } else {
                                    DayTile(cell)
                                }
                            }
                        }
                        // 补齐末行
                        repeat(7 - week.size) {
                            Box(Modifier.weight(1f)) { Spacer(Modifier.aspectRatio(1f)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 图例
            AlmanacCard(Modifier.fillMaxWidth(), gradient = false) {
                Text(
                    text = "印 记 释 义",
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = ZiWeiColors.Gold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(10.dp))
                LegendRow(ZiWeiColors.Gold.copy(alpha = 0.22f), ZiWeiColors.Gold, "紫薇星", "清修之日，未动天机")
                Spacer(Modifier.height(7.dp))
                LegendRow(Color(0xFF4A3210), Color(0xFFF5D97A), "耀金帝星", "道心圆满度满 5 星")
                Spacer(Modifier.height(7.dp))
                LegendRow(Color(0xFF452540), Color(0xFFE58FB0), "双梅印", "当日梅开二度")
                Spacer(Modifier.height(7.dp))
                LegendRow(Color(0xFF3A1A1A), ZiWeiColors.Cinnabar, "戒字印", "当日触犯天罡戒律")
                Spacer(Modifier.height(7.dp))
                LegendRow(Color(0xFF14332E), ZiWeiColors.Jade, "青莲静心印", "休养恢复，元气归元")
            }

            Spacer(Modifier.height(18.dp))

            // 报告入口
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReportEntry(
                    modifier = Modifier.weight(1f),
                    title = "紫微内丹录 · 周报",
                    subtitle = "近七日星轨总览与诊脉",
                    accent = ZiWeiColors.Gold,
                ) {
                    haptics.medium()
                    onOpenReport(ReportEngine.Kind.WEEK)
                }
                ReportEntry(
                    modifier = Modifier.weight(1f),
                    title = "紫微内丹录 · 月报",
                    subtitle = "三十日周期回望与调理",
                    accent = ZiWeiColors.Jade,
                ) {
                    haptics.medium()
                    onOpenReport(ReportEngine.Kind.MONTH)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MonthArrow(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x1AE6C17A))
            .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.5f), CircleShape)
            .tapNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 14.sp, color = ZiWeiColors.Gold)
    }
}

@Composable
private fun StatBox(modifier: Modifier, label: String, value: String, unit: String, accent: Color) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1A1E1136))
            .border(0.7.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 9.sp, color = ZiWeiColors.TextTertiary)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 19.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.width(2.dp))
            Text(unit, fontSize = 9.sp, color = accent.copy(alpha = 0.7f))
        }
    }
}

/** 单日格子：根据次数与满意度渲染不同印记 */
@Composable
private fun DayTile(cell: MainViewModel.DayCell) {
    val count = cell.count
    val (bg, fg, badge) = when {
        count >= 3 -> Triple(Color(0xFF3A1A1A), ZiWeiColors.Cinnabar, "戒")
        count == 2 -> Triple(Color(0xFF452540), Color(0xFFE58FB0), "梅")
        count == 1 -> Triple(Color(0xFF2A1C40), ZiWeiColors.Gold, null)
        else -> Triple(Color(0x0FE6C17A), ZiWeiColors.TextTertiary, null)
    }

    val borderColor = if (cell.isToday) ZiWeiColors.Gold else fg.copy(alpha = 0.35f)
    val borderWidth = if (cell.isToday) 1.4.dp else 0.6.dp

    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cell.dayOfMonth.toString(),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (count > 0) ZiWeiColors.RicePaper else ZiWeiColors.TextTertiary,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (badge != null) {
                Spacer(Modifier.height(1.dp))
                Box(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(fg.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge,
                        fontSize = 7.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFF14081F),
                    )
                }
            } else if (count > 0) {
                Spacer(Modifier.height(2.dp))
                Canvas(Modifier.size(6.dp)) {
                    val c = Offset(size.width / 2, size.height / 2)
                    drawCircle(color = fg, radius = size.minDimension / 2, center = c)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(bg: Color, fg: Color, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .border(0.7.dp, fg.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontFamily = ZiWeiSerifFamily,
            color = fg,
        )
        Spacer(Modifier.width(8.dp))
        Text(text = desc, fontSize = 9.5.sp, color = ZiWeiColors.TextTertiary)
    }
}

@Composable
private fun ReportEntry(
    modifier: Modifier,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF251545), Color(0xFF170D2C))),
            )
            .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .tapNoRipple { onClick() }
            .padding(13.dp),
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontFamily = ZiWeiSerifFamily,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = subtitle,
            fontSize = 9.sp,
            color = ZiWeiColors.TextTertiary,
            lineHeight = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = "展开 ›", fontSize = 9.5.sp, color = accent.copy(alpha = 0.8f))
    }
}
