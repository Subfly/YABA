package dev.subfly.yaba.core.components.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.toast.ToastManager

@Composable
fun YabaToastHost(modifier: Modifier = Modifier) {
    val toasts by ToastManager.visibleToasts.collectAsState()
    if (toasts.isEmpty()) return

    ToastOverlayWindow {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { ToastManager.dismissAll() },
                    ),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                toasts.forEach { toast ->
                    AnimatedVisibility(
                        visible = toast.isVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        YabaToast(
                            toast = toast,
                            isMobile = true,
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
