package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.AnimatedBottomSheet
import dev.subfly.yaba.util.LocalPaneInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarkContentDetailLayout(
    modifier: Modifier = Modifier,
    contentLayout: @Composable (
        onExpand: () -> Unit,
    ) -> Unit,
    detailLayout: @Composable (
        onHide: () -> Unit,
    ) -> Unit,
) {
    val paneInfo = LocalPaneInfo.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isBottomSheetVisible by remember { mutableStateOf(false) }

    if (paneInfo.isTwoPaneLayout.not()
        || paneInfo.isTwoPaneLayout && paneInfo.isDetailPaneLargerThanList.not()
    ) {
        BottomSheetLayout(
            modifier = Modifier,
            sheetVisible = isBottomSheetVisible,
            sheetState = sheetState,
            onChangeSheetVisibility = { isBottomSheetVisible = it },
            contentLayout = contentLayout,
            detailLayout = detailLayout,
        )
    } else {
        DrawerLayout(
            modifier = modifier,
            drawerState = drawerState,
            contentLayout = contentLayout,
            detailLayout = detailLayout,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomSheetLayout(
    modifier: Modifier,
    sheetVisible: Boolean,
    sheetState: SheetState,
    onChangeSheetVisibility: (Boolean) -> Unit,
    contentLayout: @Composable (
        onExpand: () -> Unit,
    ) -> Unit,
    detailLayout: @Composable (
        onHide: () -> Unit,
    ) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        contentLayout { onChangeSheetVisibility(true) }

        AnimatedBottomSheet(
            isVisible = sheetVisible,
            sheetState = sheetState,
            onDismissRequest = { onChangeSheetVisibility(false) },
            content = { detailLayout { onChangeSheetVisibility(false) } }
        )
    }
}

@Composable
private fun DrawerLayout(
    modifier: Modifier,
    drawerState: DrawerState,
    contentLayout: @Composable (
        onExpand: () -> Unit,
    ) -> Unit,
    detailLayout: @Composable (
        onHide: () -> Unit,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(500.dp)
            ) {
                detailLayout { scope.launch { drawerState.close() } }
            }
        },
        content = { contentLayout { scope.launch { drawerState.open() } } }
    )
}
