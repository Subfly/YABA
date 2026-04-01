package dev.subfly.yaba.ui.creation.bookmark

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.navigation.creation.BookmarkPasswordEntryRoute
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.security.PrivateBookmarkPasswordEventBus
import dev.subfly.yaba.core.security.PrivateBookmarkPasswordVerifier
import dev.subfly.yaba.core.security.PrivateBookmarkSessionGuard
import dev.subfly.yaba.core.toast.ToastIconType
import dev.subfly.yaba.core.toast.ToastManager
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.PrivateBookmarkPasswordEntryResult
import dev.subfly.yaba.util.PrivateBookmarkPasswordReason
import kotlinx.coroutines.launch

private val PinBoxWidth = 44.dp
private const val PIN_LEN = 6

@Composable
private fun PasswordFieldLabel(
        iconName: String,
        text: String,
        modifier: Modifier = Modifier,
) {
    Row(
            modifier = modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        YabaIcon(
                name = iconName,
                color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SixDigitPinRow(
        digits: List<String>,
        onDigitsChange: (List<String>) -> Unit,
        forceAllRed: Boolean,
        modifier: Modifier = Modifier,
) {
    val focusRequesters = remember { List(PIN_LEN) { FocusRequester() } }
    /** -1 = no programmatic focus; user must tap a box first (no autofocus on open). */
    var focusedIndex by remember { mutableIntStateOf(-1) }
    /** Which slot currently has keyboard focus (for masking unfocused filled slots). */
    var focusedSlotIndex by remember { mutableIntStateOf(-1) }

    val scheme = MaterialTheme.colorScheme
    val pinSlotMinContentHeight =
            with(LocalDensity.current) {
                val lh = MaterialTheme.typography.titleLarge.lineHeight
                if (lh == TextUnit.Unspecified) 32.dp else lh.toDp()
            }
    val slotBackgroundTarget = if (forceAllRed) scheme.errorContainer else scheme.surface
    val animatedSlotBg by
            animateColorAsState(
                    targetValue = slotBackgroundTarget,
                    animationSpec = tween(durationMillis = 220),
                    label = "pinRowSlotBg",
            )

    LaunchedEffect(focusedIndex) {
        if (focusedIndex in 0 until PIN_LEN) {
            focusRequesters[focusedIndex].requestFocus()
        }
    }

    fun digitAt(i: Int): String = digits.getOrNull(i).orEmpty()

    Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PIN_LEN) { index ->
            val digit = digitAt(index)
            val isSlotFocused = focusedSlotIndex == index
            val showMaskedDot = !isSlotFocused && digit.isNotEmpty()
            BasicTextField(
                    value = digit,
                    onValueChange = { new ->
                        if (new.length > 1 || new.any { !it.isDigit() }) return@BasicTextField
                        val next = MutableList(PIN_LEN) { i -> digitAt(i) }
                        next[index] = new
                        onDigitsChange(next)
                        when {
                            new.isNotEmpty() && index < PIN_LEN - 1 -> focusedIndex = index + 1
                            new.isEmpty() && index > 0 -> focusedIndex = index - 1
                        }
                    },
                    modifier =
                            Modifier.width(PinBoxWidth)
                                    .focusRequester(focusRequesters[index])
                                    .onFocusChanged { state ->
                                        focusedSlotIndex =
                                                when {
                                                    state.isFocused -> index
                                                    focusedSlotIndex == index -> -1
                                                    else -> focusedSlotIndex
                                                }
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(animatedSlotBg)
                                    .padding(vertical = 12.dp),
                    textStyle =
                            MaterialTheme.typography.titleLarge.copy(
                                    textAlign = TextAlign.Center,
                                    color = scheme.onSurface,
                            ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .heightIn(min = pinSlotMinContentHeight),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showMaskedDot) {
                                Box(
                                        Modifier.size(10.dp)
                                                .background(scheme.onSurface, CircleShape),
                                )
                            } else {
                                inner()
                            }
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkPasswordCreateSheetContent() {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val scope = rememberCoroutineScope()
    val prefsStore = SettingsStores.userPreferences

    val digits1 = remember { mutableStateListOf("", "", "", "", "", "") }
    val digits2 = remember { mutableStateListOf("", "", "", "", "", "") }
    var mismatch by remember { mutableStateOf(false) }

    val canDone = digits1.all { it.isNotEmpty() } && digits2.all { it.isNotEmpty() }

    LaunchedEffect(digits1.joinToString(), digits2.joinToString()) { mismatch = false }

    fun dismiss() {
        if (creationNavigator.size == 2) appStateManager.onHideCreationContent()
        creationNavigator.removeLastOrNull()
    }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        CenterAlignedTopAppBar(
                colors =
                        TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                        ),
                title = { Text("Create Password") },
                navigationIcon = {
                    TextButton(
                            onClick = { dismiss() },
                            colors =
                                    ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                    ),
                    ) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                            enabled = canDone,
                            onClick = {
                                val a = digits1.joinToString("")
                                val b = digits2.joinToString("")
                                if (a != b) {
                                    mismatch = true
                                    return@TextButton
                                }
                                scope.launch {
                                    prefsStore.setPrivateBookmarkPasswordHash(a)
                                    ToastManager.show(
                                            message =
                                                    "Password created successfully, now you can make your bookmarks private",
                                            iconType = ToastIconType.SUCCESS,
                                    )
                                    dismiss()
                                }
                            },
                    ) { Text("Done") }
                },
        )
        PasswordFieldLabel(iconName = "security-password", text = "Password")
        Spacer(modifier = Modifier.height(8.dp))
        SixDigitPinRow(
                digits = digits1.toList(),
                onDigitsChange = {
                    digits1.clear()
                    digits1.addAll(it)
                },
                forceAllRed = mismatch,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PasswordFieldLabel(iconName = "password-validation", text = "Confirm password")
        Spacer(modifier = Modifier.height(8.dp))
        SixDigitPinRow(
                digits = digits2.toList(),
                onDigitsChange = {
                    digits2.clear()
                    digits2.addAll(it)
                },
                forceAllRed = mismatch,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkPasswordEntrySheetContent(route: BookmarkPasswordEntryRoute) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val scope = rememberCoroutineScope()
    val prefsStore = SettingsStores.userPreferences

    val digits = remember { mutableStateListOf("", "", "", "", "", "") }
    var mismatch by remember { mutableStateOf(false) }

    fun dismiss() {
        if (creationNavigator.size == 2) appStateManager.onHideCreationContent()
        creationNavigator.removeLastOrNull()
    }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Password") },
                navigationIcon = {
                    TextButton(
                            onClick = { dismiss() },
                            colors =
                                    ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                    ),
                    ) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                            enabled = digits.all { it.isNotEmpty() },
                            onClick = {
                                scope.launch {
                                    val pin = digits.joinToString("")
                                    val stored = prefsStore.get().privateBookmarkPasswordHash
                                    if (!PrivateBookmarkPasswordVerifier.verify(pin, stored)) {
                                        mismatch = true
                                        return@launch
                                    }
                                    PrivateBookmarkSessionGuard.unlock()
                                    when (route.reason) {
                                        PrivateBookmarkPasswordReason.TOGGLE_PRIVATE_ON,
                                        PrivateBookmarkPasswordReason.TOGGLE_PRIVATE_OFF, ->
                                                route.bookmarkId?.let { id ->
                                                    AllBookmarksManager.toggleBookmarkPrivate(id)
                                                }
                                        else -> {
                                            PrivateBookmarkPasswordEventBus.emit(
                                                    PrivateBookmarkPasswordEntryResult(
                                                            bookmarkId = route.bookmarkId,
                                                            reason = route.reason,
                                                    ),
                                            )
                                        }
                                    }
                                    dismiss()
                                }
                            },
                    ) { Text("Done") }
                },
        )
        SixDigitPinRow(
                digits = digits.toList(),
                onDigitsChange = { next ->
                    digits.clear()
                    digits.addAll(next)
                },
                forceAllRed = mismatch,
                modifier = Modifier.padding(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkPasswordEditSheetContent() {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val scope = rememberCoroutineScope()
    val prefsStore = SettingsStores.userPreferences

    val oldDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val newDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val confirmDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    var mismatch by remember { mutableStateOf(false) }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Edit Password") },
                navigationIcon = {
                    TextButton(
                            onClick = {
                                if (creationNavigator.size == 2)
                                        appStateManager.onHideCreationContent()
                                creationNavigator.removeLastOrNull()
                            },
                            colors =
                                    ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                    ),
                    ) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                            enabled =
                                    oldDigits.all { it.isNotEmpty() } &&
                                            newDigits.all { it.isNotEmpty() } &&
                                            confirmDigits.all { it.isNotEmpty() },
                            onClick = {
                                scope.launch {
                                    val stored = prefsStore.get().privateBookmarkPasswordHash
                                    if (!PrivateBookmarkPasswordVerifier.verify(
                                                    oldDigits.joinToString(""),
                                                    stored
                                            )
                                    ) {
                                        mismatch = true
                                        return@launch
                                    }
                                    val n = newDigits.joinToString("")
                                    val c = confirmDigits.joinToString("")
                                    if (n != c) {
                                        mismatch = true
                                        return@launch
                                    }
                                    prefsStore.setPrivateBookmarkPasswordHash(n)
                                    ToastManager.show(
                                            message = "Password updated",
                                            iconType = ToastIconType.SUCCESS,
                                    )
                                    if (creationNavigator.size == 2)
                                            appStateManager.onHideCreationContent()
                                    creationNavigator.removeLastOrNull()
                                }
                            },
                    ) { Text("Done") }
                },
        )
        PasswordFieldLabel(iconName = "security-password", text = "Old password")
        Spacer(modifier = Modifier.height(8.dp))
        SixDigitPinRow(
                digits = oldDigits.toList(),
                onDigitsChange = {
                    oldDigits.clear()
                    oldDigits.addAll(it)
                },
                forceAllRed = mismatch,
                modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        PasswordFieldLabel(iconName = "security-password", text = "New password")
        Spacer(modifier = Modifier.height(8.dp))
        SixDigitPinRow(
                digits = newDigits.toList(),
                onDigitsChange = {
                    newDigits.clear()
                    newDigits.addAll(it)
                },
                forceAllRed = mismatch,
                modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        PasswordFieldLabel(iconName = "password-validation", text = "Confirm new password")
        Spacer(modifier = Modifier.height(8.dp))
        SixDigitPinRow(
                digits = confirmDigits.toList(),
                onDigitsChange = {
                    confirmDigits.clear()
                    confirmDigits.addAll(it)
                },
                forceAllRed = mismatch,
                modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
