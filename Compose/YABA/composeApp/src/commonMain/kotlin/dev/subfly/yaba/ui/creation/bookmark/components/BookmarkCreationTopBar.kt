package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.create_bookmark_title
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.edit_bookmark_title

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookmarkCreationTopBar(
    canPerformDone: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    CenterAlignedTopAppBar(
        modifier = modifier.padding(horizontal = 8.dp),
        colors =
            TopAppBarDefaults
                .topAppBarColors()
                .copy(containerColor = Color.Transparent),
        title = {
            Text(
                text =
                    stringResource(
                        resource =
                            if (isEditing) {
                                Res.string.edit_bookmark_title
                            } else {
                                Res.string.create_bookmark_title
                            },
                    ),
            )
        },
        navigationIcon = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    if (creationNavigator.size == 2) appStateManager.onHideCreationContent()
                    creationNavigator.removeLastOrNull()
                },
                colors =
                    ButtonDefaults.textButtonColors()
                        .copy(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
            ) { Text(text = stringResource(Res.string.cancel)) }
        },
        actions = {
            AnimatedContent(
                targetState = isSaving,
            ) { saving ->
                if (saving) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(12.dp))
                } else {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        enabled = canPerformDone,
                        onClick = onDone,
                    ) { Text(text = stringResource(Res.string.done)) }
                }
            }
        },
    )
}
