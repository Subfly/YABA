@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.folder

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.base.BaseCollectionItemView
import dev.subfly.yaba.core.components.item.base.CollectionMenuAction
import dev.subfly.yaba.core.components.item.base.CollectionSwipeAction
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.FolderCreationRoute
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.move
import yaba.composeapp.generated.resources.new_bookmark
import kotlin.uuid.ExperimentalUuidApi

/**
 * Folder row for list rendering (Home / any LazyColumn usage).
 *
 * This component intentionally renders **only a single row**.
 * Any "folder-in-folder" expansion must be handled by the parent list (state machine),
 * so we can keep the UI lazy and avoid recursive composition.
 */
@Composable
fun FolderItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel,
    parentColors: List<YabaColor> = emptyList(),
    hasChildren: Boolean = false,
    isExpanded: Boolean = false,
    allowsDeletion: Boolean = true,
    onToggleExpanded: () -> Unit = {},
    onClick: (FolderUiModel) -> Unit = {},
    onDeleteFolder: (FolderUiModel) -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    // Check if this is a system folder
    val isSystemFolder = CoreConstants.Folder.isSystemFolder(model.id)

    // Localized strings for menu items
    val newBookmarkText = stringResource(Res.string.new_bookmark)
    val editText = stringResource(Res.string.edit)
    val moveText = stringResource(Res.string.move)
    val deleteText = stringResource(Res.string.delete)

    // Create actions specific to THIS folder model
    // System folders cannot be edited or moved
    val menuActions =
        remember(model.id, isSystemFolder, newBookmarkText, editText, moveText, deleteText) {
            buildList {
                add(
                    CollectionMenuAction(
                        key = "new_bookmark_${model.id}",
                        icon = "bookmark-add-02",
                        text = newBookmarkText,
                        color = YabaColor.CYAN,
                        onClick = {
                            resultStore.setResult(ResultStoreKeys.SELECTED_FOLDER, model)
                            creationNavigator.add(BookmarkCreationRoute())
                            appStateManager.onShowCreationContent()
                        }
                    )
                )
                if (isSystemFolder.not()) {
                    add(
                        CollectionMenuAction(
                            key = "edit_${model.id}",
                            icon = "edit-02",
                            text = editText,
                            color = YabaColor.ORANGE,
                            onClick = {
                                creationNavigator.add(FolderCreationRoute(folderId = model.id))
                                appStateManager.onShowCreationContent()
                            }
                        )
                    )
                    add(
                        CollectionMenuAction(
                            key = "move_${model.id}",
                            icon = "arrow-move-up-right",
                            text = moveText,
                            color = YabaColor.TEAL,
                            onClick = {
                                creationNavigator.add(
                                    FolderSelectionRoute(
                                        mode = FolderSelectionMode.FOLDER_MOVE,
                                        contextFolderId = model.id,
                                        contextBookmarkIds = null,
                                    )
                                )
                                appStateManager.onShowCreationContent()
                            }
                        )
                    )
                }
                if (allowsDeletion) {
                    add(
                        CollectionMenuAction(
                            key = "delete_${model.id}",
                            icon = "delete-02",
                            text = deleteText,
                            color = YabaColor.RED,
                            isDangerous = true,
                            onClick = {
                                deletionDialogManager.send(
                                    DeletionState(
                                        deletionType = DeletionType.FOLDER,
                                        folderToBeDeleted = model,
                                        onConfirm = { onDeleteFolder(model) },
                                    )
                                )
                            }
                        )
                    )
                }
            }
        }

    // System folders cannot be moved or edited via swipe actions
    val leftSwipeActions = remember(model.id, isSystemFolder) {
        buildList {
            if (!isSystemFolder) {
                add(
                    CollectionSwipeAction(
                        key = "MOVE_${model.id}",
                        icon = "arrow-move-up-right",
                        color = YabaColor.TEAL,
                        onClick = {
                            creationNavigator.add(
                                FolderSelectionRoute(
                                    mode = FolderSelectionMode.FOLDER_MOVE,
                                    contextFolderId = model.id,
                                    contextBookmarkIds = null,
                                )
                            )
                            appStateManager.onShowCreationContent()
                        }
                    )
                )
            }
            add(
                CollectionSwipeAction(
                    key = "NEW_${model.id}",
                    icon = "bookmark-add-02",
                    color = YabaColor.BLUE,
                    onClick = {
                        resultStore.setResult(ResultStoreKeys.SELECTED_FOLDER, model)
                        creationNavigator.add(BookmarkCreationRoute())
                        appStateManager.onShowCreationContent()
                    }
                )
            )
        }
    }

    val rightSwipeActions = remember(model.id, isSystemFolder) {
        buildList {
            if (!isSystemFolder) {
                add(
                    CollectionSwipeAction(
                        key = "EDIT_${model.id}",
                        icon = "edit-02",
                        color = YabaColor.ORANGE,
                        onClick = {
                            creationNavigator.add(FolderCreationRoute(folderId = model.id))
                            appStateManager.onShowCreationContent()
                        }
                    )
                )
            }
            if (allowsDeletion) {
                add(
                    CollectionSwipeAction(
                        key = "DELETE_${model.id}",
                        icon = "delete-02",
                        color = YabaColor.RED,
                        onClick = {
                            deletionDialogManager.send(
                                DeletionState(
                                    deletionType = DeletionType.FOLDER,
                                    folderToBeDeleted = model,
                                    onConfirm = { onDeleteFolder(model) },
                                )
                            )
                        }
                    )
                )
            }
        }
    }

    val expandedIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
    )
    val color = remember(model.color) { Color(model.color.iconTintArgb()) }

    BaseCollectionItemView(
        modifier = modifier.fillMaxWidth(),
        label = model.label,
        icon = model.icon,
        color = model.color,
        parentColors = parentColors,
        menuActions = menuActions,
        leftSwipeActions = leftSwipeActions,
        rightSwipeActions = rightSwipeActions,
        onClick = { onClick(model) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(model.bookmarkCount.toString())
                if (hasChildren) {
                    YabaIcon(
                        modifier = Modifier
                            .rotate(expandedIconRotation)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleExpanded),
                        name = "arrow-right-01",
                        color = color,
                    )
                }
            }
        }
    )
}
