package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.util.iconTintArgb
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_description_placeholder
import yaba.composeapp.generated.resources.create_bookmark_title_placeholder
import yaba.composeapp.generated.resources.info

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookmarkInfoContent(
    label: String,
    description: String,
    onChangeLabel: (String) -> Unit,
    onChangeDescription: (String) -> Unit,
    selectedFolder: FolderUiModel?,
    enabled: Boolean = true,
    labelPlaceholder: StringResource = Res.string.create_bookmark_title_placeholder,
    descriptionPlaceholder: StringResource = Res.string.create_bookmark_description_placeholder,
    showClearLabelButton: Boolean = false,
    showInfoLabel: Boolean = true,
    onClearLabel: (() -> Unit)? = null,
    nullModelPresentableColor: YabaColor = YabaColor.BLUE,
) {
    val color by remember(selectedFolder) {
        mutableStateOf(selectedFolder?.color ?: nullModelPresentableColor)
    }

    if (showInfoLabel) {
        Spacer(modifier = Modifier.height(24.dp))
        BookmarkCreationLabel(
            label = stringResource(Res.string.info),
            iconName = "information-circle"
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(color.iconTintArgb()),
            unfocusedBorderColor = Color(color.iconTintArgb()).copy(alpha = 0.5F),
        ),
        enabled = enabled,
        value = label,
        onValueChange = onChangeLabel,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(text = stringResource(labelPlaceholder)) },
        leadingIcon = { YabaIcon(name = "text", color = color) },
        trailingIcon = {
            if (showClearLabelButton && label.isNotEmpty() && onClearLabel != null) {
                IconButton(onClick = onClearLabel) {
                    YabaIcon(name = "cancel-01", color = color)
                }
            }
        }
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        modifier = Modifier
            .heightIn(min = 120.dp, max = 240.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(color.iconTintArgb()),
            unfocusedBorderColor = Color(color.iconTintArgb()).copy(alpha = 0.5F),
        ),
        enabled = enabled,
        value = description,
        onValueChange = onChangeDescription,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(text = stringResource(descriptionPlaceholder)) },
        leadingIcon = { YabaIcon(name = "paragraph", color = color) }
    )
}
