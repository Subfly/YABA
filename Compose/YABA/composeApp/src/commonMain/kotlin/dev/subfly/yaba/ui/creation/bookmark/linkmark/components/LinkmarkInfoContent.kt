package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_description_placeholder
import yaba.composeapp.generated.resources.create_bookmark_type_placeholder
import yaba.composeapp.generated.resources.create_bookmark_url_placeholder
import yaba.composeapp.generated.resources.info

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkInfoContent(
    state: LinkmarkCreationUIState,
    onChangeLabel: (String) -> Unit,
    onClearLabel: () -> Unit,
    onChangeDescription: (String) -> Unit,
    onChangeType: (LinkType) -> Unit,
) {
    val color by remember(state.selectedFolder) {
        mutableStateOf(state.selectedFolder?.color ?: YabaColor.BLUE)
    }

    var isTypesExpanded by rememberSaveable { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(24.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.info),
        iconName = "information-circle"
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
        maxLines = 1,
        value = state.label,
        onValueChange = onChangeLabel,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(Res.string.create_bookmark_url_placeholder))
        },
        leadingIcon = {
            YabaIcon(
                name = "text",
                color = color,
            )
        },
        trailingIcon = {
            if (state.label.isNotEmpty()) {
                IconButton(onClick = onClearLabel) {
                    YabaIcon(
                        name = "cancel-01",
                        color = color,
                    )
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
        value = state.description,
        onValueChange = onChangeDescription,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(Res.string.create_bookmark_description_placeholder))
        },
        leadingIcon = {
            YabaIcon(
                name = "paragraph",
                color = color,
            )
        }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = (0.5F).dp,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(color.iconTintArgb()),
                )
                .clickable(onClick = { isTypesExpanded = true }),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    YabaIcon(
                        name = state.selectedLinkType.uiIconName(),
                        color = color,
                    )
                    Text(text = stringResource(Res.string.create_bookmark_type_placeholder))
                }
                Text(
                    text = state.selectedLinkType.uiTitle(),
                    color = Color(color.iconTintArgb()),
                )
            }
        }
        DropdownMenuPopup(
            expanded = isTypesExpanded,
            onDismissRequest = { isTypesExpanded = false },
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = LinkType.entries.size,
                )
            ) {
                LinkType.entries.fastForEachIndexed { index, type ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, LinkType.entries.size),
                        checked = type == state.selectedLinkType,
                        onCheckedChange = { _ ->
                            onChangeType(type)
                            isTypesExpanded = false
                        },
                        leadingIcon = { YabaIcon(name = type.uiIconName()) },
                        text = { Text(text = type.uiTitle()) }
                    )
                }
            }
        }
    }
}