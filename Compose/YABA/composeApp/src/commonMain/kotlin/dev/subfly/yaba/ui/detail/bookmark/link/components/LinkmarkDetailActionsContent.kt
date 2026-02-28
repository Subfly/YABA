package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.done

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkDetailActionsContent(
    modifier: Modifier = Modifier,
    mainColor: YabaColor,
    onHide: () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        TextButton(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.textButtonColors().copy(
                contentColor = Color(mainColor.iconTintArgb())
            ),
            onClick = onHide,
        ) { Text(stringResource(Res.string.done)) }
    }
}
