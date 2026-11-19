package com.ziweistar.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.ziweistar.app.data.HomeState
import com.ziweistar.app.engine.QiEngine
import com.ziweistar.app.engine.model.CaveType
import com.ziweistar.app.engine.model.Fortune
import com.ziweistar.app.engine.model.PrivacyTier
import com.ziweistar.app.engine.model.RealmTheme
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.AlmanacCard
import com.ziweistar.app.ui.components.FortuneSeal
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.SectionHeader
import com.ziweistar.app.ui.components.ShakeDetector
import com.ziweistar.app.ui.components.StarAvatar
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiType

/**
 * 首页：紫薇皇历
 */
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onStartSession: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGlossary: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val state by vm.home.collectAsState()
    val oracle by vm.oracle.collectAsState()
    val loading by vm.oracleLoading.collectAsState()
    val revealed by vm.oracleRevealed.collectAsState()
    val released by vm.tianggangReleased.collectAsState()
    val bitmaps by vm.starBitmaps.collectAsState()
    val tianggangDoc by vm.tianggangDoc.collectAsState()
    val drawnToday by vm.oracleDrawnToday.collectAsState()
    var showSeal by remember { mutableStateOf(false) }

    LaunchedEffect(state?.almanac?.solarDate) {
        if (state != null) {
            vm.ensureOracle()
            vm.loadStarBitmaps()
        }
    }

    // 摇一摇求签（设计文档 3.2）：阈值 2.2g、1.2s 防抖；
    // 今日已求过签（revealed）时不再响应，避免误触覆盖已开签的灵签。
    ShakeDetector(
        enabled = state != null && !revealed && !loading,
        thresholdG = 2.2f,
    ) {
        haptics.medium()
        showSeal = true
        vm.shakeForOracle()
    }

    val s = state ?: run {
        Box(Modifier.fillMaxSize().background(ZiWeiColors.DeepSpace), Alignment.Center) {
            CircularProgressIndicator(color = ZiWeiColors.Gold)
        }
        return
    }

    val sealed = s.isTianggangSealed && !released
    val tier = runCatching { PrivacyTier.valueOf(s.profile.privacyTier) }
        .getOrDefault(PrivacyTier.LOCAL_ONLY)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0518), ZiWeiColors.DeepSpace, Color(0xFF160A28)),
                ),
            ),
    ) {
        StarfieldBackground(animated = true, starCount = 65, seed = 3)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(46.dp))

            // ---------------- 顶部状态栏 ----------------
            TopStatusBar(
                s = s,
                onOpenSettings = onOpenSettings,
                onOpenGlossary = onOpenGlossary,
            )

            Spacer(Modifier.height(12.dp))

            // ---------------- 紫微皇历入口 ----------------
            // 日历入口放在首页正文（不再占用右上角状态栏）
            CalendarEntryCard(
                solarDate = s.almanac.solarDate,
                lunarText = s.almanac.lunarText,
                onClick = onOpenCalendar,
            )

            Spacer(Modifier.height(12.dp))

            // ---------------- 万年历宜忌 ----------------
            // 用户反馈：紫薇皇历应当像万年历那样给出当日宜忌。
            // AlmanacSnapshot 里已有 dayYi / dayJi（寿星天文历提供），此前只在 BossKey 伪装页用到。
            YiJiBoard(s = s)

            Spacer(Modifier.height(14.dp))

            // ---------------- 求签 / 灵签 ----------------
            OracleCard(
                s = s,
                revealed = revealed,
                loading = loading,
                oracle = oracle,
                onDraw = {
                    haptics.medium()
                    showSeal = true
                    vm.drawNewOracle()
                },
                drawnToday = drawnToday,
            )

            Spacer(Modifier.height(14.dp))

            // ---------------- 思维锦囊 ----------------
            AnimatedVisibility(
                visible = revealed && oracle != null,
                enter = fadeIn(tween(500)) + expandVertically(tween(500)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
            ) {
                oracle?.let { g ->
                    val lock = g.lock
                    Column {
                        SectionHeader(
                            title = "思维锦囊",
                            subtitle = "四句签诗 · 一一只对应",
                        )
                        Spacer(Modifier.height(10.dp))

                        PouchCard(
                            index = "锦囊一",
                            title = "天时洞府",
                            quote = g.oracle.pouch.cave.quote,
                            labels = g.oracle.pouch.cave.labels,
                            note = g.oracle.pouch.cave.note,
                            accent = ZiWeiColors.Gold,
                        )
                        Spacer(Modifier.height(9.dp))
                        PouchCard(
                            index = "锦囊二",
                            title = "神通功法",
                            quote = g.oracle.pouch.method.quote,
                            labels = g.oracle.pouch.method.labels,
                            note = g.oracle.pouch.method.note,
                            accent = ZiWeiColors.Cinnabar,
                        )
                        Spacer(Modifier.height(9.dp))
                        PouchCard(
                            index = "锦囊三",
                            title = "结缘星宿",
                            quote = g.oracle.pouch.star.quote,
                            labels = g.oracle.pouch.star.labels,
                            note = g.oracle.pouch.star.note,
                            accent = ZiWeiColors.Jade,
                            avatar = lock.star.avatarPath?.let { path ->
                                bitmaps.entries.firstOrNull { it.key == lock.star.starId }?.value
                            },
                            avatarFallback = lock.star.name,
                        )
                        Spacer(Modifier.height(9.dp))
                        PouchCard(
                            index = "锦囊四",
                            title = "神游意境",
                            quote = g.oracle.pouch.realm.quote,
                            labels = g.oracle.pouch.realm.labels,
                            note = g.oracle.pouch.realm.note,
                            accent = Color(0xFF9B7BD4),
                        )

                        if (g.degraded) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "※ 天机直连超时，已降级至本地《钦天监六十四签古籍模板库》",
                                fontSize = 10.sp,
                                color = ZiWeiColors.TextTertiary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            if (sealed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "※ 今日已触发天罡戒律，玄铁封印 · 宜闭关养神",
                    fontSize = 11.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Cinnabar.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            // 底部留白：给固定浮层的核心按钮让出位置（按钮高 78dp + 呼吸边距）
            Spacer(Modifier.height(120.dp))
        }

        // ---------------- 核心按钮（固定浮层，永远浮于内容之上） ----------------
        // 用户反馈：「观紫微星」应该是最上层、浮于一切内容之上。
        // 之前它放在 verticalScroll 的 Column 里，滚动后会消失，
        // 现在改为 Box 的独立子节点 + Alignment.BottomCenter，滚动内容只在下面流动。
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF0C0518).copy(alpha = 0.75f),
                                Color(0xFF0C0518).copy(alpha = 0.95f),
                            ),
                        ),
                    )
                    .padding(top = 12.dp),
            ) {
                CoreButton(
                    sealed = sealed,
                    onClick = {
                        if (sealed) {
                            showSeal = true
                        } else {
                            haptics.heavy()
                            vm.beginSession()
                            onStartSession()
                        }
                    },
                )
            }
        }

        // ---------------- 天罡戒律黄符 ----------------
        if (sealed && showSeal) {
            TianggangOverlay(
                qi = s.qi,
                todayCount = s.todayCount,
                isMale = s.isMale,
                doc = tianggangDoc,
                onNeedDoc = { vm.loadTianggangDoc() },
                onRelease = {
                    haptics.seal()
                    vm.releaseTianggang()
                    showSeal = false
                },
                onRetreat = {
                    haptics.light()
                    vm.acceptRetreat()
                    showSeal = false
                },
                onDismiss = { showSeal = false },
            )
        }

        // 免责声明
        if (!sealed && showSeal && revealed) {
            // 求签提示自动消失
            LaunchedEffect(showSeal) {
                kotlinx.coroutines.delay(1200)
                showSeal = false
            }
        }
    }

    // ---------------- 天罡戒律：自动拦截 ----------------
    // 设计文档 5.2：单日 ≥3 次或元气 <20 时，黄符应主动弹出劝阻，
    // 而不是等用户去点「玄铁封印」按钮。用 autoSealShown 保证本次进入首页
    // 只自动弹一次（用户主动关掉后不再打扰，点按钮仍可重新打开）。
    var autoSealShown by remember { mutableStateOf(false) }
    LaunchedEffect(sealed, state != null) {
        if (state == null) return@LaunchedEffect
        if (sealed && !autoSealShown) {
            autoSealShown = true
            showSeal = true
        }
    }
}

// ============================================================
// 顶部状态栏
// ============================================================

@Composable
private fun CalendarEntryCard(
    solarDate: String,
    lunarText: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF1A0F30))
            .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
            .tapNoRipple { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "紫微皇历 · 万年历",
                fontSize = 12.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = ZiWeiColors.Gold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = solarDate.replace("-", " / ") + " · " + lunarText,
                fontSize = 10.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.TextTertiary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "查看皇历 ›",
            fontSize = 11.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.Jade,
        )
    }
}

@Composable
private fun TopStatusBar(
    s: HomeState,
    onOpenSettings: () -> Unit,
    onOpenGlossary: () -> Unit,
) {
    val a = s.almanac

    // 标题行
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "紫薇皇历",
                fontSize = 22.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = ZiWeiColors.Gold,
                letterSpacing = 5.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = a.solarDate.replace("-", " / "),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ZiWeiColors.TextTertiary,
                letterSpacing = 1.sp,
            )
        }
        IconTap("释") { onOpenGlossary() }
        Spacer(Modifier.width(9.dp))
        IconTap("设") { onOpenSettings() }
    }

    Spacer(Modifier.height(10.dp))

    // 干支 + 建除 + 星宿
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        MiniChip(a.ganZhiDay, ZiWeiColors.Gold)
        MiniChip("${a.zhiXing}日", ZiWeiColors.Jade)
        MiniChip(
            text = if (a.xiuAnimal.isNotBlank()) "${a.xiu}${a.xiuZheng}${a.xiuAnimal}值宿"
            else "${a.xiu}${a.xiuZheng}值宿",
            accent = Color(0xFF9B7BD4),
        )
        if (a.jieQi.isNotBlank()) {
            MiniChip(a.jieQi, ZiWeiColors.Cinnabar)
        }
    }

    Spacer(Modifier.height(6.dp))
    Text(
        text = a.lunarText + " · " + a.yueXiang,
        fontSize = 10.sp,
        fontFamily = ZiWeiSerifFamily,
        color = ZiWeiColors.TextTertiary,
        letterSpacing = 1.sp,
    )

    Spacer(Modifier.height(12.dp))

    // 元气槽 + 连击看板
    Row(Modifier.fillMaxWidth()) {
        QiGauge(
            modifier = Modifier.weight(1f),
            qi = s.qi,
        )
        Spacer(Modifier.width(12.dp))
        StreakToken(
            modifier = Modifier.width(116.dp),
            days = s.streakDays,
            maxStreak = s.maxStreak,
            todayCount = s.todayCount,
        )
    }
}

@Composable
private fun IconTap(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF1C1032))
            .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
            .tapNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, color = ZiWeiColors.Gold, fontFamily = ZiWeiSerifFamily)
    }
}

@Composable
private fun MiniChip(text: String, accent: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.13f))
            .border(0.7.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontFamily = ZiWeiSerifFamily,
            color = accent,
            letterSpacing = 0.5.sp,
        )
    }
}

// ============================================================
// 万年历宜忌（黄历核心传统条目）
// ============================================================

@Composable
private fun YiJiBoard(s: HomeState) {
    val a = s.almanac
    val yi = a.dayYi.filter { it.isNotBlank() }.take(8)
    val ji = a.dayJi.filter { it.isNotBlank() }.take(8)

    AlmanacCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ZiWeiColors.GoldDim.copy(alpha = 0.42f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        YiJiRow(
            title = "宜",
            accent = ZiWeiColors.Jade,
            items = yi,
            emptyText = "诸事不宜",
        )
        Spacer(Modifier.height(9.dp))
        GoldDivider(alpha = 0.18f)
        Spacer(Modifier.height(9.dp))
        YiJiRow(
            title = "忌",
            accent = ZiWeiColors.Cinnabar,
            items = ji,
            emptyText = "百无禁忌",
        )
    }
}

@Composable
private fun YiJiRow(
    title: String,
    accent: Color,
    items: List<String>,
    emptyText: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14091F),
            )
        }
        Spacer(Modifier.width(9.dp))
        // 一排展示，超出以「·」分隔自然折到第二行，避免每条一个胶囊把卡片撑高
        Text(
            text = if (items.isEmpty()) emptyText else items.joinToString("  ·  "),
            fontSize = 11.5.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.TextSecondary,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QiGauge(modifier: Modifier, qi: Int) {
    val animated by animateFloatAsState(
        targetValue = qi / 100f,
        animationSpec = tween(900),
        label = "qi",
    )
    val color = when {
        qi >= 70 -> ZiWeiColors.Jade
        qi >= 35 -> ZiWeiColors.Gold
        else -> ZiWeiColors.Cinnabar
    }

    Box(
        modifier
            .height(94.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF1A0F30))
            .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.4f), RoundedCornerShape(13.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val cx = size.width / 2
            // 圆心落在底边略上方，半径同时受宽度与高度约束；留出足够内边距，保证弧线与刻度不出框
            val cy = size.height - stroke * 1.4f
            val r = minOf(size.width * 0.36f, cy - stroke * 1.5f)

            // 轨道（下半环）
            drawArc(
                color = Color(0xFF2A1A48),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // 进度
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(color.copy(alpha = 0.5f), color, color.copy(alpha = 0.75f)),
                    center = Offset(cx, cy),
                ),
                startAngle = 180f,
                sweepAngle = 180f * animated.coerceIn(0.01f, 1f),
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // 刻度
            for (i in 0..10) {
                val ang = Math.toRadians((180 + i * 18).toDouble())
                val r1 = r - stroke * 1.4f
                val r2 = r - stroke * 0.6f
                drawLine(
                    color = Color(0x33E6C17A),
                    start = Offset(
                        cx + (r1 * Math.cos(ang)).toFloat(),
                        cy + (r1 * Math.sin(ang)).toFloat(),
                    ),
                    end = Offset(
                        cx + (r2 * Math.cos(ang)).toFloat(),
                        cy + (r2 * Math.sin(ang)).toFloat(),
                    ),
                    strokeWidth = 0.8.dp.toPx(),
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$qi",
                fontSize = 25.sp,
                lineHeight = 27.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = "先天元气 · ${QiEngine.qiLabel(qi)}",
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = ZiWeiColors.TextTertiary,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun StreakToken(
    modifier: Modifier,
    days: Int,
    maxStreak: Int,
    todayCount: Int,
) {
    Box(
        modifier
            .height(92.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A1544), Color(0xFF190D2E))),
            )
            .border(0.9.dp, ZiWeiColors.GoldDim.copy(alpha = 0.55f), RoundedCornerShape(13.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = ZiWeiColors.Gold.copy(alpha = 0.22f),
                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
                style = Stroke(0.6.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "连续观星",
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.TextTertiary,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Day",
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Gold.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 3.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "$days",
                    fontSize = 24.sp,
                    lineHeight = 27.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (days > 0) ZiWeiColors.Gold else ZiWeiColors.TextTertiary,
                )
            }
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (days == 0) "养精蓄锐" else "今日 $todayCount 次",
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = if (days == 0) ZiWeiColors.Jade else ZiWeiColors.TextTertiary,
            )
        }
    }
}

// ============================================================
// 灵签
// ============================================================

@Composable
private fun OracleCard(
    s: HomeState,
    revealed: Boolean,
    loading: Boolean,
    oracle: com.ziweistar.app.engine.GeneratedOracle?,
    onDraw: () -> Unit,
    drawnToday: Boolean = false,
) {
    val fortune = oracle?.let { g ->
        Fortune.entries.firstOrNull { g.oracle.fortuneLabel.contains(it.label) }
    }
    val sealColor = when (fortune) {
        Fortune.GREAT -> ZiWeiColors.Cinnabar
        Fortune.SMALL -> ZiWeiColors.Gold
        Fortune.NEUTRAL -> ZiWeiColors.Jade
        Fortune.OMINOUS -> ZiWeiColors.Ominous
        null -> ZiWeiColors.Gold
    }

    AlmanacCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (revealed) sealColor.copy(alpha = 0.6f)
        else ZiWeiColors.GoldDim.copy(alpha = 0.4f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        if (!revealed || oracle == null) {
            // 未求签态
            Column(
                Modifier
                    .fillMaxWidth()
                    .tapNoRipple { onDraw() }
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = ZiWeiColors.Gold,
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "钦天监推演天机中…",
                        fontSize = 13.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = ZiWeiColors.Gold.copy(alpha = 0.8f),
                        letterSpacing = 2.sp,
                    )
                } else {
                    Text(
                        text = "✦",
                        fontSize = 26.sp,
                        color = ZiWeiColors.Gold.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "求 取 今 日 灵 签",
                        fontSize = 17.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = ZiWeiColors.Gold,
                        letterSpacing = 5.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "轻触或摇晃手机 · 竹筒摇签",
                        fontSize = 10.sp,
                        color = ZiWeiColors.TextTertiary,
                        letterSpacing = 1.sp,
                    )
                }
            }
            return@AlmanacCard
        }

        val g = oracle
        // 已签态
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 四角朱印：上「大吉」下「宜观星」（取自【大吉 · 宜观星】）
            FortuneSeal(
                label = g.oracle.fortuneLabel
                    .substringAfter("·", "")
                    .substringBefore("】")
                    .trim(),
                short = g.oracle.fortuneShort,
                color = sealColor,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = g.oracle.fortuneLabel,
                    fontSize = 15.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = sealColor,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = g.lock.trigram,
                    fontSize = 11.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.TextSecondary,
                    letterSpacing = 1.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        GoldDivider()
        Spacer(Modifier.height(14.dp))

        // 七言签诗
        Column(Modifier.fillMaxWidth()) {
            g.oracle.poem.forEach { line ->
                Text(
                    text = line,
                    style = ZiWeiType.PoemLine,
                    color = ZiWeiColors.RicePaper,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        GoldDivider()
        Spacer(Modifier.height(12.dp))

        Text(
            text = "命理机缘解曰",
            fontSize = 11.sp,
            fontFamily = ZiWeiSerifFamily,
            color = ZiWeiColors.Gold.copy(alpha = 0.8f),
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = g.oracle.commentary,
            style = ZiWeiType.Commentary,
            color = ZiWeiColors.TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "※ 今日已开签 · 明日再来",
                fontSize = 10.sp,
                fontFamily = ZiWeiSerifFamily,
                color = ZiWeiColors.TextTertiary,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ============================================================
// 思维锦囊卡片
// ============================================================

@Composable
private fun PouchCard(
    index: String,
    title: String,
    quote: String,
    labels: List<String>,
    note: String,
    accent: Color,
    avatar: android.graphics.Bitmap? = null,
    avatarFallback: String = "",
) {
    AlmanacCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accent.copy(alpha = 0.4f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    text = index,
                    fontSize = 9.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = accent,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.weight(1f))
            if (avatar != null || avatarFallback.isNotBlank()) {
                StarAvatar(
                    size = 34.dp,
                    image = avatar,
                    fallbackText = avatarFallback,
                    ringColor = accent,
                    active = true,
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        // 引诗
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF100724))
                .padding(horizontal = 11.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "「",
                    fontSize = 13.sp,
                    color = accent.copy(alpha = 0.55f),
                    fontFamily = ZiWeiSerifFamily,
                )
                Text(
                    text = quote,
                    fontSize = 14.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.RicePaper.copy(alpha = 0.92f),
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "」",
                    fontSize = 13.sp,
                    color = accent.copy(alpha = 0.55f),
                    fontFamily = ZiWeiSerifFamily,
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        // 标签
        if (labels.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                labels.take(3).forEach { l ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = l,
                            fontSize = 10.sp,
                            color = accent.copy(alpha = 0.95f),
                            fontFamily = ZiWeiSerifFamily,
                        )
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
        }

        Text(
            text = note,
            fontSize = 11.5.sp,
            color = ZiWeiColors.TextSecondary,
            lineHeight = 19.sp,
        )
    }
}

// ============================================================
// 核心按钮
// ============================================================

@Composable
private fun CoreButton(sealed: Boolean, onClick: () -> Unit) {
    val accent = if (sealed) ZiWeiColors.SealedIron else ZiWeiColors.Cinnabar

    Box(
        Modifier
            .fillMaxWidth()
            .height(78.dp)
            .tapNoRipple(enabled = !sealed) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.height / 2 - 2.dp.toPx()

            // 外环
            drawCircle(
                color = accent.copy(alpha = 0.30f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(1.dp.toPx()),
            )
            // 内环
            drawCircle(
                color = ZiWeiColors.Gold.copy(alpha = 0.28f),
                radius = r * 0.86f,
                center = Offset(cx, cy),
                style = Stroke(0.8.dp.toPx()),
            )
            // 二十八宿刻度
            for (i in 0 until 28) {
                val ang = Math.toRadians(i * (360.0 / 28.0))
                val major = i % 7 == 0
                val r1 = r * if (major) 0.90f else 0.94f
                drawLine(
                    color = ZiWeiColors.Gold.copy(alpha = if (major) 0.55f else 0.25f),
                    start = Offset(
                        cx + (r1 * Math.cos(ang)).toFloat(),
                        cy + (r1 * Math.sin(ang)).toFloat(),
                    ),
                    end = Offset(
                        cx + (r * Math.cos(ang)).toFloat(),
                        cy + (r * Math.sin(ang)).toFloat(),
                    ),
                    strokeWidth = if (major) 1.2.dp.toPx() else 0.6.dp.toPx(),
                )
            }
        }

        Box(
            Modifier
                .width(210.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (sealed) {
                        Brush.horizontalGradient(listOf(Color(0xFF3A3540), Color(0xFF231F2B)))
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE0574F), Color(0xFFC23A32), Color(0xFFE0574F)),
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = if (sealed) Color(0xFF6B6472) else ZiWeiColors.Gold.copy(alpha = 0.8f),
                    style = Stroke(1.4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()),
                )
            }
            Text(
                text = if (sealed) "玄 铁 封 印" else "观 紫 薇 星",
                fontSize = 19.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = if (sealed) Color(0xFF8B8394) else Color(0xFFFFF3E0),
                letterSpacing = 7.sp,
            )
        }
    }
}

// ============================================================
// 天罡戒律黄符
// ============================================================

@Composable
private fun TianggangOverlay(
    qi: Int,
    todayCount: Int,
    isMale: Boolean,
    doc: com.ziweistar.app.engine.llm.ScenarioDoc? = null,
    onNeedDoc: () -> Unit = {},
    onRelease: () -> Unit,
    onRetreat: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHaptics.current
    var holdProgress by remember { mutableStateOf(0f) }
    var holding by remember { mutableStateOf(false) }

    // 黄符展开即向双擎流水线索要劝阻批文（LLM 失败自动落本地兜底）
    LaunchedEffect(Unit) { onNeedDoc() }

    LaunchedEffect(holding) {
        if (holding) {
            holdProgress = 0f
            while (holdProgress < 1f) {
                kotlinx.coroutines.delay(100)
                holdProgress += 0.1f / 1.5f // 15 秒满
                if (holdProgress >= 1f) {
                    onRelease()
                    return@LaunchedEffect
                }
            }
        } else {
            holdProgress = 0f
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE60B0614))
            .tapNoRipple { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 黄符
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF2E2B8), Color(0xFFE8D093), Color(0xFFDCC079)),
                        ),
                    )
                    .border(2.dp, Color(0xFFB8452F), RoundedCornerShape(6.dp))
                    .padding(horizontal = 18.dp, vertical = 22.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "敕 令",
                        fontSize = 13.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB8452F),
                        letterSpacing = 6.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "天 罡 戒 律",
                        fontSize = 26.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B2E1F),
                        letterSpacing = 8.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoldDivider(alpha = 0.0f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x558B2E1F)),
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = doc?.body?.ifBlank { null }
                            ?: ("今日已起坛 $todayCount 次，先天元气仅余 $qi。\n" +
                                "精为命根，气为命主。此等频次已越常度，\n" +
                                "强而行之，必损腰肾、暗耗元神。"),
                        fontSize = 13.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFF4A2A18),
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Center,
                    )

                    // 静心咒文（批文提供时展示，替代/补充原「默诵静心咒」提示）
                    doc?.extra?.takeIf { it.isNotBlank() }?.let { mantra ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "「$mantra」",
                            fontSize = 12.sp,
                            fontFamily = ZiWeiSerifFamily,
                            color = Color(0xFF8B2E1F),
                            lineHeight = 20.sp,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isMale) "乾造之身，宜蓄不宜泄。" else "坤造之身，宜养不宜耗。",
                        fontSize = 12.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFF8B2E1F),
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(16.dp))

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x558B2E1F)),
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "若执意起坛，需以双指长按此符 15 秒，\n" +
                            "默诵静心咒，方解此印。",
                        fontSize = 11.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = Color(0xFF6B4A2E),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 主按钮：谨遵法旨
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF4E9A8E), Color(0xFF3A7A70))),
                    )
                    .tapNoRipple {
                        haptics.seal()
                        onRetreat()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "谨 遵 法 旨 · 闭 关 养 神  (+5 元气)",
                    fontSize = 14.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEAF7F4),
                    letterSpacing = 1.5.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // 静心咒解锁
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (holding) Color(0xFF3A2410) else Color(0xFF1E1428),
                    )
                    .border(
                        1.dp,
                        if (holding) Color(0xFFD9A441) else Color(0x66D9A441),
                        RoundedCornerShape(12.dp),
                    )
                    .tapNoRipple {
                        holding = !holding
                        haptics.light()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    if (holdProgress > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0x66D9A441),
                                    Color(0x99D9A441),
                                ),
                            ),
                            size = Size(size.width * holdProgress, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        )
                    }
                }
                Text(
                    text = if (holding) {
                        "静心咒默诵中… ${(holdProgress * 100).toInt()}%"
                    } else {
                        "双指长按 · 诵静心咒解锁"
                    },
                    fontSize = 13.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = Color(0xFFE8C87A),
                    letterSpacing = 1.5.sp,
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "返回皇历",
                fontSize = 12.sp,
                color = ZiWeiColors.TextTertiary,
                modifier = Modifier
                    .tapNoRipple { onDismiss() }
                    .padding(10.dp),
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}
