package dev.subfly.yaba.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.settings_collection_sorting_title
import yaba.composeapp.generated.resources.settings_sort_order_title
import yaba.composeapp.generated.resources.settings_title

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeDropdownMenu(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    onSortingChanged: (SortType) -> Unit,
    onSortOrderChanged: (SortOrderType) -> Unit,
    onSettingsClicked: () -> Unit,
) {
    var isSortingExpanded by remember { mutableStateOf(false) }
    var isSortOrderExpanded by remember { mutableStateOf(false) }

    DropdownMenuPopup(
        modifier = modifier,
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(
                index = 0,
                count = 3
            )
        ) {
            SortingSection(
                isExpanded = isSortingExpanded,
                onPressedSection = { isSortingExpanded = !isSortingExpanded },
                onDismissSubmenu = { isSortingExpanded = false },
                onSortingSelection = { sorting ->
                    onSortingChanged(sorting)
                    onDismissRequest()
                }
            )
            SortOrderSection(
                isExpanded = isSortOrderExpanded,
                onPressedSection = { isSortOrderExpanded = !isSortOrderExpanded },
                onDismissSubmenu = { isSortOrderExpanded = false },
                onSortOrderSelection = { sortOrder ->
                    onSortOrderChanged(sortOrder)
                    onDismissRequest()
                }
            )
            DropdownMenuItem(
                shapes = MenuDefaults.itemShape(2, 3),
                checked = false,
                onCheckedChange = { _ ->
                    onDismissRequest()
                    onSettingsClicked()
                },
                leadingIcon = {
                    YabaIcon(name = "settings-02")
                },
                text = {
                    Text(text = stringResource(Res.string.settings_title))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SortingSection(
    isExpanded: Boolean,
    onPressedSection: () -> Unit,
    onDismissSubmenu: () -> Unit,
    onSortingSelection: (SortType) -> Unit,
) {
    val userPreferences = LocalUserPreferences.current
    Box {
        DropdownMenuItem(
            shapes = MenuDefaults.itemShape(0, 3),
            checked = false,
            onCheckedChange = { _ -> onPressedSection() },
            leadingIcon = {
                YabaIcon(name = "sorting-04")
            },
            trailingIcon = {
                val expandedRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90F else 0F,
                )
                YabaIcon(
                    modifier = Modifier.rotate(expandedRotation),
                    name = "arrow-right-01"
                )
            },
            text = {
                Text(text = stringResource(Res.string.settings_collection_sorting_title))
            }
        )
        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = onDismissSubmenu,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = SortType.entries.size
                )
            ) {
                SortType.entries.fastForEachIndexed { index, sorting ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, SortType.entries.size),
                        checked = userPreferences.preferredCollectionSorting == sorting,
                        onCheckedChange = { _ ->
                            onSortingSelection(sorting)
                            onDismissSubmenu()
                        },
                        leadingIcon = { YabaIcon(name = sorting.uiIconName()) },
                        text = { Text(text = sorting.uiTitle()) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SortOrderSection(
    isExpanded: Boolean,
    onPressedSection: () -> Unit,
    onDismissSubmenu: () -> Unit,
    onSortOrderSelection: (SortOrderType) -> Unit,
) {
    val userPreferences = LocalUserPreferences.current
    Box {
        DropdownMenuItem(
            shapes = MenuDefaults.itemShape(1, 3),
            checked = false,
            onCheckedChange = { _ -> onPressedSection() },
            leadingIcon = {
                YabaIcon(name = "sorting-9-1")
            },
            trailingIcon = {
                val expandedRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90F else 0F,
                )
                YabaIcon(
                    modifier = Modifier.rotate(expandedRotation),
                    name = "arrow-right-01"
                )
            },
            text = {
                Text(text = stringResource(Res.string.settings_sort_order_title))
            }
        )
        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = onDismissSubmenu,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = SortOrderType.entries.size
                )
            ) {
                SortOrderType.entries.fastForEachIndexed { index, sortOrder ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, SortOrderType.entries.size),
                        checked = userPreferences.preferredCollectionSortOrder == sortOrder,
                        onCheckedChange = { _ ->
                            onSortOrderSelection(sortOrder)
                            onDismissSubmenu()
                        },
                        leadingIcon = { YabaIcon(name = sortOrder.uiIconName()) },
                        text = { Text(text = sortOrder.uiTitle()) }
                    )
                }
            }
        }
    }
}
