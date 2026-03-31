package dev.subfly.yaba.core.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

/** Material expressive typography; add custom fonts under res/font when desired. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun yabaTypography(): Typography = Typography()