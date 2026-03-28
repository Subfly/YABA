package dev.subfly.yaba.ui.detail.bookmark.link.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.navigation.creation.AnnotationCreationRoute
import dev.subfly.yaba.core.navigation.main.FolderDetailRoute
import dev.subfly.yaba.core.navigation.main.TagDetailRoute
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailActionsContent
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailFolderSectionContent
import dev.subfly.yaba.core.components.item.annotation.AnnotationItemView
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailImageSectionContent
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailInfoSectionContent
import dev.subfly.yaba.ui.detail.composables.BookmarkExtractedMetadataSection
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailReminderSectionContent
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailTagSectionContent
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailVersionItemContent
import dev.subfly.yaba.ui.detail.bookmark.link.models.DetailPage
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkDetailTocLazyItems
import dev.subfly.yaba.ui.detail.bookmark.components.flattenTocForLazyItems
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_no_tags_added_description
import yaba.composeapp.generated.resources.bookmark_detail_no_tags_added_title

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkDetailLayout(
    state: LinkmarkDetailUIState,
    onHide: () -> Unit,
    onEvent: (LinkmarkDetailEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

    val mainColor by remember(state.bookmark) {
        mutableStateOf(state.bookmark?.parentFolder?.color ?: YabaColor.BLUE)
    }

    var currentPage by remember { mutableStateOf(DetailPage.INFO) }

    var collapsedTocIds by remember { mutableStateOf(setOf<String>()) }
    val tocRows by remember(state.toc, collapsedTocIds) {
        derivedStateOf {
            state.toc?.items?.let { flattenTocForLazyItems(it, collapsedTocIds) } ?: emptyList()
        }
    }

    LaunchedEffect(state.toc) {
        collapsedTocIds = emptySet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(0.9F),
    ) {
        stickyHeader(key = "ACTIONS") {
            LinkmarkDetailActionsContent(
                modifier = Modifier.animateItem(),
                currentPage = currentPage,
                onPageChange = { currentPage = it },
                mainColor = mainColor,
                onHide = onHide,
            )
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
        state.bookmark?.let { bookmarkDetails ->
            when (currentPage) {
                DetailPage.INFO -> {
                    item(key = "LINK_IMAGE") {
                        LinkmarkDetailImageSectionContent(
                            modifier = Modifier.animateItem(),
                            bookmarkDetails = bookmarkDetails,
                            linkDetails = state.linkDetails,
                            mainColor = mainColor,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item(key = "LINK_INFO") {
                        LinkmarkDetailInfoSectionContent(
                            modifier = Modifier.animateItem(),
                            bookmarkDetails = bookmarkDetails,
                            mainColor = mainColor,
                        )
                    }
                    item(key = "LINK_METADATA") {
                        state.linkDetails?.let { link ->
                            BookmarkExtractedMetadataSection(
                                modifier = Modifier.animateItem(),
                                mainColor = mainColor,
                                metadataTitle = link.metadataTitle,
                                metadataDescription = link.metadataDescription,
                                metadataAuthor = link.metadataAuthor,
                                metadataDate = link.metadataDate,
                                audioUrl = link.audioUrl,
                                videoUrl = link.videoUrl,
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    bookmarkDetails.parentFolder?.let { folder ->
                        item(key = "FOLDER") {
                            BookmarkDetailFolderSectionContent(
                                modifier = Modifier.animateItem(),
                                folder = folder,
                                mainColor = mainColor,
                                onClickFolder = { navigator.add(FolderDetailRoute(folderId = folder.id)) },
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item(key = "TAGS") {
                        BookmarkDetailTagSectionContent(
                            modifier = Modifier.animateItem(),
                            tags = bookmarkDetails.tags,
                            onClickTag = { tag -> navigator.add(TagDetailRoute(tagId = tag.id)) },
                        )
                    }
                    state.reminderDateEpochMillis?.let { reminderMillis ->
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                        item(key = "REMINDER") {
                            BookmarkDetailReminderSectionContent(
                                modifier = Modifier.animateItem(),
                                reminderDateEpochMillis = reminderMillis,
                                mainColor = mainColor,
                                onCancelReminder = { onEvent(LinkmarkDetailEvent.OnCancelReminder) },
                            )
                        }
                    }
                }

                DetailPage.CONTENTS -> {
                    bookmarkDetailTocLazyItems(
                        toc = state.toc,
                        rows = tocRows,
                        collapsedIds = collapsedTocIds,
                        onToggleCollapse = { id ->
                            collapsedTocIds =
                                if (id in collapsedTocIds) collapsedTocIds - id else collapsedTocIds + id
                        },
                        mainColor = mainColor,
                        onItemClick = { id, extrasJson ->
                            onHide()
                            onEvent(
                                LinkmarkDetailEvent.OnNavigateToTocItem(
                                    id = id,
                                    extrasJson = extrasJson,
                                ),
                            )
                        },
                        emptyIconName = "displeased",
                        emptyLabelRes = Res.string.bookmark_detail_no_tags_added_title,
                        emptyMessage = {
                            Text(text = stringResource(Res.string.bookmark_detail_no_tags_added_description))
                        },
                    )
                }

                DetailPage.VERSIONS -> {
                    if (state.readableVersions.isEmpty()) {
                        item(key = "NO_VERSIONS") {
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                // TODO: LOCALIZATIONS
                                NoContentView(
                                    modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                                    iconName = "displeased",
                                    labelRes = Res.string.bookmark_detail_no_tags_added_title,
                                    message = { Text(text = stringResource(Res.string.bookmark_detail_no_tags_added_description)) },
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.readableVersions,
                            key = { _, version -> version.versionId },
                        ) { index, version ->
                            LinkmarkDetailVersionItemContent(
                                modifier = Modifier
                                    .animateItem()
                                    .padding(vertical = 4.dp),
                                version = version,
                                mainColor = mainColor,
                                index = index,
                                count = state.readableVersions.size,
                                isSelected = if (state.selectedReadableVersionId != null) {
                                    version.versionId == state.selectedReadableVersionId
                                } else {
                                    index == 0
                                },
                                onClick = { onEvent(LinkmarkDetailEvent.OnSelectReadableVersion(version.versionId)) },
                                onDelete = { onEvent(LinkmarkDetailEvent.OnDeleteReadableVersion(version.versionId)) },
                            )
                        }
                    }
                }

                DetailPage.ANNOTATIONS -> {
                    if (state.annotations.isEmpty()) {
                        item(key = "NO_ANNOTATIONS") {
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                // TODO: LOCALIZATIONS
                                NoContentView(
                                    modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                                    iconName = "displeased",
                                    labelRes = Res.string.bookmark_detail_no_tags_added_title,
                                    message = { Text(text = stringResource(Res.string.bookmark_detail_no_tags_added_description)) },
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.annotations,
                            key = { _, a -> a.id },
                        ) { index, annotation ->
                            AnnotationItemView(
                                modifier = Modifier
                                    .animateItem()
                                    .padding(vertical = 4.dp),
                                model = annotation,
                                index = index,
                                count = state.annotations.size,
                                onPress = {
                                    onHide()
                                    onEvent(LinkmarkDetailEvent.OnScrollToAnnotation(annotation.id))
                                },
                                onEdit = {
                                    state.bookmark?.id?.let { bookmarkId ->
                                        creationNavigator.add(
                                            AnnotationCreationRoute(
                                                bookmarkId = bookmarkId,
                                                selectionDraft = null,
                                                annotationId = annotation.id,
                                            ),
                                        )
                                        appStateManager.onShowCreationContent()
                                    }
                                },
                                onDelete = {
                                    onEvent(LinkmarkDetailEvent.OnDeleteAnnotation(annotation.id))
                                },
                            )
                        }
                    }
                }
            }
            item(key = "EXTRA_SPACER") { Spacer(modifier = Modifier.height(56.dp)) }
        }
    }
}
