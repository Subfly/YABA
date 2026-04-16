//
//  BookmarkPasswordEntrySheet.swift
//  YABA
//
//  Compose `BookmarkPasswordEntrySheetContent` parity.
//

import SwiftUI

struct BookmarkPasswordEntrySheet: View {
    @Environment(\.dismiss)
    private var dismiss

    let bookmarkId: String?
    let reason: PrivateBookmarkPasswordReason

    /// When set, invoked instead of the global event bus for non-toggle reasons (e.g. single bookmark row).
    private let onNonToggleSuccess: ((PrivateBookmarkPasswordReason) -> Void)?

    @State
    private var pin = ""

    @State
    private var mismatch = false

    init(
        bookmarkId: String?,
        reason: PrivateBookmarkPasswordReason,
        onNonToggleSuccess: ((PrivateBookmarkPasswordReason) -> Void)? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.reason = reason
        self.onNonToggleSuccess = onNonToggleSuccess
    }

    private var canDone: Bool { pin.count == 6 }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    SixDigitPinInput(pin: $pin, forceAllRed: mismatch)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                } header: {
                    PrivateBookmarkSheetSectionHeader(
                        titleKey: "Private Bookmark Enter Password Section Header",
                        icon: "security-password"
                    )
                }
            }
            .listStyle(.sidebar)
            .navigationTitle(Text("Private Bookmark Enter Password Title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel) {
                        dismiss()
                    } label: {
                        Text("Cancel")
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        verifyAndFinish()
                    } label: {
                        Text("Done")
                    }
                    .disabled(!canDone)
                }
            }
            .onChange(of: pin) { _, _ in mismatch = false }
        }
        .presentationDetents([.fraction(0.2)])
        .presentationDragIndicator(.visible)
    }

    private func verifyAndFinish() {
        let stored = PrivateBookmarkPasswordStore.storedPin()
        guard PrivateBookmarkPasswordVerifier.verify(pinDigits: pin, stored: stored) else {
            mismatch = true
            return
        }
        PrivateBookmarkSessionGuard.shared.unlock()
        switch reason {
        case .togglePrivateOn, .togglePrivateOff:
            if let bookmarkId {
                AllBookmarksManager.queueToggleBookmarkPrivate(bookmarkId: bookmarkId)
            }
        default:
            if let onNonToggleSuccess {
                onNonToggleSuccess(reason)
            } else {
                PrivateBookmarkPasswordEventBus.emit(
                    PrivateBookmarkPasswordEntryResult(bookmarkId: bookmarkId, reason: reason)
                )
            }
        }
        dismiss()
    }
}
