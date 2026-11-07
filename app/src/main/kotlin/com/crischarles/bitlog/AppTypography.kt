package com.crischarles.bitlog

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Apple SF Pro Rounded font family for authentic iOS typography.
 * Tabular figures are enabled for timer digits so numbers never shift horizontally.
 */
val SfProRoundedFamily = FontFamily(
    Font(R.font.sf_pro_rounded_regular, FontWeight.Normal),
    Font(R.font.sf_pro_rounded_medium, FontWeight.Medium),
    Font(R.font.sf_pro_rounded_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_rounded_bold, FontWeight.Bold),
)

object AppTypography {
    val TimerDisplayFullscreen = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 76.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum",
    )

    val TimerDisplay = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum",
    )

    val LargeTitle = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
    )

    val Title1 = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.3).sp,
    )

    val Title2 = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
    )

    val Headline = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    )

    val Body = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    )

    val Callout = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    )

    val Footnote = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    )

    val Caption = TextStyle(
        fontFamily = SfProRoundedFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
    )
}

/**
 * Responsive text component with no Material UI dependency.
 * Renders using BasicText with SF Pro Rounded typography and adaptive contrast color.
 */
@Composable
fun LiquidText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTypography.Body,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val finalStyle = style.copy(
        color = if (color != Color.Unspecified) color else style.color,
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        textAlign = textAlign ?: style.textAlign,
    )

    BasicText(
        text = text,
        modifier = modifier,
        style = finalStyle,
        maxLines = maxLines,
        overflow = overflow,
    )
}
