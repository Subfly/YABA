package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.ui.icon.YabaIcon

@Composable
internal fun LinkmarkLabel(
    modifier: Modifier = Modifier,
    iconName: String,
    label: String,
    extraContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            YabaIcon(name = iconName)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (extraContent != null) {
            extraContent()
        } else {
            Box(modifier = Modifier)
        }
    }
}
