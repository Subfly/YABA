package dev.subfly.yaba.ui.creation.inline.mention

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

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
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.util.InlineActionChoice
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys

private data class InlineMentionActionRow(
    val label: String,
    val iconName: String,
    val color: YabaColor,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InlineMentionActionSheetContent() {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TopBar {
            if (creationNavigator.size == 2) {
                appStateManager.onHideCreationContent()
            }
            creationNavigator.removeLastOrNull()
        }
        MentionActionSelectionContent(
            onEdit = {
                resultStore.setResult(
                    ResultStoreKeys.INLINE_MENTION_ACTION,
                    InlineActionChoice.EDIT,
                )
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
            onOpen = {
                resultStore.setResult(
                    ResultStoreKeys.INLINE_MENTION_ACTION,
                    InlineActionChoice.OPEN,
                )
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
            onRemove = {
                resultStore.setResult(
                    ResultStoreKeys.INLINE_MENTION_ACTION,
                    InlineActionChoice.REMOVE,
                )
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
        )
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
        title = { Text(text = "Mention Action") }, // TODO: localize
        navigationIcon = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss,
                colors =
                    ButtonDefaults
                        .textButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MentionActionSelectionContent(
    onEdit: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    listOf(
        InlineMentionActionRow(
            label = "Edit Mention",
            iconName = "edit-02",
            color = YabaColor.YELLOW,
            onClick = onEdit,
        ),
        InlineMentionActionRow(
            label = "Open Bookmark",
            iconName = "link-circle",
            color = YabaColor.BLUE,
            onClick = onOpen,
        ),
        InlineMentionActionRow(
            label = "Remove Mention",
            iconName = "delete-02",
            color = YabaColor.RED,
            onClick = onRemove,
        ),
    ).fastForEachIndexed { index, item ->
        SegmentedListItem(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp)),
            onClick = item.onClick,
            shapes = ListItemDefaults.segmentedShapes(index = index, count = 3),
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
