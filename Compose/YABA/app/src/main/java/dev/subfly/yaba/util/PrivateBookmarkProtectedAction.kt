package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.navigation.creation.BookmarkPasswordEntryRoute
import dev.subfly.yaba.core.security.PrivateBookmarkSessionGuard

/**
 * When the session is locked and the bookmark is private, opens the password entry sheet; otherwise
 * runs [onAllowed]. If [model] is null, the returned callback is a no-op.
 */
@Composable
fun rememberPrivateBookmarkProtectedAction(
    model: BookmarkUiModel?,
    reason: PrivateBookmarkPasswordReason,
    onAllowed: () -> Unit,
): () -> Unit {
    val unlocked by PrivateBookmarkSessionGuard.isUnlocked.collectAsStateWithLifecycle()
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    return remember(model?.id, model?.isPrivate, unlocked, reason) {
        action@{
            val m = model ?: return@action
            if (!m.isPrivate || unlocked) {
                onAllowed()
            } else {
                creationNavigator.add(
                    BookmarkPasswordEntryRoute(
                        bookmarkId = m.id,
                        reason = reason,
                    ),
                )
                appStateManager.onShowCreationContent()
            }
        }
    }
}
