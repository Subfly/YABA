package dev.subfly.yaba.ui.creation.notemark.table

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.NotemarkTableSheetResult
import dev.subfly.yaba.util.ResultStoreKeys

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotemarkTableCreationContent() {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(3) }

    fun dismiss() {
        if (creationNavigator.size == 2) {
            appStateManager.onHideCreationContent()
        }
        creationNavigator.removeLastOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        NotemarkInsertionSheetTopBar(
            canPerformDone = rows >= 1 && cols >= 1,
            onDone = {
                resultStore.setResult(
                    ResultStoreKeys.NOTEMARK_TABLE_INSERT,
                    NotemarkTableSheetResult(rows = rows, cols = cols, withHeaderRow = false),
                )
                dismiss()
            },
            onDismiss = { dismiss() },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            CounterRow(
                label = "Rows", // TODO: localize
                value = rows,
                onChange = { rows = it },
            )
            CounterRow(
                label = "Columns", // TODO: localize
                value = cols,
                onChange = { cols = it },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CounterRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = { if (value > 1) onChange(value - 1) },
                enabled = value > 1,
            ) { Text(text = "−") }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            FilledTonalIconButton(
                onClick = { if (value < 20) onChange(value + 1) },
                enabled = value < 20,
            ) { Text(text = "+") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotemarkInsertionSheetTopBar(
    canPerformDone: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults
            .topAppBarColors()
            .copy(containerColor = Color.Transparent),
        title = { Text(text = "Add Table") }, // TODO: localize
        navigationIcon = {
            TextButton(
                onClick = onDismiss,
                colors =
                    ButtonDefaults.textButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(text = stringResource(R.string.cancel)) }
        },
        actions = {
            TextButton(
                enabled = canPerformDone,
                onClick = onDone,
            ) { Text(text = stringResource(R.string.done)) }
        },
    )
}
