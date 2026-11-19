package com.ziweistar.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiColors

/**
 * 全局统一免责声明页脚。
 *
 * 文案与 [com.ziweistar.app.ui.screens.GlossaryScreen] 底部逐字一致（设计文档 1.4 合规红线）：
 * 本应用所述「玄门」「星宿」「批文」，皆为记录与自我观察之隐喻框架，
 * 不构成任何医疗、诊断或治疗建议。若身体有恙，请咨询执业医师。
 *
 * @param modifier 外部布局修饰符
 * @param compact 紧凑模式：用于弹窗 / 卡片底部等窄空间（8.5sp / 14sp 行高）
 */
@Composable
fun DisclaimerFooter(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 泥金分隔线（真实签名：GoldDivider(modifier, alpha)，默认 alpha = 0.35f）
        GoldDivider(alpha = 0.25f)

        // 分隔线自身高 9dp，这里补足到与 GlossaryScreen 一致的 16dp 视觉间距
        Spacer(Modifier.height(7.dp))

        Text(
            text = "本应用所述「玄门」「星宿」「批文」，皆为记录与自我观察之隐喻框架，\n不构成任何医疗、诊断或治疗建议。若身体有恙，请咨询执业医师。",
            fontSize = if (compact) 8.5.sp else 9.5.sp,
            color = ZiWeiColors.TextTertiary,
            lineHeight = if (compact) 14.sp else 16.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
