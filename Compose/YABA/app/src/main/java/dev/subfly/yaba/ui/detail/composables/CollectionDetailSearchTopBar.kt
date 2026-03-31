package dev.subfly.yaba.ui.detail.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.model.utils.YabaColor

private const val TOP_SCRIM_HEIGHT_DP = 56

internal fun collectionDetailAccentColor(color: YabaColor?): YabaColor {
    return if (color == null || color == YabaColor.NONE) YabaColor.BLUE else color
}

internal fun collectionDetailSearchFieldTint(accentColor: YabaColor): Color =
    Color(accentColor.iconTintArgb()).copy(alpha = 0.5f)

/**
 * Same accent-tinted container (0.5 alpha) as bookmark detail toolbar icon buttons, with
 * [MaterialTheme] on-surface content color so icons keep their usual look.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun collectionDetailIconButtonColors(accentColor: YabaColor): IconButtonColors {
    val folderTintBackground = Color(accentColor.iconTintArgb()).copy(alpha = 0.5f)
    val scheme = MaterialTheme.colorScheme
    return IconButtonDefaults.iconButtonColors(
        containerColor = folderTintBackground,
        contentColor = scheme.onSurface,
        disabledContainerColor = folderTintBackground,
        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun searchScreenIconButtonColors(): IconButtonColors {
    val scheme = MaterialTheme.colorScheme
    val bg = scheme.surfaceContainerHigh.copy(alpha = 0.5f)
    return IconButtonDefaults.iconButtonColors(
        containerColor = bg,
        contentColor = scheme.onSurface,
        disabledContainerColor = bg,
        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CollectionDetailSearchTopBar(
    accentColor: YabaColor,
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val scrimStrong = scheme.surface.copy(alpha = 0.82f)
    val scrimSoft = scheme.surface.copy(alpha = 0.38f)
    val accentTint = collectionDetailSearchFieldTint(accentColor)
    val inputFieldColors =
        SearchBarDefaults.inputFieldColors(
            focusedContainerColor = accentTint,
            unfocusedContainerColor = accentTint,
            disabledContainerColor = accentTint,
        )
    val searchBarColors =
        SearchBarDefaults.colors(
            containerColor = accentTint,
            inputFieldColors = inputFieldColors,
        )
    val appBarColors =
        SearchBarDefaults.appBarWithSearchColors(
            searchBarColors = searchBarColors,
            scrolledSearchBarContainerColor = Color.Unspecified,
            appBarContainerColor = Color.Transparent,
            scrolledAppBarContainerColor = Color.Unspecified,
            appBarNavigationIconColor = scheme.onSurface,
            appBarActionIconColor = scheme.onSurface,
        )

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(TOP_SCRIM_HEIGHT_DP.dp)
                .graphicsLayer { clip = false }
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to scrimStrong,
                            0.42f to scrimSoft,
                            0.82f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
                )
                .blur(radius = 16.dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            AppBarWithSearch(
                modifier = Modifier.fillMaxWidth(),
                state = searchBarState,
                scrollBehavior = null,
                colors = appBarColors,
                inputField = inputField,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        }
    }
}

/**
 * Global search screen: same blur + [AppBarWithSearch] chrome as collection detail, but the field
 * and icon buttons use [ColorScheme.surfaceContainerHigh] at 0.5 alpha (no folder/tag accent).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SearchScreenChromeTopBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val scrimStrong = scheme.surface.copy(alpha = 0.82f)
    val scrimSoft = scheme.surface.copy(alpha = 0.38f)
    val fieldTint = scheme.surfaceContainerHigh.copy(alpha = 0.5f)
    val inputFieldColors =
        SearchBarDefaults.inputFieldColors(
            focusedContainerColor = fieldTint,
            unfocusedContainerColor = fieldTint,
            disabledContainerColor = fieldTint,
        )
    val searchBarColors =
        SearchBarDefaults.colors(
            containerColor = fieldTint,
            inputFieldColors = inputFieldColors,
        )
    val appBarColors =
        SearchBarDefaults.appBarWithSearchColors(
            searchBarColors = searchBarColors,
            scrolledSearchBarContainerColor = Color.Unspecified,
            appBarContainerColor = Color.Transparent,
            scrolledAppBarContainerColor = Color.Unspecified,
            appBarNavigationIconColor = scheme.onSurface,
            appBarActionIconColor = scheme.onSurface,
        )

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(TOP_SCRIM_HEIGHT_DP.dp)
                .graphicsLayer { clip = false }
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to scrimStrong,
                            0.42f to scrimSoft,
                            0.82f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
                )
                .blur(radius = 16.dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            AppBarWithSearch(
                modifier = Modifier.fillMaxWidth(),
                state = searchBarState,
                scrollBehavior = null,
                colors = appBarColors,
                inputField = inputField,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        }
    }
}
