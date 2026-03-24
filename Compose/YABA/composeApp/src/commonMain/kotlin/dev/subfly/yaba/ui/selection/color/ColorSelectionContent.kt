package dev.subfly.yaba.ui.selection.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.select_color_title

@Composable
fun ColorSelectionContent(
    currentSelectedColor: YabaColor?,
    allowTransparent: Boolean = true,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val resultStore = LocalResultStore.current
    val appStateManager = LocalAppStateManager.current

    var selectedColor by rememberSaveable(currentSelectedColor) {
        mutableStateOf(
            when {
                currentSelectedColor != null -> currentSelectedColor
                allowTransparent -> YabaColor.NONE
                else -> YabaColor.YELLOW
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            isStartingFlow = creationNavigator.size <= 2,
            onDone = {
                resultStore.setResult(
                    key = ResultStoreKeys.SELECTED_COLOR,
                    value = selectedColor,
                )
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
            onDismiss = {
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContent(
            selectedColor = selectedColor,
            onSelectColor = { newColor -> selectedColor = newColor },
            allowTransparent = allowTransparent,
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
    isStartingFlow: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = Color.Transparent,
        ),
        title = { Text(text = stringResource(Res.string.select_color_title)) },
        navigationIcon = {
            if (isStartingFlow) {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
            } else {
                IconButton(onClick = onDismiss) {
                    YabaIcon(name = "arrow-left-01")
                }
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
    selectedColor: YabaColor?,
    onSelectColor: (YabaColor) -> Unit,
    allowTransparent: Boolean = true,
) {
    val colorsToShow = remember(allowTransparent) {
        if (allowTransparent) YabaColor.entries.toList()
        else YabaColor.entries.filter { it != YabaColor.NONE }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        maxItemsInEachRow = 5,
    ) {
        colorsToShow.fastForEach { color ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(color.iconTintArgb()),
                        shape = CircleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = if (color == selectedColor) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(
                        onClick = {
                            onSelectColor(color)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (color == YabaColor.NONE) {
                    YabaIcon(name = "paint-brush-02")
                }
            }
        }
    }
}
