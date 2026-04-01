package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Last row in the bookmark info section: private toggle using [BookmarkCreationLabel] + Material 3 [Switch]. */
@Composable
fun BookmarkPrivateToggleRow(
    isPrivate: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    BookmarkCreationLabel(
        iconName = if (isPrivate) "circle-unlock-02" else "circle-lock-02",
        label = if (isPrivate) "Not Private" else "Private",
        extraContent = {
            Switch(
                checked = isPrivate,
                onCheckedChange = { newChecked ->
                    if (newChecked != isPrivate) onClick()
                },
                enabled = enabled,
            )
        },
    )
}
