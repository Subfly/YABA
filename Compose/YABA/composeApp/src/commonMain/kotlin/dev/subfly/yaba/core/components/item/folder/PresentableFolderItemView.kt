package dev.subfly.yaba.core.components.item.folder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.creation.FolderCreationRoute
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.yabaClickable
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.folder_creation_select_folder_message
import kotlin.uuid.ExperimentalUuidApi

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalUuidApi::class
)
@Composable
fun PresentableFolderItemView(
    modifier: Modifier = Modifier,
    model: FolderUiModel?,
    nullModelPresentableColor: YabaColor,
    onPressed: () -> Unit,
    cornerSize: Dp = 24.dp,
) {
    val creationNavigator = LocalCreationContentNavigator.current

    var isOptionsExpanded by remember { mutableStateOf(false) }

    YabaSwipeActions(
        modifier = modifier,
        rightActions = if (model != null) {
            listOf(
                SwipeAction(
                    key = "EDIT",
                    onClick = {
                        creationNavigator.add(
                            FolderCreationRoute(folderId = model.id.toString())
                        )
                    },
                    content = {
                        Surface(
                            modifier = Modifier.size(44.dp),
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
                )
            )
        } else emptyList()
    ) {
        Box {
            ListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerSize))
                    .yabaClickable(
                        onClick = onPressed,
                        onLongClick = {
                            if (model != null) {
                                isOptionsExpanded = true
                            }
                        }
                    ),
                headlineContent = {
                    Text(
                        text = model?.label
                            ?: stringResource(Res.string.folder_creation_select_folder_message)
                    )
                },
                leadingContent = {
                    YabaIcon(
                        name = model?.icon ?: "folder-01",
                        color = model?.color ?: nullModelPresentableColor,
                    )
                },
                trailingContent = {
                    YabaIcon(
                        name = "arrow-right-01",
                        color = model?.color ?: nullModelPresentableColor,
                    )
                },
            )
            DropdownMenuPopup(
                modifier = modifier,
                expanded = isOptionsExpanded,
                onDismissRequest = { isOptionsExpanded = false },
            ) {
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
                            model?.let { nonNullModel ->
                                creationNavigator.add(
                                    FolderCreationRoute(folderId = nonNullModel.id.toString())
                                )
                            }
                        },
                        leadingIcon = { YabaIcon(name = "edit-02") },
                        text = { Text(text = stringResource(Res.string.edit)) }
                    )
                }
            }
        }
    }
}