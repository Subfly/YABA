package dev.subfly.yaba.ui.creation.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.DocmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.new_bookmark

@Composable
fun BookmarkCreationRouteSelectionContent() {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TopBar(
            onDismiss = {
                appStateManager.onHideCreationContent()
                creationNavigator.removeLastOrNull()
            }
        )
        SelectionContent()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors =
            TopAppBarDefaults.topAppBarColors()
                .copy(
                    containerColor = Color.Transparent,
                ),
        title = { Text(text = stringResource(resource = Res.string.new_bookmark)) },
        navigationIcon = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss,
                colors =
                    ButtonDefaults
                        .textButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(text = stringResource(Res.string.cancel)) }
        },
    )
}

// TODO: LOCALIZATION
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionContent() {
    val creationNavigator = LocalCreationContentNavigator.current

    listOf(
        SelectionContentItem(
            label = "New Link",
            iconName = "link-02",
            color = YabaColor.BLUE,
            onClick = {
                creationNavigator.add(LinkmarkCreationRoute(bookmarkId = null))
                creationNavigator.removeIf { it is BookmarkCreationRoute }
            },
        ),
        SelectionContentItem(
            label = "New Note",
            iconName = "note-edit",
            color = YabaColor.YELLOW,
            onClick = {
                creationNavigator.add(NotemarkCreationRoute(bookmarkId = null))
                creationNavigator.removeIf { it is BookmarkCreationRoute }
            },
        ),
        SelectionContentItem(
            label = "New Image",
            iconName = "image-03",
            color = YabaColor.GREEN,
            onClick = {
                creationNavigator.add(ImagemarkCreationRoute(bookmarkId = null))
                creationNavigator.removeIf { it is BookmarkCreationRoute }
            },
        ),
        SelectionContentItem(
            label = "New Document",
            iconName = "doc-02",
            color = YabaColor.RED,
            onClick = {
                creationNavigator.add(DocmarkCreationRoute(bookmarkId = null))
                creationNavigator.removeIf { it is BookmarkCreationRoute }
            },
        )
    ).fastForEachIndexed { index, item ->
        SegmentedListItem(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp)),
            onClick = item.onClick,
            shapes = ListItemDefaults.segmentedShapes(index = index, count = 4),
            content = { Text(item.label) },
            leadingContent = {
                YabaIcon(
                    name = item.iconName,
                    color = Color(item.color.iconTintArgb()),
                )
            },
            trailingContent = { YabaIcon(name = "arrow-right-01") },
        )
    }
}

private data class SelectionContentItem(
    val label: String,
    val iconName: String,
    val color: YabaColor,
    val onClick: () -> Unit,
)
