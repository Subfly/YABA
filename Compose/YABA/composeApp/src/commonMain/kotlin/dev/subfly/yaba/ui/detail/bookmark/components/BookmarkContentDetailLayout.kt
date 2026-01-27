package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.LocalPaneInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarkContentDetailLayout(
    modifier: Modifier = Modifier,
    contentLayout: @Composable () -> Unit,
    detailLayout: @Composable (
        isExpanded: Boolean,
        onExpand: () -> Unit,
        onHide: () -> Unit,
    ) -> Unit,
) {
    val paneInfo = LocalPaneInfo.current

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    if (paneInfo.isTwoPaneLayout.not()
        || paneInfo.isTwoPaneLayout && paneInfo.isDetailPaneLargerThanList.not()
    ) {
        BottomSheetLayout(
            modifier = Modifier,
            scaffoldState = bottomSheetScaffoldState,
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
    scaffoldState: BottomSheetScaffoldState,
    contentLayout: @Composable () -> Unit,
    detailLayout: @Composable (
        isExpanded: Boolean,
        onExpand: () -> Unit,
        onHide: () -> Unit,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(scaffoldState.bottomSheetState.targetValue) {
        println("LELE current: ${scaffoldState.bottomSheetState.currentValue}")
        println("LELE target: ${scaffoldState.bottomSheetState.targetValue}")
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = false,
        sheetDragHandle = null,
        sheetPeekHeight = 82.dp,
        sheetContent = {
            detailLayout(
                scaffoldState.bottomSheetState.targetValue != SheetValue.PartiallyExpanded
                        || scaffoldState.bottomSheetState.currentValue != SheetValue.PartiallyExpanded,
                { scope.launch { scaffoldState.bottomSheetState.expand() } },
                { scope.launch { scaffoldState.bottomSheetState.partialExpand() } }
            )
        },
        content = {
            contentLayout()
        }
    )
}

@Composable
private fun DrawerLayout(
    modifier: Modifier,
    drawerState: DrawerState,
    contentLayout: @Composable () -> Unit,
    detailLayout: @Composable (
        isExpanded: Boolean,
        onExpand: () -> Unit,
        onHide: () -> Unit,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            detailLayout(
                drawerState.currentValue == DrawerValue.Open,
                { scope.launch { drawerState.open() } },
                { scope.launch { drawerState.close() } },
            )
        },
        content = {
            contentLayout()
        }
    )
}
