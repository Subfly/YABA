package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.item.tag.PresentableTagItemView
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.util.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_add_tags
import yaba.composeapp.generated.resources.create_bookmark_edit_tags
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_description
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_title
import yaba.composeapp.generated.resources.tags_title

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookmarkTagSelectionContent(
    selectedFolder: FolderUiModel?,
    selectedTags: List<TagUiModel>,
    onSelectTags: () -> Unit,
    onNavigateToEdit: (TagUiModel) -> Unit,
    nullModelPresentableColor: YabaColor = YabaColor.BLUE,
) {
    val color = selectedFolder?.color ?: nullModelPresentableColor

    Spacer(modifier = Modifier.height(4.dp))
    BookmarkCreationLabel(
        label = stringResource(Res.string.tags_title),
        iconName = "tag-01",
        extraContent = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onSelectTags,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(color.iconTintArgb())
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YabaIcon(
                        name = if (selectedTags.isEmpty()) {
                            "plus-sign"
                        } else {
                            "edit-02"
                        },
                        color = color,
                    )
                    Text(
                        text = stringResource(
                            resource = if (selectedTags.isEmpty()) {
                                Res.string.create_bookmark_add_tags
                            } else {
                                Res.string.create_bookmark_edit_tags
                            }
                        ),
                    )
                }
            }
        }
    )
    if (selectedTags.isEmpty()) {
        Box(
            modifier = Modifier
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
                iconName = "tag-01",
                labelRes = Res.string.create_bookmark_no_tags_selected_title,
                message = { Text(text = stringResource(Res.string.create_bookmark_no_tags_selected_description)) },
            )
        }
    } else {
        selectedTags.fastForEachIndexed { index, tag ->
            PresentableTagItemView(
                modifier = Modifier.padding(horizontal = 12.dp),
                model = tag,
                nullModelPresentableColor = nullModelPresentableColor,
                cornerSize = 12.dp,
                onPressed = {
                    // TODO: NAVIGATE TO PARENT SELECTION
                },
                onNavigateToEdit = { onNavigateToEdit(tag) },
                index = index,
                count = selectedTags.size,
            )
        }
    }
}
