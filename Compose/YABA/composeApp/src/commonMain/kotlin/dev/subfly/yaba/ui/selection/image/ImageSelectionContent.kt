package dev.subfly.yaba.ui.selection.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.selection.ImageSelectionEvent
import dev.subfly.yabacore.state.selection.ImageSelectionUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.select_image_no_image_found_message
import yaba.composeapp.generated.resources.select_image_no_image_found_title
import yaba.composeapp.generated.resources.select_image_title

@Composable
fun ImageSelectionContent(
    currentSelectedImageURL: String?,
    imageDataMap: Map<String, ByteArray> = emptyMap(),
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { ImageSelectionVM() }
    val state by vm.state

    LaunchedEffect(currentSelectedImageURL, imageDataMap) {
        vm.onEvent(
            ImageSelectionEvent.OnInit(
                imageDataMap = imageDataMap,
                selectedImageUrl = currentSelectedImageURL,
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TopBar(
            state = state,
            onDone = {
                resultStore.setResult(
                    key = ResultStoreKeys.SELECTED_IMAGE,
                    value = vm.getSelectedImageUrl(),
                )
                creationNavigator.removeLastOrNull()
            }
        )
        SelectionContent(
            state = state,
            onSelectImage = { newImageUrl ->
                vm.onEvent(event = ImageSelectionEvent.OnSelectImage(imageUrl = newImageUrl))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    state: ImageSelectionUIState,
    onDone: () -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current

    CenterAlignedTopAppBar(
        modifier = modifier,
        colors =
            TopAppBarDefaults.topAppBarColors()
                .copy(
                    containerColor = Color.Transparent,
                ),
        title = { Text(text = stringResource(Res.string.select_image_title)) },
        navigationIcon = {
            IconButton(onClick = creationNavigator::removeLastOrNull) {
                YabaIcon(name = "arrow-left-01")
            }
        },
        actions = {
            TextButton(
                enabled = state.selectedImageUrl != null,
                shapes = ButtonDefaults.shapes(),
                onClick = onDone,
            ) { Text(text = stringResource(Res.string.done)) }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionContent(
    state: ImageSelectionUIState,
    onSelectImage: (String) -> Unit,
) {
    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularWavyProgressIndicator() }
        }

        state.imageDataMap.isEmpty() -> {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                NoContentView(
                    modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                    iconName = "image-not-found-01",
                    labelRes = Res.string.select_image_no_image_found_title,
                    message = {
                        Text(
                            text =
                                stringResource(
                                    Res.string.select_image_no_image_found_message
                                )
                        )
                    },
                )
            }
        }

        else -> {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.padding(horizontal = 12.dp),
                columns = StaggeredGridCells.Adaptive(minSize = 180.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = state.imageDataMap.keys.toList()) { imageUrl ->
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        YabaImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = { onSelectImage(imageUrl) }),
                            bytes = state.getImageData(imageUrl),
                        )
                        if (state.selectedImageUrl == imageUrl) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = Color.White.copy(alpha = 0.5F),
                                        shape = RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                YabaIcon(
                                    name = "checkmark-circle-02",
                                    color = YabaColor.BLUE,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
