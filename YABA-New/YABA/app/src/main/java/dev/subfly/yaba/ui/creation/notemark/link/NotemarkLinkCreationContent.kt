package dev.subfly.yaba.ui.creation.notemark.link

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.navigation.creation.NotemarkLinkSheetRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.NotemarkInlineAction
import dev.subfly.yaba.util.NotemarkLinkSheetResult
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.core.model.utils.YabaColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotemarkLinkCreationContent(route: NotemarkLinkSheetRoute) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    var linkText by remember(route.routeId, route.initialText) {
        mutableStateOf(route.initialText)
    }
    var linkUrl by remember(route.routeId, route.initialUrl) {
        mutableStateOf(route.initialUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        NotemarkLinkTopBar(
            title = if (route.isEdit) {
                "Edit Link" // TODO: localize
            } else {
                "Add Link" // TODO: localize
            },
            canPerformDone = linkText.isNotBlank() && linkUrl.isNotBlank(),
            onDone = {
                resultStore.setResult(
                    ResultStoreKeys.NOTEMARK_LINK_INSERT,
                    NotemarkLinkSheetResult(
                        text = linkText.trim(),
                        url = linkUrl.trim(),
                        action = NotemarkInlineAction.INSERT_OR_UPDATE,
                        editPos = route.editPos,
                    ),
                )
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
            onDismiss = {
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
        )

        NotemarkLinkTitleField(
            text = linkText,
            fieldAccent = YabaColor.BLUE,
            fieldAccentColor = Color(YabaColor.BLUE.iconTintArgb()),
            onTextChange = { linkText = it },
        )

        NotemarkLinkUrlField(
            url = linkUrl,
            fieldAccent = YabaColor.BLUE,
            fieldAccentColor = Color(YabaColor.BLUE.iconTintArgb()),
            onUrlChange = { linkUrl = it },
        )

        if (route.isEdit && route.editPos != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    resultStore.setResult(
                        ResultStoreKeys.NOTEMARK_LINK_INSERT,
                        NotemarkLinkSheetResult(
                            text = route.initialText,
                            url = route.initialUrl,
                            action = NotemarkInlineAction.REMOVE,
                            editPos = route.editPos,
                        ),
                    )
                    if (creationNavigator.size == 2) {
                        appStateManager.onHideCreationContent()
                    }
                    creationNavigator.removeLastOrNull()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                YabaIcon(name = "delete-02", color = YabaColor.RED)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Remove Link", // TODO: localize
                    color = Color(YabaColor.RED.iconTintArgb()),
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotemarkLinkTitleField(
    text: String,
    fieldAccent: YabaColor,
    fieldAccentColor: Color,
    onTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = fieldAccentColor,
            unfocusedBorderColor = fieldAccentColor.copy(alpha = 0.5f),
        ),
        value = text,
        onValueChange = onTextChange,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(R.string.create_bookmark_title_placeholder))
        },
        leadingIcon = { YabaIcon(name = "text", color = fieldAccent) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotemarkLinkUrlField(
    url: String,
    fieldAccent: YabaColor,
    fieldAccentColor: Color,
    onUrlChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier
            .heightIn(min = 120.dp, max = 240.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = fieldAccentColor,
            unfocusedBorderColor = fieldAccentColor.copy(alpha = 0.5f),
        ),
        value = url,
        onValueChange = onUrlChange,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(R.string.create_bookmark_url_placeholder))
        },
        leadingIcon = { YabaIcon(name = "link-04", color = fieldAccent) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotemarkLinkTopBar(
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
                colors = ButtonDefaults.textButtonColors().copy(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
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
