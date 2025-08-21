//
//  YABAKeyboardView.swift
//  YABAKeyboard
//
//  Created by Ali Taha on 20.08.2025.
//

import SwiftUI
import SwiftData

internal struct YABAKeyboardView: View {
    var needsInputModeSwitchKey: Bool = false
    var nextKeyboardAction: Selector? = nil
    
    let onClickBookmark: (String) -> Void
    let onAccept: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
        navigationSwitcher
            .frame(height: 500)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal)
            .padding(.top, 8)
    }
    
    @ViewBuilder
    private var navigationSwitcher: some View {
        if UIDevice.current.userInterfaceIdiom == .pad {
            KeyboardPadNavigationView(
                needsInputModeSwitchKey: needsInputModeSwitchKey,
                nextKeyboardAction: nextKeyboardAction,
                onClickBookmark: onClickBookmark,
                onAccept: onAccept,
                onDelete: onDelete
            )
        } else {
            KeyboardMobileNavigationView(
                onClickBookmark: onClickBookmark,
                onAccept: onAccept,
                onDelete: onDelete
            )
        }
    }
}

#Preview {
    YABAKeyboardView(
        onClickBookmark: { _ in },
        onAccept: {},
        onDelete: {}
    )
}
