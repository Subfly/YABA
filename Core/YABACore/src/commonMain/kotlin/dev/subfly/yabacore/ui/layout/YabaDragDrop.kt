@file:OptIn(ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.DropZone
import kotlin.math.max
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/*
Usage sketch:
val dragState = rememberYabaDragDropState { result ->
    when (result.payload) {
        is DragTagPayload -> if (result.target is DragBookmarkPayload) {
            // add tag to bookmark
        } else if (result.target is DragTagPayload) {
            // reorder tag using result.zone (TOP/BOTTOM)
        }
        is DragBookmarkPayload -> when (result.target) {
            is DragFolderPayload -> if (result.zone == DropZone.MIDDLE) {
                // move bookmark into folder
            }
            is DragBookmarkPayload -> {
                // reorder bookmark by zone
            }
        }
        is DragFolderPayload -> if (result.target is DragFolderPayload) {
            // reorder or nest folders (MIDDLE to nest)
        }
    }
}

YabaTagLayout(tags, TagLayoutStyle.Flow, onDrop = {}, dragDropState = dragState) { tag, dragging ->
    /* tag UI */
}
YabaFolderLayout(folders, ContentAppearance.GRID, onDrop = {}, dragDropState = dragState) { folder, dragging, appearance ->
    /* folder UI */
}
YabaBookmarkLayout(bookmarks, ContentAppearance.CARD, onDrop = {}, dragDropState = dragState) { bm, dragging, appearance, imageSize ->
    /* bookmark UI */
}

- Share one YabaDragDropState across layouts to enable cross-type drops.
- Configure grid/list/card via ContentLayoutConfig; grid uses adaptive vertical staggered grid.
*/

sealed interface YabaDragPayload {
    val id: Uuid
}

class DragTagPayload(val tag: TagUiModel) : YabaDragPayload {
    override val id: Uuid = tag.id
    override fun equals(other: Any?): Boolean = other is DragTagPayload && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

class DragFolderPayload(val folder: FolderUiModel) : YabaDragPayload {
    override val id: Uuid = folder.id
    override fun equals(other: Any?): Boolean = other is DragFolderPayload && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

class DragBookmarkPayload(val bookmark: BookmarkUiModel) : YabaDragPayload {
    override val id: Uuid = bookmark.id
    override fun equals(other: Any?): Boolean = other is DragBookmarkPayload && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

data class YabaDropResult(
    val payload: YabaDragPayload,
    val target: YabaDragPayload?,
    val zone: DropZone,
)

/**
 * Shared drag & drop coordinator for list, card, grid, and flow layouts.
 * - Attach `yabaDragSource` / `yabaDropTarget` on items that share the same state to enable
 * cross-type drops (e.g., tag -> bookmark, bookmark -> folder).
 * - DropZone.TOP / BOTTOM map to before / after; when orientation is Horizontal they correspond to
 * left / right. DropZone.MIDDLE is intended for "drop into".
 */
@Stable
class YabaDragDropState(
    private val onDrop: (YabaDropResult) -> Unit,
    private val onDragStart: ((YabaDragPayload) -> Unit)? = null,
    private val onDragEnd: (() -> Unit)? = null,
) {
    private val targets = mutableStateMapOf<YabaDragPayload, TargetMeta>()
    private val sources = mutableStateMapOf<YabaDragPayload, Rect>()

    private data class TargetMeta(
        val bounds: Rect,
        val orientation: Orientation,
    )

    private var draggingPayload: YabaDragPayload? by mutableStateOf(null)
    private var dragPosition: Offset by mutableStateOf(Offset.Unspecified)

    val activePayload: YabaDragPayload?
        get() = draggingPayload

    val currentDragPosition: Offset
        get() = dragPosition

    fun isDragging(payload: YabaDragPayload): Boolean = draggingPayload?.id == payload.id

    internal fun registerTarget(payload: YabaDragPayload, bounds: Rect, orientation: Orientation) {
        targets[payload] = TargetMeta(bounds, orientation)
    }

    internal fun registerSource(payload: YabaDragPayload, bounds: Rect) {
        sources[payload] = bounds
    }

    private fun toRoot(payload: YabaDragPayload, local: Offset): Offset {
        val sourceBounds = sources[payload] ?: return local
        return sourceBounds.topLeft + local
    }

    internal fun startDrag(payload: YabaDragPayload, startLocalPosition: Offset) {
        draggingPayload = payload
        dragPosition = toRoot(payload, startLocalPosition)
        onDragStart?.invoke(payload)
    }

    internal fun updateDrag(localPosition: Offset) {
        val payload = draggingPayload ?: return
        dragPosition = toRoot(payload, localPosition)
    }

    internal fun cancelDrag() {
        draggingPayload = null
        dragPosition = Offset.Unspecified
        onDragEnd?.invoke()
    }

    internal fun finishDrag() {
        val payload = draggingPayload ?: return
        val target = findTarget(payload, dragPosition)
        val zone = target?.second ?: DropZone.NONE
        onDrop(
            YabaDropResult(
                payload = payload,
                target = target?.first,
                zone = zone,
            )
        )
        draggingPayload = null
        dragPosition = Offset.Unspecified
        onDragEnd?.invoke()
    }

    private fun findTarget(
        payload: YabaDragPayload,
        position: Offset,
    ): Pair<YabaDragPayload, DropZone>? {
        var hit: Pair<YabaDragPayload, DropZone>? = null
        targets.forEach { (candidate, meta) ->
            if (candidate.id == payload.id) return@forEach
            if (meta.bounds.contains(position)) {
                val zone = computeZone(meta, position)
                hit = candidate to zone
                return@forEach
            }
        }
        return hit
    }

    private fun computeZone(meta: TargetMeta, position: Offset): DropZone {
        val fraction = if (meta.orientation == Orientation.Vertical) {
            val clamped = (position.y - meta.bounds.top) / max(meta.bounds.height, 1e-3f)
            clamped.coerceIn(0f, 1f)
        } else {
            val clamped = (position.x - meta.bounds.left) / max(meta.bounds.width, 1e-3f)
            clamped.coerceIn(0f, 1f)
        }
        return when {
            fraction < TOP_THRESHOLD -> DropZone.TOP
            fraction > BOTTOM_THRESHOLD -> DropZone.BOTTOM
            else -> DropZone.MIDDLE
        }
    }

    private companion object {
        const val TOP_THRESHOLD = 0.33f
        const val BOTTOM_THRESHOLD = 0.66f
    }
}

@Composable
fun rememberYabaDragDropState(
    onDrop: (YabaDropResult) -> Unit,
    onDragStart: ((YabaDragPayload) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
): YabaDragDropState = remember(onDrop, onDragStart, onDragEnd) {
    YabaDragDropState(
        onDrop = onDrop,
        onDragStart = onDragStart,
        onDragEnd = onDragEnd,
    )
}

fun Modifier.yabaDropTarget(
    state: YabaDragDropState,
    payload: YabaDragPayload,
    orientation: Orientation,
): Modifier = this.onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInRoot()
    state.registerTarget(payload, bounds, orientation)
}

fun Modifier.yabaDragSource(
    state: YabaDragDropState,
    payload: YabaDragPayload,
    enabled: Boolean = true,
): Modifier = if (!enabled) {
    this
} else {
    composed {
        val sourceModifier = Modifier.onGloballyPositioned { coordinates ->
            state.registerSource(payload, coordinates.boundsInRoot())
        }.pointerInput(state, payload) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    state.startDrag(payload, offset)
                },
                onDragEnd = { state.finishDrag() },
                onDragCancel = { state.cancelDrag() },
                onDrag = { change, _ ->
                    state.updateDrag(change.position)
                    change.consume()
                }
            )
        }
        this.then(sourceModifier)
    }
}
