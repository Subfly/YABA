package dev.subfly.yaba.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.ResultStoreKeys
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.create_tag_placeholder
import yaba.composeapp.generated.resources.create_tag_title
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.edit_tag_title
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalUuidApi::class
)
@Composable
fun TagCreationContent(
    isStartingFlow: Boolean,
    onOpenIconSelection: () -> Unit,
    onOpenColorSelection: (YabaColor) -> Unit,
    onDismiss: () -> Unit,
    tagId: Uuid? = null,
) {
    val resultStore = LocalResultStore.current

    var tag by rememberSaveable { mutableStateOf<TagUiModel?>(null) }
    val tagTitleState = rememberTextFieldState(initialText = tag?.label ?: "")
    var selectedColor by rememberSaveable { mutableStateOf(tag?.color ?: YabaColor.BLUE) }

    LaunchedEffect(tagId) {
        tagId?.let { nonNullId ->
            tag = TagManager.getTag(nonNullId)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_COLOR)) {
        resultStore.getResult<YabaColor>(ResultStoreKeys.SELECTED_COLOR)?.let { newColor ->
            selectedColor = newColor
            resultStore.removeResult(ResultStoreKeys.SELECTED_COLOR)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            isStartingFlow = isStartingFlow,
            canPerformDone = tagTitleState.text.isNotBlank(),
            isEditing = tag != null,
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(12.dp))
        CreationContent(
            tagTitleState = tagTitleState,
            selectedColor = selectedColor,
            onOpenIconSelection = onOpenIconSelection,
            onOpenColorSelection = {
                onOpenColorSelection(selectedColor)
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
    isStartingFlow: Boolean,
    isEditing: Boolean,
    canPerformDone: Boolean,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = Color.Transparent,
        ),
        title = {
            Text(
                text = stringResource(
                    resource = if (isEditing) {
                        Res.string.edit_tag_title
                    } else {
                        Res.string.create_tag_title
                    }
                ),
            )
        },
        navigationIcon = {
            if (isStartingFlow) {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                ) { Text(text = stringResource(Res.string.cancel)) }
            } else {
                IconButton(onClick = onDismiss) {
                    YabaIcon(name = "arrow-left-01")
                }
            }
        },
        actions = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                enabled = canPerformDone,
                onClick = {
                    // TODO: SAVE TAG
                    onDismiss()
                }
            ) { Text(text = stringResource(Res.string.done)) }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreationContent(
    tagTitleState: TextFieldState,
    selectedColor: YabaColor,
    onOpenIconSelection: () -> Unit,
    onOpenColorSelection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            modifier = Modifier
                .weight(1F)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = Color(selectedColor.iconTintArgb()).copy(alpha = 0.2F)
            ),
            shapes = ButtonDefaults.shapes(),
            onClick = onOpenIconSelection,
        ) {
            YabaIcon(
                name = "tag-01",
                color = selectedColor,
            )
        }
        OutlinedTextField(
            modifier = Modifier.weight(4F).fillMaxWidth(),
            state = tagTitleState,
            lineLimits = TextFieldLineLimits.SingleLine,
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text(text = stringResource(Res.string.create_tag_placeholder)) },
        )
        Button(
            modifier = Modifier
                .weight(1F)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = Color(selectedColor.iconTintArgb()).copy(alpha = 0.2F)
            ),
            shapes = ButtonDefaults.shapes(),
            onClick = onOpenColorSelection,
        ) {
            YabaIcon(
                name = "paint-board",
                color = selectedColor,
            )
        }
    }
}
