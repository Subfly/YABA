package dev.subfly.yaba.util

import androidx.compose.runtime.Composable

class NotificationPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
)

@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit,
): NotificationPermissionState
