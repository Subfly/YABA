package dev.subfly.yaba.ui.search

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.ui.layout.ContentLayoutConfig
import dev.subfly.yabacore.ui.layout.YabaBookmarkLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(modifier: Modifier = Modifier) {
    val userPreferences = LocalUserPreferences.current

    val searchBarState = rememberSearchBarState()
    val searchTextState = rememberTextFieldState()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBarWithSearch(
                scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        textFieldState = searchTextState,
                        searchBarState = searchBarState,
                        onSearch = { value ->
                            // TODO: SEARCH IN BOOKMARKS
                        },
                        leadingIcon = {

                        },
                        trailingIcon = {

                        }
                    )
                }
            )
        }
    ) { paddings ->
        YabaBookmarkLayout(
            modifier = Modifier.padding(paddings),
            bookmarks = emptyList(),
            layoutConfig = ContentLayoutConfig(
                collectionAppearance = userPreferences.preferredCollectionAppearance,
                bookmarkAppearance = userPreferences.preferredBookmarkAppearance,
                cardImageSizing = userPreferences.preferredCardImageSizing,
            ),
            onDrop = {

            },
            itemContent = { model, isDragging, appearance, cardImageSizing ->

            }
        )
    }
}
