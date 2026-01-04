package dev.subfly.yaba.ui.home.components

import androidx.compose.foundation.layout.Arrangement
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
import dev.subfly.yabacore.ui.layout.YabaContentLayoutScope
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun YabaContentLayoutScope.HomeTitleContent(
    modifier: Modifier = Modifier,
    title: StringResource,
    iconName: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        YabaIcon(name = iconName)
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
