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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.ReportEngine
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.AlmanacCard
import com.ziweistar.app.ui.components.DisclaimerFooter
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors

/**
 * 《钦天监 · 紫微内丹录》
 *
 * 设计文档 5.3：分两卷
 *  - 卷一「星轨总览」：频次条形图 / 时段分布 / 功法 TOP / 道场分布 / 走火入魔统计
 *  - 卷二「钦天监诊脉」：AI 批文（本地兜底）
 */
@Composable
fun ReportScreen(
    vm: MainViewModel,
    initialKind: ReportEngine.Kind,
    onBack: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val statsPair by vm.reportStats.collectAsState()
    val text by vm.reportText.collectAsState()
    val doc by vm.reportDoc.collectAsState()

    var kind by remember { mutableStateOf(initialKind) }
    var anchor by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(kind, anchor) {
        vm.loadReport(kind, anchor)
    }

    val period = statsPair?.first
    val stats = statsPair?.second

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground(animated = true, starCount = 55, seed = 61)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(30.dp))

            // 标题
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
                    text = "紫薇内丹录",
                    fontSize = 15.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = ZiWeiColors.Gold,
                    letterSpacing = 5.sp,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(18.dp))

            // 周/月切换
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x1A1E1136))
                    .border(0.7.dp, ZiWeiColors.GoldDim.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SegmentTab(
                    modifier = Modifier.weight(1f),
                    text = "周 报",
                    active = kind == ReportEngine.Kind.WEEK,
                ) {
                    haptics.tick(); kind = ReportEngine.Kind.WEEK; anchor = System.currentTimeMillis()
                }
                SegmentTab(
                    modifier = Modifier.weight(1f),
                    text = "月 报",
                    active = kind == ReportEngine.Kind.MONTH,
                ) {
                    haptics.tick(); kind = ReportEngine.Kind.MONTH; anchor = System.currentTimeMillis()
                }
            }

            Spacer(Modifier.height(14.dp))

            // 周期标题
            Text(
                text = period?.label ?: "载入天数中…",
                fontSize = 12.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            if (stats == null) {
                AlmanacCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = "正在推演星轨…",
                        fontSize = 12.sp,
                        color = ZiWeiColors.TextTertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (!stats.hasData) {
                AlmanacCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "此周期内尚无星轨记录",
                            fontSize = 13.sp,
                            fontFamily = ZiWeiSerifFamily,
                            color = ZiWeiColors.Gold,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "观星一次，星历方有痕。",
                            fontSize = 10.sp,
                            color = ZiWeiColors.TextTertiary,
                        )
                    }
                }
            } else {
                // ============ 卷一 ============
                VolumeHeader(index = "卷一", title = "星轨总览")

                Spacer(Modifier.height(12.dp))

                // 核心指标
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricCard(Modifier.weight(1f), "观星次数", "${stats.totalCount}", "次", ZiWeiColors.Cinnabar)
                    MetricCard(Modifier.weight(1f), "活跃天数", "${stats.activeDays}", "天", ZiWeiColors.Gold)
                    MetricCard(Modifier.weight(1f), "平均间隔", fmt1(stats.avgIntervalHours), "时", ZiWeiColors.Jade)
                }

                Spacer(Modifier.height(9.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricCard(Modifier.weight(1f), "平均时长", "${stats.avgDurationMinutes}", "分", ZiWeiColors.Gold)
                    MetricCard(Modifier.weight(1f), "道心圆满", fmt1(stats.avgSatisfaction), "星", Color(0xFFF5D97A))
                    MetricCard(Modifier.weight(1f), "最佳恢复", "${stats.bestRecoveryDays}", "天", ZiWeiColors.Jade)
                }

                Spacer(Modifier.height(14.dp))

                // 日频次条形图
                AlmanacCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = "每日观星频次",
                        fontSize = 12.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = ZiWeiColors.Gold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    BarChart(
                        values = stats.dailyCounts.map { it.second },
                        labels = stats.dailyCounts.map { it.first.substringAfter("-") },
                        accent = ZiWeiColors.Cinnabar,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StatInline("单日峰值", "${stats.maxDayCount} 次")
                        StatInline("连续峰值", "${stats.streakPeak} 天")
                        StatInline("边缘控制", "${stats.edgeControlCount} 次")
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 时段分布
                AlmanacCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = "时辰分布（子丑寅…亥）",
                        fontSize = 12.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = ZiWeiColors.Gold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    // 把 0..23 归并为 12 时辰
                    val twelve = (0 until 12).map { i ->
                        stats.hourHistogram[i * 2] + stats.hourHistogram[i * 2 + 1]
                    }
                    BarChart(
                        values = twelve,
                        labels = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"),
                        accent = Color(0xFF9B7BD4),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 功法 TOP
                if (stats.methodDistribution.isNotEmpty()) {
                    AlmanacCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = "所习功法 TOP",
                            fontSize = 12.sp,
                            fontFamily = ZiWeiSerifFamily,
                            fontWeight = FontWeight.Bold,
                            color = ZiWeiColors.Gold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        val maxV = stats.methodDistribution.maxOf { it.second }
                        stats.methodDistribution.forEach { (name, cnt) ->
                            RankBar(label = name, value = cnt, max = maxV, accent = ZiWeiColors.Cinnabar)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // 道场分布
                if (stats.venueDistribution.isNotEmpty()) {
                    AlmanacCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = "所处道场分布",
                            fontSize = 12.sp,
                            fontFamily = ZiWeiSerifFamily,
                            fontWeight = FontWeight.Bold,
                            color = ZiWeiColors.Gold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        val maxV = stats.venueDistribution.maxOf { it.second }
                        stats.venueDistribution.forEach { (name, cnt) ->
                            RankBar(label = name, value = cnt, max = maxV, accent = ZiWeiColors.Jade)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // 登顶造化分布
                if (stats.climaxDistribution.isNotEmpty()) {
                    AlmanacCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = "登顶造化分布",
                            fontSize = 12.sp,
                            fontFamily = ZiWeiSerifFamily,
                            fontWeight = FontWeight.Bold,
                            color = ZiWeiColors.Gold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        val maxV = stats.climaxDistribution.maxOf { it.second }
                        stats.climaxDistribution.forEach { (name, cnt) ->
                            RankBar(label = name, value = cnt, max = maxV, accent = Color(0xFFF5D97A))
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // 走火入魔
                if (stats.adverseTop.isNotEmpty()) {
                    AlmanacCard(
                        Modifier.fillMaxWidth(),
                        borderColor = ZiWeiColors.Cinnabar.copy(alpha = 0.4f),
                    ) {
                        Text(
                            text = "走火入魔录",
                            fontSize = 12.sp,
                            fontFamily = ZiWeiSerifFamily,
                            fontWeight = FontWeight.Bold,
                            color = ZiWeiColors.Cinnabar,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        val maxV = stats.adverseTop.maxOf { it.second }
                        stats.adverseTop.forEach { (name, cnt) ->
                            RankBar(label = name, value = cnt, max = maxV, accent = ZiWeiColors.Cinnabar)
                            Spacer(Modifier.height(6.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "严重不良反应 ${stats.severeCount} 次" +
                                if (stats.severeCount > 0) " · 建议下调频次" else " · 气机平稳",
                            fontSize = 10.sp,
                            color = if (stats.severeCount > 0) ZiWeiColors.Cinnabar
                            else ZiWeiColors.Jade,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ============ 卷二 ============
                VolumeHeader(index = "卷二", title = "钦天监诊脉")

                Spacer(Modifier.height(12.dp))

                // 卷首语四字总评（双擎批文提供时展示，本地兜底亦有）
                doc?.scrollOpen?.takeIf { it.isNotBlank() }?.let { open ->
                    Text(
                        text = "— 卷首语 · $open —",
                        fontSize = 11.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = ZiWeiColors.GoldDim,
                        letterSpacing = 3.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFF4EFE6), Color(0xFFE9E1D2)),
                            ),
                        )
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                ) {
                    Column {
                        Text(
                            text = "钦 天 监 批",
                            fontSize = 12.sp,
                            fontFamily = ZiWeiSerifFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8C6A2E),
                            letterSpacing = 4.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        GoldDivider(alpha = 0.35f)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = text.ifBlank { "推演中…" },
                            fontSize = 12.5.sp,
                            fontFamily = ZiWeiSerifFamily,
                            color = Color(0xFF2C221E),
                            lineHeight = 23.sp,
                            letterSpacing = 0.8.sp,
                        )

                        // ---------- 肉身精气诊脉分栏 ----------
                        doc?.pulse?.takeIf { it.isNotBlank() }?.let { pulse ->
                            Spacer(Modifier.height(16.dp))
                            GoldDivider(alpha = 0.25f)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "【肉身精气诊脉】",
                                fontSize = 11.5.sp,
                                fontFamily = ZiWeiSerifFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8C6A2E),
                                letterSpacing = 1.5.sp,
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                text = pulse,
                                fontSize = 12.sp,
                                fontFamily = ZiWeiSerifFamily,
                                color = Color(0xFF2C221E),
                                lineHeight = 22.sp,
                                letterSpacing = 0.6.sp,
                            )
                        }

                        // ---------- 下期天机指引 ----------
                        val rec = doc?.recommendMethod.orEmpty()
                        val adv = doc?.scheduleAdvice.orEmpty()
                        if (rec.isNotBlank() || adv.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            GoldDivider(alpha = 0.25f)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "【下期天机指引】",
                                fontSize = 11.5.sp,
                                fontFamily = ZiWeiSerifFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8C6A2E),
                                letterSpacing = 1.5.sp,
                            )
                            if (rec.isNotBlank()) {
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    text = "· 推荐调理功法：$rec",
                                    fontSize = 12.sp,
                                    fontFamily = ZiWeiSerifFamily,
                                    color = Color(0xFF4A3A28),
                                    lineHeight = 21.sp,
                                )
                            }
                            if (adv.isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = "· 节律排程：$adv",
                                    fontSize = 12.sp,
                                    fontFamily = ZiWeiSerifFamily,
                                    color = Color(0xFF4A3A28),
                                    lineHeight = 21.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                text = "— 紫微内丹录 · 钦天监印",
                                fontSize = 10.sp,
                                fontFamily = ZiWeiSerifFamily,
                                color = Color(0xFF6B5A45),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (kind == ReportEngine.Kind.WEEK) {
                        "周报于每周日 23:00 归档；如有 AI 渲染通路则经「零偏移锁死」校验后呈现。"
                    } else {
                        "月报于每月最后一日 23:00 归档；批文仅供参考，不构成医疗建议。"
                    },
                    fontSize = 9.sp,
                    color = ZiWeiColors.TextTertiary,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                // 常驻免责声明（设计文档 7 · 医疗合规）
                Spacer(Modifier.height(6.dp))
                DisclaimerFooter(compact = true)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ============================================================
// 组件
// ============================================================

@Composable
private fun SegmentTab(modifier: Modifier, text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) ZiWeiColors.Cinnabar.copy(alpha = 0.85f) else Color.Transparent)
            .tapNoRipple { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = ZiWeiSerifFamily,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) ZiWeiColors.RicePaper else ZiWeiColors.TextTertiary,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun VolumeHeader(index: String, title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(3.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ZiWeiColors.Gold),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = index,
            fontSize = 11.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.Gold.copy(alpha = 0.75f),
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontFamily = ZiWeiSerifFamily,
            fontWeight = FontWeight.Bold,
            color = ZiWeiColors.RicePaper,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.width(12.dp))
        GoldDivider(Modifier.weight(1f), alpha = 0.3f)
    }
}

@Composable
private fun MetricCard(modifier: Modifier, label: String, value: String, unit: String, accent: Color) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1A1E1136))
            .border(0.7.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 8.5.sp, color = ZiWeiColors.TextTertiary)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.width(2.dp))
            Text(unit, fontSize = 8.5.sp, color = accent.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun StatInline(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 9.sp, color = ZiWeiColors.TextTertiary)
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 10.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.Gold,
        )
    }
}

/** 简易柱状图（无第三方图表库） */
@Composable
private fun BarChart(values: List<Int>, labels: List<String>, accent: Color) {
    val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val chartHeight = 96.dp
    val labelEvery = if (values.size > 16) 3 else 1

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                values.forEachIndexed { i, v ->
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val frac = v.toFloat() / maxV
                        val barBrush: Brush = if (v == 0) {
                            Brush.verticalGradient(listOf(Color(0x14E6C17A), Color(0x14E6C17A)))
                        } else {
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.45f)),
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth(0.62f)
                                .fillMaxHeight(
                                    frac.coerceIn(0f, 1f).let { if (it < 0.045f && v > 0) 0.045f else it },
                                )
                                .clip(RoundedCornerShape(topStart = 2.5.dp, topEnd = 2.5.dp))
                                .background(barBrush),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            labels.forEachIndexed { i, lb ->
                Text(
                    text = if (i % labelEvery == 0) lb else "",
                    fontSize = 7.5.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.TextTertiary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 排行横条 */
@Composable
private fun RankBar(label: String, value: Int, max: Int, accent: Color) {
    val frac = if (max <= 0) 0f else value.toFloat() / max
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.TextSecondary,
            modifier = Modifier.width(96.dp),
            maxLines = 1,
        )
        Box(
            Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x1AE6C17A)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.5f)),
                        ),
                    ),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$value",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = accent,
            modifier = Modifier.width(22.dp),
            textAlign = TextAlign.End,
        )
    }
}

private fun fmt1(v: Double): String = String.format("%.1f", v)
