package dev.subfly.yaba.ui.selection.icon

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
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.navigation.creation.IconSelectionRoute
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.localizedDescriptionRes
import dev.subfly.yaba.util.localizedNameRes
import dev.subfly.yabacore.icons.IconCatalog
import dev.subfly.yabacore.icons.IconSubcategory
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.pick_icon_category_title

@Composable
fun IconCategorySelectionContent(currentSelectedIcon: String) {
    val creationNavigator = LocalCreationContentNavigator.current

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
            onSelectedSubcategory = { selectedSubcategory ->
                creationNavigator.add(
                    IconSelectionRoute(
                        selectedIcon = currentSelectedIcon,
                        selectedSubcategory = selectedSubcategory
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
        title = { Text(text = stringResource(Res.string.pick_icon_category_title)) },
        navigationIcon = {
            IconButton(onClick = onDismiss) { YabaIcon(name = "arrow-left-01") }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionContent(
    onSelectedSubcategory: (IconSubcategory) -> Unit,
) {
    val categories by IconCatalog.categoriesFlow.collectAsState()

    LazyColumn(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        items(
            items = categories,
            key = { it.id },
        ) { category ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 4.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val color = YabaColor.fromCode(category.color)
                    YabaIcon(
                        name = category.headerIcon,
                        color = color,
                    )
                    Text(
                        text = stringResource(category.localizedNameRes()),
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                        color = Color(color.iconTintArgb()),
                    )
                }
                category.subcategories.fastForEachIndexed { index, subcategory ->
                    SegmentedListItem(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        onClick = { onSelectedSubcategory(subcategory) },
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = category.subcategories.size),
                        content = {
                            Text(stringResource(subcategory.localizedNameRes()))
                        },
                        leadingContent = {
                            YabaIcon(
                                name = subcategory.headerIcon,
                                color = YabaColor.fromCode(subcategory.color)
                            )
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "${subcategory.iconCount}")
                                YabaIcon(name = "arrow-right-01")
                            }
                        },
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 4.dp).padding(horizontal = 12.dp),
                    text = stringResource(category.localizedDescriptionRes()),
                    style = MaterialTheme.typography.bodySmallEmphasized,
                )
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}
