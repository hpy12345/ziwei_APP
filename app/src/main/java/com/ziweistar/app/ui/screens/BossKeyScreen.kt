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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.AlmanacEngine
import com.ziweistar.app.engine.model.AlmanacSnapshot
import com.ziweistar.app.ui.components.tapNoRipple
import kotlinx.coroutines.delay
import java.util.Date

/**
 * 【瞬隐·老黄历】（Boss Key 伪装屏）
 *
 * 设计文档 1.2：双指下滑 / 屏幕朝下 0.2s 内瞬切为「普通老黄历」界面，
 * 全屏无任何敏感内容，可放心交给旁人查看。
 *
 * 此屏展示真实农历/干支/建除/星宿/宜忌，视觉与其他万年历 App 无异。
 * 点击任意处 3 秒后自动返回；也可按系统返回键退出。
 */
@Composable
fun BossKeyScreen(onExit: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val snap: AlmanacSnapshot = remember(now) { AlmanacEngine.snapshot(Date(now)) }

    val date = remember(now) { java.util.Calendar.getInstance().apply { timeInMillis = now } }
    val weekNames = listOf("日", "一", "二", "三", "四", "五", "六")

    // 点击任意处 → 计时 3 秒后自动返回首页（给用户留出「假装在看黄历」的时间，
    // 避免刚摸到屏幕就被切换回去暴露）。
    var pendingExit by remember { mutableStateOf(false) }
    LaunchedEffect(pendingExit) {
        if (pendingExit) {
            delay(3_000)
            onExit()
        }
    }
    // 系统返回键同样可退出
    androidx.activity.compose.BackHandler(enabled = true) { onExit() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F3EA))
            .tapNoRipple(enabled = true) { pendingExit = true },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(46.dp))

            // 顶部：公历
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "${date.get(java.util.Calendar.YEAR)} 年 " +
                            "${date.get(java.util.Calendar.MONTH) + 1} 月",
                        fontSize = 13.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFF6B6257),
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${date.get(java.util.Calendar.DAY_OF_MONTH)}",
                        fontSize = 54.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB03A2E),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "星期" + weekNames[(date.get(java.util.Calendar.DAY_OF_WEEK) + 6) % 7],
                        fontSize = 12.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFF6B6257),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = snap.lunarText,
                        fontSize = 12.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFF3A322A),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 干支四柱栏
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFFFFF))
                    .border(0.7.dp, Color(0xFFE2D9C8), RoundedCornerShape(8.dp))
                    .padding(vertical = 13.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PillarCell("干支", snap.ganZhiDay)
                    PillarCell("建除", snap.zhiXing + "日")
                    PillarCell("星宿", snap.xiu + "宿")
                    PillarCell("七曜", snap.xiuZheng)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 节气 / 月相
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoRowCard(
                    modifier = Modifier.weight(1f),
                    label = "节气",
                    value = if (snap.jieQi.isNotBlank()) snap.jieQi else "非节气日",
                    sub = "下一节气：" + snap.nextJieQi,
                )
                InfoRowCard(
                    modifier = Modifier.weight(1f),
                    label = "月相",
                    value = snap.yueXiang,
                    sub = "星宿吉凶：" + snap.xiuLuck + " · " + snap.xiuShou,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 宜
            AlmanacYiJi(
                title = "宜",
                accent = Color(0xFF2E7D5B),
                items = snap.dayYi.take(10),
            )

            Spacer(Modifier.height(10.dp))

            // 忌
            AlmanacYiJi(
                title = "忌",
                accent = Color(0xFFB03A2E),
                items = snap.dayJi.take(10),
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "数据依据 寿星天文历 · 紫微排盘宝典",
                fontSize = 9.5.sp,
                color = Color(0xFF9C9284),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(52.dp))
        }

        // 退出提示
        Text(
            text = "轻触任意处返回",
            fontSize = 9.sp,
            color = Color(0xFFB0A695),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
        )
    }
}

@Composable
private fun PillarCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = ZiWeiSerifFamily,
            color = Color(0xFF9C9284),
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontFamily = ZiWeiSerifFamily,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A322A),
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun InfoRowCard(modifier: Modifier, label: String, value: String, sub: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFFFF))
            .border(0.7.dp, Color(0xFFE2D9C8), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = ZiWeiSerifFamily,
            color = Color(0xFF9C9284),
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = ZiWeiSerifFamily,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A322A),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = sub,
            fontSize = 9.sp,
            color = Color(0xFF8A8073),
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun AlmanacYiJi(title: String, accent: Color, items: List<String>) {    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFFFF))
            .border(0.7.dp, Color(0xFFE2D9C8), RoundedCornerShape(8.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(22.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = if (items.isEmpty()) "诸事不宜" else items.firstOrNull() ?: "",
                fontSize = 12.sp,
                fontFamily = ZiWeiSerifFamily,
                color = Color(0xFF3A322A),
            )
        }
        if (items.size > 1) {
            Spacer(Modifier.height(9.dp))
            Text(
                text = items.drop(1).joinToString("　"),
                fontSize = 11.sp,
                fontFamily = ZiWeiSerifFamily,
                color = Color(0xFF6B6257),
                lineHeight = 18.sp,
            )
        }
    }
}

private fun AlmanacSnapshot.chongSafe(): String = runCatching { "${ganZhiDay}日" }.getOrNull().orEmpty()
