package dev.subfly.yaba.ui.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.ResultStoreKeys
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.localizedNameRes
import dev.subfly.yabacore.icons.IconCatalog
import dev.subfly.yabacore.icons.IconSubcategory
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.done

@Composable
fun IconSelectionContent(
    currentSelectedIcon: String,
    selectedSubcategory: IconSubcategory,
    onDismiss: () -> Unit,
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
            titleRes = selectedSubcategory.localizedNameRes(),
            onDone = {
                resultStore.setResult(
                    key = ResultStoreKeys.SELECTED_ICON,
                    value = selectedIcon,
                )
                onDismiss()
                IconCatalog.resetIcons()
            },
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContent(
            selectedSubcategory = selectedSubcategory,
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
    titleRes: StringResource,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = Color.Transparent,
        ),
        title = { Text(text = stringResource(titleRes)) },
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
    selectedSubcategory: IconSubcategory,
    selectedIcon: String,
    onSelectIcon: (String) -> Unit,
) {
    val icons by IconCatalog.iconsFlow.collectAsState()

    LaunchedEffect(Unit) {
        IconCatalog.resetIcons()
        IconCatalog.loadIcons(selectedSubcategory.id)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = icons,
            key = { it.name },
        ) { icon ->
            IconButton(
                modifier = Modifier.size(56.dp),
                onClick = { onSelectIcon(icon.name) },
                colors = IconButtonDefaults.iconButtonColors().copy(
                    containerColor = if (selectedIcon == icon.name) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                )
            ) {
                YabaIcon(
                    modifier = Modifier.size(48.dp),
                    name = icon.name,
                )
            }
        }
    }
}
