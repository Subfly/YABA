package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable

@Composable
fun BookmarkPinToggleRow(
        isPinned: Boolean,
        enabled: Boolean,
        onClick: () -> Unit,
) {
    BookmarkCreationLabel(
            iconName = if (isPinned) "pin-off" else "pin",
            label = if (isPinned) "Unpin" else "Pin",
            extraContent = {
                Switch(
                        checked = isPinned,
                        onCheckedChange = { newChecked -> if (newChecked != isPinned) onClick() },
                        enabled = enabled,
                )
            },
    )
}
