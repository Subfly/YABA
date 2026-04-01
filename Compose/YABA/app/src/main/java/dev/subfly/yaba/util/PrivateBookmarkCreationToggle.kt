package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.subfly.yaba.core.navigation.creation.BookmarkPasswordCreateRoute
import dev.subfly.yaba.core.navigation.creation.BookmarkPasswordEntryRoute
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.preferences.UserPreferences
import dev.subfly.yaba.core.security.PrivateBookmarkSessionGuard

/**
 * Toggles private intent for **new** bookmarks (no [BookmarkUiModel] yet): create-password if
 * missing, password entry when the session is locked, then invokes [onToggleAllowed].
 */
@Composable
fun rememberPrivateBookmarkCreationToggle(
        onToggleAllowed: () -> Unit,
): () -> Unit {
    val prefs by
            SettingsStores.userPreferences.preferencesFlow.collectAsStateWithLifecycle(
                    initialValue = UserPreferences(),
            )
    val unlocked by PrivateBookmarkSessionGuard.isUnlocked.collectAsStateWithLifecycle()
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    return remember(prefs.privateBookmarkPasswordHash, unlocked) {
        {
            if (prefs.privateBookmarkPasswordHash.isBlank()) {
                creationNavigator.add(BookmarkPasswordCreateRoute())
                appStateManager.onShowCreationContent()
                return@remember
            }
            if (!unlocked) {
                creationNavigator.add(
                        BookmarkPasswordEntryRoute(
                                bookmarkId = null,
                                reason = PrivateBookmarkPasswordReason.UNLOCK_SESSION,
                        ),
                )
                appStateManager.onShowCreationContent()
                return@remember
            }
            onToggleAllowed()
        }
    }
}
