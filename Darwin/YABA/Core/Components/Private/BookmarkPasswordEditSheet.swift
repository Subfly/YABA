//
//  BookmarkPasswordEditSheet.swift
//  YABA
//
//  Compose `BookmarkPasswordEditSheetContent` parity (optional entry from settings when wired).
//

import SwiftUI

struct BookmarkPasswordEditSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    @State
    private var oldPin = ""

    @State
    private var newPin = ""

    @State
    private var confirmPin = ""

    @State
    private var mismatch = false

    private var canDone: Bool {
        oldPin.count == 6 && newPin.count == 6 && confirmPin.count == 6
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    SixDigitPinInput(pin: $oldPin, forceAllRed: mismatch)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                } header: {
                    PrivateBookmarkSheetSectionHeader(
                        titleKey: "Private Bookmark Old Password Field",
                        icon: "security-password"
                    )
                }

                Section {
                    SixDigitPinInput(pin: $newPin, forceAllRed: mismatch)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                } header: {
                    PrivateBookmarkSheetSectionHeader(
                        titleKey: "Private Bookmark New Password Field",
                        icon: "security-password"
                    )
                }

                Section {
                    SixDigitPinInput(pin: $confirmPin, forceAllRed: mismatch)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                } header: {
                    PrivateBookmarkSheetSectionHeader(
                        titleKey: "Private Bookmark Confirm New Password Field",
                        icon: "password-validation"
                    )
                }
            }
            .listStyle(.sidebar)
            .navigationTitle(Text("Private Bookmark Edit Password Title"))
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
                        done()
                    } label: {
                        Text("Done")
                    }
                    .disabled(!canDone)
                }
            }
            .onChange(of: oldPin) { _, _ in mismatch = false }
            .onChange(of: newPin) { _, _ in mismatch = false }
            .onChange(of: confirmPin) { _, _ in mismatch = false }
        }
        .presentationDetents([.fraction(0.4)])
        .presentationDragIndicator(.visible)
    }

    private func done() {
        let stored = PrivateBookmarkPasswordStore.storedPin()
        guard PrivateBookmarkPasswordVerifier.verify(pinDigits: oldPin, stored: stored) else {
            mismatch = true
            return
        }
        if newPin != confirmPin {
            mismatch = true
            return
        }
        PrivateBookmarkPasswordStore.setStoredPin(newPin)
        CoreToastManager.shared.show(
            message: LocalizedStringKey("Private Bookmark Password Updated Toast"),
            iconType: .success
        )
        dismiss()
    }
}
