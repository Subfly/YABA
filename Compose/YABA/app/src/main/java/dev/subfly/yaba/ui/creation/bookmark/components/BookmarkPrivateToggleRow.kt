package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.model.utils.YabaColor

@Composable
fun BookmarkPrivateToggleRow(
    isPrivate: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    folderColor: YabaColor,
) {
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            YabaIcon(
                name = if (isPrivate) "circle-unlock-02" else "circle-lock-02",
                color = folderColor,
            )
            Text(
                text = if (isPrivate) "Not Private" else "Private",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Switch(
            checked = isPrivate,
            onCheckedChange = { newChecked -> if (newChecked != isPrivate) onClick() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(folderColor.iconTintArgb()),
            ),
        )
    }
}
