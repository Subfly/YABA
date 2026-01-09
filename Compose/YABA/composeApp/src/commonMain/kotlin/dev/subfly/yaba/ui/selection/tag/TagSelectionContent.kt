package dev.subfly.yaba.ui.selection.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.tag.PresentableTagItemView
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.selection.TagSelectionEvent
import dev.subfly.yabacore.state.selection.TagSelectionUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.select_tags_no_more_tags_left_description
import yaba.composeapp.generated.resources.select_tags_no_more_tags_left_title
import yaba.composeapp.generated.resources.select_tags_no_tags_available_description
import yaba.composeapp.generated.resources.select_tags_no_tags_available_title
import yaba.composeapp.generated.resources.select_tags_no_tags_found_in_search_description
import yaba.composeapp.generated.resources.select_tags_no_tags_found_in_search_title
import yaba.composeapp.generated.resources.select_tags_no_tags_selected_message
import yaba.composeapp.generated.resources.select_tags_no_tags_selected_title
import yaba.composeapp.generated.resources.select_tags_title
import yaba.composeapp.generated.resources.selectable_tags_label_title
import yaba.composeapp.generated.resources.selected_tags_label_title
import yaba.composeapp.generated.resources.tags_search_prompt
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun TagSelectionContent(alreadySelectedTagIds: List<String>) {
    val creationNavigator = LocalCreationContentNavigator.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { TagSelectionVM() }
    val state by vm.state

    LaunchedEffect(alreadySelectedTagIds) {
        vm.onEvent(TagSelectionEvent.OnInit(selectedTagIds = alreadySelectedTagIds))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TopBar(
            onDone = {
                resultStore.setResult(
                    key = ResultStoreKeys.SELECTED_TAGS,
                    value = vm.getSelectedTags(),
                )
                creationNavigator.removeLastOrNull()
            }
        )
        SearchBarContent(
            currentQuery = state.searchQuery,
            onQueryChange = { newQuery ->
                vm.onEvent(
                    event = TagSelectionEvent.OnSearchQueryChanged(query = newQuery)
                )
            },
            onSearch = { finalQuery ->
                vm.onEvent(
                    event = TagSelectionEvent.OnSearchQueryChanged(query = finalQuery)
                )
            },
            onClear = {
                vm.onEvent(
                    event = TagSelectionEvent.OnSearchQueryChanged(query = "")
                )
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContent(
            state = state,
            onTagSelected = { model ->
                vm.onEvent(event = TagSelectionEvent.OnSelectTag(model))
            },
            onTagDeselected = { model ->
                vm.onEvent(event = TagSelectionEvent.OnDeselectTag(model))
            },
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
) {
    val creationNavigator = LocalCreationContentNavigator.current

    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = Color.Transparent,
        ),
        title = { Text(text = stringResource(Res.string.select_tags_title)) },
        navigationIcon = {
            IconButton(onClick = creationNavigator::removeLastOrNull) {
                YabaIcon(name = "arrow-left-01")
            }
        },
        actions = {
            IconButton(onClick = { creationNavigator.add(TagCreationRoute(tagId = null)) }) {
                YabaIcon(
                    name = "add-01",
                    color = ButtonDefaults.textButtonColors().contentColor,
                )
            }

            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDone,
            ) { Text(text = stringResource(Res.string.done)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarContent(
    currentQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
) {
    val searchbarState = rememberSearchBarState()

    SearchBar(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        state = searchbarState,
        inputField = {
            SearchBarDefaults.InputField(
                query = currentQuery,
                expanded = false,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onExpandedChange = {},
                placeholder = { Text(text = stringResource(Res.string.tags_search_prompt)) },
                leadingIcon = { YabaIcon(name = "search-01") },
                trailingIcon = { IconButton(onClick = onClear) { YabaIcon(name = "cancel-01") } }
            )
        }
    )
}

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionContent(
    state: TagSelectionUIState,
    onTagSelected: (TagUiModel) -> Unit,
    onTagDeselected: (TagUiModel) -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        item {
            Row(
                modifier = Modifier.animateItem().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                YabaIcon(name = "checkmark-badge-02")
                Text(
                    text = stringResource(Res.string.selected_tags_label_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            state.isLoading -> {
                item {
                    Box(
                        modifier = Modifier.animateItem().fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) { CircularWavyProgressIndicator() }
                }
            }

            state.selectedTags.isEmpty() -> {
                item {
                    NoContentBox(
                        modifier = Modifier.animateItem(),
                        iconName = "tags",
                        labelRes = Res.string.select_tags_no_tags_selected_title,
                        message = { Text(text = stringResource(Res.string.select_tags_no_tags_selected_message)) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            else -> {
                items(
                    items = state.selectedTags,
                    key = { it.id.toString() },
                ) { model ->
                    PresentableTagItemView(
                        modifier = Modifier.animateItem().padding(horizontal = 12.dp),
                        model = model,
                        cornerSize = 12.dp,
                        nullModelPresentableColor = YabaColor.BLUE,
                        onPressed = { onTagDeselected(model) },
                        onNavigateToEdit = { creationNavigator.add(TagCreationRoute(tagId = model.id.toString())) }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }

        item {
            Row(
                modifier = Modifier.animateItem().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                YabaIcon(name = "tag-01")
                Text(
                    text = stringResource(Res.string.selectable_tags_label_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            state.isLoading -> {
                item {
                    Box(
                        modifier = Modifier.animateItem().fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) { CircularWavyProgressIndicator() }
                }
            }

            state.hasNoTags -> {
                item {
                    NoContentBox(
                        modifier = Modifier.animateItem(),
                        iconName = "tags",
                        labelRes = Res.string.select_tags_no_tags_available_title,
                        message = { Text(text = stringResource(Res.string.select_tags_no_tags_available_description)) },
                    )
                }
            }

            state.searchQuery.isEmpty() && state.availableTags.isEmpty() -> {
                item {
                    NoContentBox(
                        modifier = Modifier.animateItem(),
                        iconName = "tags",
                        labelRes = Res.string.select_tags_no_more_tags_left_title,
                        message = { Text(text = stringResource(Res.string.select_tags_no_more_tags_left_description)) },
                    )
                }
            }

            state.searchQuery.isNotEmpty() && state.availableTags.isEmpty() -> {
                item {
                    NoContentBox(
                        modifier = Modifier.animateItem(),
                        iconName = "search-01",
                        labelRes = Res.string.select_tags_no_tags_found_in_search_title,
                        message = { Text(text = stringResource(Res.string.select_tags_no_tags_found_in_search_description, state.searchQuery)) },
                    )
                }
            }

            else -> {
                items(
                    items = state.availableTags,
                    key = { it.id.toString() },
                ) { model ->
                    PresentableTagItemView(
                        modifier = Modifier.animateItem().padding(horizontal = 12.dp),
                        model = model,
                        cornerSize = 12.dp,
                        nullModelPresentableColor = YabaColor.BLUE,
                        onPressed = { onTagSelected(model) },
                        onNavigateToEdit = { creationNavigator.add(TagCreationRoute(tagId = model.id.toString())) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoContentBox(
    modifier: Modifier = Modifier,
    iconName: String,
    labelRes: StringResource,
    message: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        NoContentView(
            modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
            iconName = iconName,
            labelRes = labelRes,
            message = message,
        )
    }
}
