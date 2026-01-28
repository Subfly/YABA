@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.bookmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.TagsRowContent
import dev.subfly.yaba.core.components.item.base.BaseBookmarkItemView
import dev.subfly.yaba.core.components.item.base.BookmarkOptionsMenu
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.yabaRightClick
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_no_tags_added_title
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.move
import yaba.composeapp.generated.resources.share
import kotlin.uuid.ExperimentalUuidApi

/**
 * Entry point for bookmark item rendering.
 * Adapts to different appearances: LIST, CARD (big/small image), and GRID.
 *
 * @param model The bookmark data to display
 * @param appearance The display mode (LIST, CARD, GRID)
 * @param cardImageSizing The image sizing for card view (BIG or SMALL)
 * @param imageFilePath Optional override for the local file path. Defaults to model.localImagePath.
 * @param onClick Callback when the item is clicked
 * @param onDeleteBookmark Callback when the bookmark should be deleted
 * @param onShareBookmark Callback when the bookmark should be shared
 * @param containerColor The background color for the list item container
 */
@Composable
fun BookmarkItemView(
    modifier: Modifier = Modifier,
    model: BookmarkUiModel,
    appearance: BookmarkAppearance,
    cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    imageFilePath: String? = model.localImagePath,
    iconFilePath: String? = model.localIconPath,
    isAddedToSelection: Boolean = false,
    onClick: () -> Unit = {},
    onDeleteBookmark: (BookmarkUiModel) -> Unit = {},
    onShareBookmark: (BookmarkUiModel) -> Unit = {},
    index: Int = 0,
    count: Int = 1,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current

    var isOptionsExpanded by remember { mutableStateOf(false) }

    // Get color from parent folder or default to blue
    val folderColor by remember(model.parentFolder) {
        mutableStateOf(model.parentFolder?.color ?: YabaColor.BLUE)
    }

    // Localized strings for menu items
    val editText = stringResource(Res.string.edit)
    val moveText = stringResource(Res.string.move)
    val shareText = stringResource(Res.string.share)
    val deleteText = stringResource(Res.string.delete)

    // Create menu actions for this bookmark
    val menuActions = remember(model.id, editText, moveText, shareText, deleteText) {
        listOf(
            BookmarkMenuAction(
                key = "edit_${model.id}",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE,
                onClick = {
                    when (model.kind) {
                        BookmarkKind.LINK -> {
                            creationNavigator.add(LinkmarkCreationRoute(bookmarkId = model.id))
                        }

                        BookmarkKind.NOTE -> {

                        }

                        BookmarkKind.IMAGE -> {

                        }

                        BookmarkKind.FILE -> {

                        }
                    }
                    appStateManager.onShowCreationContent()
                }
            ),
            BookmarkMenuAction(
                key = "move_${model.id}",
                icon = "arrow-move-up-right",
                text = moveText,
                color = YabaColor.TEAL,
                onClick = {
                    creationNavigator.add(
                        FolderSelectionRoute(
                            mode = FolderSelectionMode.BOOKMARKS_MOVE,
                            contextFolderId = model.folderId,
                            contextBookmarkIds = listOf(model.id),
                        )
                    )
                    appStateManager.onShowCreationContent()
                }
            ),
            BookmarkMenuAction(
                key = "share_${model.id}",
                icon = "share-03",
                text = shareText,
                color = YabaColor.INDIGO,
                onClick = { onShareBookmark(model) }
            ),
            BookmarkMenuAction(
                key = "delete_${model.id}",
                icon = "delete-02",
                text = deleteText,
                color = YabaColor.RED,
                isDangerous = true,
                onClick = {
                    deletionDialogManager.send(
                        DeletionState(
                            deletionType = DeletionType.BOOKMARK,
                            bookmarkToBeDeleted = model,
                            onConfirm = { onDeleteBookmark(model) },
                        )
                    )
                }
            ),
        )
    }

    // Swipe actions (only for list view)
    val leftSwipeActions = remember(model.id) {
        listOf(
            BookmarkSwipeAction(
                key = "SHARE_${model.id}",
                icon = "share-03",
                color = YabaColor.INDIGO,
                onClick = { onShareBookmark(model) }
            ),
            BookmarkSwipeAction(
                key = "MOVE_${model.id}",
                icon = "arrow-move-up-right",
                color = YabaColor.TEAL,
                onClick = {
                    creationNavigator.add(
                        FolderSelectionRoute(
                            mode = FolderSelectionMode.BOOKMARKS_MOVE,
                            contextFolderId = model.folderId,
                            contextBookmarkIds = listOf(model.id),
                        )
                    )
                    appStateManager.onShowCreationContent()
                }
            ),
        )
    }

    val rightSwipeActions = remember(model.id) {
        listOf(
            BookmarkSwipeAction(
                key = "EDIT_${model.id}",
                icon = "edit-02",
                color = YabaColor.ORANGE,
                onClick = {
                    when (model.kind) {
                        BookmarkKind.LINK -> {
                            creationNavigator.add(LinkmarkCreationRoute(bookmarkId = model.id))
                        }

                        BookmarkKind.NOTE -> {

                        }

                        BookmarkKind.IMAGE -> {

                        }

                        BookmarkKind.FILE -> {

                        }
                    }
                    appStateManager.onShowCreationContent()
                }
            ),
            BookmarkSwipeAction(
                key = "DELETE_${model.id}",
                icon = "delete-02",
                color = YabaColor.RED,
                onClick = {
                    deletionDialogManager.send(
                        DeletionState(
                            deletionType = DeletionType.BOOKMARK,
                            bookmarkToBeDeleted = model,
                            onConfirm = { onDeleteBookmark(model) },
                        )
                    )
                }
            ),
        )
    }

    BaseBookmarkItemView(
        modifier = modifier,
        appearance = appearance,
        cardImageSizing = cardImageSizing,
        menuActions = menuActions,
        leftSwipeActions = if (appearance == BookmarkAppearance.LIST) leftSwipeActions else emptyList(),
        rightSwipeActions = if (appearance == BookmarkAppearance.LIST) rightSwipeActions else emptyList(),
        isOptionsExpanded = isOptionsExpanded,
        onDismissOptions = { isOptionsExpanded = false },
    ) {
        when (appearance) {
            BookmarkAppearance.LIST -> {
                ListItemContent(
                    model = model,
                    folderColor = folderColor,
                    imageFilePath = imageFilePath,
                    isAddedToSelection = isAddedToSelection,
                    onClick = onClick,
                    onLongClick = { isOptionsExpanded = true },
                    index = index,
                    count = count,
                    containerColor = containerColor,
                )
            }

            BookmarkAppearance.CARD -> {
                when (cardImageSizing) {
                    CardImageSizing.BIG -> {
                        CardBigItemContent(
                            model = model,
                            folderColor = folderColor,
                            imageFilePath = imageFilePath,
                            iconFilePath = iconFilePath,
                            isAddedToSelection = isAddedToSelection,
                            menuActions = menuActions,
                            onClick = onClick,
                            onLongClick = { isOptionsExpanded = true },
                        )
                    }

                    CardImageSizing.SMALL -> {
                        CardSmallItemContent(
                            model = model,
                            folderColor = folderColor,
                            imageFilePath = imageFilePath,
                            iconFilePath = iconFilePath,
                            isAddedToSelection = isAddedToSelection,
                            menuActions = menuActions,
                            onClick = onClick,
                            onLongClick = { isOptionsExpanded = true },
                        )
                    }
                }
            }

            BookmarkAppearance.GRID -> {
                GridItemContent(
                    model = model,
                    folderColor = folderColor,
                    imageFilePath = imageFilePath,
                    isAddedToSelection = isAddedToSelection,
                    onClick = onClick,
                    onLongClick = { isOptionsExpanded = true },
                )
            }
        }
    }
}

/**
 * List view for model items.
 * Displays icon/image, title, and description in a compact row layout.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListItemContent(
    model: BookmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    isAddedToSelection: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    index: Int,
    count: Int,
    containerColor: Color,
) {
    SegmentedListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .yabaRightClick(onRightClick = onLongClick),
        onClick = onClick,
        onLongClick = onLongClick,
        colors = ListItemDefaults.colors(containerColor = containerColor),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = {
            Text(
                text = model.label,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (!model.description.isNullOrBlank()) {
                val modelDescription = model.description as String
                Text(
                    text = modelDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            BookmarkImageContent(
                imageFilePath = imageFilePath,
                bookmarkKind = model.kind,
                folderColor = folderColor,
                size = ItemImageSize.SMALL,
                isAddedToSelection = isAddedToSelection,
            )
        },
    )
}

/**
 * Card view with big image for model items.
 * Image is displayed at the top, followed by title, description, and tags.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CardBigItemContent(
    model: BookmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    iconFilePath: String?,
    isAddedToSelection: Boolean,
    menuActions: List<BookmarkMenuAction>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Big image at the top
            BookmarkImageContent(
                modifier = Modifier.fillMaxWidth(),
                imageFilePath = imageFilePath,
                bookmarkKind = model.kind,
                folderColor = folderColor,
                size = ItemImageSize.BIG,
                isAddedToSelection = isAddedToSelection,
            )

            // Title
            Text(
                text = model.label,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Description
            if (!model.description.isNullOrBlank()) {
                val modelDescription = model.description as String
                Text(
                    text = modelDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Tags row with options button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TagsRowContent(
                    modifier = Modifier.weight(1f),
                    tags = model.tags,
                    emptyStateTextRes = Res.string.bookmark_no_tags_added_title,
                    emptyStateColor = folderColor,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    YabaImage(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        filePath = iconFilePath
                    )
                    CardOptionsButton(
                        menuActions = menuActions,
                        folderColor = folderColor,
                    )
                }
            }
        }
    }
}

/**
 * Card view with small image for model items.
 * Image is displayed inline with title, followed by description and tags.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CardSmallItemContent(
    model: BookmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    iconFilePath: String?,
    isAddedToSelection: Boolean,
    menuActions: List<BookmarkMenuAction>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Image and title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookmarkImageContent(
                    imageFilePath = imageFilePath,
                    bookmarkKind = model.kind,
                    folderColor = folderColor,
                    size = ItemImageSize.SMALL,
                    isAddedToSelection = isAddedToSelection,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = model.label,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Description
            if (!model.description.isNullOrBlank()) {
                val modelDescription = model.description as String
                Text(
                    text = modelDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Tags row with options button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TagsRowContent(
                    modifier = Modifier.weight(1f),
                    tags = model.tags,
                    emptyStateTextRes = Res.string.bookmark_no_tags_added_title,
                    emptyStateColor = folderColor,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    YabaImage(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        filePath = iconFilePath
                    )
                    CardOptionsButton(
                        menuActions = menuActions,
                        folderColor = folderColor,
                    )
                }
            }
        }
    }
}

/**
 * Grid view for model items.
 * Displays image, title, and description in a vertical layout.
 * No options button visible - uses long press for context menu.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GridItemContent(
    model: BookmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    isAddedToSelection: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Image at the top
            BookmarkImageContent(
                modifier = Modifier.fillMaxWidth(),
                imageFilePath = imageFilePath,
                bookmarkKind = model.kind,
                folderColor = folderColor,
                size = ItemImageSize.GRID,
                isAddedToSelection = isAddedToSelection,
            )

            // Title
            Text(
                text = model.label,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Description
            if (!model.description.isNullOrBlank()) {
                val modelDescription = model.description as String
                Text(
                    text = modelDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Enum representing different image sizes for model items
 */
private enum class ItemImageSize {
    SMALL,  // 64x64 for list and card small
    BIG,    // Full width, 128dp height for card big
    GRID,   // Full width, 128dp height for grid
}

/**
 * Shared image component for bookmark items.
 * Displays the bookmark image if available, or a placeholder icon.
 */
@Composable
private fun BookmarkImageContent(
    modifier: Modifier = Modifier,
    imageFilePath: String?,
    bookmarkKind: BookmarkKind,
    folderColor: YabaColor,
    size: ItemImageSize,
    isAddedToSelection: Boolean,
) {
    val color = Color(folderColor.iconTintArgb())

    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (size) {
            ItemImageSize.SMALL -> {
                if (imageFilePath != null) {
                    YabaImage(
                        modifier = modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        filePath = imageFilePath,
                    )
                } else {
                    Surface(
                        modifier = modifier.size(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.3f),
                    ) {
                        YabaIcon(
                            modifier = Modifier.padding(16.dp),
                            name = bookmarkKind.uiIconName(),
                            color = folderColor,
                        )
                    }
                }
            }

            ItemImageSize.BIG, ItemImageSize.GRID -> {
                if (imageFilePath != null) {
                    YabaImage(
                        modifier = modifier
                            .height(128.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        filePath = imageFilePath,
                    )
                } else {
                    Surface(
                        modifier = modifier.height(128.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.3f),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            YabaIcon(
                                modifier = Modifier.size(48.dp),
                                name = bookmarkKind.uiIconName(),
                                color = folderColor,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            modifier = Modifier.matchParentSize(),
            visible = isAddedToSelection,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.75F)),
                contentAlignment = Alignment.Center,
            ) {
                YabaIcon(
                    name = "checkmark-circle-02",
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Options button for card views.
 * Shows a clickable icon that triggers the options menu.
 */
@Composable
private fun CardOptionsButton(
    menuActions: List<BookmarkMenuAction>,
    folderColor: YabaColor,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val color = Color(folderColor.iconTintArgb())

    Box {
        Surface(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { isMenuExpanded = true },
                    onLongClick = { isMenuExpanded = true },
                ),
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.3f),
        ) {
            YabaIcon(
                modifier = Modifier.padding(5.dp),
                name = "more-horizontal-circle-02",
                color = folderColor,
            )
        }

        BookmarkOptionsMenu(
            menuActions = menuActions,
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        )
    }
}
