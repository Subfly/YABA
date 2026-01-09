package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

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
import androidx.compose.ui.util.fastForEach
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.tag.PresentableTagItemView
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagSelectionRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.Text
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_add_tags
import yaba.composeapp.generated.resources.create_bookmark_edit_tags
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_description
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_title
import yaba.composeapp.generated.resources.tags_title
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkTagSelectionContent(
    state: LinkmarkCreationUIState,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

    Spacer(modifier = Modifier.height(4.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.tags_title),
        iconName = "tag-01",
        extraContent = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    creationNavigator.add(
                        TagSelectionRoute(
                            selectedTagIds = state.selectedTags.map {
                                it.id.toString()
                            }
                        )
                    )
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(
                        (state.selectedFolder?.color ?: YabaColor.BLUE).iconTintArgb()
                    )
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YabaIcon(
                        name = if (state.selectedTags.isEmpty()) {
                            "plus-sign"
                        } else {
                            "edit-02"
                        },
                        color = state.selectedFolder?.color ?: YabaColor.BLUE,
                    )
                    Text(
                        text = stringResource(
                            resource = if (state.selectedTags.isEmpty()) {
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
    if (state.selectedTags.isEmpty()) {
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
        state.selectedTags.fastForEach { tag ->
            PresentableTagItemView(
                modifier = Modifier.padding(horizontal = 12.dp),
                model = tag,
                nullModelPresentableColor = YabaColor.BLUE,
                cornerSize = 12.dp,
                onPressed = {
                    // TODO: NAVIGATE TO PARENT SELECTION
                },
                onNavigateToEdit = {
                    creationNavigator.add(
                        TagCreationRoute(tagId = tag.id.toString())
                    )
                    appStateManager.onShowCreationContent()
                },
            )
        }
    }
}
