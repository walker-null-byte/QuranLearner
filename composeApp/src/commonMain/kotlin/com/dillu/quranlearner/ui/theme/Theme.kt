package com.dillu.quranlearner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import quranlearner.composeapp.generated.resources.Res
import quranlearner.composeapp.generated.resources.al_qalam_quran_majeed_regular
import quranlearner.composeapp.generated.resources.inter_regular
import quranlearner.composeapp.generated.resources.noto_naskh_arabic_regular

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Noor Color Palette — Deep Navy + Emerald + Amber
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object NoorColors {
    // Primary (Emerald)
    val Primary = Color(0xFF4EDEA3)
    val PrimaryContainer = Color(0xFF10B981)
    val OnPrimary = Color(0xFF003824)
    val OnPrimaryContainer = Color(0xFF00422B)

    // Secondary (Amber)
    val Secondary = Color(0xFFFFB95F)
    val SecondaryContainer = Color(0xFFEE9800)
    val OnSecondary = Color(0xFF472A00)
    val OnSecondaryContainer = Color(0xFF5B3800)

    // Tertiary (Sage)
    val Tertiary = Color(0xFF95D3BA)
    val TertiaryContainer = Color(0xFF71AF97)
    val OnTertiary = Color(0xFF003829)
    val OnTertiaryContainer = Color(0xFF004231)

    // Surfaces (Deep Navy)
    val Background = Color(0xFF0B1326)
    val Surface = Color(0xFF0B1326)
    val SurfaceDim = Color(0xFF0B1326)
    val SurfaceContainerLowest = Color(0xFF060E20)
    val SurfaceContainerLow = Color(0xFF131B2E)
    val SurfaceContainer = Color(0xFF171F33)
    val SurfaceContainerHigh = Color(0xFF222A3D)
    val SurfaceContainerHighest = Color(0xFF2D3449)
    val SurfaceBright = Color(0xFF31394D)
    val SurfaceVariant = Color(0xFF2D3449)

    // On-Surface
    val OnSurface = Color(0xFFDAE2FD)
    val OnSurfaceVariant = Color(0xFFBBCABF)
    val OnBackground = Color(0xFFDAE2FD)

    // Outline
    val Outline = Color(0xFF86948A)
    val OutlineVariant = Color(0xFF3C4A42)

    // Error
    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF93000A)
    val OnError = Color(0xFF690005)
    val OnErrorContainer = Color(0xFFFFDAD6)

    // Inverse
    val InverseSurface = Color(0xFFDAE2FD)
    val InverseOnSurface = Color(0xFF283044)
    val InversePrimary = Color(0xFF006C49)
}

private val NoorDarkColorScheme = darkColorScheme(
    primary = NoorColors.Primary,
    onPrimary = NoorColors.OnPrimary,
    primaryContainer = NoorColors.PrimaryContainer,
    onPrimaryContainer = NoorColors.OnPrimaryContainer,
    secondary = NoorColors.Secondary,
    onSecondary = NoorColors.OnSecondary,
    secondaryContainer = NoorColors.SecondaryContainer,
    onSecondaryContainer = NoorColors.OnSecondaryContainer,
    tertiary = NoorColors.Tertiary,
    onTertiary = NoorColors.OnTertiary,
    tertiaryContainer = NoorColors.TertiaryContainer,
    onTertiaryContainer = NoorColors.OnTertiaryContainer,
    background = NoorColors.Background,
    onBackground = NoorColors.OnBackground,
    surface = NoorColors.Surface,
    onSurface = NoorColors.OnSurface,
    surfaceVariant = NoorColors.SurfaceVariant,
    onSurfaceVariant = NoorColors.OnSurfaceVariant,
    surfaceDim = NoorColors.SurfaceDim,
    surfaceBright = NoorColors.SurfaceBright,
    surfaceContainerLowest = NoorColors.SurfaceContainerLowest,
    surfaceContainerLow = NoorColors.SurfaceContainerLow,
    surfaceContainer = NoorColors.SurfaceContainer,
    surfaceContainerHigh = NoorColors.SurfaceContainerHigh,
    surfaceContainerHighest = NoorColors.SurfaceContainerHighest,
    outline = NoorColors.Outline,
    outlineVariant = NoorColors.OutlineVariant,
    error = NoorColors.Error,
    onError = NoorColors.OnError,
    errorContainer = NoorColors.ErrorContainer,
    onErrorContainer = NoorColors.OnErrorContainer,
    inverseSurface = NoorColors.InverseSurface,
    inverseOnSurface = NoorColors.InverseOnSurface,
    inversePrimary = NoorColors.InversePrimary,
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Custom Typography Holder (accessible via LocalNoorTypography)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class NoorTypography(
    /** Indo-Pak mushaf text — Al Qalam Quran Majeed (full IndoPak glyph coverage). */
    val arabicDisplay: TextStyle,
    /** Uthmani mushaf text — uses Noto Naskh Arabic for full Quranic Unicode coverage (avoids tofu). */
    val arabicUthmani: TextStyle,
    val displayLg: TextStyle,
    val headlineLg: TextStyle,
    val headlineMd: TextStyle,
    val bodyLg: TextStyle,
    val bodyMd: TextStyle,
    val labelSm: TextStyle,
)

val LocalNoorTypography = staticCompositionLocalOf<NoorTypography> {
    error("NoorTypography not provided")
}

/** Quran body: Noto Naskh for Uthmani data, Al Qalam for Indo-Pak. */
fun NoorTypography.arabicForQuranScript(script: String): TextStyle =
    if (script == "Uthmani") arabicUthmani else arabicDisplay

/**
 * Removes codepoints no bundled font can render so they never show as tofu boxes:
 * - U+E000..U+F8FF private-use area (tajweed annotation marks leaked into the IndoPak data)
 * - U+FEFF zero-width no-break space, U+200B zero-width space
 *
 * With [stripStopMarks] the floating waqf/stop signs (jeem, three-dots, seen, meem, noon…)
 * are removed too — the IndoPak data stacks 2-3 of them at one spot (e.g. 2:2 ۛۚۖ) and they
 * render as stray floating dots. Essential diacritics (sukun U+06E1, dagger alif U+0670,
 * madda U+06E4, small letters, sajdah U+06E9) are always kept.
 */
fun String.sanitizeQuranText(stripStopMarks: Boolean = false): String {
    val sb = StringBuilder(length)
    for (c in this) {
        val v = c.code
        if (v in 0xE000..0xF8FF || v == 0xFEFF || v == 0x200B) continue
        if (stripStopMarks && isQuranStopMark(v)) continue
        sb.append(c)
    }
    return sb.toString()
}

private fun isQuranStopMark(v: Int): Boolean =
    v in 0x06D6..0x06DC || // small high ligature marks, jeem, lam-alef, three dots, seen
        v == 0x06DF ||     // small high rounded zero
        v == 0x06E2 ||     // small high meem (mandatory-stop sign)
        v == 0x06E8 ||     // small high noon
        v == 0x06EB ||     // empty centre high stop
        v == 0x06ED        // small low meem

@Composable
fun NoorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always dark for now — the design is built around the deep navy palette
    val colorScheme = NoorDarkColorScheme

    val interFamily = FontFamily(Font(Res.font.inter_regular))
    val alQalamFamily = FontFamily(Font(Res.font.al_qalam_quran_majeed_regular))
    val notoNaskhArabicFamily = FontFamily(Font(Res.font.noto_naskh_arabic_regular))

    val noorTypography = NoorTypography(
        arabicDisplay = TextStyle(
            fontFamily = alQalamFamily,
            fontSize = 36.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Normal,
        ),
        arabicUthmani = TextStyle(
            fontFamily = notoNaskhArabicFamily,
            fontSize = 36.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Normal,
        ),
        displayLg = TextStyle(
            fontFamily = interFamily,
            fontSize = 42.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
        ),
        headlineLg = TextStyle(
            fontFamily = interFamily,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineMd = TextStyle(
            fontFamily = interFamily,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLg = TextStyle(
            fontFamily = interFamily,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMd = TextStyle(
            fontFamily = interFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        ),
        labelSm = TextStyle(
            fontFamily = interFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        ),
    )

    // Map to Material3 Typography using Inter
    val materialTypography = Typography(
        displayLarge = noorTypography.displayLg,
        headlineLarge = noorTypography.headlineLg,
        headlineMedium = noorTypography.headlineMd,
        bodyLarge = noorTypography.bodyLg,
        bodyMedium = noorTypography.bodyMd,
        labelSmall = noorTypography.labelSm,
        titleLarge = noorTypography.headlineMd,
        titleMedium = TextStyle(
            fontFamily = interFamily,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleSmall = TextStyle(
            fontFamily = interFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        labelMedium = TextStyle(
            fontFamily = interFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = interFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodySmall = TextStyle(
            fontFamily = interFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
        ),
    )

    CompositionLocalProvider(LocalNoorTypography provides noorTypography) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = materialTypography,
            content = content
        )
    }
}
