package com.ziweistar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily
import com.ziweistar.app.ui.theme.ZiWeiColors
import com.ziweistar.app.ui.theme.ZiWeiType

/** 泥金细边框 + 玄冥底座的标准卡槽 */
@Composable
fun AlmanacCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ZiWeiColors.GoldDim,
    gradient: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.8.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (gradient) Color.Transparent else ZiWeiColors.XuanMing,
        ),
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (gradient) Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF241544), Color(0xFF1A0F30)),
                        ),
                    ) else Modifier,
                )
                .padding(contentPadding),
            content = content,
        )
    }
}

/** 朱砂大印按钮 */
@Composable
fun SealButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sealed: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (sealed) Brush.verticalGradient(listOf(Color(0xFF3A3540), Color(0xFF231F2B)))
                else Brush.verticalGradient(listOf(Color(0xFFE0574F), Color(0xFFB8392F))),
            ),
    ) {
        // 顶部泥金描边
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = if (sealed) Color(0xFF6B6472) else ZiWeiColors.Gold.copy(alpha = 0.75f),
                style = Stroke(
                    width = 1.6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f),
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
            )
        }
        androidx.compose.material3.TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) { content() }
    }
}

/** 吉凶朱印印章 */
@Composable
fun FortuneSeal(
    modifier: Modifier = Modifier,
    label: String,
    short: String,
    color: Color,
) {
    Box(
        modifier = modifier
            .size(84.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = color.copy(alpha = 0.16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            )
            drawRoundRect(
                color = color.copy(alpha = 0.9f),
                style = Stroke(width = 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            )
            drawRoundRect(
                color = color.copy(alpha = 0.45f),
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    size.width - 8.dp.toPx(),
                    size.height - 8.dp.toPx(),
                ),
                style = Stroke(width = 0.8.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = short,
                color = color,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ZiWeiSerifFamily,
            )
            Text(
                text = label,
                color = color.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontFamily = ZiWeiSerifFamily,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

/** 泥金分隔线（中间带星点） */
@Composable
fun GoldDivider(modifier: Modifier = Modifier, alpha: Float = 0.35f) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(9.dp),
    ) {
        val cy = size.height / 2
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    ZiWeiColors.Gold.copy(alpha = alpha),
                    ZiWeiColors.Gold.copy(alpha = alpha),
                    Color.Transparent,
                ),
            ),
            start = Offset(0f, cy),
            end = Offset(size.width, cy),
            strokeWidth = 0.8.dp.toPx(),
        )
        drawCircle(
            color = ZiWeiColors.Gold.copy(alpha = alpha * 1.8f),
            radius = 1.8.dp.toPx(),
            center = Offset(size.width / 2, cy),
        )
    }
}

/** 小型标签胶囊 */
@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    text: String,
    active: Boolean = false,
    accent: Color = ZiWeiColors.Gold,
    onClick: (() -> Unit)? = null,
) {
    val bg = if (active) accent.copy(alpha = 0.20f) else Color(0xFF221540)
    val fg = if (active) accent else ZiWeiColors.TextSecondary
    val border = if (active) accent.copy(alpha = 0.85f) else Color(0x33E6C17A)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(
                if (onClick != null) Modifier.tapNoRipple { onClick() } else Modifier,
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = border,
                style = Stroke(0.9.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
            )
        }
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

/** 区块标题：竖线 + 泥金宋体 */
@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    accent: Color = ZiWeiColors.Gold,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = ZiWeiType.SectionTitle,
                color = accent,
                fontSize = 16.sp,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ZiWeiType.Caption,
                    color = ZiWeiColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
        }
        trailing?.invoke()
    }
}

/** 空态占位 */
@Composable
fun EmptyHint(modifier: Modifier = Modifier, text: String) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ZiWeiType.Commentary,
            color = ZiWeiColors.TextTertiary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** 圆形头像框（泥金环） */
@Composable
fun StarAvatar(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 46.dp,
    image: android.graphics.Bitmap? = null,
    fallbackText: String = "星",
    ringColor: Color = ZiWeiColors.Gold,
    active: Boolean = false,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val r = this.size.minDimension / 2
            drawCircle(
                color = if (active) ringColor.copy(alpha = 0.35f) else Color(0xFF2A1A48),
                radius = r,
            )
            drawCircle(
                color = ringColor.copy(alpha = if (active) 1f else 0.55f),
                radius = r - 1.dp.toPx(),
                style = Stroke(width = if (active) 2.dp.toPx() else 1.dp.toPx()),
            )
        }
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(size - 8.dp)
                    .clip(CircleShape),
            )
        } else {            Text(
                text = fallbackText.take(1),
                color = ringColor,
                fontSize = (size.value * 0.36f).sp,
                fontFamily = ZiWeiSerifFamily,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
