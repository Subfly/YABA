package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit,
): NotificationPermissionState {
    return remember {
        NotificationPermissionState(
            hasPermission = false,
            requestPermission = { onResult(false) },
        )
    }
}
