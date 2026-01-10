package dev.subfly.yaba.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CenterAlignedTopAppBar
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
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.yaba

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchClicked: () -> Unit,
    onBookmarkAppearanceChanged: (BookmarkAppearance) -> Unit,
    onCardSizingChanged: (CardImageSizing) -> Unit,
    onSortingChanged: (SortType) -> Unit,
) {
    var isMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    if (Platform == YabaPlatform.ANDROID) {
        MediumTopAppBar(
            modifier = modifier,
            scrollBehavior = scrollBehavior,
            title = {
                Text(text = stringResource(Res.string.yaba))
            },
            actions = {
                IconButton(onClick = onSearchClicked) {
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
                        onBookmarkAppearanceChanged = onBookmarkAppearanceChanged,
                        onSortingChanged = onSortingChanged,
                        onCardSizingChanged = onCardSizingChanged,
                        onSettingsClicked = {
                            // TODO: NAVIGATE TO SETTINGS
                        }
                    )
                }
            }
        )
    } else {
        CenterAlignedTopAppBar(
            modifier = modifier,
            title = {
                Text(text = stringResource(Res.string.yaba))
            },
            actions = {
                IconButton(onClick = onSearchClicked) {
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
                        onBookmarkAppearanceChanged = onBookmarkAppearanceChanged,
                        onCardSizingChanged = onCardSizingChanged,
                        onSortingChanged = onSortingChanged,
                        onSettingsClicked = {
                            // TODO: NAVIGATE TO SETTINGS
                        }
                    )
                }
            }
        )
    }
}
