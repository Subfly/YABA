package dev.subfly.yaba.core.components.item.annotation

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.subfly.yaba.core.components.item.base.BaseAnnotationItemView
import dev.subfly.yaba.core.components.item.base.CollectionMenuAction
import dev.subfly.yaba.core.components.item.base.CollectionSwipeAction
import dev.subfly.yaba.core.model.ui.AnnotationUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

/**
 * Interactive annotation row: swipe (edit/delete), overflow menu, and primary [onPress].
 */
@Composable
fun AnnotationItemView(
    modifier: Modifier = Modifier,
    model: AnnotationUiModel,
    index: Int,
    count: Int,
    onPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val editText = stringResource(R.string.edit)
    val deleteText = stringResource(R.string.delete)

    val menuActions =
        remember(model.id, editText, deleteText, onEdit, onDelete) {
            listOf(
                CollectionMenuAction(
                    key = "edit_${model.id}",
                    icon = "edit-02",
                    text = editText,
                    color = YabaColor.ORANGE,
                    onClick = onEdit,
                ),
                CollectionMenuAction(
                    key = "delete_${model.id}",
                    icon = "delete-02",
                    text = deleteText,
                    color = YabaColor.RED,
                    isDangerous = true,
                    onClick = onDelete,
                ),
            )
        }

    val rightSwipeActions =
        remember(model.id, onEdit, onDelete) {
            listOf(
                CollectionSwipeAction(
                    key = "edit_annotation_${model.id}",
                    icon = "edit-02",
                    color = YabaColor.ORANGE,
                    onClick = onEdit,
                ),
                CollectionSwipeAction(
                    key = "delete_annotation_${model.id}",
                    icon = "delete-02",
                    color = YabaColor.RED,
                    onClick = onDelete,
                ),
            )
        }

    BaseAnnotationItemView(
        modifier = modifier,
        model = model,
        menuActions = menuActions,
        rightSwipeActions = rightSwipeActions,
        interactive = true,
        onClick = onPress,
        index = index,
        count = count,
        containerColor = containerColor,
    )
}

/**
 * Read-only annotation row for previews (no swipe, tap, or long-press).
 */
@Composable
fun AnnotationPreviewItemView(
    modifier: Modifier = Modifier,
    model: AnnotationUiModel,
    index: Int = 0,
    count: Int = 1,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    BaseAnnotationItemView(
        modifier = modifier,
        model = model,
        interactive = false,
        index = index,
        count = count,
        containerColor = containerColor,
    )
}
