package dev.subfly.yaba.core.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.subfly.yaba.R

private val QuicksandFontFamily = FontFamily(
    Font(R.font.quicksand_light, FontWeight.Light),
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold),
)

/** Material expressive typography using Quicksand (same family as Compose / web-components). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun yabaTypography(): Typography = Typography().run {
    val fontFamily = QuicksandFontFamily
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayLargeEmphasized = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displayMediumEmphasized = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        displaySmallEmphasized = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineLargeEmphasized = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineMediumEmphasized = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        headlineSmallEmphasized = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleLargeEmphasized = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleMediumEmphasized = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        titleSmallEmphasized = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyLargeEmphasized = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodyMediumEmphasized = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        bodySmallEmphasized = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelLargeEmphasized = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelMediumEmphasized = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
        labelSmallEmphasized = labelSmall.copy(fontFamily = fontFamily),
    )
}
