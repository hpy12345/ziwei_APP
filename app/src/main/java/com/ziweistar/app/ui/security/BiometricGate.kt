package com.ziweistar.app.ui.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.ui.components.StarfieldBackground
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 天机加锁：生物识别门禁。
 *
 * 设计文档 1.1 / 7：开启「生物识别锁」后，每次观星前需指纹 / 面容验证；
 * 「暂无指纹」时自动落到设备锁屏密码（DEVICE_CREDENTIAL 兜底）。
 *
 * 三条铁律（既防社死，也防把自己锁死在门外）：
 *  1. enabled == false → 立刻放行，不闪任何多余界面；
 *  2. 设备没有可用认证方式（未录指纹 / 无硬件 / 硬件不可用 / 宿主不是 FragmentActivity）→ 立刻放行；
 *  3. 认证失败只**停在门禁页**展示错误 + 「重 试」，绝不自动循环弹窗。
 *
 * ⚠️ 宿主前提：androidx.biometric:biometric:1.1.0 的 BiometricPrompt **只接受 FragmentActivity**，
 * 没有 Activity 版本的重载（已用 javap 核对 1.1.0 aar，仅有 FragmentActivity / Fragment 两套构造器）。
 * 本工程 MainActivity.kt:44 已是 `class MainActivity : FragmentActivity()`，门禁可正常弹窗。
 * 但 BaseAliasActivity（AliasActivities.kt:12）仍是 ComponentActivity，若把它当作宿主的 Context 传入，
 * findFragmentActivity() 会返回 null 并按第 2 条放行——不崩，但不会真的验证。
 */
@Composable
fun BiometricGate(
    enabled: Boolean,
    title: String = "天 机 加 锁",
    subtitle: String = "请以指纹 / 面容验证，方可观星",
    onPass: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current

    // BiometricPrompt 1.1.0 只能挂在 FragmentActivity 上；拿不到就按「不可用」处理（见下方 LaunchedEffect）
    val activity = remember(context) { context.findFragmentActivity() }

    // 始终保持最新的回调：父级 lambda 每次重组都会换实例，不能用旧闭包
    val latestOnPass = rememberUpdatedState(onPass)

    // 放行一次性锁。用普通 State 对象并在回调里显式读 .value，
    // 避免闭包捕获旧的 Boolean 导致 onPass 被重复调用。
    val passLock = remember { mutableStateOf(false) }

    // 门禁页状态（显式持有 MutableState 对象：回调里直接写 .value，避免闭包捕获过期的委托属性）
    val errorState = remember { mutableStateOf<String?>(null) } // 非空 = 停在门禁页，展示错误 + 「重 试」
    val promptingState = remember { mutableStateOf(false) }     // 系统弹窗是否进行中（防重复唤起）
    val retryTickState = remember { mutableStateOf(0) }         // 手动重试信号：自增 = 重新弹一次

    // 唯一放行出口（幂等）
    val passOnce: () -> Unit = {
        if (!passLock.value) {
            passLock.value = true
            latestOnPass.value()
        }
    }

    // PromptInfo 与文案绑定，文案变了才重建。
    // 定制 ROM 可能在 build() 阶段拒绝该认证组合，这里兜住异常并当作「不可用」放行。
    val promptInfo: BiometricPrompt.PromptInfo? = remember(title, subtitle) {
        runCatching {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                // 只用 WEAK + DEVICE_CREDENTIAL：
                // biometric 1.1.0 下 BIOMETRIC_STRONG 与 DEVICE_CREDENTIAL 组合会在部分机型抛异常。
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                // 注意：允许 DEVICE_CREDENTIAL 时**不能**再调 setNegativeButtonText，
                // 否则 Builder.build() 会抛 IllegalArgumentException（"Can't have both ..."）。
                .build()
        }.getOrNull()
    }

    // 成功 / 失败处理每次重组都刷新，供下面单例 prompt 的回调读取（避免捕获旧状态）
    val onAuthSuccess = rememberUpdatedState<() -> Unit> {
        promptingState.value = false
        haptics.light()
        passOnce()
    }
    val onAuthError = rememberUpdatedState<(CharSequence) -> Unit> { message ->
        // 不自动重弹：退回门禁页展示错误 + 「重 试」，等用户主动点击
        promptingState.value = false
        haptics.tick()
        errorState.value = friendlyError(message)
    }

    // 每个 Activity 只建一个 BiometricPrompt：
    // 1.1.0 内部靠 Fragment 承载弹窗，反复 new 会在 FragmentManager 里堆实例，
    // 因此「重 试」复用同一个 prompt，而不是每次重新构造。
    val prompt: BiometricPrompt? = remember(activity) {
        activity?.let { host ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthSuccess.value()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户取消 / 指纹锁死 / 硬件异常都走这里；由调用方决定文案，绝不自动重弹
                    onAuthError.value(errString)
                }

                // onAuthenticationFailed（单次指纹不匹配）不覆写：
                // 系统弹窗会让用户就地重按，不应结束流程。
            }
            // 回调必须在主线程：统一走主线程 Executor
            BiometricPrompt(host, ContextCompat.getMainExecutor(host), callback)
        }
    }

    // 进页面即判定；retryTickState 变化 = 用户点了「重 试」，重走一遍判定并重新弹窗
    LaunchedEffect(enabled, activity, promptInfo, prompt, retryTickState.value) {
        // ① 未开启生物识别锁：直接放行
        if (!enabled) {
            passOnce()
            return@LaunchedEffect
        }
        // ② 宿主不是 FragmentActivity：1.1.0 无法弹窗，优雅放行，不锁死用户
        val systemPrompt = prompt
        if (systemPrompt == null) {
            passOnce()
            return@LaunchedEffect
        }
        // ③ 设备无可用认证方式（canAuthenticate != BIOMETRIC_SUCCESS 的一切状态）：同样优雅放行
        if (!hasUsableAuthenticator(context.applicationContext)) {
            passOnce()
            return@LaunchedEffect
        }
        // ④ PromptInfo 构建失败（定制 ROM 拒绝该组合）：放行
        val info = promptInfo
        if (info == null) {
            passOnce()
            return@LaunchedEffect
        }

        promptingState.value = true
        errorState.value = null

        val launched = runCatching { systemPrompt.authenticate(info) }.isSuccess
        if (!launched) {
            // 系统拒绝创建弹窗（生物识别服务异常等）：放行，绝不把用户锁在门外
            promptingState.value = false
            passOnce()
        }
    }

    // ---------------- 门禁页视觉：紫微深空 + 泥金星印 ----------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0512), ZiWeiColors.DeepSpace, Color(0xFF1B0F2E)),
                ),
            ),
    ) {
        StarfieldBackground(starCount = 80, animated = true, seed = 23)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 圆形泥金描边印章（外圈刻度环 + 内圈双环 + 中心「机」字）
            SealLockIcon(modifier = Modifier.size(126.dp))

            Spacer(Modifier.height(30.dp))

            Text(
                text = title,
                color = ZiWeiColors.Gold,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = subtitle,
                style = ZiWeiType.Commentary,
                color = ZiWeiColors.TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(26.dp))

            val err = errorState.value
            if (err != null) {
                Text(
                    text = err,
                    color = ZiWeiColors.Cinnabar,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))

                // 泥金描边「重 试」印章按钮
                val busy = promptingState.value
                val retryInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .border(
                            width = 0.9.dp,
                            color = ZiWeiColors.Gold.copy(alpha = if (busy) 0.30f else 0.82f),
                            shape = RoundedCornerShape(50),
                        )
                        .background(
                            color = ZiWeiColors.Gold.copy(alpha = if (busy) 0.03f else 0.08f),
                            shape = RoundedCornerShape(50),
                        )
                        .clickable(
                            interactionSource = retryInteraction,
                            indication = null,
                            enabled = !busy,
                        ) {
                            haptics.medium()
                            errorState.value = null
                            retryTickState.value += 1
                        }
                        .padding(horizontal = 34.dp, vertical = 11.dp),
                ) {
                    Text(
                        text = "重 试",
                        color = ZiWeiColors.Gold.copy(alpha = if (busy) 0.4f else 1f),
                        fontFamily = ZiWeiSerifFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 4.sp,
                    )
                }
            } else {
                Text(
                    text = "正 在 唤 起 验 证 …",
                    color = ZiWeiColors.TextTertiary,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                )
            }
        }

        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp),
            text = "紫 微 内 丹 录 · 天 机 不 可 泄",
            color = ZiWeiColors.TextTertiary.copy(alpha = 0.55f),
            fontFamily = ZiWeiSerifFamily,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
        )
    }
}

// ==================== 内部实现 ====================

/**
 * 允许的认证方式：BIOMETRIC_WEAK（弱生物识别，兼容光学 / 屏下指纹）+ DEVICE_CREDENTIAL（锁屏密码兜底）。
 *
 * 为什么用 WEAK 而不是 STRONG（已反编译 biometric-1.1.0 的 AuthenticatorUtils.isSupportedCombination 核对）：
 *  - BIOMETRIC_STRONG | DEVICE_CREDENTIAL（0x802F）只在 **API 28 与 29** 上返回 false，其余版本合法；
 *  - BIOMETRIC_WEAK | DEVICE_CREDENTIAL（0x80FF）被无条件返回 true，全版本安全。
 * 本工程 minSdk 26，必然覆盖 28/29 这两档，所以选 WEAK 才能全版本不抛
 * IllegalArgumentException（"Authenticator combination is unsupported on API xx"）。
 */
private val ALLOWED_AUTHENTICATORS: Int =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * 把系统返回的英文错误文案映射成门禁页上的中文古风提示。
 *
 * 为什么需要：BiometricPrompt 的 onAuthenticationError 回调里 errString 是系统给的
 * 本地化文案，但部分 ROM（含本机模拟器）直接返回英文原文，例如「Authentication canceled」，
 * 直接显示会与整页中文风格割裂。按关键字匹配做保守映射，匹配不到就退回统一话术。
 */
private fun friendlyError(raw: CharSequence?): String {
    val s = raw?.toString().orEmpty().trim()
    if (s.isEmpty()) return "验 证 未 通 过 ， 请 重 试"
    val lower = s.lowercase()
    return when {
        "cancel" in lower -> "验 证 已 取 消 ， 请 重 试"
        "lockout" in lower || "too many" in lower -> "错误次数过多，请稍后再试"
        "not recognized" in lower || "no match" in lower -> "指 纹 未 识 别 ， 请 重 试"
        "negative" in lower -> "验 证 已 中 止"
        "unavailable" in lower || "hardware" in lower -> "生物识别暂不可用，请改用锁屏密码"
        "timeout" in lower -> "验 证 超 时 ， 请 重 试"
        else -> s
    }
}

/**
 * 设备当前是否真的能发起认证（已录指纹 / 面容，或已设置锁屏密码）。
 * 任何异常（含定制 ROM 让 BiometricManager 抛错）一律视为「不可用」，由调用方放行。
 */
private fun hasUsableAuthenticator(context: Context): Boolean = runCatching {
    BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS
}.getOrDefault(false)

/**
 * 从任意 Context 向上寻找宿主 Activity，且仅当它是 FragmentActivity 时返回，否则返回 null。
 *
 * 不用 androidx.core 的 Context.findActivity()：它是 internal，跨模块不可见，这里自行遍历 ContextWrapper。
 */
internal fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    var guard = 0
    while (ctx != null && guard < 16) {
        if (ctx is FragmentActivity) return ctx
        val wrapper: ContextWrapper = ctx as? ContextWrapper ?: return null
        val base = wrapper.baseContext
        if (base === ctx) return null
        ctx = base
        guard += 1
    }
    return null
}

/**
 * 圆形泥金印章图标：
 *  - 外圈 24 道放射刻度（每 6 道加粗，暗合十二时辰 / 二十四节气）
 *  - 内圈实线环 + 虚线环（印章双框）
 *  - 中心「机」字，宋体加粗
 */
@Composable
private fun SealLockIcon(
    modifier: Modifier = Modifier,
    glyph: String = "机",
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 外圈细环
            drawCircle(
                color = ZiWeiColors.Gold.copy(alpha = 0.30f),
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 0.9.dp.toPx()),
            )

            // 外圈放射刻度
            val ticks = 24
            for (i in 0 until ticks) {
                val angle = i * (2f * PI.toFloat() / ticks)
                val dx = cos(angle)
                val dy = sin(angle)
                val major = i % 6 == 0
                val inner = radius - 6.dp.toPx()
                val outer = radius - (if (major) 1.2.dp.toPx() else 3.2.dp.toPx())
                drawLine(
                    color = ZiWeiColors.Gold.copy(alpha = if (major) 0.58f else 0.22f),
                    start = Offset(center.x + dx * inner, center.y + dy * inner),
                    end = Offset(center.x + dx * outer, center.y + dy * outer),
                    strokeWidth = (if (major) 1.4.dp else 0.8.dp).toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // 环内极淡底色，让中心字浮起来
            drawCircle(
                color = ZiWeiColors.Gold.copy(alpha = 0.06f),
                radius = radius * 0.52f,
                center = center,
            )

            // 内圈实线环（主印框）
            drawCircle(
                color = ZiWeiColors.Gold.copy(alpha = 0.92f),
                radius = radius * 0.62f,
                center = center,
                style = Stroke(width = 1.6.dp.toPx()),
            )

            // 内圈虚线环
            drawCircle(
                color = ZiWeiColors.Gold.copy(alpha = 0.42f),
                radius = radius * 0.54f,
                center = center,
                style = Stroke(
                    width = 0.9.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                ),
            )
        }

        Text(
            text = glyph,
            color = ZiWeiColors.Gold,
            fontFamily = ZiWeiSerifFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            letterSpacing = 1.sp,
        )
    }
}
