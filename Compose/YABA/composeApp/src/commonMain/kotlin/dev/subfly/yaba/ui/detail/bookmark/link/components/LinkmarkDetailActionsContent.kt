package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailPageSegmentedRow
import dev.subfly.yaba.ui.detail.bookmark.link.models.DetailPage
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.util.iconTintArgb
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
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TextButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 12.dp),
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.textButtonColors().copy(
                contentColor = Color(mainColor.iconTintArgb())
            ),
            onClick = onHide,
        ) { Text(stringResource(Res.string.done)) }

        BookmarkDetailPageSegmentedRow(
            pages = DetailPage.entries.toList(),
            currentPage = currentPage,
            onPageChange = onPageChange,
            label = { it.label },
            iconName = { it.iconName },
        )
    }
}
