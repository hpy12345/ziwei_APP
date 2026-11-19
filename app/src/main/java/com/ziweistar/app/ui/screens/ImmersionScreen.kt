package com.ziweistar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.ui.components.BreathingRing
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import kotlinx.coroutines.delay
import java.util.Locale

/** 入定静养时长：15 分钟（设计文档 3.3「收功后入定」） */
private const val IMMERSION_SECONDS = 15 * 60

/**
 * 入定：收功后的十五分钟温养沉浸页。
 *
 * 设计文档 3.3：
 *  - 深空紫渐变 + 星野背景，全屏无干扰
 *  - 中央太极呼吸导引环（4-7-8 慢呼吸），环心显示 mm:ss 倒计时
 *  - 三行古风养生提示：温水一盏 / 勿立即沐浴 / 勿即视屏
 *  - 底部「出 定」：可随时提前结束；归零时以盖印震感提示，可正式出定
 *
 * @param qi 当前先天元气值，用于页脚的状态展示
 * @param onDismiss 出定回调（返回上层）
 */
@Composable
fun ImmersionScreen(
    qi: Int,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHaptics.current

    // 剩余秒数：用 mutableIntStateOf 保存，每秒递减 1
    var remainSec by remember { mutableIntStateOf(IMMERSION_SECONDS) }
    // 归零盖印只触发一次
    var sealFired by remember { mutableStateOf(false) }

    val finished = remainSec <= 0

    // 每秒 tick 一次，归零即停
    LaunchedEffect(Unit) {
        while (remainSec > 0) {
            delay(1000L)
            remainSec = (remainSec - 1).coerceAtLeast(0)
        }
    }

    // 归零瞬间：古琴泛音般的盖印震感（Haptics.seal 已在 HapticEngine 中实现）
    // 以 finished 为 key，仅在状态翻转时执行一次，避免每秒重复启动协程
    LaunchedEffect(finished) {
        if (finished && !sealFired) {
            sealFired = true
            haptics.seal()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080312), Color(0xFF150A28), Color(0xFF080312)),
                ),
            ),
    ) {
        // 星野：缓慢呼吸闪烁
        StarfieldBackground(animated = true, starCount = 120, seed = 415)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(54.dp))

            // 主标题
            Text(
                text = "入 定",
                fontSize = 17.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = ZiWeiColors.Gold,
                letterSpacing = 10.sp,
            )

            Spacer(Modifier.height(7.dp))

            // 副标题
            Text(
                text = "收 功 · 温 养 十 五 分",
                fontSize = 10.5.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.TextTertiary,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.weight(1f))

            // 中央呼吸环 + 环心倒计时
            Box(
                Modifier.size(250.dp),
                contentAlignment = Alignment.Center,
            ) {
                BreathingRing(
                    inhaleSec = 4f,
                    holdSec = 7f,
                    exhaleSec = 8f,
                    color = ZiWeiColors.Jade,
                    running = true,
                )
                Text(
                    text = formatCountdown(remainSec),
                    fontSize = 38.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    color = ZiWeiColors.RicePaper,
                    letterSpacing = 2.sp,
                )
            }

            Spacer(Modifier.height(24.dp))

            // 三行养生提示（古风文案）
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "温水一盏，缓润喉舌，令气血各归其经",
                    fontSize = 11.5.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.TextSecondary,
                    lineHeight = 18.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "勿立即沐浴，毛孔方张，恐寒邪乘虚而入",
                    fontSize = 11.5.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.TextSecondary,
                    lineHeight = 18.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "勿即视屏，闭目养神，使神魂归于其舍",
                    fontSize = 11.5.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.TextSecondary,
                    lineHeight = 18.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(22.dp))

            // 当前元气值
            Text(
                text = "先天元气 · $qi",
                fontSize = 12.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.Gold.copy(alpha = 0.85f),
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.weight(1f))

            // 出定按钮：未归零可提前结束；归零后为正式出定
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (finished) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3E7F76), Color(0xFF27554F)),
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1B1030), Color(0xFF1B1030)),
                            )
                        },
                    )
                    .border(
                        1.2.dp,
                        if (finished) ZiWeiColors.Jade else ZiWeiColors.GoldDim.copy(alpha = 0.6f),
                        RoundedCornerShape(28.dp),
                    )
                    .tapNoRipple {
                        haptics.light()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (finished) "出 定" else "出 定（可提前结束）",
                    fontSize = 15.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (finished) Color(0xFFEAFBF6) else ZiWeiColors.Gold,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(Modifier.height(34.dp))
        }
    }
}

/** mm:ss（例：14:59），固定 ASCII 数字，避免区域化数字字形 */
private fun formatCountdown(totalSec: Int): String {
    val sec = totalSec.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60)
}
