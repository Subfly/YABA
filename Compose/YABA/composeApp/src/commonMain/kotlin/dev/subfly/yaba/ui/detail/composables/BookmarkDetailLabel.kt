package dev.subfly.yaba.ui.detail.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon

@Composable
internal fun BookmarkDetailLabel(
    modifier: Modifier = Modifier,
    iconName: String,
    label: String,
) {
    Row(
        modifier = modifier.padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        YabaIcon(name = iconName)
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
