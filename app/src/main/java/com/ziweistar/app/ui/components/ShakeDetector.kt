package com.ziweistar.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/** 摇一摇防抖窗口：1.2s 内只认一次，避免连续抖动打出连环事件 */
private const val SHAKE_DEBOUNCE_MS = 1200L

/**
 * 摇一摇检测（无 UI 输出，纯副作用组件）。
 *
 * 用于「摇出今日签」等交互：把 [ShakeDetector] 放到任意 Composable 里，它不会绘制任何东西。
 *
 * 判定方式：加速度计合向量 |a| = sqrt(x² + y² + z²)，静态时约等于 [SensorManager.GRAVITY_EARTH]（9.81）。
 * 当 |a| 超过 `thresholdG * GRAVITY_EARTH` 即视为一次摇动；配合 1.2s 防抖，一次摇动只回调一次。
 *
 * @param enabled       false 时不注册传感器（省电），已注册的会被反注册
 * @param thresholdG    触发阈值，单位 g。2.2f 属于「明显甩动」，日常行走 / 放下手机的 1.2~1.8g 不会误触
 * @param onShake       触发回调；用 rememberUpdatedState 保证始终调用最新的 lambda
 *
 * 线程说明：注册监听时不传 Handler，因此 [onShake] 在**主线程**回调，可直接修改 Compose 状态。
 */
@Composable
fun ShakeDetector(
    enabled: Boolean = true,
    thresholdG: Float = 2.2f,
    onShake: () -> Unit,
) {
    val context = LocalContext.current

    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val accelerometer = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // 保存最新的回调与参数：Effect 只在 enabled 变化时重建，但阈值/回调要能实时生效
    val latestOnShake = rememberUpdatedState(onShake)
    val latestEnabled = rememberUpdatedState(enabled)
    val latestThresholdG = rememberUpdatedState(thresholdG)

    // 上次触发时间戳（单元素数组作为可变承载单元，避免用 Compose 状态导致每次摇动都触发重组）
    val lastShakeAt = remember { LongArray(1) }

    DisposableEffect(enabled, sensorManager, accelerometer) {
        val manager = sensorManager
        val sensor = accelerometer

        if (!enabled || manager == null || sensor == null) {
            // 未开启 / 设备无加速度计：不注册，什么都不做
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val e = event ?: return
                    if (e.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                    if (!latestEnabled.value) return

                    val x = e.values[0]
                    val y = e.values[1]
                    val z = e.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z)

                    // 与重力加速度比较：超过 thresholdG 倍 G 才算摇动
                    val trigger = latestThresholdG.value * SensorManager.GRAVITY_EARTH
                    if (magnitude <= trigger) return

                    val now = System.currentTimeMillis()
                    if (now - lastShakeAt[0] < SHAKE_DEBOUNCE_MS) return
                    lastShakeAt[0] = now

                    latestOnShake.value()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            manager.registerListener(
                listener,
                sensor,
                // GAME 档（约 50Hz）对甩动这一类短促手势足够灵敏；比 UI 档更跟手，比 FASTEST 省电
                SensorManager.SENSOR_DELAY_GAME,
            )

            onDispose {
                // 无条件反注册：对未注册的监听器调用是安全的空操作，兜底防泄漏
                manager.unregisterListener(listener)
            }
        }
    }
}
