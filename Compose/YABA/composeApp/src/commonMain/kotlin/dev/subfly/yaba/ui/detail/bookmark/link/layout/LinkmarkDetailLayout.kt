package dev.subfly.yaba.ui.detail.bookmark.link.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.main.FolderDetailRoute
import dev.subfly.yaba.core.navigation.main.TagDetailRoute
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailActionsContent
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailFolderSectionContent
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailImageSectionContent
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailInfoSectionContent
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailTagSectionContent
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkDetailLayout(
    state: LinkmarkDetailUIState,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onHide: () -> Unit,
    onEvent: (LinkmarkDetailEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current

    val mainColor by remember(state.bookmark) {
        mutableStateOf(state.bookmark?.parentFolder?.color ?: YabaColor.BLUE)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(0.9F),
        userScrollEnabled = isExpanded,
    ) {
        item (key = "ACTIONS") {
            LinkmarkDetailActionsContent(
                modifier = Modifier.animateItem(),
                isExpanded = isExpanded,
                onExpand = onExpand,
                onHide = onHide,
            )
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
        // TODO: ADD HIGHLIGHTS SEGMENTED SWITCH HERE :)
        state.bookmark?.let { bookmarkDetails ->
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
                    linkDetails = state.linkDetails,
                    mainColor = mainColor,
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            bookmarkDetails.parentFolder?.let { folder ->
                item(key = "FOLDER") {
                    LinkmarkDetailFolderSectionContent(
                        modifier = Modifier.animateItem(),
                        folder = folder,
                        onClickFolder = { navigator.add(FolderDetailRoute(folderId = folder.id)) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item(key = "TAGS") {
                LinkmarkDetailTagSectionContent(
                    modifier = Modifier.animateItem(),
                    tags = bookmarkDetails.tags,
                    onClickTag = { tag -> navigator.add(TagDetailRoute(tagId = tag.id)) }
                )
            }
            item(key = "EXTRA_SPACER") { Spacer(modifier = Modifier.height(56.dp)) }
        }
    }
}
