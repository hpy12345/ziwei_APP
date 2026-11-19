package com.ziweistar.app.ui.alias

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.ziweistar.app.MainActivity

/**
 * 应用图标伪装（防社死机制之一）
 *
 * 设计文档 1.1：支持在设置中一键切换为
 * 《钦天万年历》/《紫微排盘宝典》/《节气养生录》图标。
 *
 * 实现方式：通过 PackageManager 启用/禁用各 Alias Activity 的 LAUNCHER 入口，
 * 主入口与三个伪装入口互斥，同一时刻只有一个可见。
 */
enum class IconAlias(
    val label: String,
    internal val activityClass: String,
) {
    REAL("紫薇星（原始）", MainActivity::class.java.name),
    QINTIAN("钦天万年历", "com.ziweistar.app.ui.alias.AliasLauncherActivity"),
    PAIPAN("紫微排盘宝典", "com.ziweistar.app.ui.alias.AliasPaipanActivity"),
    JIEQI("节气养生录", "com.ziweistar.app.ui.alias.AliasJieqiActivity");

    companion object {
        fun of(name: String?): IconAlias =
            entries.firstOrNull { it.name == name } ?: REAL
    }
}

object AliasManager {

    /**
     * 切换桌面图标。
     * 注意：真实入口与伪装入口同时启用时，桌面上会出现两个图标 —— 必须互斥。
     */
    fun apply(context: Context, alias: IconAlias) {
        val pm = context.packageManager
        val pkg = context.packageName

        for (a in IconAlias.entries) {
            val state = if (a == alias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, a.activityClass),
                    state,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
        // 部分 ROM 需要延迟刷新桌面才会更新图标
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                .setPackage(pkg)
            context.packageManager.queryIntentActivities(intent, 0)
        }
    }

    /** 恢复原始图标（用于焚毁天机后） */
    fun resetToReal(context: Context) = apply(context, IconAlias.REAL)
}
