package dev.subfly.yaba.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.subfly.yaba.core.components.NoContentView
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.no_selected_collection_message
import yaba.composeapp.generated.resources.no_selected_collection_title

@Composable
fun EmptyDetailView() {
    Scaffold { paddings ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
            contentAlignment = Alignment.Center,
        ) {
            NoContentView(
                iconName = "dashboard-square-02",
                labelRes = Res.string.no_selected_collection_title,
                message = { Text(text = stringResource(Res.string.no_selected_collection_message)) }
            )
        }
    }
}
