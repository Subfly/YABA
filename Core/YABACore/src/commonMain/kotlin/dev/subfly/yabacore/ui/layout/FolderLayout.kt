@file:OptIn(ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.ContentAppearance
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun YabaFolderLayout(
    folders: List<FolderUiModel>,
    appearance: ContentAppearance,
    onDrop: (YabaDropResult) -> Unit,
    modifier: Modifier = Modifier,
    dragDropState: YabaDragDropState = rememberYabaDragDropState(onDrop),
    layoutConfig: ContentLayoutConfig = ContentLayoutConfig(appearance = appearance),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (
        folder: FolderUiModel,
        isDragging: Boolean,
        appearance: ContentAppearance,
    ) -> Unit,
) {
    val resolvedAppearance =
        when (appearance) {
            ContentAppearance.CARD -> ContentAppearance.LIST
            else -> appearance
        }
    when (resolvedAppearance) {
        ContentAppearance.LIST, ContentAppearance.CARD ->
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(layoutConfig.list.itemSpacing),
                contentPadding = contentPadding,
            ) {
                items(
                    items = folders,
                    key = { "${it.id} ${it.label}" },
                ) { folder ->
                    FolderItem(
                        folder = folder,
                        appearance = resolvedAppearance,
                        state = dragDropState,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.animateItem(),
                        itemContent = itemContent,
                    )
                }
            }

        ContentAppearance.GRID ->
            LazyVerticalStaggeredGrid(
                modifier = modifier,
                columns = StaggeredGridCells.Adaptive(layoutConfig.grid.minCellWidth),
                verticalItemSpacing = layoutConfig.grid.verticalSpacing,
                horizontalArrangement =
                    Arrangement.spacedBy(layoutConfig.grid.horizontalSpacing),
                contentPadding = contentPadding,
            ) {
                items(
                    items = folders,
                    key = { "${it.id} ${it.label}" },
                ) { folder ->
                    FolderItem(
                        folder = folder,
                        appearance = resolvedAppearance,
                        state = dragDropState,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.animateItem(),
                        itemContent = itemContent,
                    )
                }
            }
    }
}

@Composable
private fun FolderItem(
    folder: FolderUiModel,
    appearance: ContentAppearance,
    state: YabaDragDropState,
    orientation: Orientation,
    modifier: Modifier = Modifier,
    itemContent: @Composable (
        folder: FolderUiModel,
        isDragging: Boolean,
        appearance: ContentAppearance,
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
            appearance
        )
    }
}
