package com.ziweistar.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.fragment.app.FragmentActivity
import com.ziweistar.app.ui.HapticEngine
import com.ziweistar.app.ui.nav.ZiWeiNavHost
import com.ziweistar.app.ui.theme.LocalHaptics
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiTheme
import kotlinx.coroutines.delay

/**
 * 主入口。
 *
 * 防社死机制（设计文档 1.2）：
 *  1. 双指下滑 → 瞬切《钦天万年历》老黄历界面（Boss Key）
 *  2. 屏幕朝下放置 0.2s → 同上
 *  3. 多任务缩略图模糊（FLAG_SECURE）
 *
 * 继承 FragmentActivity 而非 ComponentActivity：androidx.biometric 1.1.0 的
 * BiometricPrompt 只接受 FragmentActivity 作为宿主（设计文档 7 · 生物识别锁）。
 */
class MainActivity : FragmentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    /** 屏幕朝下：重力 z 轴朝下 */
    @Volatile
    private var faceDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 多任务缩略图模糊 / 防截屏
        // BuildConfig.SCREENSHOT_ALLOWED 仅用于开发期视觉走查，正式包恒为 false
        if (!BuildConfig.SCREENSHOT_ALLOWED) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        setupSensors()

        val haptics = HapticEngine.create(this)

        setContent {
            var bossKey by remember { mutableStateOf(false) }

            // 屏幕朝下 0.2s 确认后触发
            LaunchedEffect(Unit) {
                while (true) {
                    if (faceDown && !bossKey) {
                        delay(200)
                        if (faceDown) bossKey = true
                    }
                    delay(120)
                }
            }

            CompositionLocalProvider(LocalHaptics provides haptics) {
                ZiWeiTheme {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(ZiWeiColors.DeepSpace)
                            .pointerInput(Unit) {
                                // 双指下滑检测
                                awaitPointerEventScope {
                                    var startY = 0f
                                    var startTime = 0L
                                    var twoFinger = false
                                    while (true) {
                                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                                        val pressed = ev.changes.count { it.pressed }
                                        when {
                                            ev.changes.any { it.changedToDown() } -> {
                                                if (pressed >= 2) {
                                                    twoFinger = true
                                                    startY = ev.changes
                                                        .map { it.position.y }
                                                        .average()
                                                        .toFloat()
                                                    startTime = System.currentTimeMillis()
                                                }
                                            }

                                            twoFinger && ev.changes.any { it.pressed } -> {
                                                val curY = ev.changes
                                                    .map { it.position.y }
                                                    .average()
                                                    .toFloat()
                                                val dt = System.currentTimeMillis() - startTime
                                                if (curY - startY > 220f && dt < 700) {
                                                    twoFinger = false
                                                    bossKey = true
                                                }
                                            }

                                            ev.changes.none { it.pressed } -> twoFinger = false
                                        }
                                    }
                                }
                            },
                    ) {
                        ZiWeiNavHost(
                            bossKey = bossKey,
                            onBossKeyExit = { bossKey = false },
                        )
                    }
                }
            }
        }
    }

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        faceDown = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val z = event.values[2]
        faceDown = z < -8.5f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
