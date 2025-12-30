package dev.subfly.yaba.ui.creation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.subfly.yaba.core.components.AnimatedBottomSheet
import dev.subfly.yaba.core.navigation.ColorSelectionRoute
import dev.subfly.yaba.core.navigation.EmptyRoute
import dev.subfly.yaba.core.navigation.TagCreationRoute
import dev.subfly.yaba.core.navigation.creationNavigationConfig
import dev.subfly.yaba.ui.selection.ColorSelectionContent
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun CreationSheet(
    modifier: Modifier = Modifier,
    shouldShow: Boolean,
    flowStartRoute: NavKey?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val stack = rememberNavBackStack(
        configuration = creationNavigationConfig,
        flowStartRoute ?: EmptyRoute,
    )

    LaunchedEffect(flowStartRoute) {
        flowStartRoute?.let { route ->
            if (stack.isNotEmpty()) {
                stack.clear()
                stack.add(EmptyRoute)
                stack.add(route)
            }
        }
    }

    AnimatedBottomSheet(
        modifier = modifier,
        isVisible = shouldShow,
        sheetState = sheetState,
        showDragHandle = true,
        onDismissRequest = onDismiss,
    ) {
        NavDisplay(
            backStack = stack,
            onBack = {
                if (stack.size <= 2) {
                    onDismiss()
                }
                stack.removeLastOrNull()
            },
            entryProvider = entryProvider {
                entry<TagCreationRoute> { key ->
                    TagCreationContent(
                        tagId = key.tagId,
                        isStartingFlow = stack.size <= 2,
                        onOpenIconSelection = { },
                        onOpenColorSelection = { currentSelectedColor ->
                            stack.add(ColorSelectionRoute(currentSelectedColor))
                        },
                        onDismiss = {
                            if (stack.size <= 2) {
                                onDismiss()
                            }
                            stack.removeLastOrNull()
                        }
                    )
                }
                entry<ColorSelectionRoute> { key ->
                    ColorSelectionContent(
                        currentSelectedColor = key.color,
                        onDismiss = stack::removeLastOrNull
                    )
                }
                entry<EmptyRoute> {
                    // Only old Compose users will remember why we had to put 1.dp boxes in sheets...
                    Box(modifier = Modifier.size(1.dp))
                }
            }
        )
    }
}
