package com.sunlink.svitgrid.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * SunLink Hybrid Premium typography, mapped to the Stitch tokens:
 * Mona Sans → SansSerif display/headline/title,
 * Commissioner → SansSerif body/label,
 * Red Hat Mono → Monospace technical values with tabular figures.
 */
object SunLinkTypography {
    private val DisplayFont = FontFamily.SansSerif
    private val TextFont = FontFamily.SansSerif
    private val MonoFont = FontFamily.Monospace
    private const val Tabular = "tnum"

    val display = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, fontFeatureSettings = Tabular
    )
    val headline = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    )
    val title = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp
    )
    val metric = TextStyle(
        fontFamily = MonoFont, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, fontFeatureSettings = Tabular
    )
    val body = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    )
    val label = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp
    )
    val caption = TextStyle(
        fontFamily = TextFont, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, fontFeatureSettings = Tabular
    )
    val mono = TextStyle(
        fontFamily = MonoFont, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = Tabular
    )
}
