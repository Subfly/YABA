package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yaba.util.LocalPaneInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarkContentDetailLayout(
    modifier: Modifier = Modifier,
    contentLayout: @Composable () -> Unit,
    detailLayout: @Composable () -> Unit,
) {
    val paneInfo = LocalPaneInfo.current

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    if (paneInfo.isTwoPaneLayout.not()
        || paneInfo.isTwoPaneLayout && paneInfo.isDetailPaneLargerThanList.not()) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetLayout(
    modifier: Modifier,
    scaffoldState: BottomSheetScaffoldState,
    contentLayout: @Composable () -> Unit,
    detailLayout: @Composable () -> Unit,
) {
    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetContent = {
            detailLayout()
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
    detailLayout: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            detailLayout()
        },
        content = {
            contentLayout()
        }
    )
}
