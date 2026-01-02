package dev.subfly.yaba.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
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
import dev.subfly.yaba.ui.creation.tag.TagCreationContent
import dev.subfly.yaba.ui.selection.ColorSelectionContent
import dev.subfly.yaba.ui.selection.IconCategorySelectionContent
import dev.subfly.yaba.ui.selection.IconSelectionContent
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
            modifier = Modifier.animateContentSize(),
            backStack = stack,
            transitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
            popTransitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
            predictivePopTransitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
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
                        onOpenIconSelection = { currentSelectedIcon ->
                            stack.add(IconCategorySelectionRoute(currentSelectedIcon))
                        },
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
                        onDismiss = stack::removeLastOrNull,
                    )
                }
                entry<IconCategorySelectionRoute> { key ->
                    IconCategorySelectionContent(
                        currentSelectedIcon = key.selectedIcon,
                        onSelectedSubcategory = { icon, category ->
                            stack.add(IconSelectionRoute(icon, category))
                        },
                        onDismiss = stack::removeLastOrNull,
                    )
                }
                entry<IconSelectionRoute> { key ->
                    IconSelectionContent(
                        currentSelectedIcon = key.selectedIcon,
                        selectedSubcategory = key.selectedSubcategory,
                        onDismiss = {
                            // Remove both the icon selection and icon subcategory selection
                            stack.removeLastOrNull()
                            stack.removeLastOrNull()
                        }
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
