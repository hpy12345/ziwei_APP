package com.ziweistar.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.ui.components.StarDial
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.theme.ZiWeiColors
import kotlinx.coroutines.delay

/**
 * 启动页：紫微深空 + 缓慢旋转的星轨 + 「紫薇星」题字
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    hasProfile: () -> Boolean,
    /** 数据库首帧是否已就绪；未就绪时不可判定去向，必须等 */
    isReady: () -> Boolean = { true },
) {
    val fade = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(durationMillis = 900, easing = LinearEasing))
        delay(700)
        // 数据未就绪则轮询等待（最多 3 秒），避免把老用户误判成新用户
        var waited = 0
        while (!isReady() && waited < 3000) {
            delay(80)
            waited += 80
        }
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0518), ZiWeiColors.DeepSpace, Color(0xFF180C2C)),
                ),
            ),
    ) {
        StarfieldBackground(animated = true, starCount = 120, seed = 31)

        Box(
            Modifier
                .align(Alignment.Center)
                .size(300.dp)
                .padding(bottom = 40.dp),
        ) {
            StarDial(running = true, innerAlpha = 0.55f)
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "紫薇星",
                fontSize = 46.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 14.sp,
                color = ZiWeiColors.Gold.copy(alpha = fade.value),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "钦 天 监 · 紫 微 内 丹 录",
                fontSize = 12.sp,
                fontFamily = ZiWeiSerifFamily,
                letterSpacing = 3.sp,
                color = ZiWeiColors.Gold.copy(alpha = 0.55f * fade.value),
                textAlign = TextAlign.Center,
            )
        }

        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            text = "观天之道 · 执天之行",
            fontSize = 11.sp,
            fontFamily = ZiWeiSerifFamily,
            letterSpacing = 4.sp,
            color = ZiWeiColors.TextTertiary.copy(alpha = fade.value),
        )
    }
}
