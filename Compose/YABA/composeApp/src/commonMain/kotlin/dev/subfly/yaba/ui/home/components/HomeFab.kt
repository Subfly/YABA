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
import androidx.compose.ui.graphics.Color
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.FolderCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalUuidApi::class)
@Composable
internal fun HomeFab(modifier: Modifier = Modifier) {
    val userPreferences = LocalUserPreferences.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

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
                containerColor = { _ -> Color(YabaColor.BLUE.iconTintArgb()) }
            ) {
                YabaIcon(
                    modifier = Modifier.rotate(fabIconRotation),
                    name = "add-01",
                    color = Color.White,
                )
            }
        },
        content = {
            FloatingActionButtonMenuItem(
                onClick = {
                    creationNavigator.add(BookmarkCreationRoute())
                    isFabExpanded = false
                    appStateManager.onShowCreationContent()
                },
                text = {},
                containerColor = Color(YabaColor.BLUE.iconTintArgb()),
                icon = { YabaIcon(name = "bookmark-02", color = Color.White) }
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    creationNavigator.add(FolderCreationRoute(folderId = null))
                    isFabExpanded = false
                    appStateManager.onShowCreationContent()
                },
                text = {},
                containerColor = Color(YabaColor.BLUE.iconTintArgb()),
                icon = { YabaIcon(name = "folder-01", color = Color.White) }
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    creationNavigator.add(TagCreationRoute(tagId = null))
                    isFabExpanded = false
                    appStateManager.onShowCreationContent()
                },
                text = {},
                containerColor = Color(YabaColor.BLUE.iconTintArgb()),
                icon = { YabaIcon(name = "tag-01", color = Color.White) }
            )
        }
    )
}