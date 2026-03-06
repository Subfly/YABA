package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.ui.detail.bookmark.link.models.DetailPage
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.done

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkDetailActionsContent(
    modifier: Modifier = Modifier,
    currentPage: DetailPage,
    onPageChange: (DetailPage) -> Unit,
    mainColor: YabaColor,
    onHide: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TextButton(
            modifier = Modifier.align(Alignment.End),
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.textButtonColors().copy(
                contentColor = Color(mainColor.iconTintArgb())
            ),
            onClick = onHide,
        ) { Text(stringResource(Res.string.done)) }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DetailPage.entries.fastForEachIndexed { i, page ->
                SegmentedButton(
                    selected = currentPage == page,
                    onClick = { onPageChange(page) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                    label = { Text(text = page.label) },
                    icon = { YabaIcon(name = page.iconName) }
                )
            }
        }
    }
}
