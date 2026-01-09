@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.bookmark.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.bookmark.base.BaseBookmarkItemView
import dev.subfly.yaba.core.components.item.bookmark.base.BookmarkMenuAction
import dev.subfly.yaba.core.components.item.bookmark.base.BookmarkSwipeAction
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.yabaClickable
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
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
 * Entry point for link bookmark (Linkmark) item rendering.
 * Adapts to different appearances: LIST, CARD (big/small image), and GRID.
 *
 * @param model The linkmark data to display
 * @param appearance The display mode (LIST, CARD, GRID)
 * @param cardImageSizing The image sizing for card view (BIG or SMALL)
 * @param imageFilePath The local file path for the bookmark image (if available)
 * @param onClick Callback when the item is clicked
 * @param onDeleteBookmark Callback when the bookmark should be deleted
 * @param onShareBookmark Callback when the bookmark should be shared
 */
@Composable
fun LinkmarkItemView(
    modifier: Modifier = Modifier,
    model: LinkmarkUiModel,
    appearance: BookmarkAppearance,
    cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    imageFilePath: String? = null,
    onClick: () -> Unit = {},
    onDeleteBookmark: (LinkmarkUiModel) -> Unit = {},
    onShareBookmark: (LinkmarkUiModel) -> Unit = {},
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

    // Create menu actions for this linkmark
    val menuActions = remember(model.id, editText, moveText, shareText, deleteText) {
        listOf(
            BookmarkMenuAction(
                key = "edit_${model.id}",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE,
                onClick = {
                    creationNavigator.add(LinkmarkCreationRoute(bookmarkId = model.id.toString()))
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
                            mode = FolderSelectionMode.BOOKMARK_MOVE,
                            contextFolderId = model.folderId.toString(),
                            contextBookmarkId = model.id.toString(),
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
                            mode = FolderSelectionMode.BOOKMARK_MOVE,
                            contextFolderId = model.folderId.toString(),
                            contextBookmarkId = model.id.toString(),
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
                    creationNavigator.add(LinkmarkCreationRoute(bookmarkId = model.id.toString()))
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
                LinkmarkListItemContent(
                    model = model,
                    folderColor = folderColor,
                    imageFilePath = imageFilePath,
                    onClick = onClick,
                    onLongClick = { isOptionsExpanded = true },
                )
            }

            BookmarkAppearance.CARD -> {
                when (cardImageSizing) {
                    CardImageSizing.BIG -> {
                        LinkmarkCardBigItemContent(
                            model = model,
                            folderColor = folderColor,
                            imageFilePath = imageFilePath,
                            menuActions = menuActions,
                            onClick = onClick,
                            onLongClick = { isOptionsExpanded = true },
                        )
                    }

                    CardImageSizing.SMALL -> {
                        LinkmarkCardSmallItemContent(
                            model = model,
                            folderColor = folderColor,
                            imageFilePath = imageFilePath,
                            menuActions = menuActions,
                            onClick = onClick,
                            onLongClick = { isOptionsExpanded = true },
                        )
                    }
                }
            }

            BookmarkAppearance.GRID -> {
                LinkmarkGridItemContent(
                    model = model,
                    folderColor = folderColor,
                    imageFilePath = imageFilePath,
                    onClick = onClick,
                    onLongClick = { isOptionsExpanded = true },
                )
            }
        }
    }
}

/**
 * List view for linkmark items.
 * Displays icon/image, title, and description in a compact row layout.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkmarkListItemContent(
    model: LinkmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .yabaClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        colors = ListItemDefaults.colors().copy(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        headlineContent = {
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
                linkType = model.linkType,
                folderColor = folderColor,
                size = LinkmarkImageSize.SMALL,
            )
        },
    )
}

/**
 * Card view with big image for linkmark items.
 * Image is displayed at the top, followed by title, description, and tags.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkmarkCardBigItemContent(
    model: LinkmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    menuActions: List<BookmarkMenuAction>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .yabaClickable(
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
                linkType = model.linkType,
                folderColor = folderColor,
                size = LinkmarkImageSize.BIG,
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
                    folderColor = folderColor,
                )
                CardOptionsButton(
                    menuActions = menuActions,
                    folderColor = folderColor,
                )
            }
        }
    }
}

/**
 * Card view with small image for linkmark items.
 * Image is displayed inline with title, followed by description and tags.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkmarkCardSmallItemContent(
    model: LinkmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    menuActions: List<BookmarkMenuAction>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .yabaClickable(
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
                    linkType = model.linkType,
                    folderColor = folderColor,
                    size = LinkmarkImageSize.SMALL,
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
                    folderColor = folderColor,
                )
                CardOptionsButton(
                    menuActions = menuActions,
                    folderColor = folderColor,
                )
            }
        }
    }
}

/**
 * Grid view for linkmark items.
 * Displays image, title, and description in a vertical layout.
 * No options button visible - uses long press for context menu.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkmarkGridItemContent(
    model: LinkmarkUiModel,
    folderColor: YabaColor,
    imageFilePath: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .yabaClickable(
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
                linkType = model.linkType,
                folderColor = folderColor,
                size = LinkmarkImageSize.GRID,
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
 * Enum representing different image sizes for linkmark items
 */
private enum class LinkmarkImageSize {
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
    linkType: dev.subfly.yabacore.model.utils.LinkType,
    folderColor: YabaColor,
    size: LinkmarkImageSize,
) {
    val color = Color(folderColor.iconTintArgb())

    when (size) {
        LinkmarkImageSize.SMALL -> {
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
                        name = linkType.uiIconName(),
                        color = folderColor,
                    )
                }
            }
        }

        LinkmarkImageSize.BIG, LinkmarkImageSize.GRID -> {
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
                            name = linkType.uiIconName(),
                            color = folderColor,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tags row component showing bookmark tags.
 * Displays up to 5 tags with a +N indicator if there are more.
 */
@Composable
private fun TagsRowContent(
    modifier: Modifier = Modifier,
    tags: List<TagUiModel>,
    folderColor: YabaColor,
) {
    val color = Color(folderColor.iconTintArgb())

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (tags.isEmpty()) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.3f),
            ) {
                YabaIcon(
                    modifier = Modifier.padding(4.dp),
                    name = "tags",
                    color = folderColor,
                )
            }
            Text(
                text = stringResource(Res.string.bookmark_no_tags_added_title),
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val displayTags = tags.take(5)
                displayTags.forEach { tag ->
                    val tagColor = Color(tag.color.iconTintArgb())
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = tagColor.copy(alpha = 0.3f),
                    ) {
                        YabaIcon(
                            modifier = Modifier.padding(5.dp),
                            name = tag.icon,
                            color = tag.color,
                        )
                    }
                }
            }
            if (tags.size > 5) {
                Text(
                    text = "+${tags.size - 5}",
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
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
                .yabaClickable(
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

        dev.subfly.yaba.core.components.item.bookmark.base.BookmarkOptionsMenu(
            menuActions = menuActions,
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        )
    }
}

