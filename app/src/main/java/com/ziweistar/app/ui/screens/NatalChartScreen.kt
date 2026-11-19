package com.ziweistar.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.engine.model.Gender
import com.ziweistar.app.engine.model.RelationshipStatus
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.SealButton
import com.ziweistar.app.ui.components.StarDial
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.ui.theme.ZiWeiType

/**
 * 立命盘：分步填写。
 *
 * 设计改版说明（用户反馈）：
 *  1. 去掉「第一步 · 天命排盘」这类多余的步骤副标题；
 *  2. 原本全部堆在一页，用户需要长距离滚动才能填完，且容易漏填。
 *     现改为一步一项：乾坤定局 → 生辰八字 → 红尘羁绊，各自独占一屏；
 *  3. 考虑「不知道出生时辰」的真实情况：提供「不详」开关，直接按子时（00:00）推演。
 */
@Composable
fun NatalChartScreen(
    vm: MainViewModel,
    onNext: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val draft by vm.draft.collectAsState()
    val natal = remember(
        draft.birthYear, draft.birthMonth, draft.birthDay,
        draft.birthHour, draft.birthMinute,
    ) { draft.natalPreview }

    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0518), ZiWeiColors.DeepSpace, Color(0xFF170B29)),
                ),
            ),
    ) {
        StarfieldBackground(animated = false, starCount = 60, seed = 5)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(42.dp))

            Text(
                text = "立 命 盘",
                style = ZiWeiType.SealTitle,
                color = ZiWeiColors.Gold,
                fontSize = 26.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))

            StepDots(current = step, total = totalSteps)

            Spacer(Modifier.height(14.dp))

            // 星盘 + 八字预览：始终置顶，随填写实时变化
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                contentAlignment = Alignment.Center,
            ) {
                StarDial(running = true, innerAlpha = 0.5f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = natal.lunarBirthText.ifBlank { "—" },
                        fontSize = 12.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = ZiWeiColors.Gold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = natal.fourPillars,
                        fontSize = 15.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = ZiWeiColors.RicePaper,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                    )
                    if (natal.dayMaster.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "日主${natal.dayMaster}${natal.dayMasterStrength}",
                            fontSize = 11.sp,
                            fontFamily = ZiWeiSerifFamily,
                            color = ZiWeiColors.Jade,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            GoldDivider()
            Spacer(Modifier.height(12.dp))

            // 当前步骤内容（可滚动，避免小屏被裁切）
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (fadeIn(tween(220))).togetherWith(fadeOut(tween(160)))
                    },
                    label = "onboarding-step",
                ) { s ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when (s) {
                            0 -> StepGender(draft = draft, onChange = { male ->
                                haptics.light()
                                vm.updateDraft { it.copy(isMale = male) }
                            })

                            1 -> StepBirth(
                                draft = draft,
                                onYear = { y -> vm.updateDraft { it.copy(birthYear = y) } },
                                onMonth = { m ->
                                    vm.updateDraft {
                                        val maxDay = daysInMonth(it.birthYear, m)
                                        it.copy(
                                            birthMonth = m,
                                            birthDay = it.birthDay.coerceAtMost(maxDay),
                                        )
                                    }
                                },
                                onDay = { d -> vm.updateDraft { it.copy(birthDay = d) } },
                                onHour = { h -> vm.updateDraft { it.copy(birthHour = h) } },
                                onMinute = { mi -> vm.updateDraft { it.copy(birthMinute = mi) } },
                                onHourUnknown = { unknown ->
                                    haptics.tick()
                                    // 时辰不详 → 以子时（00:00）推演
                                    vm.updateDraft {
                                        if (unknown) it.copy(birthHourUnknown = true, birthHour = 0, birthMinute = 0)
                                        else it.copy(birthHourUnknown = false)
                                    }
                                },
                            )

                            else -> StepRelationship(draft = draft, onChange = { r ->
                                haptics.tick()
                                vm.updateDraft { it.copy(relationship = r) }
                            })
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // 底部按钮：上一步 / 下一步
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    Box(
                        Modifier
                            .weight(0.42f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(Color(0xFF1B1030))
                            .border(
                                0.9.dp,
                                ZiWeiColors.GoldDim.copy(alpha = 0.5f),
                                RoundedCornerShape(27.dp),
                            )
                            .tapNoRipple {
                                haptics.light()
                                step -= 1
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "上 一 步",
                            fontSize = 14.sp,
                            fontFamily = ZiWeiSerifFamily,
                            color = ZiWeiColors.TextSecondary,
                            letterSpacing = 3.sp,
                        )
                    }
                }

                SealButton(
                    modifier = Modifier
                        .weight(if (step > 0) 0.58f else 1f)
                        .height(54.dp),
                    onClick = {
                        haptics.seal()
                        if (step < totalSteps - 1) step += 1 else onNext()
                    },
                ) {
                    Text(
                        text = if (step < totalSteps - 1) "下 一 步" else "推 演 命 盘",
                        fontSize = 16.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFF3E0),
                        letterSpacing = 3.sp,
                    )
                }
            }
        }
    }
}

// ============================================================
// 步骤指示器
// ============================================================

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until total) {
            if (i > 0) {
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .width(18.dp)
                        .height(0.9.dp)
                        .background(
                            if (i <= current) ZiWeiColors.Gold.copy(alpha = 0.7f)
                            else ZiWeiColors.GoldDim.copy(alpha = 0.3f),
                        ),
                )
            }
            Box(
                Modifier
                    .size(if (i == current) 9.dp else 6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (i <= current) ZiWeiColors.Gold
                        else ZiWeiColors.GoldDim.copy(alpha = 0.35f),
                    ),
            )
        }
    }
}

// ============================================================
// 第 1 步：乾坤定局
// ============================================================

@Composable
private fun StepGender(
    draft: MainViewModel.OnboardingDraft,
    onChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("乾坤定局")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "选定后，功法库与推演规则将整体切换",
            fontSize = 11.sp,
            color = ZiWeiColors.TextTertiary,
        )
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GenderOption(
                modifier = Modifier.weight(1f),
                label = Gender.QIAN.label,
                sub = Gender.QIAN.classicLabel,
                symbol = "☰",
                selected = draft.isMale,
            ) { onChange(true) }
            GenderOption(
                modifier = Modifier.weight(1f),
                label = Gender.KUN.label,
                sub = Gender.KUN.classicLabel,
                symbol = "☷",
                selected = !draft.isMale,
            ) { onChange(false) }
        }
    }
}

// ============================================================
// 第 2 步：生辰八字
// ============================================================

@Composable
private fun StepBirth(
    draft: MainViewModel.OnboardingDraft,
    onYear: (Int) -> Unit,
    onMonth: (Int) -> Unit,
    onDay: (Int) -> Unit,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
    onHourUnknown: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("生辰八字 · 公历")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "拨动滚轮，星盘实时推演农历与四柱",
            fontSize = 11.sp,
            color = ZiWeiColors.TextTertiary,
        )
        Spacer(Modifier.height(12.dp))

        WheelPicker(
            label = "年",
            range = 1930..2020,
            value = draft.birthYear,
            display = { "$it" },
            onChange = onYear,
        )
        WheelPicker(
            label = "月",
            range = 1..12,
            value = draft.birthMonth,
            display = { "$it" },
            onChange = onMonth,
        )
        WheelPicker(
            label = "日",
            range = 1..daysInMonth(draft.birthYear, draft.birthMonth),
            value = draft.birthDay,
            display = { "$it" },
            onChange = onDay,
        )

        Spacer(Modifier.height(10.dp))

        // 时辰不详开关
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (draft.birthHourUnknown) ZiWeiColors.Gold.copy(alpha = 0.10f)
                    else Color(0xFF1B1030),
                )
                .border(
                    0.9.dp,
                    if (draft.birthHourUnknown) ZiWeiColors.Gold
                    else ZiWeiColors.GoldDim.copy(alpha = 0.3f),
                    RoundedCornerShape(10.dp),
                )
                .tapNoRipple { onHourUnknown(!draft.birthHourUnknown) }
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "不知道出生时辰",
                    fontSize = 13.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = if (draft.birthHourUnknown) FontWeight.Bold else FontWeight.Normal,
                    color = if (draft.birthHourUnknown) ZiWeiColors.Gold else ZiWeiColors.TextSecondary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "勾选后按子时（00:00）推演，不影响其余排盘",
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                )
            }
            Box(
                Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (draft.birthHourUnknown) ZiWeiColors.Gold
                        else Color.Transparent,
                    )
                    .border(
                        1.dp,
                        if (draft.birthHourUnknown) ZiWeiColors.Gold
                        else ZiWeiColors.GoldDim.copy(alpha = 0.45f),
                        RoundedCornerShape(4.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (draft.birthHourUnknown) {
                    Text(
                        text = "✓",
                        fontSize = 12.sp,
                        color = Color(0xFF2C221E),
                    )
                }
            }
        }

        if (!draft.birthHourUnknown) {
            Spacer(Modifier.height(4.dp))
            WheelPicker(
                label = "时",
                range = 0..23,
                value = draft.birthHour,
                display = { String.format("%02d", it) },
                onChange = onHour,
            )
            WheelPicker(
                label = "分",
                range = 0..59,
                value = draft.birthMinute,
                display = { String.format("%02d", it) },
                onChange = onMinute,
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "※ 已按子时推演：年、月、日三柱为实，时柱为推定值",
                fontSize = 10.sp,
                color = ZiWeiColors.Cinnabar.copy(alpha = 0.85f),
                lineHeight = 15.sp,
            )
        }
    }
}

// ============================================================
// 第 3 步：红尘羁绊
// ============================================================

@Composable
private fun StepRelationship(
    draft: MainViewModel.OnboardingDraft,
    onChange: (RelationshipStatus) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("红尘羁绊 · 感情状态")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "此项影响推演时的宜忌侧重",
            fontSize = 11.sp,
            color = ZiWeiColors.TextTertiary,
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            RelationshipStatus.entries.forEach { r ->
                SelectRow(
                    title = r.classicLabel,
                    subtitle = r.label,
                    selected = draft.relationship == r,
                ) { onChange(r) }
            }
        }
    }
}

// ============================================================
// 复用组件
// ============================================================

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(5.dp)
                .background(ZiWeiColors.Cinnabar, androidx.compose.foundation.shape.CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontFamily = ZiWeiSerifFamily,
            fontWeight = FontWeight.Bold,
            color = ZiWeiColors.Gold,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun GenderOption(
    modifier: Modifier,
    label: String,
    sub: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ZiWeiColors.Gold.copy(alpha = 0.13f) else Color(0xFF1B1030))
            .border(
                1.dp,
                if (selected) ZiWeiColors.Gold else ZiWeiColors.GoldDim.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp),
            )
            .tapNoRipple { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = symbol,
                fontSize = 22.sp,
                color = if (selected) ZiWeiColors.Gold else ZiWeiColors.TextTertiary,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = label,
                fontSize = 17.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) ZiWeiColors.Gold else ZiWeiColors.TextSecondary,
            )
            Text(
                text = sub,
                fontSize = 10.sp,
                color = ZiWeiColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun WheelPicker(
    label: String,
    range: IntRange,
    value: Int,
    display: (Int) -> String,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.TextTertiary,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        StepButton("−") {
            val prev = if (value - 1 < range.first) range.last else value - 1
            onChange(prev)
        }
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF1B1030))
                .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = display(value),
                fontSize = 18.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Medium,
                color = ZiWeiColors.Gold,
                letterSpacing = 1.sp,
            )
        }
        StepButton("+") {
            val next = if (value + 1 > range.last) range.first else value + 1
            onChange(next)
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    val haptics = LocalHaptics.current
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF231540))
            .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
            .tapNoRipple {
                haptics.tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 19.sp, color = ZiWeiColors.Gold, fontFamily = ZiWeiSerifFamily)
    }
}

@Composable
private fun SelectRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ZiWeiColors.Gold.copy(alpha = 0.10f) else Color(0xFF1B1030))
            .border(
                0.8.dp,
                if (selected) ZiWeiColors.Gold.copy(alpha = 0.8f) else ZiWeiColors.GoldDim.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp),
            )
            .tapNoRipple { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontFamily = ZiWeiSerifFamily,
                color = if (selected) ZiWeiColors.Gold else ZiWeiColors.TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            Text(text = subtitle, fontSize = 10.sp, color = ZiWeiColors.TextTertiary)
        }
        if (selected) {
            Text("✓", fontSize = 16.sp, color = ZiWeiColors.Gold)
        }
    }
}

private fun daysInMonth(year: Int, month: Int): Int {
    val cal = java.util.Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
    }
    return cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
}
