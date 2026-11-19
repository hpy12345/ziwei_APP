package com.ziweistar.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 无涟漪点击（本应用整体走"古籍/玄学"质感，禁用 Material 默认水波）
 */
@Composable
fun Modifier.tapNoRipple(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    if (!enabled) return this
    val source = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
        ),
    )
}

/** 非 Composable 上下文的版本（需要外部传入 interactionSource） */
fun Modifier.tapRaw(source: MutableInteractionSource, onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = source, indication = null, onClick = onClick))
