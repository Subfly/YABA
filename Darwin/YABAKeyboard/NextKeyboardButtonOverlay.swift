//
//  NextKeyboardButtonOverlay.swift
//  YABA
//
//  Created by Ali Taha on 22.08.2025.
//
//  Taken from Itsuki: https://levelup.gitconnected.com/swiftui-create-systemwide-custom-keyboard-ef4c79ecb89a

import UIKit
import SwiftUI

struct NextKeyboardButtonOverlay: UIViewRepresentable {
    let action: Selector

    func makeUIView(context: Context) -> UIButton {
        let button = UIButton()
        button.addTarget(nil, action: action, for: .allTouchEvents)
        return button
    }
    func updateUIView(_ button: UIButton, context: Context) {}
}
