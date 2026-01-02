package dev.subfly.yaba.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.subfly.yaba.core.components.AnimatedBottomSheet
import dev.subfly.yaba.ui.creation.tag.TagCreationContent
import dev.subfly.yaba.ui.selection.ColorSelectionContent
import dev.subfly.yaba.ui.selection.IconCategorySelectionContent
import dev.subfly.yaba.ui.selection.IconSelectionContent
import dev.subfly.yaba.util.LocalCreationContentNavigator
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun CreationSheet(
    modifier: Modifier = Modifier,
    shouldShow: Boolean,
    onDismiss: () -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val sheetState = rememberModalBottomSheetState()

    AnimatedBottomSheet(
        modifier = modifier,
        isVisible = shouldShow,
        sheetState = sheetState,
        showDragHandle = true,
        onDismissRequest = onDismiss,
    ) {
        NavDisplay(
            modifier = Modifier.animateContentSize(),
            backStack = creationNavigator,
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
                if (creationNavigator.size == 1) {
                    onDismiss()
                }
                creationNavigator.removeLastOrNull()
            },
            entryProvider = entryProvider {
                entry<TagCreationRoute> { key ->
                    TagCreationContent(
                        tagId = key.tagId,
                        onDismiss = {
                            if (creationNavigator.size == 1) {
                                onDismiss()
                            }
                            creationNavigator.removeLastOrNull()
                        }
                    )
                }
                entry<ColorSelectionRoute> { key ->
                    ColorSelectionContent(
                        currentSelectedColor = key.color,
                        onDismiss = creationNavigator::removeLastOrNull,
                    )
                }
                entry<IconCategorySelectionRoute> { key ->
                    IconCategorySelectionContent(
                        currentSelectedIcon = key.selectedIcon,
                        onSelectedSubcategory = { icon, category ->
                            creationNavigator.add(IconSelectionRoute(icon, category))
                        },
                        onDismiss = creationNavigator::removeLastOrNull,
                    )
                }
                entry<IconSelectionRoute> { key ->
                    IconSelectionContent(
                        currentSelectedIcon = key.selectedIcon,
                        selectedSubcategory = key.selectedSubcategory,
                        onDismiss = {
                            // Remove both the icon selection and icon subcategory selection
                            creationNavigator.removeLastOrNull()
                            creationNavigator.removeLastOrNull()
                        }
                    )
                }
            }
        )
    }
}
