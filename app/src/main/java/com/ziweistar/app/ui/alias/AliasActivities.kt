package com.ziweistar.app.ui.alias

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.ziweistar.app.MainActivity

/**
 * 伪装入口基类：点击桌面伪装图标 → 直接进入应用主界面（内部逻辑与真实入口一致）。
 * 三个 disguise activity 各自声明不同的 label / icon，由 AliasManager 互斥启用。
 *
 * 注意：这里刻意**不**承载任何业务 UI，只做一次转发就 finish()，
 * 因此不需要 FragmentActivity（BiometricPrompt 只在 MainActivity 那侧用）。
 * 生物识别门禁由 MainActivity 里的 ZiWeiNavHost 统一拦截，从哪个入口进来都会被要求验证。
 */
abstract class BaseAliasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 透传给主入口逻辑：直接以原始 MainActivity 的 Extras 启动自己
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            putExtras(this@BaseAliasActivity.intent)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
        finish()
    }
}

class AliasLauncherActivity : BaseAliasActivity()
class AliasPaipanActivity : BaseAliasActivity()
class AliasJieqiActivity : BaseAliasActivity()
