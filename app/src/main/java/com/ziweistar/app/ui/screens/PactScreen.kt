package com.ziweistar.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.model.PrivacyTier
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.AlmanacCard
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.SealButton
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.TagChip
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiType

/**
 * 第二步：签署《天机盟约》—— 三级隐私授权
 */
@Composable
fun PactScreen(
    vm: MainViewModel,
    onAgreed: (PrivacyTier) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val draft by vm.draft.collectAsState()
    val tier = draft.privacyTier

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0518), ZiWeiColors.DeepSpace, Color(0xFF170B29)),
                ),
            ),
    ) {
        StarfieldBackground(animated = false, starCount = 70, seed = 11)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(52.dp))

            Text(
                text = "天 机 盟 约",
                style = ZiWeiType.SealTitle,
                color = ZiWeiColors.Gold,
                fontSize = 26.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "· 隐私与安全告知 ·",
                style = ZiWeiType.Caption,
                color = ZiWeiColors.TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(22.dp))
            GoldDivider()
            Spacer(Modifier.height(22.dp))

            AlmanacCard(borderColor = ZiWeiColors.Gold.copy(alpha = 0.5f)) {
                Text(
                    text = "接下来的【肉身法相】与【双修频次】涉及阁下最私密之身体禀赋。",
                    style = ZiWeiType.Commentary,
                    color = ZiWeiColors.RicePaper,
                )
                Spacer(Modifier.height(14.dp))

                Text(
                    text = "安全铁律",
                    fontSize = 13.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = ZiWeiColors.Cinnabar,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "阁下填写的【结缘星宿】照片及原始尺寸数值，全部只保存在这台手机上，不上传任何云端服务器。\n\n" +
                        "在请求 AI 批命时，系统只会把尺寸换成「雄伟型」「清奇骨相」这类笼统说法发出去，具体数字一概不发。",
                    style = ZiWeiType.Commentary,
                    color = ZiWeiColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "阁下是否允准开启肉身法相定制？",
                fontSize = 15.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.Gold,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            // 三个授权选项
            tier.let { current ->
                PactOption(
                    title = "[允准，开启深度定制]",
                    desc = "批文最贴合本人的身体禀赋，细节只在换算后发出",
                    selected = current == PrivacyTier.FULL,
                    accent = ZiWeiColors.Gold,
                ) {
                    haptics.light()
                    vm.updateDraft { it.copy(privacyTier = PrivacyTier.FULL) }
                }
                Spacer(Modifier.height(10.dp))
                PactOption(
                    title = "[仅本地保存，不传 AI]",
                    desc = "数据只用于本机的记录统计，AI 那边按通用模板写",
                    selected = current == PrivacyTier.LOCAL_ONLY,
                    accent = ZiWeiColors.Jade,
                ) {
                    haptics.light()
                    vm.updateDraft { it.copy(privacyTier = PrivacyTier.LOCAL_ONLY) }
                }
                Spacer(Modifier.height(10.dp))
                PactOption(
                    title = "[婉拒，仅凭生辰推演]",
                    desc = "跳过身体尺码，直接按生辰八字推演",
                    selected = current == PrivacyTier.DECLINE,
                    accent = ZiWeiColors.TextTertiary,
                ) {
                    haptics.light()
                    vm.updateDraft { it.copy(privacyTier = PrivacyTier.DECLINE) }
                }
            }

            Spacer(Modifier.height(26.dp))

            SealButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    haptics.seal()
                    onAgreed(tier)
                },
            ) {
                Text(
                    text = if (tier == PrivacyTier.DECLINE) "立 约 · 仅凭生辰" else "立 约 · 开启法相",
                    fontSize = 17.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFF3E0),
                    letterSpacing = 4.sp,
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "← 返回修改生辰",
                fontSize = 12.sp,
                color = ZiWeiColors.TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .tapNoRipple { onBack() },
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PactOption(
    title: String,
    desc: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .tapNoRipple { onClick() },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .width(18.dp)
                .height(18.dp)
                .background(
                    if (selected) accent.copy(alpha = 0.25f) else Color.Transparent,
                    androidx.compose.foundation.shape.CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = if (selected) accent else accent.copy(alpha = 0.4f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (selected) 5.dp.toPx() else 1.dp.toPx(),
                    ),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) accent else ZiWeiColors.TextSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = ZiWeiColors.TextTertiary,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
internal fun TagRow(
    labels: List<String>,
    selected: String?,
    accent: Color = ZiWeiColors.Gold,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { l ->
            TagChip(text = l, active = l == selected, accent = accent) { onSelect(l) }
        }
    }
}
