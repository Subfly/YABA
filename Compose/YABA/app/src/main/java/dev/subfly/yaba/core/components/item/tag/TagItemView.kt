@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.tag

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.subfly.yaba.R
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
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.YabaColor
import kotlin.uuid.ExperimentalUuidApi

/**
 * Tag item view using list appearance. Uses [BaseCollectionItemView] for consistent styling with
 * folders.
 *
 * Note: Tags always use LIST appearance as GRID view is not supported for collections.
 *
 * @param model The tag data to display
 * @param onDeleteTag Callback when the tag should be deleted
 */
@Composable
fun TagItemView(
    modifier: Modifier = Modifier,
    model: TagUiModel,
    allowsDeletion: Boolean = true,
    onClick: (TagUiModel) -> Unit = {},
    onDeleteTag: (TagUiModel) -> Unit,
    index: Int = 0,
    count: Int = 1,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    showBookmarkCounts: Boolean = true,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    // System tags cannot be edited, deleted, or opened via overflow/swipe — tap only.
    val isSystemTag = CoreConstants.Tag.isSystemTag(model.id)

    // Localized strings for menu items
    val newBookmarkText = stringResource(R.string.new_bookmark)
    val editText = stringResource(R.string.edit)
    val deleteText = stringResource(R.string.delete)

    val menuActions =
        remember(model, isSystemTag, allowsDeletion, newBookmarkText, editText, deleteText) {
            if (isSystemTag) {
                emptyList()
            } else {
                buildList {
                    add(
                        CollectionMenuAction(
                            key = "new_bookmark",
                            icon = "bookmark-add-02",
                            text = newBookmarkText,
                            color = YabaColor.CYAN,
                            onClick = {
                                resultStore.setResult(ResultStoreKeys.SELECTED_TAGS, listOf(model))
                                creationNavigator.add(BookmarkCreationRoute())
                                appStateManager.onShowCreationContent()
                            }
                        )
                    )
                    add(
                        CollectionMenuAction(
                            key = "edit",
                            icon = "edit-02",
                            text = editText,
                            color = YabaColor.ORANGE,
                            onClick = {
                                creationNavigator.add(
                                    TagCreationRoute(tagId = model.id)
                                )
                                appStateManager.onShowCreationContent()
                            }
                        )
                    )
                    if (allowsDeletion) {
                        add(
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
                            )
                        )
                    }
                }
            }
        }

    val leftSwipeActions =
        remember(model, isSystemTag) {
            if (isSystemTag) {
                emptyList()
            } else {
                listOf(
                    CollectionSwipeAction(
                        key = "NEW",
                        icon = "bookmark-add-02",
                        color = YabaColor.BLUE,
                        onClick = {
                            resultStore.setResult(ResultStoreKeys.SELECTED_TAGS, listOf(model))
                            creationNavigator.add(BookmarkCreationRoute())
                            appStateManager.onShowCreationContent()
                        }
                    ),
                )
            }
        }

    val rightSwipeActions =
        remember(model, isSystemTag, allowsDeletion) {
            if (isSystemTag) {
                emptyList()
            } else {
                buildList {
                    add(
                        CollectionSwipeAction(
                            key = "EDIT",
                            icon = "edit-02",
                            color = YabaColor.ORANGE,
                            onClick = {
                                creationNavigator.add(
                                    TagCreationRoute(tagId = model.id)
                                )
                                appStateManager.onShowCreationContent()
                            }
                        )
                    )
                    if (allowsDeletion) {
                        add(
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
                            )
                        )
                    }
                }
            }
        }

    BaseCollectionItemView(
        modifier = modifier,
        label = model.label,
        icon = model.icon,
        color = model.color,
        menuActions = menuActions,
        leftSwipeActions = leftSwipeActions,
        rightSwipeActions = rightSwipeActions,
        onClick = { onClick(model) },
        trailingContent = {
            if (showBookmarkCounts) {
                Text(model.bookmarkCount.toString())
            } else Box(modifier = Modifier)
        },
        index = index,
        count = count,
        containerColor = containerColor,
        enableContextMenuInteractions = !isSystemTag,
    )
}
