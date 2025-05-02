//
//  ToastState.swift
//  YABA
//
//  Created by Ali Taha on 1.05.2025.
//

import SwiftUI

struct ToastState {
    var message: LocalizedStringKey = ""
    var contentColor: Color? = nil
    var accentColor: Color? = nil
    var acceptText: LocalizedStringKey? = nil
    var iconType: ToastIconType = .none
    var duration: ToastDuration = .short
    var position: ToastPosition = .bottom
    var onAcceptPressed: (() -> Void)? = nil
}
