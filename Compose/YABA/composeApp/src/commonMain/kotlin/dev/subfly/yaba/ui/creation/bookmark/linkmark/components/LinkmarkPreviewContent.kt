package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.model.utils.uiTitle
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_description_placeholder
import yaba.composeapp.generated.resources.bookmark_title_placeholder
import yaba.composeapp.generated.resources.preview

@Composable
internal fun ColumnScope.LinkmarkPreviewContent(
    state: LinkmarkCreationUIState,
    onChangePreviewType: () -> Unit,
    onOpenImageSelector: () -> Unit,
) {
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
                        color = ButtonDefaults.textButtonColors().contentColor,
                    )
                    Text(text = state.contentAppearance.uiTitle())

                    if (state.contentAppearance == ContentAppearance.CARD) {
                        YabaIcon(
                            name = state.cardImageSizing.uiIconName(),
                            color = ButtonDefaults.textButtonColors().contentColor,
                        )
                        Text(text = state.cardImageSizing.uiTitle())
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

                    } else {

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
