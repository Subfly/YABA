@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.tag

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.subfly.yaba.core.components.item.base.BaseCollectionItemView
import dev.subfly.yaba.core.components.item.base.CollectionMenuAction
import dev.subfly.yaba.core.components.item.base.CollectionSwipeAction
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.new_bookmark
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
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    // Check if this is a system tag
    val isSystemTag = CoreConstants.Tag.isSystemTag(model.id)

    // Localized strings for menu items
    val newBookmarkText = stringResource(Res.string.new_bookmark)
    val editText = stringResource(Res.string.edit)
    val deleteText = stringResource(Res.string.delete)

    // System tags cannot be edited
    val menuActions =
        remember(model, isSystemTag, newBookmarkText, editText, deleteText) {
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
                if (!isSystemTag) {
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
                }
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

    val leftSwipeActions =
        remember(model) {
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

    // System tags cannot be edited via swipe actions
    val rightSwipeActions =
        remember(model, isSystemTag) {
            buildList {
                if (!isSystemTag) {
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
                }
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

    BaseCollectionItemView(
        modifier = modifier,
        label = model.label,
        icon = model.icon,
        color = model.color,
        menuActions = menuActions,
        leftSwipeActions = leftSwipeActions,
        rightSwipeActions = rightSwipeActions,
        onClick = { onClick(model) },
        trailingContent = { Text(model.bookmarkCount.toString()) },
        index = index,
        count = count,
        containerColor = containerColor,
    )
}
