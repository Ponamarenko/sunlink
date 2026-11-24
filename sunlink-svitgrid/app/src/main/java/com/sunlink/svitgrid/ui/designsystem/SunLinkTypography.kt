package com.sunlink.svitgrid.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sunlink.svitgrid.R

/**
 * Stitch typography, bundled from the official Google Fonts URLs referenced by
 * the approved design:
 * Mona Sans — display / headline / title
 * Commissioner — body / label / caption
 * Red Hat Mono — technical values and tabular numeric data
 */
object SunLinkTypography {
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
    private const val Tabular = "tnum"

    val display = TextStyle(
        fontFamily = MonaSans, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, fontFeatureSettings = Tabular
    )
    val headline = TextStyle(
        fontFamily = MonaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    )
    val title = TextStyle(
        fontFamily = MonaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp
    )
    val metric = TextStyle(
        fontFamily = RedHatMono, fontWeight = FontWeight.Medium,
        fontSize = 26.sp, lineHeight = 32.sp, fontFeatureSettings = Tabular
    )
    val body = TextStyle(
        fontFamily = Commissioner, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    )
    val label = TextStyle(
        fontFamily = Commissioner, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp
    )
    val caption = TextStyle(
        fontFamily = Commissioner, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, fontFeatureSettings = Tabular
    )
    val mono = TextStyle(
        fontFamily = RedHatMono, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = Tabular
    )
}
