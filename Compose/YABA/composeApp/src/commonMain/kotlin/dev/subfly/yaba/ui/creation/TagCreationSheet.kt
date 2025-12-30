package dev.subfly.yaba.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.AnimatedBottomSheet
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.create_tag_placeholder
import yaba.composeapp.generated.resources.create_tag_title
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.edit_tag_title

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun TagCreationSheet(
    modifier: Modifier = Modifier,
    shouldShow: Boolean,
    onDismiss: () -> Unit,
    tag: TagUiModel? = null,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val tagTitleState = rememberTextFieldState(initialText = tag?.label ?: "")
    var selectedColor by remember { mutableStateOf(tag?.color ?: YabaColor.BLUE) }

    AnimatedBottomSheet(
        modifier = modifier,
        isVisible = shouldShow,
        sheetState = sheetState,
        showDragHandle = true,
        onDismissRequest = onDismiss,
    ) {
        TopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            isEditing = tag != null,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
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
                onClick = {
                    // TODO: OPEN ICON PICKER
                },
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
                onClick = {
                    // TODO: OPEN COLOR PICKER
                },
            ) {
                YabaIcon(
                    name = "paint-board",
                    color = selectedColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    isEditing: Boolean,
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
            OutlinedButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss,
            ) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
        actions = {
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    // TODO: SAVE TAG
                    onDismiss()
                }
            ) {
                Text(text = stringResource(Res.string.done))
            }
        }
    )
}
