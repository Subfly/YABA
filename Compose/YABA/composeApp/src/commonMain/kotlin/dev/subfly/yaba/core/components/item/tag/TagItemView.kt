@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.base.BaseCollectionItemView
import dev.subfly.yaba.core.components.item.base.CollectionMenuAction
import dev.subfly.yaba.core.components.item.base.CollectionSwipeAction
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.new_bookmark
import kotlin.uuid.ExperimentalUuidApi

/**
 * Tag item view that adapts to different appearances.
 * Uses [BaseCollectionItemView] for consistent styling with folders.
 *
 * @param model The tag data to display
 * @param appearance The display mode (LIST/CARD or GRID)
 * @param onDeleteTag Callback when the tag should be deleted
 */
@Composable
fun TagItemView(
    modifier: Modifier = Modifier,
    model: TagUiModel,
    appearance: ContentAppearance,
    onDeleteTag: (TagUiModel) -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current

    val color by remember(model) {
        mutableStateOf(Color(model.color.iconTintArgb()))
    }

    // Localized strings for menu items
    val newBookmarkText = stringResource(Res.string.new_bookmark)
    val editText = stringResource(Res.string.edit)
    val deleteText = stringResource(Res.string.delete)

    val menuActions = remember(model, newBookmarkText, editText, deleteText) {
        listOf(
            CollectionMenuAction(
                key = "new_bookmark",
                icon = "bookmark-add-02",
                text = newBookmarkText,
                color = YabaColor.CYAN,
                onClick = {
                    // TODO: ADD FUNCTIONALITY TO HAVE BASE CONTEXT
                    creationNavigator.add(BookmarkCreationRoute(bookmarkId = null))
                    appStateManager.onShowSheet()
                }
            ),
            CollectionMenuAction(
                key = "edit",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE,
                onClick = {
                    creationNavigator.add(TagCreationRoute(tagId = model.id.toString()))
                    appStateManager.onShowSheet()
                }
            ),
            CollectionMenuAction(
                key = "delete",
                icon = "delete-02",
                text = deleteText,
                color = YabaColor.RED,
                isDangerous = true,
                onClick = {
                    deletionDialogManager.send(
                        DeletionState(
                            deletionType = DeletionType.TAG,
                            tagToBeDeleted = model,
                            onConfirm = { onDeleteTag(model) },
                        )
                    )
                }
            ),
        )
    }

    val leftSwipeActions = remember(model) {
        listOf(
            CollectionSwipeAction(
                key = "NEW",
                icon = "bookmark-add-02",
                color = YabaColor.BLUE,
                onClick = {
                    creationNavigator.add(BookmarkCreationRoute(bookmarkId = null))
                    appStateManager.onShowSheet()
                }
            ),
        )
    }

    val rightSwipeActions = remember(model) {
        listOf(
            CollectionSwipeAction(
                key = "EDIT",
                icon = "edit-02",
                color = YabaColor.ORANGE,
                onClick = {
                    creationNavigator.add(TagCreationRoute(tagId = model.id.toString()))
                    appStateManager.onShowSheet()
                }
            ),
            CollectionSwipeAction(
                key = "DELETE",
                icon = "delete-02",
                color = YabaColor.RED,
                onClick = {
                    deletionDialogManager.send(
                        DeletionState(
                            deletionType = DeletionType.TAG,
                            tagToBeDeleted = model,
                            onConfirm = { onDeleteTag(model) },
                        )
                    )
                }
            ),
        )
    }

    BaseCollectionItemView(
        modifier = modifier,
        label = model.label,
        description = null,
        icon = model.icon,
        color = model.color,
        appearance = appearance,
        menuActions = menuActions,
        leftSwipeActions = leftSwipeActions,
        rightSwipeActions = rightSwipeActions,
        onClick = {
            // TODO: NAVIGATE TO TAG DETAIL
        },
        listTrailingContent = {
            Text(model.bookmarkCount.toString())
        },
        gridTrailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(model.bookmarkCount.toString())
                YabaIcon(
                    name = "bookmark-02",
                    color = color,
                )
            }
        }
    )
}
