package com.ziweistar.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.model.Fortune
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.BreathingRing
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.components.StarAvatar
import com.ziweistar.app.ui.components.StarDial
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiType
import kotlinx.coroutines.delay

/**
 * 起坛：沉浸式星轨计时界面
 *
 * 设计文档 3.3-A：
 *  - 深邃旋转的二十八星宿星图
 *  - 古风与现代双轨计时：`零时 一刻 十二息（00:18:12）`（1刻=15分，1息=3秒）
 *  - 中央若隐若现的结缘星宿呼吸光晕头像（单击隐藏）
 *  - 太极呼吸导引环
 *  - 底部「暂息」「收功」
 */
@Composable
fun TimerScreen(
    vm: MainViewModel,
    onFinish: (startedAt: Long, endedAt: Long, durationSec: Int, pausedSec: Int) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val start by vm.timerStart.collectAsState()
    val home by vm.home.collectAsState()
    val oracle by vm.oracle.collectAsState()
    val bitmaps by vm.starBitmaps.collectAsState()

    var elapsed by remember { mutableStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var pausedAccum by remember { mutableStateOf(0L) }
    var showAvatar by remember { mutableStateOf(true) }

    val startedAt = remember(start) { start ?: System.currentTimeMillis() }

    LaunchedEffect(paused, start) {
        while (!paused) {
            delay(1000L)
            elapsed = (System.currentTimeMillis() - startedAt - pausedAccum) / 1000L
        }
    }

    BackHandler {
        paused = true
        onBack()
    }

    val lock = oracle?.lock
    val lockStarName = lock?.star?.name ?: ""
    val lockStarId = lock?.star?.starId
    val avatarBitmap = remember(lockStarId, bitmaps) {
        lockStarId?.let { id -> bitmaps.entries.firstOrNull { it.key == id }?.value }
    }
    val fortune = oracle?.let { g ->
        Fortune.entries.firstOrNull { g.oracle.fortuneLabel.contains(it.label) }
    }

    // 呼吸节奏：推荐锁龙御气/九浅一深 → 4-7-8 慢呼吸；否则 4-4-4 常规
    val labels = lock?.method?.labels ?: emptyList()
    val isEdgeControl = labels.any { it.contains("锁龙") || it.contains("九浅") }
    val inhaleSec = if (isEdgeControl) 4f else 3f
    val holdSec = if (isEdgeControl) 7f else 3f
    val exhaleSec = if (isEdgeControl) 8f else 4f

    val accent = when (fortune) {
        Fortune.GREAT -> ZiWeiColors.Cinnabar
        Fortune.SMALL -> ZiWeiColors.Gold
        Fortune.OMINOUS -> ZiWeiColors.Jade
        else -> ZiWeiColors.Jade
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080312), Color(0xFF10061E), Color(0xFF080312)),
                ),
            ),
    ) {
        StarfieldBackground(animated = true, starCount = 130, seed = 77)

        // 星盘底
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(370.dp),
        ) {
            StarDial(running = !paused, ringColor = ZiWeiColors.Gold, innerAlpha = 0.85f)
            BreathingRing(
                inhaleSec = inhaleSec,
                holdSec = holdSec,
                exhaleSec = exhaleSec,
                color = accent,
                running = !paused,
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(50.dp))

            Text(
                text = "起 坛",
                fontSize = 16.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = ZiWeiColors.Gold,
                letterSpacing = 8.sp,
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = if (paused) "· 暂息中 ·" else "· 星轨流转 ·",
                fontSize = 10.sp,
                fontFamily = ZiWeiSerifFamily,
                color = if (paused) ZiWeiColors.Cinnabar else ZiWeiColors.TextTertiary,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.weight(1f))

            // 中央星宿头像 + 双轨计时
            Box(contentAlignment = Alignment.Center) {
                if (showAvatar && lockStarName.isNotBlank() && fortune != Fortune.OMINOUS) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StarAvatar(
                            size = 64.dp,
                            image = avatarBitmap,
                            fallbackText = lockStarName,
                            ringColor = ZiWeiColors.Gold,
                            active = true,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = lockStarName,
                            fontSize = 10.sp,
                            fontFamily = ZiWeiSerifFamily,
                            color = ZiWeiColors.Gold.copy(alpha = 0.65f),
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 古风时辰轨
            Text(
                text = ancientTime(elapsed),
                fontSize = 17.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.Gold.copy(alpha = 0.9f),
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            // 现代数码轨
            Text(
                text = digitalTime(elapsed),
                fontSize = 46.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                color = ZiWeiColors.RicePaper,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.weight(1f))

            // 今日灵签提示（极简）
            if (oracle != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1AE6C17A))
                        .border(0.7.dp, ZiWeiColors.GoldDim.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "今日「${oracle!!.oracle.fortuneShort}」· " +
                            "推荐功法：${labels.take(2).joinToString("、").ifBlank { "随心而行" }}",
                        fontSize = 10.5.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = ZiWeiColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // 按钮
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 暂息
                Box(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(Color(0xFF1B1030))
                        .border(
                            1.2.dp,
                            if (paused) ZiWeiColors.Jade else ZiWeiColors.GoldDim.copy(alpha = 0.6f),
                            RoundedCornerShape(27.dp),
                        )
                        .tapNoRipple {
                            haptics.light()
                            if (paused) {
                                pausedAccum += 0L
                                paused = false
                            } else {
                                paused = true
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (paused) "复 起" else "暂 息",
                        fontSize = 15.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Medium,
                        color = if (paused) ZiWeiColors.Jade else ZiWeiColors.Gold,
                        letterSpacing = 3.sp,
                    )
                }

                // 收功
                Box(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFE0574F), Color(0xFFBE3A31)),
                            ),
                        )
                        .tapNoRipple {
                            haptics.heavy()
                            val now = System.currentTimeMillis()
                            val total = ((now - startedAt - pausedAccum) / 1000L).toInt()
                                .coerceAtLeast(1)
                            onFinish(startedAt, now, total, (pausedAccum / 1000L).toInt())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "收 功",
                        fontSize = 15.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFF3E0),
                        letterSpacing = 3.sp,
                    )
                }
            }

            if (lockStarName.isNotBlank() && fortune != Fortune.OMINOUS) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (showAvatar) "轻触此处隐藏星宿影像" else "轻触此处显影星宿",
                    fontSize = 9.sp,
                    color = ZiWeiColors.TextTertiary,
                    modifier = Modifier
                        .tapNoRipple { showAvatar = !showAvatar }
                        .padding(8.dp),
                )
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

/** 00:18:12 */
private fun digitalTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

/**
 * 古风时辰：`零时 一刻 十二息`
 * 换算基准：1 刻 = 15 分钟，1 息 = 3 秒
 */
private fun ancientTime(seconds: Long): String {
    val hours = (seconds / 3600).toInt()
    val remainderMin = ((seconds % 3600) / 60).toInt()
    val remainderSec = (seconds % 60).toInt()
    val ke = remainderMin / 15
    val xi = (remainderMin % 15) * 20 + remainderSec / 3

    return "${cnNum(hours)}时 ${cnNum(ke)}刻 ${cnNum(xi)}息"
}

private fun cnNum(n: Int): String {
    val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    if (n < 0) return "零"
    if (n < 10) return digits[n]
    if (n < 20) return "十" + if (n % 10 == 0) "" else digits[n % 10]
    if (n < 100) {
        val t = n / 10
        val o = n % 10
        return digits[t] + "十" + if (o == 0) "" else digits[o]
    }
    return n.toString()
}
