package com.sunlink.svitgrid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sunlink.svitgrid.R

private val MonaSans = FontFamily(
    Font(R.font.mona_sans_400, FontWeight.W400),
    Font(R.font.mona_sans_600, FontWeight.W600),
    Font(R.font.mona_sans_700, FontWeight.W700)
)
private val Commissioner = FontFamily(
    Font(R.font.commissioner_400, FontWeight.W400),
    Font(R.font.commissioner_500, FontWeight.W500),
    Font(R.font.commissioner_600, FontWeight.W600)
)
private val RedHatMono = FontFamily(
    Font(R.font.red_hat_mono_400, FontWeight.W400),
    Font(R.font.red_hat_mono_500, FontWeight.W500)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineLarge = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = MonaSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Commissioner, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Commissioner, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Commissioner, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Commissioner, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Commissioner, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Commissioner, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Commissioner, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp)
)
