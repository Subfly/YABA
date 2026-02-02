package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.ui.image.YabaImage

@Composable
internal fun MarkdownImageBlock(block: PreviewBlockUiModel.Image) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        YabaImage(
            filePath = block.path,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 420.dp),
            contentDescription = block.alt,
        )
        block.caption?.let { cap ->
            Text(
                text = cap,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
