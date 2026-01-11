package dev.subfly.yaba.core.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import yaba.composeapp.generated.resources.Quicksand_Bold
import yaba.composeapp.generated.resources.Quicksand_Light
import yaba.composeapp.generated.resources.Quicksand_Medium
import yaba.composeapp.generated.resources.Quicksand_Regular
import yaba.composeapp.generated.resources.Quicksand_SemiBold
import yaba.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun quicksandFontFamily() = FontFamily(
    Font(Res.font.Quicksand_Light, weight = FontWeight.Light),
    Font(Res.font.Quicksand_Regular, weight = FontWeight.Normal),
    Font(Res.font.Quicksand_Medium, weight = FontWeight.Medium),
    Font(Res.font.Quicksand_SemiBold, weight = FontWeight.SemiBold),
    Font(Res.font.Quicksand_Bold, weight = FontWeight.Bold)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun yabaTypography() = Typography().run {
    val fontFamily = quicksandFontFamily()
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
        bodyLarge = bodyLarge.copy(fontFamily =  fontFamily),
        bodyLargeEmphasized = bodyLarge.copy(fontFamily =  fontFamily),
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