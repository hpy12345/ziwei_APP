package com.ziweistar.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.ziweistar.app.ui.theme.ZiWeiColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 深邃夜空星图背景（紫微深空 + 稀疏星点 + 极淡星云雾）
 */
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 90,
    animated: Boolean = true,
    seed: Int = 7,
) {
    data class S(val x: Float, val y: Float, val r: Float)

    val stars = remember(seed, starCount) {
        val rnd = Random(seed)
        List(starCount) { S(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 1.4f + 0.35f) }
    }

    val twinkle by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "twinkle",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A1445).copy(alpha = 0.55f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.12f),
                radius = w * 0.9f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1B2A4A).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * 0.1f, h * 0.78f),
                radius = w * 0.85f,
            ),
        )
        for (i in stars.indices) {
            val s = stars[i]
            val alpha = if (animated) twinkle * (0.4f + 0.6f * (i % 5) / 4f) else 0.75f
            drawCircle(
                color = Color(0xFFE8E0FF).copy(alpha = alpha.coerceIn(0.15f, 1f)),
                radius = s.r * density * 0.55f + 0.6f,
                center = Offset(s.x * w, s.y * h),
            )
        }
    }
}

/**
 * 二十八宿星轨罗盘（0.5°/s 恒速顺时针静谧流转）
 */
@Composable
fun StarDial(
    modifier: Modifier = Modifier,
    running: Boolean = true,
    ringColor: Color = ZiWeiColors.Gold,
    accentColor: Color = ZiWeiColors.Cinnabar,
    innerAlpha: Float = 0.9f,
) {
    val rotation by rememberInfiniteTransition(label = "dial").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            // 0.5°/s → 360° 需要 720 秒
            animation = tween(durationMillis = 720_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dialRotation",
    )
    val effectiveRotation = if (running) rotation else 0f

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(size.width, size.height) / 2f
        val rOuter = maxR * 0.96f
        val rMid = maxR * 0.78f
        val rInner = maxR * 0.62f

        drawCircle(ringColor.copy(alpha = 0.42f * innerAlpha), rOuter, Offset(cx, cy), style = Stroke(1.4.dp.toPx()))
        drawCircle(ringColor.copy(alpha = 0.24f * innerAlpha), rMid, Offset(cx, cy), style = Stroke(0.8.dp.toPx()))
        drawCircle(ringColor.copy(alpha = 0.30f * innerAlpha), rInner, Offset(cx, cy), style = Stroke(0.9.dp.toPx()))

        // 二十八宿刻度
        for (i in 0 until 28) {
            val angle = i * (360f / 28f)
            val rad = Math.toRadians(angle.toDouble())
            val major = i % 7 == 0
            val r1 = rOuter - if (major) 16.dp.toPx() else 9.dp.toPx()
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            drawLine(
                color = ringColor.copy(alpha = (if (major) 0.85f else 0.42f) * innerAlpha),
                start = Offset(cx + r1 * c, cy + r1 * s),
                end = Offset(cx + rOuter * c, cy + rOuter * s),
                strokeWidth = if (major) 1.6.dp.toPx() else 0.9.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 十二时辰刻度
        for (i in 0 until 12) {
            val angle = i * 30f - 90f
            val rad = Math.toRadians(angle.toDouble())
            val r1 = rMid - 7.dp.toPx()
            drawCircle(
                color = ringColor.copy(alpha = 0.55f * innerAlpha),
                radius = 1.5.dp.toPx(),
                center = Offset(cx + r1 * cos(rad).toFloat(), cy + r1 * sin(rad).toFloat()),
            )
        }

        // 旋转的星宿光带 + 指针
        rotate(degrees = effectiveRotation, pivot = Offset(cx, cy)) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        ringColor.copy(alpha = 0.45f),
                        ringColor.copy(alpha = 0.85f),
                        Color.Transparent,
                    ),
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(cx - rInner, cy - rInner),
                size = Size(rInner * 2, rInner * 2),
                style = Stroke(2.dp.toPx()),
            )
            drawLine(
                color = ringColor.copy(alpha = 0.9f),
                start = Offset(cx, cy - rInner),
                end = Offset(cx, cy - rOuter + 18.dp.toPx()),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
            )
            for (i in 0 until 7) {
                val a = Math.toRadians(i * 51.43)
                val rr = rInner * 0.86f
                drawCircle(
                    color = ringColor.copy(alpha = 0.65f),
                    radius = (if (i % 2 == 0) 2.2f else 1.3f) * density,
                    center = Offset(cx + rr * cos(a).toFloat(), cy + rr * sin(a).toFloat()),
                )
            }
        }
    }
}

/**
 * 太极呼吸导引环
 *
 * 按 4-7-8 慢呼吸节律缩放：吸气 → 屏息 → 呼气。
 * 用于【锁龙御气】推荐时帮助降低交感神经兴奋度。
 */
@Composable
fun BreathingRing(
    modifier: Modifier = Modifier,
    inhaleSec: Float = 4f,
    holdSec: Float = 7f,
    exhaleSec: Float = 8f,
    color: Color = ZiWeiColors.Jade,
    running: Boolean = true,
) {
    val total = inhaleSec + holdSec + exhaleSec
    val cycleMs = (total * 1000).toInt().coerceAtLeast(1000)

    val progress by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathProgress",
    )
    val p = if (running) progress else 0f

    // 分段：吸气 0→inhale 缩放 0.62→1.0；屏息 保持 1.0；呼气 1.0→0.62
    val inhaleFrac = inhaleSec / total
    val holdFrac = (inhaleSec + holdSec) / total
    val scale = when {
        p < inhaleFrac -> 0.62f + (1.0f - 0.62f) * (p / inhaleFrac)
        p < holdFrac -> 1.0f
        else -> 1.0f - (1.0f - 0.62f) * ((p - holdFrac) / (1f - holdFrac))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val base = minOf(size.width, size.height) / 2f

        drawCircle(
            color = color.copy(alpha = 0.14f),
            radius = base * 0.72f,
            center = Offset(cx, cy),
            style = Stroke(1.dp.toPx()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.22f),
                    color.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = base,
            ),
            radius = base * scale,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = color.copy(alpha = 0.55f),
            radius = base * 0.72f * scale,
            center = Offset(cx, cy),
            style = Stroke(1.6.dp.toPx()),
        )
    }
}
