package dev.subfly.yaba.ui.creation.notemark.mention

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.core.navigation.creation.BookmarkSelectionRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkMentionSheetRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.NotemarkInlineAction
import dev.subfly.yaba.util.NotemarkMentionSheetResult
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.creation.notemark.NotemarkMentionCreationEvent
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.create_bookmark_title_placeholder
import yaba.composeapp.generated.resources.done

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotemarkMentionCreationContent(route: NotemarkMentionSheetRoute) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val vm = viewModel(key = route.routeId) { NotemarkMentionCreationVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    val fieldAccent = YabaColor.BLUE
    val fieldAccentColor = Color(fieldAccent.iconTintArgb())

    LaunchedEffect(route.routeId) {
        vm.onEvent(
            NotemarkMentionCreationEvent.OnInit(
                initialText = route.initialText,
                initialBookmarkId = route.initialBookmarkId,
                isEdit = route.isEdit,
                editPos = route.editPos,
            ),
        )
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_BOOKMARK)) {
        val selectedId = resultStore.getResult<String>(ResultStoreKeys.SELECTED_BOOKMARK)
            ?: return@LaunchedEffect
        resultStore.removeResult(ResultStoreKeys.SELECTED_BOOKMARK)
        vm.onEvent(NotemarkMentionCreationEvent.OnBookmarkPickedFromSelection(bookmarkId = selectedId))
    }

    val bookmark = state.selectedBookmark
    val canPerformDone = state.mentionText.isNotBlank() && bookmark != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        NotemarkMentionTopBar(
            title = "Mention Bookmark", // TODO: localize
            canPerformDone = canPerformDone,
            onDone = {
                val b = bookmark ?: return@NotemarkMentionTopBar
                resultStore.setResult(
                    ResultStoreKeys.NOTEMARK_MENTION_INSERT,
                    NotemarkMentionSheetResult(
                        text = state.mentionText.trim(),
                        bookmarkId = b.id,
                        bookmarkKindCode = b.kind.code,
                        bookmarkLabel = b.label,
                        action = NotemarkInlineAction.INSERT_OR_UPDATE,
                        editPos = route.editPos,
                    ),
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

        NotemarkMentionTitleField(
            text = state.mentionText,
            fieldAccent = fieldAccent,
            fieldAccentColor = fieldAccentColor,
            onTextChange = { vm.onEvent(NotemarkMentionCreationEvent.OnChangeMentionText(it)) },
        )

        if (bookmark != null) {
            BookmarkItemView(
                model = bookmark,
                appearance = BookmarkAppearance.LIST,
                isInSelectionMode = true,
                onClick = {
                    creationNavigator.add(
                        BookmarkSelectionRoute(
                            selectedBookmarkId = state.selectedBookmarkId,
                        ),
                    )
                },
                onDeleteBookmark = {},
                onShareBookmark = {},
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        creationNavigator.add(
                            BookmarkSelectionRoute(
                                selectedBookmarkId = state.selectedBookmarkId,
                            ),
                        )
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "Select bookmark", // TODO: localize
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Tap to choose a bookmark", // TODO: localize
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (route.isEdit && route.editPos != null && bookmark != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    resultStore.setResult(
                        ResultStoreKeys.NOTEMARK_MENTION_INSERT,
                        NotemarkMentionSheetResult(
                            text = state.mentionText,
                            bookmarkId = bookmark.id,
                            bookmarkKindCode = bookmark.kind.code,
                            bookmarkLabel = bookmark.label,
                            action = NotemarkInlineAction.REMOVE,
                            editPos = route.editPos,
                        ),
                    )
                    if (creationNavigator.size == 2) {
                        appStateManager.onHideCreationContent()
                    }
                    creationNavigator.removeLastOrNull()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                YabaIcon(name = "delete-02", color = YabaColor.RED)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Remove Mention", // TODO: localize
                    color = Color(YabaColor.RED.iconTintArgb()),
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotemarkMentionTitleField(
    text: String,
    fieldAccent: YabaColor,
    fieldAccentColor: Color,
    onTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = fieldAccentColor,
            unfocusedBorderColor = fieldAccentColor.copy(alpha = 0.5f),
        ),
        value = text,
        onValueChange = onTextChange,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(Res.string.create_bookmark_title_placeholder))
        },
        leadingIcon = { YabaIcon(name = "text", color = fieldAccent) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotemarkMentionTopBar(
    title: String,
    canPerformDone: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults
            .topAppBarColors()
            .copy(containerColor = Color.Transparent),
        title = { Text(text = title) },
        navigationIcon = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors().copy(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(text = stringResource(Res.string.cancel)) }
        },
        actions = {
            TextButton(
                enabled = canPerformDone,
                onClick = onDone,
            ) { Text(text = stringResource(Res.string.done)) }
        },
    )
}
