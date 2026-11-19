package com.ziweistar.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ZiWeiColorScheme = darkColorScheme(
    primary = ZiWeiColors.Gold,
    onPrimary = ZiWeiColors.DeepSpace,
    primaryContainer = ZiWeiColors.XuanMingLight,
    onPrimaryContainer = ZiWeiColors.Gold,
    secondary = ZiWeiColors.Cinnabar,
    onSecondary = Color.White,
    secondaryContainer = ZiWeiColors.XuanMing,
    onSecondaryContainer = ZiWeiColors.Cinnabar,
    tertiary = ZiWeiColors.Jade,
    onTertiary = ZiWeiColors.DeepSpace,
    background = ZiWeiColors.DeepSpace,
    onBackground = ZiWeiColors.RicePaper,
    surface = ZiWeiColors.DeepSpace,
    onSurface = ZiWeiColors.RicePaper,
    surfaceVariant = ZiWeiColors.XuanMing,
    onSurfaceVariant = ZiWeiColors.TextSecondary,
    outline = ZiWeiColors.GoldDim,
    outlineVariant = ZiWeiColors.XuanMingLight,
    error = ZiWeiColors.Cinnabar,
    onError = Color.White,
    scrim = Color(0xCC000000),
)

/**
 * 全局触觉反馈引擎（Haptic Engine）句柄。
 * 设计文档 1.1：撕页老黄历 / 竹筒摇签 / 朱砂盖印 三档震感 + 星轨阻尼。
 */
data class Haptics(
    val tick: () -> Unit = {},
    val light: () -> Unit = {},
    val medium: () -> Unit = {},
    val heavy: () -> Unit = {},
    /** 古琴泛音 + 沉重顿挫（盖印瞬间） */
    val seal: () -> Unit = {},
)

val LocalHaptics = staticCompositionLocalOf { Haptics() }

@Composable
fun ZiWeiTheme(
    darkTheme: Boolean = true, // Dark-First：本应用常驻暗色
    haptics: Haptics = Haptics(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    CompositionLocalProvider(LocalHaptics provides haptics) {
        MaterialTheme(
            colorScheme = ZiWeiColorScheme,
            typography = ZiWeiTypography,
            content = content,
        )
    }
}
