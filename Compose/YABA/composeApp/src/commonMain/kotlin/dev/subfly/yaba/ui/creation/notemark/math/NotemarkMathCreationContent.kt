package dev.subfly.yaba.ui.creation.notemark.math

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.creation.NotemarkMathSheetRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.NotemarkMathSheetResult
import dev.subfly.yaba.util.ResultStoreKeys
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.done

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotemarkMathCreationContent(route: NotemarkMathSheetRoute) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    var latexText by remember(route.routeId, route.initialLatex) {
        mutableStateOf(route.initialLatex)
    }

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
        NotemarkMathSheetTopBar(
            title = if (route.isEdit) {
                "Edit Math"
            } else {
                "Add Math"
            },
            canPerformDone = latexText.isNotBlank(),
            onDone = {
                resultStore.setResult(
                    ResultStoreKeys.NOTEMARK_MATH_INSERT,
                    NotemarkMathSheetResult(
                        isBlock = route.isBlock,
                        latex = latexText.trim(),
                        isEdit = route.isEdit,
                        editPos = route.editPos,
                    ),
                )
                dismiss()
            },
            onDismiss = { dismiss() },
        )

        OutlinedTextField(
            modifier = Modifier
                .heightIn(min = 120.dp, max = 240.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
            value = latexText,
            onValueChange = { latexText = it },
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text(text = "Equation here in LaTeX (rendered with KaTeX)") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotemarkMathSheetTopBar(
    title: String,
    canPerformDone: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults
            .topAppBarColors()
            .copy(containerColor = Color.Transparent),
        title = { Text(text = title) },
        navigationIcon = {
            TextButton(
                onClick = onDismiss,
                colors =
                    ButtonDefaults.textButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(text = stringResource(Res.string.cancel)) }
        },
        actions = {
            TextButton(
                enabled = canPerformDone,
                onClick = onDone,
            ) { Text(text = stringResource(Res.string.done)) }
        },
    )
}
