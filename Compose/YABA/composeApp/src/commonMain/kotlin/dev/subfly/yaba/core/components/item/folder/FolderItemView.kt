@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.folder

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.CollectionAppearance
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
 * Entry point for folder item rendering.
 * For top-level folders, pass an empty parentColors list.
 */
@Composable
fun FolderItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel,
    appearance: CollectionAppearance,
    parentColors: List<YabaColor> = emptyList(),
    onDeleteFolder: (FolderUiModel) -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current

    // Localized strings for menu items
    val newBookmarkText = stringResource(Res.string.new_bookmark)
    val editText = stringResource(Res.string.edit)
    val moveText = stringResource(Res.string.move)
    val deleteText = stringResource(Res.string.delete)

    // Create actions specific to THIS folder model
    val menuActions = remember(model.id, newBookmarkText, editText, moveText, deleteText) {
        listOf(
            CollectionMenuAction(
                key = "new_bookmark_${model.id}",
                icon = "bookmark-add-02",
                text = newBookmarkText,
                color = YabaColor.CYAN,
                onClick = {
                    creationNavigator.add(BookmarkCreationRoute())
                    appStateManager.onShowCreationContent()
                }
            ),
            CollectionMenuAction(
                key = "edit_${model.id}",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE,
                onClick = {
                    creationNavigator.add(FolderCreationRoute(folderId = model.id.toString()))
                    appStateManager.onShowCreationContent()
                }
            ),
            CollectionMenuAction(
                key = "move_${model.id}",
                icon = "arrow-move-up-right",
                text = moveText,
                color = YabaColor.TEAL,
                onClick = {
                    creationNavigator.add(
                        FolderSelectionRoute(
                            mode = FolderSelectionMode.FOLDER_MOVE,
                            contextFolderId = model.id.toString(),
                            contextBookmarkId = null,
                        )
                    )
                    appStateManager.onShowCreationContent()
                }
            ),
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
            ),
        )
    }

    val leftSwipeActions = remember(model.id) {
        listOf(
            CollectionSwipeAction(
                key = "MOVE_${model.id}",
                icon = "arrow-move-up-right",
                color = YabaColor.TEAL,
                onClick = {
                    creationNavigator.add(
                        FolderSelectionRoute(
                            mode = FolderSelectionMode.FOLDER_MOVE,
                            contextFolderId = model.id.toString(),
                            contextBookmarkId = null,
                        )
                    )
                    appStateManager.onShowCreationContent()
                }
            ),
            CollectionSwipeAction(
                key = "NEW_${model.id}",
                icon = "bookmark-add-02",
                color = YabaColor.BLUE,
                onClick = {
                    creationNavigator.add(BookmarkCreationRoute())
                    appStateManager.onShowCreationContent()
                }
            ),
        )
    }

    val rightSwipeActions = remember(model.id) {
        listOf(
            CollectionSwipeAction(
                key = "EDIT_${model.id}",
                icon = "edit-02",
                color = YabaColor.ORANGE,
                onClick = {
                    creationNavigator.add(FolderCreationRoute(folderId = model.id.toString()))
                    appStateManager.onShowCreationContent()
                }
            ),
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
            ),
        )
    }

    when (appearance) {
        CollectionAppearance.LIST -> {
            FolderListItemView(
                modifier = modifier,
                model = model,
                appearance = appearance,
                parentColors = parentColors,
                menuActions = menuActions,
                leftSwipeActions = leftSwipeActions,
                rightSwipeActions = rightSwipeActions,
                onDeleteFolder = onDeleteFolder,
            )
        }

        CollectionAppearance.GRID -> {
            val color by remember(model) {
                mutableStateOf(Color(model.color.iconTintArgb()))
            }

            BaseCollectionItemView(
                modifier = modifier,
                label = model.label,
                description = model.description,
                icon = model.icon,
                color = model.color,
                appearance = appearance,
                menuActions = menuActions,
                onClick = {
                    // TODO: NAVIGATE FOLDER DETAIL
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
    }
}

/**
 * List view for folders with support for expandable children.
 * Parent color bars are rendered inside BaseCollectionItemView.
 */
@Composable
private fun FolderListItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel,
    appearance: CollectionAppearance,
    parentColors: List<YabaColor>,
    menuActions: List<CollectionMenuAction>,
    leftSwipeActions: List<CollectionSwipeAction>,
    rightSwipeActions: List<CollectionSwipeAction>,
    onDeleteFolder: (FolderUiModel) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val expandedIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
    )
    val color by remember(model) {
        mutableStateOf(Color(model.color.iconTintArgb()))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Main folder item - parent colors are rendered inside BaseCollectionItemView
        BaseCollectionItemView(
            label = model.label,
            description = model.description,
            icon = model.icon,
            color = model.color,
            appearance = CollectionAppearance.LIST,
            parentColors = parentColors,
            menuActions = menuActions,
            leftSwipeActions = leftSwipeActions,
            rightSwipeActions = rightSwipeActions,
            onClick = {
                // TODO: NAVIGATE TO FOLDER DETAIL
            },
            listTrailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(model.bookmarkCount.toString())
                    if (model.children.isNotEmpty()) {
                        YabaIcon(
                            modifier = Modifier
                                .rotate(expandedIconRotation)
                                .clip(CircleShape)
                                .clickable(onClick = {isExpanded = !isExpanded }),
                            name = "arrow-right-01",
                            color = color,
                        )
                    }
                }
            }
        )

        // Render children if expanded - each child is a full FolderItemView with its own actions
        if (model.children.isNotEmpty() && isExpanded) {
            model.children.forEach { childModel ->
                // Pass current folder's color to the child's parent colors
                FolderItemView(
                    model = childModel,
                    appearance = appearance,
                    parentColors = parentColors + model.color,
                    onDeleteFolder = onDeleteFolder,
                )
            }
        }
    }
}
