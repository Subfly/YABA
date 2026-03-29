package dev.subfly.yaba.core.components.toast

import androidx.compose.runtime.Composable
import dev.subfly.yabacore.toast.PlatformToastText
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun resolveToastText(text: PlatformToastText, formatArgs: List<Any>): String =
    if (formatArgs.isEmpty()) stringResource(text)
    else stringResource(text, *formatArgs.toTypedArray())
