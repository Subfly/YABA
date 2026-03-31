package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared preview section used as the first element in all bookmark creation flows.
 * Wraps type-specific preview content (link card, image picker, PDF picker) with a consistent
 * label and optional extra content (e.g. cycle appearance, clear button).
 */
@Composable
fun BookmarkPreviewContent(
    label: String,
    iconName: String,
    extraContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    BookmarkCreationLabel(
        label = label,
        iconName = iconName,
        extraContent = extraContent,
    )
    Spacer(modifier = Modifier.height(12.dp))
    content()
}
