package com.ziweistar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.model.BoneType
import com.ziweistar.app.engine.model.CupSize
import com.ziweistar.app.engine.model.DragonShape
import com.ziweistar.app.engine.model.JadeGateShape
import com.ziweistar.app.engine.model.MenstrualPhase
import com.ziweistar.app.engine.model.PartnerFrequency
import com.ziweistar.app.engine.model.PhallusTier
import com.ziweistar.app.engine.PhallusTierResolver
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.SealButton
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiType

/**
 * 立命盘 第三步：肉身法相采集
 */
@Composable
fun PhysiqueScreen(
    vm: MainViewModel,
    onFinish: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val draft by vm.draft.collectAsState()

    val bmi = draft.bmi
    val bone = BoneType.fromBmi(bmi)
    val tier = PhallusTierResolver.resolve(
        draft.erectLengthCm.toDouble(), draft.erectGirthCm.toDouble(),
    )
    val dragon = PhallusTierResolver.dragonShape(
        draft.flaccidLengthCm.toDouble(), draft.erectLengthCm.toDouble(),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0518), ZiWeiColors.DeepSpace, Color(0xFF170B29)),
                ),
            ),
    ) {
        StarfieldBackground(animated = false, starCount = 50, seed = 19)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(46.dp))
            Text(
                text = "肉 身 法 相",
                style = ZiWeiType.SealTitle,
                color = ZiWeiColors.Gold,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "比体量，定骨相",
                fontSize = 11.sp,
                color = ZiWeiColors.TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            GoldDivider()
            Spacer(Modifier.height(16.dp))

            // ---- 身量体魄 ----
            PhysiqueLabel("身量体魄")
            Spacer(Modifier.height(9.dp))

            MetricSlider(
                label = "身高",
                value = draft.heightCm,
                range = 140..210,
                unit = "cm",
                onChange = { v -> vm.updateDraft { it.copy(heightCm = v) } },
            )
            Spacer(Modifier.height(6.dp))
            MetricSlider(
                label = "体重",
                value = draft.weightKg,
                range = 35..150,
                unit = "kg",
                onChange = { v -> vm.updateDraft { it.copy(weightKg = v) } },
            )

            Spacer(Modifier.height(12.dp))
            BoneCard(bmi = bmi, bone = bone)

            Spacer(Modifier.height(22.dp))

            // ---- 双修频次 ----
            PhysiqueLabel("双修频次 · 伴侣性爱频率")
            Spacer(Modifier.height(9.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                PartnerFrequency.entries.forEach { f ->
                    OptionRow(
                        title = f.classicLabel,
                        subtitle = f.label + if (f.isHighIntensity) " · 高频" else "",
                        selected = draft.partnerFrequency == f,
                    ) {
                        haptics.tick()
                        vm.updateDraft { it.copy(partnerFrequency = f) }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            if (draft.isMale) {
                PhysiqueLabel("龙根法相 · 计量精确到 0.1cm")
                Spacer(Modifier.height(9.dp))

                Text(
                    text = "潜龙勿用（疲软状态）",
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Jade,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                FloatSlider(
                    label = "长度",
                    value = draft.flaccidLengthCm,
                    min = 3f,
                    max = 15f,
                    unit = "cm",
                    onChange = { v -> vm.updateDraft { it.copy(flaccidLengthCm = v) } },
                )
                FloatSlider(
                    label = "周长",
                    value = draft.flaccidGirthCm,
                    min = 5f,
                    max = 14f,
                    unit = "cm",
                    onChange = { v -> vm.updateDraft { it.copy(flaccidGirthCm = v) } },
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "飞龙在天（勃起状态）",
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Cinnabar,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                FloatSlider(
                    label = "长度",
                    value = draft.erectLengthCm,
                    min = 8f,
                    max = 22f,
                    unit = "cm",
                    onChange = { v -> vm.updateDraft { it.copy(erectLengthCm = v) } },
                )
                FloatSlider(
                    label = "周长",
                    value = draft.erectGirthCm,
                    min = 8f,
                    max = 18f,
                    unit = "cm",
                    onChange = { v -> vm.updateDraft { it.copy(erectGirthCm = v) } },
                )
                Spacer(Modifier.height(12.dp))
                DragonCard(tier = tier, dragon = dragon)
            } else {
                PhysiqueLabel("玉女法相")
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "云峰规模（罩杯）",
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Jade,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    CupSize.entries.forEach { c ->
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (draft.cupSize == c) ZiWeiColors.Gold.copy(alpha = 0.22f)
                                    else Color(0xFF1B1030),
                                )
                                .border(
                                    0.8.dp,
                                    if (draft.cupSize == c) ZiWeiColors.Gold
                                    else ZiWeiColors.GoldDim.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp),
                                )
                                .tapNoRipple {
                                    haptics.tick()
                                    vm.updateDraft { it.copy(cupSize = c) }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = c.label.take(2),
                                fontSize = 12.sp,
                                fontFamily = ZiWeiSerifFamily,
                                color = if (draft.cupSize == c) ZiWeiColors.Gold
                                else ZiWeiColors.TextSecondary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "玉门形制（外阴形状）",
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Jade,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    JadeGateShape.entries.forEach { j ->
                        val sel = draft.jadeGateShape == j
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (sel) ZiWeiColors.Gold.copy(alpha = 0.10f)
                                    else Color(0xFF1B1030),
                                )
                                .border(
                                    0.8.dp,
                                    if (sel) ZiWeiColors.Gold.copy(alpha = 0.85f)
                                    else ZiWeiColors.GoldDim.copy(alpha = 0.25f),
                                    RoundedCornerShape(10.dp),
                                )
                                .tapNoRipple {
                                    haptics.tick()
                                    vm.updateDraft { it.copy(jadeGateShape = j) }
                                }
                                .padding(horizontal = 13.dp, vertical = 11.dp),
                        ) {
                            Text(
                                text = j.label,
                                fontSize = 14.sp,
                                fontFamily = ZiWeiSerifFamily,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) ZiWeiColors.Gold else ZiWeiColors.TextSecondary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = j.hint,
                                fontSize = 10.sp,
                                color = ZiWeiColors.TextTertiary,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "月信潮汐（可选）",
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Jade,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    MenstrualPhase.entries.forEach { m ->
                        OptionRow(
                            title = m.label,
                            subtitle = when (m) {
                                MenstrualPhase.NONE -> "不标记，按平常日推演"
                                MenstrualPhase.PERIOD -> "引擎将强制锁定外部轻柔功法，禁纳入式"
                                MenstrualPhase.OVULATION -> "氤氲期，敏感度提升，宜温和"
                            },
                            selected = draft.menstrualPhase == m,
                        ) {
                            haptics.tick()
                            vm.updateDraft { it.copy(menstrualPhase = m) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            SealButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    haptics.seal()
                    vm.completeOnboarding { onFinish() }
                },
            ) {
                Text(
                    text = "法 相 既 成 · 入 紫 薇 皇 历",
                    fontSize = 15.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFF3E0),
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PhysiqueLabel(text: String) {
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
private fun BoneCard(bmi: Double, bone: BoneType) {
    val accent = when (bone) {
        BoneType.QINGQI -> ZiWeiColors.Jade
        BoneType.YUNTING -> ZiWeiColors.Gold
        BoneType.FENGLONG -> ZiWeiColors.Cinnabar
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(0.9.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(11.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = bone.classicLabel,
                fontSize = 15.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "BMI ${"%.1f".format(bmi)}",
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = ZiWeiColors.TextTertiary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = bone.physiqueHint,
            fontSize = 11.sp,
            color = ZiWeiColors.TextSecondary,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun DragonCard(tier: PhallusTier, dragon: DragonShape?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(ZiWeiColors.Cinnabar.copy(alpha = 0.08f))
            .border(0.9.dp, ZiWeiColors.Cinnabar.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tier.label,
                fontSize = 15.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = ZiWeiColors.Cinnabar,
                letterSpacing = 1.sp,
            )
            if (dragon != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = dragon.label,
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Gold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = when (tier) {
                PhallusTier.MAJESTIC -> "尺寸雄伟，充血量大，易因干摩擦而充血水肿磨损。宜【甘露沐身】护盾，忌快攻干涩。"
                PhallusTier.REFINED -> "精巧敏锐，神经末梢密度高，极易受强震过载。宜【灵犀点穴】精准低幅，忌高频强吸。"
                PhallusTier.BALANCED -> "尺寸匀停，阴阳均衡，百无禁忌，可随当日心境自由择法。"
            } + if (dragon == DragonShape.BLOOD) " 膨胀比高（血肉龙），前戏需充足充血引导。" else
                if (dragon == DragonShape.BONE) " 膨胀比低（筋骨龙），充血快速，可直接进入主戏。" else "",
            fontSize = 11.sp,
            color = ZiWeiColors.TextSecondary,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun MetricSlider(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onChange: (Int) -> Unit,
) {
    val haptics = LocalHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.TextSecondary,
            modifier = Modifier.width(38.dp),
        )
        StepButtonSmall("−") {
            onChange((value - 1).coerceAtLeast(range.first))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            SliderTrack(
                fraction = (value - range.first).toFloat() / (range.last - range.first),
                onFraction = { frac ->
                    haptics.tick()
                    val span = range.last - range.first
                    onChange((range.first + Math.round(frac * span)).coerceIn(range.first, range.last))
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$value $unit",
                fontSize = 15.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Medium,
                color = ZiWeiColors.Gold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        StepButtonSmall("+") {
            onChange((value + 1).coerceAtMost(range.last))
        }
    }
}

@Composable
private fun FloatSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    onChange: (Float) -> Unit,
) {
    val haptics = LocalHaptics.current
    // 统一按 0.1 精度吸附，修正浮点误差后再回调
    fun snap(raw: Float): Float =
        (Math.round(raw * 10f) / 10f).coerceIn(min, max)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.TextSecondary,
            modifier = Modifier.width(38.dp),
        )
        StepButtonSmall("−") {
            onChange(snap(value - 0.1f))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            SliderTrack(
                fraction = (value - min) / (max - min),
                onFraction = { frac ->
                    haptics.tick()
                    // 拖动时把触点的横向比例直接换算成数值（0.1 精度），
                    // 不再把像素/刻度当步数用，否则会出现「拖不动」的假象。
                    onChange(snap(min + frac * (max - min)))
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${"%.1f".format(value)} $unit",
                fontSize = 15.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Medium,
                color = ZiWeiColors.Cinnabar,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        StepButtonSmall("+") {
            onChange(snap(value + 0.1f))
        }
    }
}

/**
 * 可拖动的滑条。
 *
 * 之前的实现只有一个 tapNoRipple：点击时把「当前位置对应的刻度」当成了新值，
 * 于是无论点哪里数值几乎不变 —— 用户感知就是「拖不动」。
 * 现在改为 pointerInput + detectDragGestures / detectTapGestures，
 * 用触点的横向像素比例直接换算 fraction，支持拖动、点按两端。
 */
@Composable
private fun SliderTrack(
    fraction: Float,
    onFraction: (Float) -> Unit,
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    val clamped = fraction.coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                    },
                ) { change, _ ->
                    onFraction((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val cy = size.height / 2
            drawLine(
                color = Color(0xFF2A1A48),
                start = androidx.compose.ui.geometry.Offset(0f, cy),
                end = androidx.compose.ui.geometry.Offset(size.width, cy),
                strokeWidth = 4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            val x = (size.width * clamped).coerceIn(0f, size.width)
            drawLine(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(ZiWeiColors.Gold.copy(alpha = 0.5f), ZiWeiColors.Gold),
                ),
                start = androidx.compose.ui.geometry.Offset(0f, cy),
                end = androidx.compose.ui.geometry.Offset(x, cy),
                strokeWidth = 4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            // 触控热区放大到 40dp 直径，避免细滑条难以抓取
            drawCircle(
                color = ZiWeiColors.Gold.copy(alpha = 0.18f),
                radius = 14.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, cy),
            )
            drawCircle(
                color = ZiWeiColors.Gold,
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, cy),
            )
            drawCircle(
                color = ZiWeiColors.DeepSpace,
                radius = 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, cy),
            )
        }
    }
}

@Composable
private fun StepButtonSmall(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF231540))
            .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .tapNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, color = ZiWeiColors.Gold, fontFamily = ZiWeiSerifFamily)
    }
}

@Composable
private fun OptionRow(
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
                if (selected) ZiWeiColors.Gold.copy(alpha = 0.8f)
                else ZiWeiColors.GoldDim.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp),
            )
            .tapNoRipple { onClick() }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontFamily = ZiWeiSerifFamily,
                color = if (selected) ZiWeiColors.Gold else ZiWeiColors.TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                    lineHeight = 14.sp,
                )
            }
        }
        if (selected) Text("✓", fontSize = 15.sp, color = ZiWeiColors.Gold)
    }
}
