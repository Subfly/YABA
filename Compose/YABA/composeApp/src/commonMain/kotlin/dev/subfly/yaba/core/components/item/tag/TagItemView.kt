package dev.subfly.yaba.core.components.item.tag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.new_bookmark
import kotlin.uuid.ExperimentalUuidApi

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalUuidApi::class
)
@Composable
fun TagItemView(
    modifier: Modifier = Modifier,
    model: TagUiModel,
    onDeleteTag: (TagUiModel) -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current

    var isOptionsExpanded by remember { mutableStateOf(false) }
    val color by remember(model) {
        mutableStateOf(Color(model.color.iconTintArgb()))
    }

    Box {
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onLongClick = { isOptionsExpanded = true },
                    onClick = {
                        // TODO: NAVIGATE TO TAG DETAIL
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.15F),
            border = BorderStroke(width = 2.dp, color = color),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                YabaIcon(
                    name = model.icon,
                    color = color,
                )
                Text(
                    text = model.label,
                    color = color,
                )
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
                    count = 2
                )
            ) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    onCheckedChange = { _ ->
                        isOptionsExpanded = false
                        // TODO: ADD FUNCTIONALITY TO HAVE BASE CONTEXT
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
                    shapes = MenuDefaults.itemShape(1, 2),
                    checked = false,
                    onCheckedChange = { _ ->
                        isOptionsExpanded = false
                        creationNavigator.add(TagCreationRoute(tagId = model.id.toString()))
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
                                deletionType = DeletionType.TAG,
                                tagToBeDeleted = model,
                                onConfirm = { onDeleteTag(model) },
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
