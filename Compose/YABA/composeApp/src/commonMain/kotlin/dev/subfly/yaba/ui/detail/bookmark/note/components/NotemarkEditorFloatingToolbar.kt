package dev.subfly.yaba.ui.detail.bookmark.note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderFloatingToolbarColors
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors
import dev.subfly.yabacore.model.utils.NoteSaveMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon

private val toolbarOffset = 8.dp

/**
 * Note editor chrome: highlight + save (manual mode). Further formatting groups: TODO placeholders.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BoxScope.NotemarkEditorFloatingToolbar(
    modifier: Modifier = Modifier,
    color: YabaColor,
    isVisible: Boolean,
    saveMode: NoteSaveMode,
    isDirty: Boolean,
    isSaving: Boolean,
    canCreateHighlight: Boolean,
    onHighlightClick: () -> Unit,
    onManualSaveClick: () -> Unit,
) {
    val toolbarColors = bookmarkReaderFloatingToolbarColors(color)

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = -toolbarOffset)
            .then(modifier)
            .zIndex(1f),
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = toolbarColors,
        ) {
            // TODO(formatt): bold / italic / headings group
            // TODO(formatt): lists & task list group
            // TODO(formatt): insert link / media group

            IconButton(
                onClick = onHighlightClick,
                enabled = canCreateHighlight,
                colors = bookmarkReaderToolbarIconButtonColors(color),
                shapes = IconButtonDefaults.shapes(),
            ) {
                YabaIcon(name = "highlighter", color = Color.White)
            }

            if (saveMode == NoteSaveMode.MANUAL) {
                IconButton(
                    onClick = onManualSaveClick,
                    enabled = isDirty && !isSaving,
                    colors = bookmarkReaderToolbarIconButtonColors(color),
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    YabaIcon(name = "tick-02", color = Color.White)
                }
            }
        }
    }
}
