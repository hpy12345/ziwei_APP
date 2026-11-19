package com.ziweistar.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors

/**
 * 玄门黑话释义（设计文档 1.4 术语黑话对照表）
 */
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    val haptics = LocalHaptics.current

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground(animated = false, starCount = 45, seed = 133)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(30.dp))

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
                    text = "玄 门 释 义",
                    fontSize = 15.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = ZiWeiColors.Gold,
                    letterSpacing = 4.sp,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF251545), Color(0xFF170D2C))),
                    )
                    .border(
                        0.8.dp,
                        ZiWeiColors.GoldDim.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        text = "紫薇星者，取「紫微垣」居中之象，以钦天监之笔，记昼夜气机之盈亏。",
                        fontSize = 12.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = ZiWeiColors.TextSecondary,
                        lineHeight = 21.sp,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "本卷汇录玄门术语与尘世白话之对照，以便观星时无惑。",
                        fontSize = 11.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = ZiWeiColors.TextTertiary,
                        lineHeight = 19.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            GlossaryGroup(
                title = "核心动作",
                entries = listOf(
                    "观星 / 施法" to "自慰 / 独处自我探索",
                    "起坛 / 收功" to "开始计时 / 结束并保存记录",
                    "暂息 / 散功" to "暂停计时 / 中途放弃未继续",
                    "入定" to "贤者时间（高潮后的不应期与心率平复期）",
                ),
            )

            Spacer(Modifier.height(14.dp))

            GlossaryGroup(
                title = "状态与结果",
                entries = listOf(
                    "飞升 / 登顶" to "射精（男）/ 单次达高潮（女）",
                    "九天揽月" to "连续多重高潮（女性 / 男性前列腺高潮）",
                    "锁龙御气" to "男性边缘控制（Edging）",
                    "九浅一深" to "女性边缘控制",
                    "微醺即止" to "未达高潮，但过程身心愉悦、主动温和结束",
                ),
            )

            Spacer(Modifier.height(14.dp))

            GlossaryGroup(
                title = "思维要素",
                entries = listOf(
                    "天时洞府" to "发生的时间段（十二时辰）与所处物理空间",
                    "神通功法" to "自慰的具体手法、体位或使用的器具",
                    "结缘星宿" to "幻想对象（支持上传自定义照片/立绘）",
                    "所借灵媒" to "助兴内容形式（视频、音频、小说、纯脑补等）",
                ),
            )

            Spacer(Modifier.height(14.dp))

            GlossaryGroup(
                title = "生理禀赋",
                entries = listOf(
                    "骨相 / 气血" to "身高、体重换算的 BMI 体魄定性与精气盈亏度",
                    "龙根 / 玉门" to "男性生殖器尺寸 / 女性胸部罩杯与外阴形态",
                    "元气值（Qi）" to "基于近期频次、时长与不良反应计算的生理恢复指数",
                ),
            )

            Spacer(Modifier.height(14.dp))

            GlossaryGroup(
                title = "身体别名",
                entries = listOf(
                    "龙根 / 金茎" to "阴茎",
                    "玉门 / 幽谷" to "外阴 / 阴道",
                    "灵珠 / 绛珠" to "阴蒂",
                    "云峰 / 酥山" to "胸部",
                    "甘露" to "润滑液",
                    "潜龙 / 飞龙" to "疲软 / 勃起状态",
                ),
            )

            Spacer(Modifier.height(14.dp))

            GlossaryGroup(
                title = "吉凶与仪式",
                entries = listOf(
                    "吉凶总印" to "今日总体吉凶判定（大吉 / 小吉 / 平 / 大凶）",
                    "建除十二神" to "建除满平定执破危成收开闭，今日行事之宜忌",
                    "二十八星宿" to "今日值宿之星与七曜气机",
                    "铁券定局" to "本地规则引擎算出的确定性结论，AI 不可篡改",
                    "梅开二度" to "6 小时内再次观星，或同日第 2 次",
                    "天罡戒律" to "单日 ≥3 次或元气 < 20 时的封禁劝诫",
                    "走火入魔录" to "不良反应记录（腰膝酸软、头昏耳鸣等）",
                    "钦天监" to "本应用内的天象观测与批文机构（虚构）",
                ),
            )

            Spacer(Modifier.height(24.dp))

            GoldDivider(alpha = 0.25f)

            Spacer(Modifier.height(16.dp))

            Text(
                text = "本应用所述「玄门」「星宿」「批文」，皆为记录与自我观察之隐喻框架，\n不构成任何医疗、诊断或治疗建议。若身体有恙，请咨询执业医师。",
                fontSize = 9.5.sp,
                color = ZiWeiColors.TextTertiary,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GlossaryGroup(title: String, entries: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1A1E1136))
            .border(
                0.7.dp,
                ZiWeiColors.GoldDim.copy(alpha = 0.28f),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ZiWeiColors.Gold),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = ZiWeiColors.Gold,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        entries.forEachIndexed { i, (term, meaning) ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = term,
                    fontSize = 11.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Jade,
                    modifier = Modifier.width(92.dp),
                    lineHeight = 17.sp,
                )
                Text(
                    text = meaning,
                    fontSize = 10.5.sp,
                    color = ZiWeiColors.TextSecondary,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            if (i != entries.lastIndex) {
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}
