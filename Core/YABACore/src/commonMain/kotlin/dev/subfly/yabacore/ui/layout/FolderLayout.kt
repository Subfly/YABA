@file:OptIn(ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.ui.FolderUiModel
import kotlin.uuid.ExperimentalUuidApi

/**
 * Layout for displaying folders in a list format.
 * 
 * Note: Folders always use LIST appearance as GRID view is not supported
 * for collections that can have nested children (folder-in-folder).
 */
@Composable
fun YabaFolderLayout(
    folders: List<FolderUiModel>,
    layoutConfig: ContentLayoutConfig,
    onDrop: (YabaDropResult) -> Unit,
    modifier: Modifier = Modifier,
    dragDropState: YabaDragDropState = rememberYabaDragDropState(onDrop),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (
        folder: FolderUiModel,
        isDragging: Boolean,
    ) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
        contentPadding = contentPadding,
    ) {
        items(
            items = folders,
            key = { "${it.id} ${it.label}" },
        ) { folder ->
            FolderItem(
                folder = folder,
                state = dragDropState,
                orientation = Orientation.Vertical,
                modifier = Modifier.animateItem(),
                itemContent = itemContent,
            )
        }
    }
}

@Composable
private fun FolderItem(
    folder: FolderUiModel,
    state: YabaDragDropState,
    orientation: Orientation,
    modifier: Modifier = Modifier,
    itemContent: @Composable (
        folder: FolderUiModel,
        isDragging: Boolean,
    ) -> Unit,
) {
    val payload = remember(folder.id) { DragFolderPayload(folder) }
    val isDragging = state.isDragging(payload)

    Box(
        modifier = modifier
            .yabaDropTarget(state, payload, orientation)
            .yabaDragSource(state, payload),
    ) {
        itemContent(
            folder,
            isDragging,
        )
    }
}
