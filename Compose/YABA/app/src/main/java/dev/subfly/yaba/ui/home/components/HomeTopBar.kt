package dev.subfly.yaba.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.res.stringResource
import dev.subfly.yaba.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeTopBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    sessionUnlocked: Boolean,
    onLockPrivateSessionClick: () -> Unit,
    onSearchClicked: () -> Unit,
    onSortingChanged: (SortType) -> Unit,
    onSortOrderChanged: (SortOrderType) -> Unit,
) {
    var isMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    if (Platform == YabaPlatform.ANDROID) {
        MediumTopAppBar(
            modifier = modifier,
            scrollBehavior = scrollBehavior,
            title = {
                Text(text = stringResource(R.string.yaba))
            },
            actions = {
                AnimatedVisibility(visible = sessionUnlocked) {
                    IconButton(
                        onClick = onLockPrivateSessionClick,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        YabaIcon(name = "circle-unlock-02")
                    }
                }
                IconButton(
                    onClick = onSearchClicked,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    YabaIcon(name = "search-01")
                }
                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    IconButton(
                        onClick = { isMenuExpanded = !isMenuExpanded },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        YabaIcon(name = "more-horizontal-circle-02")
                    }
                    HomeDropdownMenu(
                        isExpanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        onSortingChanged = onSortingChanged,
                        onSortOrderChanged = onSortOrderChanged,
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
                Text(text = stringResource(R.string.yaba))
            },
            actions = {
                AnimatedVisibility(visible = sessionUnlocked) {
                    IconButton(
                        onClick = onLockPrivateSessionClick,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        YabaIcon(name = "circle-unlock-02")
                    }
                }
                IconButton(
                    onClick = onSearchClicked,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    YabaIcon(name = "search-01")
                }
                Box(
                    modifier = Modifier.wrapContentSize(Alignment.TopStart)
                ) {
                    IconButton(
                        onClick = { isMenuExpanded = !isMenuExpanded },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        YabaIcon(name = "more-horizontal-circle-02")
                    }
                    HomeDropdownMenu(
                        isExpanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        onSortingChanged = onSortingChanged,
                        onSortOrderChanged = onSortOrderChanged,
                        onSettingsClicked = {
                            // TODO: NAVIGATE TO SETTINGS
                        }
                    )
                }
            }
        )
    }
}
