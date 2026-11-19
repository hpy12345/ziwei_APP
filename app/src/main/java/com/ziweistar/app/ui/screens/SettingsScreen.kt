package com.ziweistar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.engine.model.PrivacyTier
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.components.AlmanacCard
import com.ziweistar.app.ui.components.GoldDivider
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.components.tapNoRipple
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors

/**
 * 天机阁（设置）
 *
 * 生物识别、LLM 通路、隐私授权、焚毁天机。
 * （原「伪装之术」按用户要求移除：换图标并不能真正隐藏使用痕迹，反而增加维护成本。）
 */
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenGlossary: () -> Unit,
    onBurnt: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val home by vm.home.collectAsState()
    val profile by vm.profile.collectAsState()

    var showBurnConfirm by remember { mutableStateOf(false) }
    var showLlmDialog by remember { mutableStateOf(false) }

    val currentTier = runCatching {
        PrivacyTier.valueOf(profile?.privacyTier ?: PrivacyTier.FULL.name)
    }.getOrDefault(PrivacyTier.FULL)

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground(animated = false, starCount = 60, seed = 47)

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
                    text = "天 机 阁",
                    fontSize = 15.sp,
                    fontFamily = ZiWeiSerifFamily,
                    fontWeight = FontWeight.Bold,
                    color = ZiWeiColors.Gold,
                    letterSpacing = 5.sp,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(20.dp))

            // ===== 隐私授权 =====
            SettingsCard(title = "天 机 盟 约", accent = ZiWeiColors.Jade) {
                Text(
                    text = "此档决定「观星」时，批文由谁推演。可随时改档。",
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PrivacyTier.entries.forEach { tier ->
                        val active = currentTier == tier
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (active) ZiWeiColors.Jade.copy(alpha = 0.13f)
                                    else Color(0xFF1B1030),
                                )
                                .border(
                                    0.8.dp,
                                    if (active) ZiWeiColors.Jade.copy(alpha = 0.8f)
                                    else ZiWeiColors.GoldDim.copy(alpha = 0.25f),
                                    RoundedCornerShape(9.dp),
                                )
                                .tapNoRipple {
                                    haptics.light()
                                    vm.updatePrivacyTier(tier)
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                        ) {
                            Column {
                                Text(
                                    text = tier.label,
                                    fontSize = 12.sp,
                                    fontFamily = ZiWeiSerifFamily,
                                    color = if (active) ZiWeiColors.Jade else ZiWeiColors.TextSecondary,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = tier.desc,
                                    fontSize = 9.sp,
                                    color = ZiWeiColors.TextTertiary,
                                    lineHeight = 13.sp,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== 生物识别 =====
            SettingsCard(title = "生 物 识 别 锁", accent = ZiWeiColors.Jade) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "开启后每次启动需指纹 / 面容验证",
                            fontSize = 11.sp,
                            color = ZiWeiColors.TextSecondary,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "「暂无指纹」时自动落到设备锁屏密码",
                            fontSize = 9.sp,
                            color = ZiWeiColors.TextTertiary,
                        )
                    }
                    Switch(
                        checked = profile?.biometricLock == true,
                        onCheckedChange = { on ->
                            haptics.medium()
                            vm.updateBiometric(on)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZiWeiColors.Jade,
                            checkedTrackColor = ZiWeiColors.Jade.copy(alpha = 0.35f),
                            uncheckedThumbColor = ZiWeiColors.TextTertiary,
                            uncheckedTrackColor = Color(0xFF2A1C40),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== 推演引擎 =====
            SettingsCard(title = "远 程 推 演 引 擎", accent = Color(0xFF9B7BD4)) {
                Text(
                    text = "留空则全部在本机推演，不联网。填入服务地址后，交由远程推演按定局润色诗文。",
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(11.dp))
                val configured = !profile?.llmEndpoint.isNullOrBlank()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (configured) ZiWeiColors.Jade else ZiWeiColors.TextTertiary),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (configured) {
                            "已接通 · ${profile?.llmModel?.ifBlank { "默认模型" }}"
                        } else {
                            "未接通 · 纯本地模式"
                        },
                        fontSize = 11.sp,
                        color = if (configured) ZiWeiColors.Jade else ZiWeiColors.TextTertiary,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ZiWeiColors.Gold.copy(alpha = 0.14f))
                            .border(0.8.dp, ZiWeiColors.Gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .tapNoRipple {
                                haptics.light()
                                showLlmDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "配置",
                            fontSize = 11.sp,
                            fontFamily = ZiWeiSerifFamily,
                            color = ZiWeiColors.Gold,
                        )
                    }
                }
                if (configured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = profile?.llmEndpoint ?: "",
                        fontSize = 9.sp,
                        color = ZiWeiColors.TextTertiary,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== 焚毁天机 =====
            SettingsCard(
                title = "焚 毁 天 机",
                accent = ZiWeiColors.Cinnabar,
                borderColor = ZiWeiColors.Cinnabar.copy(alpha = 0.45f),
            ) {
                Text(
                    text = "抹除本机全部记录：历次归档、星宿图鉴照片、以及解锁所需的密钥。此操作不可撤销，亦无法找回。",
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(12.dp))
                var holdProgress by remember { mutableFloatStateOf(0f) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF3A1212),
                                    Color(0xFF5A1A1A).copy(
                                        alpha = 0.4f + 0.6f * holdProgress,
                                    ),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            ZiWeiColors.Cinnabar.copy(alpha = 0.6f + 0.4f * holdProgress),
                            RoundedCornerShape(25.dp),
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    holdProgress = 0f
                                    val start = System.currentTimeMillis()
                                    var cancelled = false
                                    try {
                                        while (System.currentTimeMillis() - start < 5000) {
                                            holdProgress = ((System.currentTimeMillis() - start) / 5000f)
                                                .coerceIn(0f, 1f)
                                            kotlinx.coroutines.delay(40)
                                        }
                                        if (!cancelled) {
                                            holdProgress = 1f
                                            showBurnConfirm = true
                                        }
                                    } finally {
                                        cancelled = true
                                        holdProgress = 0f
                                    }
                                    tryAwaitRelease()
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (holdProgress > 0.02f) {
                            "继续长按 ${"%.1f".format((1f - holdProgress) * 5)} 秒…"
                        } else {
                            "长 按 五 秒 · 焚 毁 天 机"
                        },
                        fontSize = 12.5.sp,
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        color = ZiWeiColors.Cinnabar,
                        letterSpacing = 2.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            GoldDivider(alpha = 0.22f)

            Spacer(Modifier.height(14.dp))

            Text(
                text = "紫薇星 ${"2.0.0"} · 钦天监内丹录\n" +
                    "本应用所有生理数值与图像都只存在这台手机上，绝不联网上传。",
                fontSize = 9.sp,
                color = ZiWeiColors.TextTertiary,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    // 焚毁确认
    if (showBurnConfirm) {
        AlertDialog(
            onDismissRequest = { showBurnConfirm = false },
            containerColor = ZiWeiColors.XuanMing,
            title = {
                Text(
                    text = "焚毁天机？",
                    fontFamily = ZiWeiSerifFamily,
                    fontSize = 17.sp,
                    color = ZiWeiColors.Cinnabar,
                    letterSpacing = 2.sp,
                )
            },
            text = {
                Text(
                    text = "将永久抹除全部观星记录、星宿图鉴、密钥与配置。此操作不可撤销。",
                    fontSize = 12.sp,
                    color = ZiWeiColors.TextSecondary,
                    lineHeight = 19.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBurnConfirm = false
                        haptics.seal()
                        vm.burnEverything { onBurnt() }
                    },
                ) {
                    Text("确认焚毁", color = ZiWeiColors.Cinnabar, fontFamily = ZiWeiSerifFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBurnConfirm = false }) {
                    Text("取消", color = ZiWeiColors.TextTertiary)
                }
            },
        )
    }

    // LLM 配置
    if (showLlmDialog) {
        LlmConfigDialog(
            initialEndpoint = profile?.llmEndpoint ?: "",
            initialApiKey = profile?.llmApiKey ?: "",
            initialModel = profile?.llmModel ?: "",
            onDismiss = { showLlmDialog = false },
            onConfirm = { e, k, m ->
                vm.updateLlm(e, k, m)
                showLlmDialog = false
            },
        )
    }
}

// ============================================================
// 组件
// ============================================================

@Composable
private fun SettingsCard(
    title: String,
    accent: Color,
    borderColor: Color = ZiWeiColors.GoldDim.copy(alpha = 0.28f),
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1A1E1136))
            .border(0.7.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(11.dp))
        content()
    }
}

@Composable
private fun LlmConfigDialog(
    initialEndpoint: String,
    initialApiKey: String,
    initialModel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var endpoint by remember { mutableStateOf(initialEndpoint) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel) }
    var reveal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZiWeiColors.XuanMing,
        title = {
            Text(
                text = "远程推演引擎",
                fontFamily = ZiWeiSerifFamily,
                fontSize = 16.sp,
                color = ZiWeiColors.Gold,
                letterSpacing = 2.sp,
            )
        },
        text = {
            Column {
                Text(
                    text = "填入任意兼容的推演服务地址即可。留空则纯本地推演。",
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(12.dp))
                DialogField("服务地址", endpoint, "https://api.xxx.com/v1") { endpoint = it }
                Spacer(Modifier.height(10.dp))
                DialogField("模型名", model, "deepseek-chat") { model = it }
                Spacer(Modifier.height(10.dp))
                DialogField(
                    label = "密钥",
                    value = apiKey,
                    placeholder = "sk-...",
                    password = !reveal,
                    trailing = if (reveal) "隐" else "显",
                    onTrailing = { reveal = !reveal },
                ) { apiKey = it }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "※ 密钥只存在这台手机上，不会同步到云端。",
                    fontSize = 9.sp,
                    color = ZiWeiColors.TextTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(endpoint.trim(), apiKey.trim(), model.trim()) }) {
                Text("保存", color = ZiWeiColors.Gold, fontFamily = ZiWeiSerifFamily)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    endpoint = ""
                    apiKey = ""
                    model = ""
                    onConfirm("", "", "")
                },
            ) {
                Text("清空并断开", color = ZiWeiColors.Cinnabar.copy(alpha = 0.85f), fontSize = 11.sp)
            }
        },
    )
}

@Composable
private fun DialogField(
    label: String,
    value: String,
    placeholder: String,
    password: Boolean = false,
    trailing: String? = null,
    onTrailing: () -> Unit = {},
    onChange: (String) -> Unit,
) {
    Column {
        Text(text = label, fontSize = 9.5.sp, color = ZiWeiColors.Jade)
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1B1030))
                .border(0.8.dp, ZiWeiColors.GoldDim.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 11.sp, color = ZiWeiColors.TextTertiary)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    textStyle = TextStyle(
                        color = ZiWeiColors.Gold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    cursorBrush = SolidColor(ZiWeiColors.Gold),
                    singleLine = true,
                    visualTransformation = if (password) PasswordVisualTransformation()
                    else androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    fontSize = 10.sp,
                    color = ZiWeiColors.TextTertiary,
                    modifier = Modifier
                        .tapNoRipple { onTrailing() }
                        .padding(start = 8.dp),
                )
            }
        }
    }
}
