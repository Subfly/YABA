package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.R
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookmarkInfoContent(
    label: String,
    description: String,
    onChangeLabel: (String) -> Unit,
    onChangeDescription: (String) -> Unit,
    selectedFolder: FolderUiModel?,
    isPinned: Boolean,
    onPinToggle: () -> Unit,
    enabled: Boolean = true,
    @StringRes labelPlaceholder: Int = R.string.create_bookmark_title_placeholder,
    @StringRes descriptionPlaceholder: Int = R.string.create_bookmark_description_placeholder,
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
            label = stringResource(R.string.info),
            iconName = "information-circle",
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
        },
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
        leadingIcon = { YabaIcon(name = "paragraph", color = color) },
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        BookmarkPinToggleRow(
            isPinned = isPinned,
            enabled = enabled,
            onClick = onPinToggle,
            folderColor = color,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
