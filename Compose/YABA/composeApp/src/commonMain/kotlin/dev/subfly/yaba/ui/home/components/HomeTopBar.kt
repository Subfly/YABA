package dev.subfly.yaba.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.yaba

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    onOptionsClicked: () -> Unit,
) {
    var isMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    MediumTopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        title = {
            Text(text = stringResource(Res.string.yaba))
        },
        actions = {
            IconButton(onClick = onOptionsClicked) {
                YabaIcon(name = "search-01")
            }
            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopStart)
            ) {
                IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                    YabaIcon(name = "more-horizontal-circle-02")
                }
                HomeDropdownMenu(
                    isExpanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    onSettingsClicked = {
                        // TODO: NAVIGATE TO SETTINGS
                    }
                )
            }
        }
    )
}
