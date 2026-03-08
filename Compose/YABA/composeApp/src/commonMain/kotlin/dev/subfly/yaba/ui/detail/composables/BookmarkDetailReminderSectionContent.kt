package dev.subfly.yaba.ui.detail.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_remind_me_title
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun BookmarkDetailReminderSectionContent(
    modifier: Modifier = Modifier,
    reminderDateEpochMillis: Long,
    mainColor: YabaColor,
    onCancelReminder: () -> Unit,
) {
    val formattedDate = remember(reminderDateEpochMillis) {
        formatDateTime(Instant.fromEpochMilliseconds(reminderDateEpochMillis))
    }

    val cancelReminderSwipeAction = remember(onCancelReminder) {
        listOf(
            SwipeAction(
                key = "cancel_reminder",
                onClick = onCancelReminder,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(YabaColor.RED.iconTintArgb()),
                ) {
                    YabaIcon(
                        modifier = Modifier.padding(12.dp),
                        name = "delete-02",
                        color = Color.White,
                    )
                }
            },
        )
    }

    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BookmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp),
            iconName = "notification-01",
            label = stringResource(Res.string.bookmark_detail_remind_me_title),
        )

        YabaSwipeActions(
            actionWidth = 54.dp,
            rightActions = cancelReminderSwipeAction,
        ) {
            SegmentedListItem(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                onClick = {},
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                content = { Text(formattedDate) },
                leadingContent = { YabaIcon(name = "clock-01", color = mainColor) },
            )
        }
    }
}
