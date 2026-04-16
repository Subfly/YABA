//
//  BookmarkPasswordCreateSheet.swift
//  YABA
//
//  Compose `BookmarkPasswordCreateSheetContent` parity.
//

import SwiftUI

struct BookmarkPasswordCreateSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    @State
    private var pass = ""

    @State
    private var confirm = ""

    @State
    private var mismatch = false

    private var canDone: Bool {
        pass.count == 6 && confirm.count == 6
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    SixDigitPinInput(pin: $pass, forceAllRed: mismatch)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                } header: {
                    PrivateBookmarkSheetSectionHeader(
                        titleKey: "Private Bookmark Password Label",
                        icon: "security-password"
                    )
                }

                Section {
                    SixDigitPinInput(pin: $confirm, forceAllRed: mismatch)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                } header: {
                    PrivateBookmarkSheetSectionHeader(
                        titleKey: "Private Bookmark Confirm Password Label",
                        icon: "password-validation"
                    )
                }
            }
            .listStyle(.sidebar)
            .navigationTitle(Text("Private Bookmark Create Password Title"))
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
            .onChange(of: pass) { _, _ in mismatch = false }
            .onChange(of: confirm) { _, _ in mismatch = false }
        }
        .presentationDetents([.fraction(0.4)])
        .presentationDragIndicator(.visible)
    }

    private func done() {
        if pass != confirm {
            mismatch = true
            return
        }
        PrivateBookmarkPasswordStore.setStoredPin(pass)
        CoreToastManager.shared.show(
            message: LocalizedStringKey("Private Bookmark Password Created Toast"),
            iconType: .success
        )
        dismiss()
    }
}
