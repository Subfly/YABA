package dev.subfly.yaba.ui.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.ResultStoreKeys
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.icons.IconCatalog
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.pick_icon_category_title

@Composable
fun IconCategorySelectionContent(
    currentSelectedIcon: String,
    onDismiss: () -> Unit
) {
    val resultStore = LocalResultStore.current
    var selectedIcon by rememberSaveable(currentSelectedIcon) {
        mutableStateOf(currentSelectedIcon)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9F)
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            onDone = {
                resultStore.setResult(
                    key = ResultStoreKeys.SELECTED_ICON,
                    value = selectedIcon,
                )
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContent(
            selectedIcon = selectedIcon,
            onSelectIcon = { newIcon -> selectedIcon = newIcon }
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = Color.Transparent,
        ),
        title = { Text(text = stringResource(Res.string.pick_icon_category_title)) },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                YabaIcon(name = "arrow-left-01")
            }
        },
        actions = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDone,
            ) { Text(text = stringResource(Res.string.done)) }
        }
    )
}

@Composable
private fun SelectionContent(
    selectedIcon: String,
    onSelectIcon: (String) -> Unit,
){
    LazyColumn {
        items(
            items = IconCatalog.categories(),
            key = { it.id },
        ) { category ->
            Text(category.name)
        }
    }
}
