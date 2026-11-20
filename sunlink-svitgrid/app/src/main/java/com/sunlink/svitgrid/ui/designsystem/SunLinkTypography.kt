package com.sunlink.svitgrid.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Единая типографическая система. Числовые стили используют tabular figures
 * (`tnum`), чтобы ширина показаний не «прыгала» при обновлении телеметрии.
 */
object SunLinkTypography {
    private val DisplayFont = FontFamily.Monospace
    private val TextFont = FontFamily.SansSerif
    private const val Tabular = "tnum"

    val display = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
        fontSize = 38.sp, lineHeight = 44.sp, fontFeatureSettings = Tabular
    )
    val headline = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    )
    val title = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp
    )
    val metric = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, fontFeatureSettings = Tabular
    )
    val body = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp
    )
    val label = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp
    )
    val caption = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 14.sp, fontFeatureSettings = Tabular
    )
}
