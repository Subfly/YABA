package dev.subfly.yaba.core.components.toast

import androidx.compose.runtime.Composable
import dev.subfly.yabacore.toast.PlatformToastText

@Composable
expect fun resolveToastText(text: PlatformToastText, formatArgs: List<Any> = emptyList()): String
