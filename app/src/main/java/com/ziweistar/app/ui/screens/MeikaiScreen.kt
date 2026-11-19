package com.ziweistar.app.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 【梅开二度】烫金贺卡
 *
 * 设计文档 5.1：距上次收功不足 6 小时，或同一自然日内完成第 2 次收功且无严重不良反应时弹出。
 * 视觉：烫金喜报卷轴，飘落金色梅花，盖下朱砂大印「梅开二度」。
 */
@Composable
fun MeikaiScreen(
    onDismiss: () -> Unit,
    doc: com.ziweistar.app.engine.llm.ScenarioDoc? = null,
    onNeedDoc: () -> Unit = {},
) {
    val haptics = LocalHaptics.current

    // 卡片弹出即向双擎流水线索要批文（LLM 失败自动落本地兜底）
    LaunchedEffect(Unit) { onNeedDoc() }

    // 入场
    val cardScale = remember { Animatable(0.82f) }
    val cardAlpha = remember { Animatable(0f) }
    val sealScale = remember { Animatable(2.1f) }
    val sealAlpha = remember { Animatable(0f) }
    var sealed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        haptics.light()
        cardAlpha.animateTo(1f, tween(420))
    }
    LaunchedEffect(Unit) {
        cardScale.animateTo(1f, tween(620, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(560)
        sealAlpha.animateTo(1f, tween(180))
        haptics.heavy()
        sealScale.animateTo(1f, tween(320))
        kotlinx.coroutines.delay(340)
        haptics.seal()
        sealed = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF00B0614)),
    ) {
        StarfieldBackground(animated = true, starCount = 80, seed = 5)

        // 飘落梅花
        PlumBlossomRain()

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .scale(cardScale.value)
                    .alpha(cardAlpha.value)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3A2418), Color(0xFF241209), Color(0xFF3A2418)),
                        ),
                    )
                    .border(
                        1.4.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE6C17A),
                                Color(0xFFF5D97A),
                                Color(0xFFB08A3E),
                                Color(0xFFE6C17A),
                            ),
                        ),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 26.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "喜　报",
                        fontSize = 11.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6C17A),
                        letterSpacing = 8.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    GoldDivider(alpha = 0.5f)
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "梅 开 二 度",
                        fontSize = 34.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5D97A),
                        letterSpacing = 7.sp,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "双 星 拱 照 · 气 贯 周 天",
                        fontSize = 11.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFFE6C17A).copy(alpha = 0.85f),
                        letterSpacing = 3.sp,
                    )

                    Spacer(Modifier.height(18.dp))
                    GoldDivider(alpha = 0.35f)
                    Spacer(Modifier.height(18.dp))

                    // 正文：优先取双擎流水线批文（LLM / 本地兜底），未就绪时用固定文案
                    Text(
                        text = doc?.body?.ifBlank { null }
                            ?: "阁下今日炉鼎再燃，星轨已烙双梅之印。\n" +
                            "然则一鼓作气，再而衰，三而竭。\n" +
                            "温汤一盏，且收心神；事不过三，切记切记。",
                        fontSize = 12.5.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFFEADFC8),
                        lineHeight = 24.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                    )

                    // 养生即刻处方（仅批文提供时展示）
                    doc?.extra?.takeIf { it.isNotBlank() }?.let { rx ->
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1FE6C17A))
                                .border(0.7.dp, Color(0x66E6C17A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = "【即刻处方】$rx",
                                fontSize = 11.sp,
                                fontFamily = ZiWeiSerifFamily,
                                color = Color(0xFFF5D97A),
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // 朱砂大印「梅开二度」
                    Box(
                        Modifier
                            .scale(sealScale.value)
                            .alpha(sealAlpha.value)
                            .size(132.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(3.dp, ZiWeiColors.Cinnabar, RoundedCornerShape(10.dp))
                            .padding(5.dp)
                            .border(1.dp, ZiWeiColors.Cinnabar.copy(alpha = 0.7f), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PlumGlyph(size = 15.dp, color = ZiWeiColors.Cinnabar)
                                PlumGlyph(size = 15.dp, color = ZiWeiColors.Cinnabar)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "梅",
                                fontSize = 20.sp,
                                fontFamily = ZiWeiSerifFamily,
                                fontWeight = FontWeight.Bold,
                                color = ZiWeiColors.Cinnabar,
                                lineHeight = 22.sp,
                            )
                            Text(
                                text = "开",
                                fontSize = 20.sp,
                                fontFamily = ZiWeiSerifFamily,
                                fontWeight = FontWeight.Bold,
                                color = ZiWeiColors.Cinnabar,
                                lineHeight = 22.sp,
                            )
                            Text(
                                text = "二度",
                                fontSize = 15.sp,
                                fontFamily = ZiWeiSerifFamily,
                                fontWeight = FontWeight.Bold,
                                color = ZiWeiColors.Cinnabar,
                                lineHeight = 17.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "依律：6 小时内再起坛者，梅开二度",
                        fontSize = 9.sp,
                        color = Color(0xFFB09878),
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            // 按钮
            Box(
                Modifier
                    .alpha(cardAlpha.value)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE6C17A), Color(0xFFC9A45C), Color(0xFFE6C17A)),
                        ),
                    )
                    .tapNoRipple {
                        haptics.light()
                        onDismiss()
                    }
                    .padding(horizontal = 46.dp, vertical = 15.dp),
            ) {
                Text(
                    text = "已 领 法 旨",
                    fontSize = 14.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C221E),
                    letterSpacing = 5.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "日历已烙金色双梅印",
                fontSize = 9.5.sp,
                color = Color(0xFF7C6E93),
                modifier = Modifier.alpha(cardAlpha.value),
            )
        }
    }
}

/** 梅花五瓣 */
@Composable
private fun PlumGlyph(size: androidx.compose.ui.unit.Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val r = this.size.minDimension * 0.36f
        repeat(5) { i ->
            val ang = Math.toRadians(-90.0 + i * 72.0)
            val px = cx + (r * cos(ang)).toFloat()
            val py = cy + (r * sin(ang)).toFloat()
            drawCircle(color = color, radius = r * 0.78f, center = Offset(px, py))
        }
        drawCircle(color = Color(0xFF2C221E), radius = r * 0.34f, center = Offset(cx, cy))
    }
}

/** 飘落梅花：Canvas 上按时间推进的多颗花瓣 */
@Composable
private fun PlumBlossomRain() {
    val petals = remember {
        val rnd = Random(31415)
        List(26) {
            Petal(
                x0 = rnd.nextFloat(),
                speed = 0.06f + rnd.nextFloat() * 0.11f,
                phase = rnd.nextFloat(),
                size = 5f + rnd.nextFloat() * 7f,
                drift = (rnd.nextFloat() - 0.5f) * 0.18f,
                alpha = 0.25f + rnd.nextFloat() * 0.5f,
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(9000, easing = LinearEasing))
    }
    val c = Color(0xFFE6C17A)
    Canvas(Modifier.fillMaxSize()) {
        petals.forEach { p ->
            val prog = ((p.phase + t.value * p.speed * 8f) % 1.0f)
            val y = prog * size.height * 1.15f - size.height * 0.08f
            val x = (p.x0 + p.drift * prog) * size.width
            val rot = prog * 360f
            rotate(degrees = rot, pivot = Offset(x, y)) {
                repeat(5) { i ->
                    val ang = Math.toRadians(-90.0 + i * 72.0)
                    val pr = p.size * 0.5f
                    val px = x + (pr * cos(ang)).toFloat()
                    val py = y + (pr * sin(ang)).toFloat()
                    drawCircle(
                        color = c.copy(alpha = p.alpha * (1f - prog * 0.5f)),
                        radius = pr * 0.7f,
                        center = Offset(px, py),
                    )
                }
            }
        }
    }
}

private data class Petal(
    val x0: Float,
    val speed: Float,
    val phase: Float,
    val size: Float,
    val drift: Float,
    val alpha: Float,
)
