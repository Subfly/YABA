@file:OptIn(ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.ui.TagUiModel
import kotlin.uuid.ExperimentalUuidApi

enum class TagLayoutStyle {
    Flow,
    Horizontal,
}

@Composable
fun YabaTagLayout(
        tags: List<TagUiModel>,
        layoutStyle: TagLayoutStyle,
        onDrop: (YabaDropResult) -> Unit,
        modifier: Modifier = Modifier,
        dragDropState: YabaDragDropState = rememberYabaDragDropState(onDrop),
        horizontalSpacing: Dp = 8.dp,
        verticalSpacing: Dp = 8.dp,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        itemContent: @Composable (tag: TagUiModel, isDragging: Boolean) -> Unit,
) {
    when (layoutStyle) {
        TagLayoutStyle.Flow ->
                FlowRow(
                        modifier = modifier.padding(contentPadding),
                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                ) {
                    tags.forEach { tag ->
                        TagItem(
                                tag = tag,
                                state = dragDropState,
                                orientation = Orientation.Horizontal,
                                modifier = Modifier,
                                itemContent = itemContent,
                        )
                    }
                }
        TagLayoutStyle.Horizontal ->
                LazyRow(
                        modifier = modifier,
                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                        contentPadding = contentPadding,
                ) {
                    items(
                            items = tags,
                            key = { "${it.id} ${it.label}" },
                    ) { tag ->
                        TagItem(
                                tag = tag,
                                state = dragDropState,
                                orientation = Orientation.Horizontal,
                                modifier = Modifier.animateItem(),
                                itemContent = itemContent,
                        )
                    }
                }
    }
}

@Composable
private fun TagItem(
        tag: TagUiModel,
        state: YabaDragDropState,
        orientation: Orientation,
        modifier: Modifier,
        itemContent: @Composable (tag: TagUiModel, isDragging: Boolean) -> Unit,
) {
    val payload = remember(tag.id) { DragTagPayload(tag) }
    val isDragging = state.isDragging(payload)

    Box(
            modifier =
                    modifier.yabaDropTarget(state, payload, orientation)
                            .yabaDragSource(state, payload),
    ) { itemContent(tag, isDragging) }
}
