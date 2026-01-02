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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.subfly.yaba.core.components.AnimatedBottomSheet
import dev.subfly.yaba.ui.creation.folder.FolderCreationContent
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

    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible.not()) {
            creationNavigator.removeIf { it !is EmptyRoute }
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
            backStack = creationNavigator,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
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
                // Means next pop up destination is Empty Route,
                // so dismiss first, then remove the last item
                if (creationNavigator.size == 2) {
                    onDismiss()
                }
                creationNavigator.removeLastOrNull()
            },
            entryProvider = entryProvider {
                entry<TagCreationRoute> { key ->
                    TagCreationContent(
                        tagId = key.tagId,
                        onDismiss = {
                            // Means next pop up destination is Empty Route,
                            // so dismiss first, then remove the last item
                            if (creationNavigator.size == 2) {
                                onDismiss()
                            }
                            creationNavigator.removeLastOrNull()
                        }
                    )
                }
                entry<FolderCreationRoute> { key ->
                    FolderCreationContent(
                        folderId = key.folderId,
                        onDismiss = {
                            // Means next pop up destination is Empty Route,
                            // so dismiss first, then remove the last item
                            if (creationNavigator.size == 2) {
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
                entry<EmptyRoute> {
                    // Only old Compose users will remember why we had to put 1.dp boxes in sheets...
                    Box(modifier = Modifier.size((0.1).dp))
                }
            }
        )
    }
}
