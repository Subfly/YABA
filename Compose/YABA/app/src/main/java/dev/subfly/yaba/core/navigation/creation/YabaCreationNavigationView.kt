package dev.subfly.yaba.core.navigation.creation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.subfly.yaba.ui.creation.annotation.AnnotationCreationContent
import dev.subfly.yaba.ui.creation.bookmark.BookmarkCreationRouteSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.BookmarkPasswordCreateSheetContent
import dev.subfly.yaba.ui.creation.bookmark.BookmarkPasswordEditSheetContent
import dev.subfly.yaba.ui.creation.bookmark.BookmarkPasswordEntrySheetContent
import dev.subfly.yaba.ui.creation.bookmark.canvmark.CanvmarkCreationContent
import dev.subfly.yaba.ui.creation.bookmark.docmark.DocmarkCreationContent
import dev.subfly.yaba.ui.creation.bookmark.imagemark.ImagemarkCreationContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.LinkmarkCreationContent
import dev.subfly.yaba.ui.creation.bookmark.notemark.NotemarkCreationContent
import dev.subfly.yaba.ui.creation.folder.FolderCreationContent
import dev.subfly.yaba.ui.creation.inline.link.InlineLinkActionSheetContent
import dev.subfly.yaba.ui.creation.inline.link.InlineLinkSheetContent
import dev.subfly.yaba.ui.creation.inline.mention.InlineMentionActionSheetContent
import dev.subfly.yaba.ui.creation.inline.mention.InlineMentionSheetContent
import dev.subfly.yaba.ui.creation.notemark.math.NotemarkMathCreationContent
import dev.subfly.yaba.ui.creation.notemark.table.NotemarkTableCreationContent
import dev.subfly.yaba.ui.creation.tag.TagCreationContent
import dev.subfly.yaba.ui.selection.bookmark.BookmarkSelectionContent
import dev.subfly.yaba.ui.selection.color.ColorSelectionContent
import dev.subfly.yaba.ui.selection.folder.FolderSelectionContent
import dev.subfly.yaba.ui.selection.icon.IconCategorySelectionContent
import dev.subfly.yaba.ui.selection.icon.IconSelectionContent
import dev.subfly.yaba.ui.selection.tag.TagSelectionContent
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator

@Composable
fun YabaCreationNavigationView(
    modifier: Modifier = Modifier,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

    NavDisplay(
        modifier = modifier,
        backStack = creationNavigator,
        sceneStrategies = emptyList(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        popTransitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        predictivePopTransitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        onBack = {
            // Means next pop up destination is Empty Route,
            // so dismiss first, then remove the last item
            if (creationNavigator.size == 2) {
                appStateManager.onHideCreationContent()
            }
            creationNavigator.removeLastOrNull()
        },
        entryProvider = entryProvider {
            entry<TagCreationRoute> { key ->
                TagCreationContent(tagId = key.tagId)
            }
            entry<FolderCreationRoute> { key ->
                FolderCreationContent(folderId = key.folderId)
            }
            entry<ColorSelectionRoute> { key ->
                ColorSelectionContent(
                    currentSelectedColor = key.selectedColor,
                    allowTransparent = key.allowTransparent,
                )
            }
            entry<IconCategorySelectionRoute> { key ->
                IconCategorySelectionContent(currentSelectedIcon = key.selectedIcon)
            }
            entry<IconSelectionRoute> { key ->
                IconSelectionContent(
                    currentSelectedIcon = key.selectedIcon,
                    selectedCategory = key.selectedCategory,
                )
            }
            entry<BookmarkCreationRoute> {
                BookmarkCreationRouteSelectionContent()
            }
            entry<LinkmarkCreationRoute> { key ->
                LinkmarkCreationContent(
                    bookmarkId = key.bookmarkId,
                    initialUrl = key.initialUrl,
                )
            }
            entry<NotemarkCreationRoute> { key ->
                NotemarkCreationContent(bookmarkId = key.bookmarkId)
            }
            entry<CanvmarkCreationRoute> { key ->
                CanvmarkCreationContent(bookmarkId = key.bookmarkId)
            }
            entry<ImagemarkCreationRoute> { key ->
                ImagemarkCreationContent(bookmarkId = key.bookmarkId)
            }
            entry<DocmarkCreationRoute> { key ->
                DocmarkCreationContent(bookmarkId = key.bookmarkId)
            }
            entry<FolderSelectionRoute> { key ->
                FolderSelectionContent(
                    mode = key.mode,
                    contextFolderId = key.contextFolderId,
                    contextBookmarkIds = key.contextBookmarkIds,
                )
            }
            entry<TagSelectionRoute> { key ->
                TagSelectionContent(alreadySelectedTagIds = key.selectedTagIds)
            }
            entry<AnnotationCreationRoute> { key ->
                AnnotationCreationContent(
                    bookmarkId = key.bookmarkId,
                    selectionDraft = key.selectionDraft,
                    annotationId = key.annotationId,
                )
            }
            entry<NotemarkTableCreationRoute> {
                NotemarkTableCreationContent()
            }
            entry<NotemarkMathSheetRoute> { key ->
                NotemarkMathCreationContent(route = key)
            }
            entry<InlineLinkSheetRoute> { key ->
                InlineLinkSheetContent(route = key)
            }
            entry<InlineLinkActionSheetRoute> { _ ->
                InlineLinkActionSheetContent()
            }
            entry<InlineMentionSheetRoute> { key ->
                InlineMentionSheetContent(route = key)
            }
            entry<InlineMentionActionSheetRoute> { _ ->
                InlineMentionActionSheetContent()
            }
            entry<BookmarkSelectionRoute> { key ->
                BookmarkSelectionContent(selectedBookmarkId = key.selectedBookmarkId)
            }
            entry<BookmarkPasswordCreateRoute> { _ ->
                BookmarkPasswordCreateSheetContent()
            }
            entry<BookmarkPasswordEditRoute> { _ ->
                BookmarkPasswordEditSheetContent()
            }
            entry<BookmarkPasswordEntryRoute> { key ->
                BookmarkPasswordEntrySheetContent(route = key)
            }
            entry<EmptyCreationRoute> {
                // Only old Compose users will remember why we had to put 1.dp boxes in sheets...
                Box(modifier = Modifier.size((0.1).dp))
            }
        }
    )
}
