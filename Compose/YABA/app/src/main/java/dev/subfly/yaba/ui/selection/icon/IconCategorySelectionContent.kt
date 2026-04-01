package dev.subfly.yaba.ui.selection.icon

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.navigation.creation.IconSelectionRoute
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.core.icons.IconCategory
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.selection.icon.IconCategorySelectionEvent

// TODO(localization): Category names come from bundled JSON (English).

@Composable
fun IconCategorySelectionContent(currentSelectedIcon: String) {
    val creationNavigator = LocalCreationContentNavigator.current

    val vm = viewModel { IconCategorySelectionVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.onEvent(IconCategorySelectionEvent.OnInit)
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxHeight(0.9F)
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            onDismiss = creationNavigator::removeLastOrNull,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContent(
            categories = state.categories,
            onSelectedCategory = { selectedCategory ->
                creationNavigator.add(
                    IconSelectionRoute(
                        selectedIcon = currentSelectedIcon,
                        selectedCategory = selectedCategory,
                    )
                )
            }
        )
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
        title = { Text(text = stringResource(R.string.pick_icon_category_title)) },
        navigationIcon = {
            IconButton(onClick = onDismiss) { YabaIcon(name = "arrow-left-01") }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionContent(
    categories: List<IconCategory>,
    onSelectedCategory: (IconCategory) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = categories,
            key = { it.id },
        ) { category ->
            SegmentedListItem(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                onClick = { onSelectedCategory(category) },
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                content = {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingContent = {
                    YabaIcon(
                        name = category.headerIcon,
                        color = YabaColor.fromCode(category.color),
                    )
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "${category.iconCount}")
                        YabaIcon(name = "arrow-right-01")
                    }
                },
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}
