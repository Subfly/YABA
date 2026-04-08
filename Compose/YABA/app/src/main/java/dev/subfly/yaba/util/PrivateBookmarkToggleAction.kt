package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.navigation.creation.BookmarkPasswordCreateRoute
import dev.subfly.yaba.core.navigation.creation.BookmarkPasswordEntryRoute
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.preferences.UserPreferences
import dev.subfly.yaba.core.security.PrivateBookmarkSessionGuard

/**
 * Starts the private / not-private flow: create password if missing, password entry if locked, or
 * toggles immediately when the session is already unlocked. If [model] is null, the returned
 * callback is a no-op.
 */
@Composable
fun rememberPrivateBookmarkToggleAction(
    model: BookmarkUiModel?,
): () -> Unit {
    val prefs by
        SettingsStores.userPreferences.preferencesFlow.collectAsStateWithLifecycle(
            initialValue = UserPreferences(),
        )
    val unlocked by PrivateBookmarkSessionGuard.isUnlocked.collectAsStateWithLifecycle()
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    return remember(model?.id, model?.isPrivate, prefs.privateBookmarkPasswordHash, unlocked) {
        action@{
            val m = model ?: return@action
            if (!m.isPrivate && prefs.privateBookmarkPasswordHash.isBlank()) {
                creationNavigator.add(BookmarkPasswordCreateRoute())
                appStateManager.onShowCreationContent()
                return@action
            }
            if (!unlocked) {
                creationNavigator.add(
                    BookmarkPasswordEntryRoute(
                        bookmarkId = m.id,
                        reason =
                            if (m.isPrivate) {
                                PrivateBookmarkPasswordReason.TOGGLE_PRIVATE_OFF
                            } else {
                                PrivateBookmarkPasswordReason.TOGGLE_PRIVATE_ON
                            },
                    ),
                )
                appStateManager.onShowCreationContent()
                return@action
            }
            AllBookmarksManager.toggleBookmarkPrivate(m.id)
        }
    }
}
