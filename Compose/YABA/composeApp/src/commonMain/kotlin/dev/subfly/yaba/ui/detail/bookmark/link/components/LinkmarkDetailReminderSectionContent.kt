package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_remind_me_title
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkDetailReminderSectionContent(
    modifier: Modifier = Modifier,
    reminderDateEpochMillis: Long,
    mainColor: YabaColor,
    onCancelReminder: () -> Unit,
) {
    val formattedDate = remember(reminderDateEpochMillis) {
        formatDateTime(Instant.fromEpochMilliseconds(reminderDateEpochMillis))
    }

    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LinkmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp),
            iconName = "notification-01",
            label = stringResource(Res.string.bookmark_detail_remind_me_title),
        )

        SegmentedListItem(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            content = { Text(formattedDate) },
            leadingContent = { YabaIcon(name = "clock-01", color = mainColor) },
            trailingContent = {
                IconButton(onClick = onCancelReminder) {
                    YabaIcon(name = "delete-02", color = YabaColor.RED)
                }
            },
        )
    }
}
