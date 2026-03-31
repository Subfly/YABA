package dev.subfly.yaba.core.state.creation.notemark

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.BookmarkUiModel

@Immutable
data class NotemarkMentionCreationUIState(
    val mentionText: String = "",
    val selectedBookmarkId: String? = null,
    val selectedBookmark: BookmarkUiModel? = null,
    val isEdit: Boolean = false,
    val editPos: Int? = null,
)
