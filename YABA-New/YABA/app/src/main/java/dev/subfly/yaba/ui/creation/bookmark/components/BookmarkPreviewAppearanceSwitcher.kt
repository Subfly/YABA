package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.model.utils.uiIconName

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookmarkPreviewAppearanceSwitcher(
    bookmarkAppearance: BookmarkAppearance,
    cardImageSizing: CardImageSizing,
    color: YabaColor,
    onClick: () -> Unit,
) {
    TextButton(
        shapes = ButtonDefaults.shapes(),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            YabaIcon(
                name = bookmarkAppearance.uiIconName(),
                color = color,
            )
            Text(
                text = bookmarkAppearance.uiTitle(),
                color = Color(color.iconTintArgb()),
            )
            if (bookmarkAppearance == BookmarkAppearance.CARD) {
                YabaIcon(
                    name = cardImageSizing.uiIconName(),
                    color = Color(color.iconTintArgb()),
                )
                Text(
                    text = cardImageSizing.uiTitle(),
                    color = Color(color.iconTintArgb()),
                )
            }
        }
    }
}
