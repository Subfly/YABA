package dev.subfly.yaba.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.ui.icon.YabaIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeFab(
    modifier: Modifier = Modifier,
    onCreateBookmark: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateTag: () -> Unit,
) {
    val userPreferences = LocalUserPreferences.current

    var isFabExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    val fabIconRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 45F else 0F
    )

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = isFabExpanded,
        horizontalAlignment = when (userPreferences.preferredFabPosition) {
            FabPosition.LEFT -> Alignment.Start
            FabPosition.RIGHT -> Alignment.End
            FabPosition.CENTER -> Alignment.CenterHorizontally
        },
        button = {
            ToggleFloatingActionButton(
                checked = isFabExpanded,
                onCheckedChange = { isFabExpanded = it },
            ) {
                YabaIcon(
                    modifier = Modifier.rotate(fabIconRotation),
                    name = "add-01",
                )
            }
        },
        content = {
            FloatingActionButtonMenuItem(
                onClick = {
                    isFabExpanded = false
                    onCreateBookmark()
                },
                text = {},
                icon = { YabaIcon(name = "bookmark-02") }
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    isFabExpanded = false
                    onCreateFolder()
                },
                text = {},
                icon = { YabaIcon(name = "folder-01") }
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    isFabExpanded = false
                    onCreateTag()
                },
                text = {},
                icon = { YabaIcon(name = "tag-01") }
            )
        }
    )
}