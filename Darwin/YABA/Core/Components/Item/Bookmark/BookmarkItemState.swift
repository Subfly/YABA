//
//  BookmarkItemState.swift
//  YABA
//

import SwiftUI

@MainActor
@Observable
internal class BookmarkItemState {
    var shouldShowEditSheet: Bool = false
    var shouldShowShareSheet: Bool = false
    var shouldShowDeleteAlert: Bool = false
    var shouldShowPinSheet: Bool = false
    var shouldShowPrivateSheet: Bool = false
    var shouldShowMoveSheet: Bool = false

    /// Private-bookmark PIN sheets (`PrivateBookmarkPinRoute`).
    var privatePinRoute: PrivateBookmarkPinRoute?

    var isHovered: Bool = false
    var isTargeted: Bool = false
}
