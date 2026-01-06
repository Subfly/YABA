package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.model.utils.uiTitle
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_description_placeholder
import yaba.composeapp.generated.resources.bookmark_title_placeholder
import yaba.composeapp.generated.resources.preview

@Composable
internal fun LinkmarkPreviewContent(
    state: LinkmarkCreationUIState,
    onChangePreviewType: () -> Unit,
    onOpenImageSelector: () -> Unit,
) {
    val color by remember(state.selectedFolder) {
        mutableStateOf(state.selectedFolder?.color ?: YabaColor.BLUE)
    }

    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.preview),
        iconName = "image-03",
        extraContent = {
            TextButton(onClick = onChangePreviewType) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YabaIcon(
                        name = state.contentAppearance.uiIconName(),
                        color = state.selectedFolder?.color ?: YabaColor.BLUE,
                    )
                    Text(
                        text = state.contentAppearance.uiTitle(),
                        color = Color(color.iconTintArgb()),
                    )

                    if (state.contentAppearance == ContentAppearance.CARD) {
                        YabaIcon(
                            name = state.cardImageSizing.uiIconName(),
                            color = Color(color.iconTintArgb()),
                        )
                        Text(
                            text = state.cardImageSizing.uiTitle(),
                            color = Color(color.iconTintArgb()),
                        )
                    }
                }
            }
        }
    )
    Spacer(modifier = Modifier.height(12.dp))
    PreviewContent(
        state = state,
        onClick = onOpenImageSelector,
    )
}

@Composable
private fun PreviewContent(
    state: LinkmarkCreationUIState,
    onClick: () -> Unit,
) {
    val color by remember(state.selectedFolder) {
        mutableStateOf(state.selectedFolder?.color ?: YabaColor.BLUE)
    }
    
    when (state.contentAppearance) {
        ContentAppearance.LIST -> {
            ListItem(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick),
                headlineContent = {
                    Text(
                        text = state.label.ifBlank {
                            stringResource(Res.string.bookmark_title_placeholder)
                        },
                    )
                },
                supportingContent = {
                    Text(
                        text = state.description.ifBlank {
                            stringResource(Res.string.bookmark_description_placeholder)
                        },
                    )
                },
                leadingContent = {
                    if (state.imageData == null) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(color.iconTintArgb()).copy(alpha = 0.5F),
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(16.dp),
                                name = state.selectedLinkType.uiIconName(),
                                color = color,
                            )
                        }
                    } else {
                        YabaImage(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            bytes = state.imageData,
                        )
                    }
                },
            )
        }
        ContentAppearance.CARD -> {
            when (state.cardImageSizing) {
                CardImageSizing.BIG -> {

                }
                CardImageSizing.SMALL -> {

                }
            }
        }
        ContentAppearance.GRID -> {

        }
    }
}
