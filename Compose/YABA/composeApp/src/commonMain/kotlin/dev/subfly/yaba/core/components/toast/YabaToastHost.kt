package dev.subfly.yaba.core.components.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yabacore.toast.ToastManager

@Composable
fun YabaToastHost(modifier: Modifier = Modifier) {
    val toasts by ToastManager.visibleToasts.collectAsState()
    if (toasts.isEmpty()) return

    val isMobile = Platform == YabaPlatform.ANDROID

    ToastOverlayWindow {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = if (isMobile) Alignment.BottomCenter else Alignment.BottomEnd,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = if (isMobile) Alignment.CenterHorizontally else Alignment.End,
            ) {
                toasts.forEach { toast ->
                    AnimatedVisibility(
                        visible = toast.isVisible,
                        enter = if (isMobile) {
                            slideInVertically(initialOffsetY = { it }) + fadeIn()
                        } else {
                            slideInVertically(initialOffsetY = { it / 2 }) +
                                slideInHorizontally(initialOffsetX = { it / 2 }) +
                                fadeIn()
                        },
                        exit = if (isMobile) {
                            slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        } else {
                            slideOutVertically(targetOffsetY = { it / 2 }) +
                                slideOutHorizontally(targetOffsetX = { it }) +
                                fadeOut()
                        },
                    ) {
                        YabaToast(
                            toast = toast,
                            isMobile = isMobile,
                            onDismissRequest = { ToastManager.dismiss(toast.id) },
                            onAcceptRequest = if (toast.acceptText != null) {
                                { ToastManager.accept(toast.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}
