package com.ziweistar.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ziweistar.app.ui.theme.Haptics

/**
 * 触觉反馈引擎实现（Haptic Engine）
 *
 * 设计文档 1.1：
 *  - 撕页老黄历：UIImpactFeedbackGenerator(.light) 连续脆响 → LIGHT tick
 *  - 竹筒摇签：阈值 > 2.2g，线性马达模拟竹签碰撞清脆震动 → 短促连击
 *  - 朱砂盖印：Heavy Impact 沉重顿挫 → HEAVY + 双段
 */
object HapticEngine {

    fun create(context: Context): Haptics {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        fun oneShot(ms: Long, amplitude: Int) {
            if (vibrator == null || !vibrator.hasVibrator()) return
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255)),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(ms)
                }
            }
        }

        fun waveform(pattern: LongArray, amplitudes: IntArray) {
            if (vibrator == null || !vibrator.hasVibrator()) return
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        }

        return Haptics(
            // 极轻：滑动选择、Tab 切换
            tick = { oneShot(8L, 60) },
            // 轻：撕页、点选
            light = { oneShot(16L, 110) },
            // 中：摇签、翻页
            medium = { oneShot(26L, 165) },
            // 重：确认收功
            heavy = { oneShot(42L, 230) },
            // 盖印：朱砂重顿 + 古琴泛音感的双段节奏
            seal = {
                waveform(
                    longArrayOf(0, 30, 45, 90, 30, 140),
                    intArrayOf(0, 255, 0, 200, 0, 120),
                )
            },
        )
    }
}
