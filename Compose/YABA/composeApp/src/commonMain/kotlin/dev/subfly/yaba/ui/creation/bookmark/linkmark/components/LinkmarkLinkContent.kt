package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.creation.linkmark.LinkmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_creation_link_info_message
import yaba.composeapp.generated.resources.create_bookmark_cleaned_url_placeholder
import yaba.composeapp.generated.resources.create_bookmark_url_placeholder
import yaba.composeapp.generated.resources.link

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkLinkContent(
    state: LinkmarkCreationUIState,
    onChangeUrl: (String) -> Unit,
) {
    val color by remember(state.selectedFolder) {
        mutableStateOf(state.selectedFolder?.color ?: YabaColor.BLUE)
    }

    Spacer(modifier = Modifier.height(24.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.link),
        iconName = "link-04"
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(color.iconTintArgb()),
            unfocusedBorderColor = Color(color.iconTintArgb()).copy(alpha = 0.5F),
        ),
        value = state.url,
        onValueChange = onChangeUrl,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(Res.string.create_bookmark_url_placeholder))
        },
        leadingIcon = {
            YabaIcon(
                name = "link-02",
                color = color,
            )
        }
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            disabledLeadingIconColor = OutlinedTextFieldDefaults.colors().disabledTextColor,
        ),
        value = state.cleanedUrl,
        onValueChange = {},
        enabled = false,
        placeholder = {
            Text(text = stringResource(Res.string.create_bookmark_cleaned_url_placeholder))
        },
        leadingIcon = {
            YabaIcon(
                name = "clean",
                color = color,
            )
        }
    )
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResource(Res.string.bookmark_creation_link_info_message),
        style = MaterialTheme.typography.bodySmallEmphasized,
    )
}
