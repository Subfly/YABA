@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.folder

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.FolderCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
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
    if (appearance == ContentAppearance.LIST) {
        ListFolderItemView(modifier, model, onDeleteFolder)
    } else {
        GridFolderItemView(modifier, model)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListFolderItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel,
    onDeleteFolder: (FolderUiModel) -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current

    var isExpanded by remember { mutableStateOf(false) }
    var isOptionsExpanded by remember { mutableStateOf(false) }
    val expandedIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90F else 0f,
    )
    val color by remember(model) {
        mutableStateOf(Color(model.color.iconTintArgb()))
    }

    Box {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            YabaSwipeActions(
                modifier = Modifier.padding(horizontal = 12.dp),
                actionSpacing = 0.dp,
                leftActions = listOf(
                    SwipeAction(
                        key = "MOVE",
                        onClick = {
                            // TODO: OPEN MOVE SHEET WITH THIS SHIT
                            println("LELE: MOVE")
                        },
                        content = {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = Color(YabaColor.TEAL.iconTintArgb())
                            ) {
                                YabaIcon(
                                    modifier = Modifier.padding(12.dp),
                                    name = "arrow-move-up-right",
                                    color = Color.White,
                                )
                            }
                        }
                    ),
                    SwipeAction(
                        key = "NEW",
                        onClick = {
                            creationNavigator.add(BookmarkCreationRoute(bookmarkId = null))
                            appStateManager.onShowSheet()
                        },
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color(YabaColor.BLUE.iconTintArgb())
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(12.dp),
                                name = "bookmark-add-02",
                                color = Color.White,
                            )
                        }
                    }
                ),
                rightActions = listOf(
                    SwipeAction(
                        key = "EDIT",
                        onClick = {
                            creationNavigator.add(
                                FolderCreationRoute(folderId = model.id.toString())
                            )
                            appStateManager.onShowSheet()
                        },
                        content = {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = Color(YabaColor.ORANGE.iconTintArgb())
                            ) {
                                YabaIcon(
                                    modifier = Modifier.padding(12.dp),
                                    name = "edit-02",
                                    color = Color.White,
                                )
                            }
                        }
                    ),
                    SwipeAction(
                        key = "DELETE",
                        onClick = {
                            deletionDialogManager.send(
                                DeletionState(
                                    deletionType = DeletionType.FOLDER,
                                    folderToBeDeleted = model,
                                    onConfirm = { onDeleteFolder(model) },
                                )
                            )
                        },
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color(YabaColor.RED.iconTintArgb())
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(12.dp),
                                name = "delete-02",
                                color = Color.White,
                            )
                        }
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onLongClick = { isOptionsExpanded = true },
                                onClick = {
                                    // TODO: NAVIGATE FOLDER DETAIL
                                }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.1F)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 24.dp,
                                    vertical = 18.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                YabaIcon(
                                    name = model.icon,
                                    color = color,
                                )
                                Text(model.label)
                            }
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
                    }
                }
            }

            if (model.children.isNotEmpty() && isExpanded) {
                model.children.fastForEachIndexed { _, childModel ->
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
                        ListFolderItemView(
                            model = childModel,
                            onDeleteFolder = { onDeleteFolder(childModel) },
                        )
                    }
                }
            }
        }

        DropdownMenuPopup(
            modifier = modifier,
            expanded = isOptionsExpanded,
            onDismissRequest = { isOptionsExpanded = false },
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = 3
                )
            ) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 3),
                    checked = false,
                    onCheckedChange = { _ ->
                        isOptionsExpanded = false
                        creationNavigator.add(BookmarkCreationRoute(bookmarkId = null))
                        appStateManager.onShowSheet()
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "bookmark-add-02",
                            color = YabaColor.CYAN,
                        )
                    },
                    text = { Text(text = stringResource(Res.string.new_bookmark)) }
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 3),
                    checked = false,
                    onCheckedChange = { _ ->
                        isOptionsExpanded = false
                        creationNavigator.add(
                            FolderCreationRoute(folderId = model.id.toString())
                        )
                        appStateManager.onShowSheet()
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "edit-02",
                            color = YabaColor.ORANGE,
                        )
                    },
                    text = { Text(text = stringResource(Res.string.edit)) }
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(2, 3),
                    checked = false,
                    onCheckedChange = { _ ->
                        isOptionsExpanded = false
                        // TODO: SHOW MOVE DIALOG
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "arrow-move-up-right",
                            color = YabaColor.TEAL,
                        )
                    },
                    text = { Text(text = stringResource(Res.string.move)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = 1
                )
            ) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 1),
                    checked = false,
                    onCheckedChange = { _ ->
                        isOptionsExpanded = false
                        deletionDialogManager.send(
                            DeletionState(
                                deletionType = DeletionType.FOLDER,
                                folderToBeDeleted = model,
                                onConfirm = { onDeleteFolder(model) },
                            )
                        )
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "delete-02",
                            color = YabaColor.RED,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(Res.string.delete),
                            color = Color(YabaColor.RED.iconTintArgb())
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun GridFolderItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel,
) {

}
