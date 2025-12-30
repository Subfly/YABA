package dev.subfly.yaba.ui.selection

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import dev.subfly.yabacore.icons.IconCatalog

@Composable
fun IconSelectionContent() {

}

@Composable
private fun SelectionContent(
    selectedIcon: String,
    onSelectIcon: (String) -> Unit,
){
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
    ) {
        IconCatalog.categories().forEach { category ->

        }
    }
}
