@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.folder

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.subfly.yabacore.model.utils.ContentAppearance
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

@Composable
fun FolderItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel,
    appearance: ContentAppearance,
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

    val menuActions = remember(model, newBookmarkText, editText, moveText, deleteText) {
        listOf(
            CollectionMenuAction(
                key = "new_bookmark",
                icon = "bookmark-add-02",
                text = newBookmarkText,
                color = YabaColor.CYAN,
                onClick = {
                    creationNavigator.add(BookmarkCreationRoute())
                    appStateManager.onShowCreationContent()
                }
            ),
            CollectionMenuAction(
                key = "edit",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE,
                onClick = {
                    creationNavigator.add(FolderCreationRoute(folderId = model.id.toString()))
                    appStateManager.onShowCreationContent()
                }
            ),
            CollectionMenuAction(
                key = "move",
                icon = "arrow-move-up-right",
                text = moveText,
                color = YabaColor.TEAL,
                onClick = {
                    creationNavigator.add(
                        FolderSelectionRoute(
                            mode = FolderSelectionMode.PARENT_SELECTION,
                            contextFolderId = model.id.toString(),
                        )
                    )
                    appStateManager.onShowCreationContent()
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
                            deletionType = DeletionType.FOLDER,
                            folderToBeDeleted = model,
                            onConfirm = { onDeleteFolder(model) },
                        )
                    )
                }
            ),
        )
    }

    val leftSwipeActions = remember(model) {
        listOf(
            CollectionSwipeAction(
                key = "MOVE",
                icon = "arrow-move-up-right",
                color = YabaColor.TEAL,
                onClick = {
                    creationNavigator.add(
                        FolderSelectionRoute(
                            mode = FolderSelectionMode.PARENT_SELECTION,
                            contextFolderId = model.id.toString(),
                        )
                    )
                    appStateManager.onShowCreationContent()
                }
            ),
            CollectionSwipeAction(
                key = "NEW",
                icon = "bookmark-add-02",
                color = YabaColor.BLUE,
                onClick = {
                    creationNavigator.add(BookmarkCreationRoute())
                    appStateManager.onShowCreationContent()
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
                    creationNavigator.add(FolderCreationRoute(folderId = model.id.toString()))
                    appStateManager.onShowCreationContent()
                }
            ),
            CollectionSwipeAction(
                key = "DELETE",
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
        ContentAppearance.LIST, ContentAppearance.CARD -> {
            FolderListItemView(
                modifier = modifier,
                model = model,
                menuActions = menuActions,
                leftSwipeActions = leftSwipeActions,
                rightSwipeActions = rightSwipeActions,
                onDeleteFolder = onDeleteFolder,
            )
        }

        ContentAppearance.GRID -> {
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
 */
@Composable
private fun FolderListItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel,
    menuActions: List<CollectionMenuAction>,
    leftSwipeActions: List<CollectionSwipeAction>,
    rightSwipeActions: List<CollectionSwipeAction>,
    onDeleteFolder: (FolderUiModel) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val expandedIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90F else 0f,
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
        BaseCollectionItemView(
            label = model.label,
            description = model.description,
            icon = model.icon,
            color = model.color,
            appearance = ContentAppearance.LIST,
            menuActions = menuActions,
            leftSwipeActions = leftSwipeActions,
            rightSwipeActions = rightSwipeActions,
            onClick = {
                if (model.children.isNotEmpty()) {
                    isExpanded = !isExpanded
                } else {
                    // TODO: NAVIGATE FOLDER DETAIL
                }
            },
            listTrailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(model.bookmarkCount.toString())
                    if (model.children.isNotEmpty()) {
                        YabaIcon(
                            modifier = Modifier.rotate(expandedIconRotation),
                            name = "arrow-right-01",
                            color = color,
                        )
                    }
                }
            }
        )

        // Render children if expanded
        if (model.children.isNotEmpty() && isExpanded) {
            model.children.forEach { childModel ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    FolderListItemView(
                        model = childModel,
                        menuActions = menuActions,
                        leftSwipeActions = leftSwipeActions,
                        rightSwipeActions = rightSwipeActions,
                        onDeleteFolder = { onDeleteFolder(childModel) },
                    )
                }
            }
        }
    }
}
