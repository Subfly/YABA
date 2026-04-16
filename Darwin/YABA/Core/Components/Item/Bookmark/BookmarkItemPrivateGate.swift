//
//  BookmarkItemPrivateGate.swift
//  YABA
//
//  Compose `PrivateBookmarkOpenClick`, `PrivateBookmarkProtectedAction`, `PrivateBookmarkToggleAction` parity.
//

import SwiftUI

@MainActor
enum BookmarkItemPrivateGate {
    /// `rememberPrivateBookmarkOpenClick`
    static func performOpen(
        bookmark: BookmarkModel,
        itemState: BookmarkItemState,
        onOpen: @escaping (BookmarkModel) -> Void
    ) {
        if !bookmark.isPrivate {
            onOpen(bookmark)
            return
        }
        if !PrivateBookmarkPasswordStore.hasPin {
            itemState.privatePinRoute = .create
            return
        }
        if PrivateBookmarkSessionGuard.shared.isLocked {
            itemState.privatePinRoute = .entry(
                bookmarkId: bookmark.bookmarkId,
                reason: .openBookmark
            )
            return
        }
        onOpen(bookmark)
    }

    /// `rememberPrivateBookmarkProtectedAction`
    static func performProtected(
        bookmark: BookmarkModel,
        reason: PrivateBookmarkPasswordReason,
        itemState: BookmarkItemState,
        onAllowed: @escaping () -> Void
    ) {
        if !bookmark.isPrivate || PrivateBookmarkSessionGuard.shared.isUnlocked {
            onAllowed()
            return
        }
        itemState.privatePinRoute = .entry(bookmarkId: bookmark.bookmarkId, reason: reason)
    }

    /// `rememberPrivateBookmarkToggleAction`
    static func performTogglePrivate(bookmark: BookmarkModel, itemState: BookmarkItemState) {
        if !bookmark.isPrivate, !PrivateBookmarkPasswordStore.hasPin {
            itemState.privatePinRoute = .create
            return
        }
        if PrivateBookmarkSessionGuard.shared.isLocked {
            let reason: PrivateBookmarkPasswordReason = bookmark.isPrivate ? .togglePrivateOff : .togglePrivateOn
            itemState.privatePinRoute = .entry(bookmarkId: bookmark.bookmarkId, reason: reason)
            return
        }
        AllBookmarksManager.queueToggleBookmarkPrivate(bookmarkId: bookmark.bookmarkId)
    }

    static func shareURL(for bookmark: BookmarkModel) -> URL? {
        if let urlString = bookmark.linkDetail?.url,
           let u = URL(string: urlString), !urlString.isEmpty
        {
            return u
        }
        return nil
    }
}
