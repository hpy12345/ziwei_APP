package com.ziweistar.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ziweistar.app.ui.theme.ZiWeiSerifFamily

val ZiWeiTypography = Typography(
    displayLarge = ZiWeiType.SealTitle.copy(fontSize = 44.sp),
    headlineLarge = ZiWeiType.SealTitle.copy(fontSize = 30.sp),
    headlineMedium = ZiWeiType.SectionTitle.copy(fontSize = 24.sp),
    headlineSmall = ZiWeiType.SectionTitle,
    titleLarge = ZiWeiType.SectionTitle.copy(fontSize = 17.sp),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.6.sp,
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
    ),
    bodyLarge = ZiWeiType.Body.copy(fontSize = 15.sp),
    bodyMedium = ZiWeiType.Body,
    bodySmall = ZiWeiType.Caption,
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = ZiWeiSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 3.sp,
    ),
    labelMedium = ZiWeiType.Caption.copy(letterSpacing = 1.sp),
    labelSmall = ZiWeiType.Caption.copy(fontSize = 11.sp),
)
