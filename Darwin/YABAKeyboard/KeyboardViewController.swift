//
//  KeyboardViewController.swift
//  YABAKeyboard
//
//  Created by Ali Taha on 20.08.2025.
//

import UIKit
import SwiftUI

internal class KeyboardLayoutState: ObservableObject {
    @Published var isFloating: Bool = false
}

class KeyboardViewController: UIInputViewController {
    private let layoutState = KeyboardLayoutState()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let yabaKeyboardViewController = UIHostingController(
            rootView: YABAKeyboardView(
                needsInputModeSwitchKey: self.needsInputModeSwitchKey,
                nextKeyboardAction: #selector(self.handleInputModeList(from:with:)),
                onClickBookmark: { [weak self] text in
                    guard let self else { return }
                    let previousInput = self.textDocumentProxy.documentContextBeforeInput
                    self.textDocumentProxy.setMarkedText(
                        text,
                        selectedRange: .init(
                            location: previousInput?.count ?? 0,
                            length: text.count
                        )
                    )
                },
                onAccept: { [weak self] in
                    guard let self else { return }
                    self.textDocumentProxy.insertText("\n")
                },
                onDelete: { [weak self] in
                    guard let self,
                          self.textDocumentProxy.hasText else { return }
                    self.textDocumentProxy.deleteBackward()
                }
            )
            .environmentObject(layoutState)
            .modelContext(YabaModelContainer.getContext())
        )
        
        let yabaKeyboardView = yabaKeyboardViewController.view
        yabaKeyboardView?.translatesAutoresizingMaskIntoConstraints = false
        yabaKeyboardView?.backgroundColor = .clear
        
        self.addChild(yabaKeyboardViewController)
        if let yabaKeyboardView {
            self.view.addSubview(yabaKeyboardView)
        }
        yabaKeyboardViewController.didMove(toParent: self)
        
        if let yabaKeyboardView {
            NSLayoutConstraint.activate([
                yabaKeyboardView.leftAnchor.constraint(equalTo: view.leftAnchor),
                yabaKeyboardView.topAnchor.constraint(equalTo: view.topAnchor),
                yabaKeyboardView.rightAnchor.constraint(equalTo: view.rightAnchor),
                yabaKeyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
            ])
        }
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        let screenWidth = UIScreen.main.bounds.width
        let isFloating = self.view.frame.width < screenWidth
        
        if layoutState.isFloating != isFloating {
            layoutState.isFloating = isFloating
        }
    }
}
