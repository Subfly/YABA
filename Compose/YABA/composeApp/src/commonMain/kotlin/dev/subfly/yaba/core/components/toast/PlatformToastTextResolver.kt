package dev.subfly.yaba.core.components.toast

import androidx.compose.runtime.Composable
import dev.subfly.yabacore.toast.PlatformToastText

@Composable
expect fun resolveToastText(text: PlatformToastText): String
