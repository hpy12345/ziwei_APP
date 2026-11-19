package com.ziweistar.app.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.model.AdverseEffect
import com.ziweistar.app.engine.model.ClimaxFemale
import com.ziweistar.app.engine.model.ClimaxMale
import com.ziweistar.app.engine.model.MediumKind
import com.ziweistar.app.engine.model.Method
import com.ziweistar.app.engine.model.MethodLibrary
import com.ziweistar.app.engine.model.MethodSect
import com.ziweistar.app.engine.model.StarAttribute
import com.ziweistar.app.engine.model.VenueKind
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.SectionHeader
import com.ziweistar.app.ui.components.StarAvatar
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiType

/**
 * 收功：多维归档结算抽屉
 *
 * 设计文档 3.3-B：7 项结构化归档表单
 */
@Composable
fun ArchiveScreen(
    vm: MainViewModel,
    startedAt: Long,
    endedAt: Long,
    durationSeconds: Int,
    pausedSeconds: Int,
    onSaved: (MainViewModel.ArchiveResult) -> Unit,
    onCancel: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val home by vm.home.collectAsState()
    val draft by vm.archive.collectAsState()
    val bitmaps by vm.starBitmaps.collectAsState()
    val oracle by vm.oracle.collectAsState()

    var showNewStar by remember { mutableStateOf(false) }
    var showBurnConfirm by remember { mutableStateOf(false) }

    val isMale = home?.isMale ?: true
    val methods = MethodLibrary.forGender(isMale)

    Box(
        Modifier
            .fillMaxSize()
            // 完全不透明：下方计时页的巨号数字即使只剩 3/255 的透光量也会隐约透出，
            // 与归档表单文字叠在一起。抽屉之上的星野由下方 StarfieldBackground 提供。
            .background(Color(0xFF0B0614)),
    ) {
        StarfieldBackground(animated = false, starCount = 40, seed = 91)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(34.dp))

            // 顶部：净耗时
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF2A1544), Color(0xFF180C2C))),
                    )
                    .border(1.dp, ZiWeiColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "收 功 · 多维归档结算",
                        fontSize = 15.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = ZiWeiColors.Gold,
                        letterSpacing = 4.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = fmtDuration(durationSeconds),
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        color = ZiWeiColors.RicePaper,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "本次净耗时" + if (pausedSeconds > 0) "（已扣除暂停 $pausedSeconds 秒）" else "",
                        fontSize = 10.sp,
                        color = ZiWeiColors.TextTertiary,
                    )

                    if (oracle != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(ZiWeiColors.Gold.copy(alpha = 0.16f))
                                .border(0.9.dp, ZiWeiColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                                .tapNoRipple {
                                    haptics.light()
                                    vm.prefillFromOracle()
                                }
                                .padding(horizontal = 15.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "✨ 依今日灵签一键填入",
                                fontSize = 12.sp,
                                fontFamily = ZiWeiSerifFamily,
                                color = ZiWeiColors.Gold,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 1. 所习功法
            FormSection(index = "一", title = "所习功法（可多选）") {
                MethodSect.entries.forEach { sect ->
                    val group = methods.filter { it.sect == sect }
                    if (group.isEmpty()) return@forEach
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${sect.label} · ${sect.desc}",
                        fontSize = 11.sp,
                        fontFamily = ZiWeiSerifFamily,
                        color = ZiWeiColors.Jade.copy(alpha = 0.85f),
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowTags(
                        items = group.map { it.name to it.name },
                        selected = draft.methodNames.toSet(),
                        accent = ZiWeiColors.Gold,
                    ) { name ->
                        haptics.tick()
                        vm.updateArchive { d ->
                            val s = d.methodNames.toMutableList()
                            if (s.contains(name)) s.remove(name) else s.add(name)
                            d.copy(methodNames = s)
                        }
                    }
                }
                // 选中功法副标题
                val sel = draft.methodNames.mapNotNull { MethodLibrary.byName(it) }
                if (sel.isNotEmpty()) {
                    Spacer(Modifier.height(9.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF100724))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        sel.forEach { m ->
                            Text(
                                text = "· ${m.name}：${m.subtitle}  [M=${"%.1f".format(m.coefficient)}]",
                                fontSize = 9.5.sp,
                                color = ZiWeiColors.TextTertiary,
                                lineHeight = 14.sp,
                            )
                        }
                        if (sel.any { it.highRisk }) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "⚠ 含高危功法，请注意频次",
                                fontSize = 9.5.sp,
                                color = ZiWeiColors.Cinnabar.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2. 结缘星宿
            FormSection(index = "二", title = "结缘星宿（幻想对象）") {
                val stars = home?.starFigures ?: emptyList()
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.tapNoRipple {
                                haptics.light()
                                vm.updateArchive { it.copy(starId = null, starName = "无相神游") }
                            },
                        ) {
                            StarAvatar(
                                size = 56.dp,
                                image = null,
                                fallbackText = "无",
                                ringColor = ZiWeiColors.TextTertiary,
                                active = draft.starName.isBlank() || draft.starName == "无相神游",
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "无相神游",
                                fontSize = 9.sp,
                                color = ZiWeiColors.TextTertiary,
                            )
                        }
                    }
                    items(stars.size) { i ->
                        val s = stars[i]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.tapNoRipple {
                                haptics.medium()
                                vm.updateArchive {
                                    it.copy(
                                        starId = s.id,
                                        starName = s.name,
                                        starAttribute = s.attribute,
                                    )
                                }
                            },
                        ) {
                            StarAvatar(
                                size = 56.dp,
                                image = bitmaps[s.id],
                                fallbackText = s.name,
                                ringColor = ZiWeiColors.Gold,
                                active = draft.starId == s.id,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = s.name,
                                fontSize = 9.sp,
                                color = if (draft.starId == s.id) ZiWeiColors.Gold
                                else ZiWeiColors.TextTertiary,
                                maxLines = 1,
                            )
                        }
                    }
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.tapNoRipple {
                                haptics.light()
                                showNewStar = true
                            },
                        ) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1B1030))
                                    .border(
                                        0.9.dp,
                                        ZiWeiColors.GoldDim.copy(alpha = 0.6f),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", fontSize = 22.sp, color = ZiWeiColors.Gold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("册封新星宿", fontSize = 9.sp, color = ZiWeiColors.TextTertiary)
                        }
                    }
                }

                if (draft.starName.isNotBlank() && draft.starName != "无相神游") {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "已选：${draft.starName}",
                            fontSize = 10.5.sp,
                            color = ZiWeiColors.Gold,
                            fontFamily = ZiWeiSerifFamily,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ZiWeiColors.Jade.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = draft.starAttribute.label,
                                fontSize = 9.sp,
                                color = ZiWeiColors.Jade,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. 所借灵媒
            FormSection(index = "三", title = "所借灵媒（可多选）") {
                FlowTags(
                    items = MediumKind.entries.map { it.name to it.classicLabel },
                    selected = draft.mediums.toSet(),
                    accent = Color(0xFF9B7BD4),
                ) { name ->
                    haptics.tick()
                    vm.updateArchive { d ->
                        val s = d.mediums.toMutableList()
                        if (s.contains(name)) s.remove(name) else s.add(name)
                        d.copy(mediums = s)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4. 所处道场
            FormSection(index = "四", title = "所处道场（单选）") {
                FlowTags(
                    items = VenueKind.entries.map { it.label to it.classicLabel },
                    selected = setOf(draft.venue),
                    accent = ZiWeiColors.Jade,
                    single = true,
                ) { label ->
                    haptics.tick()
                    vm.updateArchive { it.copy(venue = label) }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 5. 登顶造化
            FormSection(index = "五", title = "登顶造化") {
                val options = if (isMale) {
                    ClimaxMale.entries.map { it.name to it.classicLabel }
                } else {
                    ClimaxFemale.entries.map { it.name to it.classicLabel }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    options.forEach { (name, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (draft.climax == name) ZiWeiColors.Cinnabar.copy(alpha = 0.14f)
                                    else Color(0xFF1B1030),
                                )
                                .border(
                                    0.8.dp,
                                    if (draft.climax == name) ZiWeiColors.Cinnabar.copy(alpha = 0.85f)
                                    else ZiWeiColors.GoldDim.copy(alpha = 0.22f),
                                    RoundedCornerShape(9.dp),
                                )
                                .tapNoRipple {
                                    haptics.light()
                                    vm.updateArchive { it.copy(climax = name) }
                                }
                                .padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                fontFamily = ZiWeiSerifFamily,
                                color = if (draft.climax == name) ZiWeiColors.Cinnabar
                                else ZiWeiColors.TextSecondary,
                                fontWeight = if (draft.climax == name) FontWeight.Bold
                                else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 6. 道心圆满度
            FormSection(index = "六", title = "道心圆满度（半星精度）") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    for (i in 1..5) {
                        val filled = draft.satisfaction >= i - 0.25f
                        val half = !filled && draft.satisfaction >= i - 0.75f
                        StarTap(filled = filled, half = half) {
                            haptics.tick()
                            // 点击切半星：整星位置再点 → 半星
                            val target = if (draft.satisfaction == i.toFloat()) i - 0.5f
                            else i.toFloat()
                            vm.updateArchive { it.copy(satisfaction = target.coerceIn(0.5f, 5f)) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${"%.1f".format(draft.satisfaction)} 星 · " + satisfactionLabel(draft.satisfaction),
                    fontSize = 12.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Gold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 7. 走火入魔录
            FormSection(index = "七", title = "走火入魔录（不良反应）") {
                FlowTags(
                    items = AdverseEffect.entries.map { it.name to it.classicLabel },
                    selected = draft.adverse.map { it.name }.toSet(),
                    accent = ZiWeiColors.Cinnabar,
                ) { name ->
                    haptics.tick()
                    val effect = AdverseEffect.valueOf(name)
                    vm.updateArchive { d ->
                        val s = d.adverse.toMutableSet()
                        if (effect == AdverseEffect.NONE) {
                            // 互斥：选中「气息平稳」清空所有不良项
                            if (s.contains(AdverseEffect.NONE)) {
                                s.clear()
                            } else {
                                s.clear()
                                s.add(AdverseEffect.NONE)
                            }
                        } else {
                            s.remove(AdverseEffect.NONE)
                            if (s.contains(effect)) s.remove(effect) else s.add(effect)
                            if (s.isEmpty()) s.add(AdverseEffect.NONE)
                        }
                        d.copy(adverse = s)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // 保存
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE6C17A), Color(0xFFC9A45C), Color(0xFFE6C17A)),
                        ),
                    )
                    .tapNoRipple {
                        haptics.seal()
                        vm.saveArchive(startedAt, endedAt, durationSeconds, pausedSeconds) {
                            onSaved(it)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "盖 印 归 档",
                    fontSize = 17.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C221E),
                    letterSpacing = 7.sp,
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "舍弃本次记录",
                fontSize = 12.sp,
                color = ZiWeiColors.TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .tapNoRipple { onCancel() }
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showNewStar) {
        NewStarDialog(
            onDismiss = { showNewStar = false },
            onConfirm = { name, attr, uri ->
                vm.addStar(name, attr, uri) {
                    // 新建后自动选中
                    val newStar = vm.home.value?.starFigures?.lastOrNull()
                    if (newStar != null) {
                        vm.updateArchive {
                            it.copy(starId = newStar.id, starName = newStar.name, starAttribute = newStar.attribute)
                        }
                    }
                    showNewStar = false
                }
            },
        )
    }
}

// ============================================================
// 表单组件
// ============================================================

@Composable
private fun FormSection(
    index: String,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1A1E1136))
            .border(
                0.7.dp,
                ZiWeiColors.GoldDim.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp),
            )
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ZiWeiColors.Cinnabar.copy(alpha = 0.22f))
                    .border(0.8.dp, ZiWeiColors.Cinnabar.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index,
                    fontSize = 10.sp,
                    lineHeight = 22.sp,
                    fontFamily = ZiWeiSerifFamily,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = ZiWeiColors.Cinnabar,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = ZiWeiColors.Gold,
                letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowTags(
    items: List<Pair<String, String>>,
    selected: Set<String>,
    accent: Color,
    single: Boolean = false,
    onToggle: (String) -> Unit,
) {
    // 每个标签按自身文字宽度排布、单行不换行，一行放不下才折到下一行
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items.forEach { item ->
            TagBox(
                text = item.second,
                active = selected.contains(item.first),
                accent = accent,
            ) { onToggle(item.first) }
        }
    }
}

@Composable
private fun TagBox(
    modifier: Modifier = Modifier,
    text: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) accent.copy(alpha = 0.16f) else Color(0xFF1B1030))
            .border(
                0.8.dp,
                if (active) accent.copy(alpha = 0.9f) else ZiWeiColors.GoldDim.copy(alpha = 0.22f),
                RoundedCornerShape(8.dp),
            )
            .tapNoRipple { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontFamily = ZiWeiSerifFamily,
            color = if (active) accent else ZiWeiColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun StarTap(filled: Boolean, half: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .tapNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(34.dp)) {
            drawStar5(filled = filled, half = half)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar5(filled: Boolean, half: Boolean) {
    val cx = size.width / 2
    val cy = size.height / 2
    val rOut = size.minDimension / 2
    val rIn = rOut * 0.42f
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) rOut else rIn
        val ang = Math.toRadians(-90.0 + i * 36.0)
        val x = cx + (r * Math.cos(ang)).toFloat()
        val y = cy + (r * Math.sin(ang)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    if (half) {
        // 左半填充
        drawPath(path, color = ZiWeiColors.Gold)
        drawRect(color = Color(0xFF1B1030).copy(alpha = 0.85f), size = androidx.compose.ui.geometry.Size(size.width / 2, size.height), topLeft = Offset(size.width / 2, 0f))
        drawPath(path, color = ZiWeiColors.Gold, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    } else if (filled) {
        drawPath(path, color = ZiWeiColors.Gold)
    } else {
        drawPath(path, color = ZiWeiColors.Gold.copy(alpha = 0.35f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
    }
}

private fun satisfactionLabel(v: Float): String = when {
    v >= 5f -> "羽化登仙"
    v >= 4.5f -> "极乐圆满"
    v >= 4f -> "身心舒畅"
    v >= 3.5f -> "尚可称意"
    v >= 3f -> "平淡无奇"
    v >= 2f -> "略有失落"
    else -> "索然无味"
}

private fun fmtDuration(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

// ============================================================
// 册封新星宿
// ============================================================

@Composable
private fun NewStarDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, attribute: StarAttribute, uri: Uri?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var attr by remember { mutableStateOf(StarAttribute.PURE_LOVE) }
    var uri by remember { mutableStateOf<Uri?>(null) }

    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { u -> uri = u }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZiWeiColors.XuanMing,
        title = {
            Text(
                text = "册封新星宿",
                fontFamily = ZiWeiSerifFamily,
                fontSize = 16.sp,
                color = ZiWeiColors.Gold,
                letterSpacing = 2.sp,
            )
        },
        text = {
            Column {
                Text(
                    text = "图像只保存在这台手机上，绝不上传服务器。",
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B1030))
                            .border(0.9.dp, ZiWeiColors.GoldDim.copy(alpha = 0.6f), CircleShape)
                            .tapNoRipple { picker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (uri == null) "上传" else "已选",
                            fontSize = 9.sp,
                            color = ZiWeiColors.Gold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B1030))
                            .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        BasicTextField(
                            value = name,
                            onValueChange = { if (it.length <= 12) name = it },
                            textStyle = TextStyle(
                                color = ZiWeiColors.Gold,
                                fontSize = 13.sp,
                                fontFamily = ZiWeiSerifFamily,
                            ),
                            cursorBrush = SolidColor(ZiWeiColors.Gold),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (name.isEmpty()) {
                                    Text(
                                        text = "名称（如：白月光）",
                                        fontSize = 12.sp,
                                        color = ZiWeiColors.TextTertiary,
                                    )
                                }
                                inner()
                            },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "必选属性标签",
                    fontSize = 11.sp,
                    fontFamily = ZiWeiSerifFamily,
                    color = ZiWeiColors.Jade,
                )
                Spacer(Modifier.height(7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StarAttribute.entries.forEach { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (attr == a) ZiWeiColors.Gold.copy(alpha = 0.14f)
                                    else Color(0xFF1B1030),
                                )
                                .tapNoRipple { attr = a }
                                .padding(horizontal = 11.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = a.label,
                                fontSize = 12.sp,
                                fontFamily = ZiWeiSerifFamily,
                                color = if (attr == a) ZiWeiColors.Gold else ZiWeiColors.TextSecondary,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (a.woodFireAligned) "木火日契合" else "土金水日契合",
                                fontSize = 8.5.sp,
                                color = ZiWeiColors.TextTertiary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), attr, uri) },
            ) {
                Text("册封", color = ZiWeiColors.Gold, fontFamily = ZiWeiSerifFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = ZiWeiColors.TextTertiary)
            }
        },
    )
}
